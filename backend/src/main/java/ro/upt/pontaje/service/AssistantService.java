package ro.upt.pontaje.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ro.upt.pontaje.dto.assistant.ChatMessage;
import ro.upt.pontaje.exception.BadRequestException;
import ro.upt.pontaje.model.User;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Asistent AI bazat pe Groq (API compatibil OpenAI). Cheia rămâne pe server.
 */
@Service
public class AssistantService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssistantService.class);

    private final RestClient restClient = RestClient.create();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.assistant.groq.api-key:}")
    private String apiKey;

    @Value("${app.assistant.groq.model:llama-3.3-70b-versatile}")
    private String model;

    @Value("${app.assistant.groq.base-url:https://api.groq.com/openai/v1}")
    private String baseUrl;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Trimite conversația către Groq și întoarce răspunsul asistentului.
     */
    public String chat(List<ChatMessage> history, User user) {
        if (!isConfigured()) {
            throw new BadRequestException(
                    "Asistentul AI nu este configurat. Setați GROQ_API_KEY în mediul aplicației.");
        }

        List<Map<String, String>> messages = buildMessages(history, user);

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.4,
                "max_tokens", 600
        );

        try {
            String resp = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode node = mapper.readTree(resp);
            String content = node.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BadRequestException("Asistentul nu a returnat niciun răspuns.");
            }
            return content.trim();
        } catch (RestClientResponseException e) {
            log.warn("Eroare Groq {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException(
                    "Serviciul AI a returnat o eroare (" + e.getStatusCode().value() + "). Verificați cheia GROQ_API_KEY.");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Asistent indisponibil: {}", e.getMessage());
            throw new BadRequestException("Asistentul nu a putut răspunde momentan. Încercați din nou.");
        }
    }

    /**
     * Streaming: scrie răspunsul în flux NDJSON ({"t":token} ... {"done":true}).
     */
    public void streamChat(List<ChatMessage> history, User user, OutputStream out) {
        try {
            if (!isConfigured()) {
                writeLine(out, Map.of("error",
                        "Asistentul AI nu este configurat. Setați GROQ_API_KEY în mediul aplicației."));
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("messages", buildMessages(history, user));
            payload.put("temperature", 0.4);
            payload.put("max_tokens", 600);
            payload.put("stream", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();

            HttpResponse<Stream<String>> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != 200) {
                writeLine(out, Map.of("error",
                        "Serviciul AI a returnat eroare (" + response.statusCode() + "). Verificați GROQ_API_KEY."));
                return;
            }

            try (Stream<String> lines = response.body()) {
                Iterator<String> it = lines.iterator();
                while (it.hasNext()) {
                    String line = it.next();
                    if (line == null || !line.startsWith("data:")) continue;
                    String data = line.substring(5).trim();
                    if (data.isEmpty()) continue;
                    if (data.equals("[DONE]")) break;
                    try {
                        JsonNode node = mapper.readTree(data);
                        String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            writeLine(out, Map.of("t", delta));
                        }
                    } catch (Exception ignore) {
                        // ignorăm liniile care nu sunt JSON valid (ex: keep-alive)
                    }
                }
            }
            writeLine(out, Map.of("done", true));
        } catch (Exception e) {
            log.warn("Stream asistent eșuat: {}", e.getMessage());
            try {
                writeLine(out, Map.of("error", "Asistentul nu a putut răspunde momentan. Încercați din nou."));
            } catch (IOException ignored) {
                // clientul a închis conexiunea
            }
        }
    }

    private void writeLine(OutputStream out, Map<String, Object> obj) throws IOException {
        out.write((mapper.writeValueAsString(obj) + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Construiește lista de mesaje (system prompt + ultimele ~12 mesaje din istoric).
     */
    private List<Map<String, String>> buildMessages(List<ChatMessage> history, User user) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt(user)));

        List<ChatMessage> recent = history == null ? List.of() : history;
        int from = Math.max(0, recent.size() - 12);
        for (int i = from; i < recent.size(); i++) {
            ChatMessage m = recent.get(i);
            if (m == null || m.content() == null || m.content().isBlank()) continue;
            String role = "assistant".equalsIgnoreCase(m.role()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", m.content()));
        }
        return messages;
    }

    private String systemPrompt(User user) {
        String name = user != null && user.getFullName() != null ? user.getFullName() : "utilizator";
        String role = user != null && user.getRole() != null ? user.getRole().name() : "necunoscut";
        String roleRo = switch (role) {
            case "CADRU_DIDACTIC" -> "cadru didactic";
            case "SECRETARIAT" -> "secretariat";
            case "ADMIN" -> "administrator";
            default -> role;
        };

        return """
                Ești asistentul virtual al aplicației „Pontaje UPT”, un sistem web de gestionare a \
                pontajelor pentru cadrele didactice și secretariatul Universității Politehnica Timișoara.
                Răspunzi SCURT (1-4 propoziții), clar și prietenos, în limba română, folosind diacritice.
                Ajuți utilizatorul să înțeleagă și să folosească aplicația. NU inventa funcții inexistente.

                Funcționalitățile aplicației:
                - Orar: cadrele didactice își adaugă activitățile săptămânale (curs, seminar, laborator) \
                cu disciplină, sală și interval orar; un interval ocupat în aceeași zi este blocat.
                - Pontaj: înregistrarea orelor lucrate pe zile (tip „în normă” sau „plată cu ora”), apoi \
                trimiterea pontajului lunar către secretariat pentru aprobare.
                - Documente: generarea și descărcarea Anexei 1 în PDF (evidența orelor lucrate).
                - Centralizator (doar secretariat/administrator): vizualizarea pontajelor trimise, \
                aprobarea lor, descărcarea raportului centralizat PDF și trimiterea raportului pe email.
                - Profil: actualizarea datelor personale și schimbarea parolei.

                Utilizatorul curent: %s, rol: %s.
                Dacă întrebarea nu ține de aplicație, răspunde politicos și readu discuția la subiectele aplicației.
                """.formatted(name, roleRo);
    }
}
