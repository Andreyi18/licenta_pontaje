package ro.upt.pontaje.dto.assistant;

/**
 * Un mesaj din conversația cu asistentul. role = "user" | "assistant".
 */
public record ChatMessage(String role, String content) {}
