package ro.upt.pontaje.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ro.upt.pontaje.dto.assistant.ChatRequest;
import ro.upt.pontaje.dto.assistant.ChatResponse;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.service.AssistantService;

import java.util.Map;

/**
 * Endpoint pentru asistentul AI (proxy către Groq).
 */
@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    /**
     * Indică dacă asistentul AI este activat (are cheie configurată).
     * GET /api/assistant/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Boolean>> config() {
        return ResponseEntity.ok(Map.of("enabled", assistantService.isConfigured()));
    }

    /**
     * Trimite conversația și primește răspunsul asistentului.
     * POST /api/assistant/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@AuthenticationPrincipal User user,
                                             @RequestBody ChatRequest request) {
        String reply = assistantService.chat(request.messages(), user);
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    /**
     * Răspuns în flux (streaming), token cu token, în format NDJSON.
     * POST /api/assistant/chat/stream
     */
    @PostMapping(value = "/chat/stream", produces = "application/x-ndjson")
    public ResponseEntity<StreamingResponseBody> chatStream(@AuthenticationPrincipal User user,
                                                            @RequestBody ChatRequest request) {
        StreamingResponseBody body = out -> assistantService.streamChat(request.messages(), user, out);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-ndjson"))
                .body(body);
    }
}
