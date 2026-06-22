package ro.upt.pontaje.dto.assistant;

import java.util.List;

/**
 * Cererea de chat: istoricul conversației (ultimele mesaje).
 */
public record ChatRequest(List<ChatMessage> messages) {}
