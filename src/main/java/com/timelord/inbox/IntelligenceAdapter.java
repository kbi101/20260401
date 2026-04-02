package com.timelord.inbox;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Real implementation of IntelligencePort using Spring AI and Ollama/Qwen.
 */
@Component
class IntelligenceAdapter implements IntelligencePort {
    private static final java.util.concurrent.Semaphore OLLAMA_SEMAPHORE = new java.util.concurrent.Semaphore(1);

    private final ChatModel chatModel;

    public IntelligenceAdapter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public EmailSummary summarize(EmailPayload email) {
        try {
            OLLAMA_SEMAPHORE.acquire();
            return doSummarize(email);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI processing interrupted", e);
        } finally {
            OLLAMA_SEMAPHORE.release();
        }
    }

    private EmailSummary doSummarize(EmailPayload email) {
        System.out.println("Processing AI Summary for: " + email.subject());
        String template = """
            You are Timelord Inbox Intelligence.
            Summarize the following email and extract key action items.
            Format your response strictly as follows (no prose):
            SUMMARY: <one sentence summary>
            ACTIONS: <item 1>, <item 2>
            SENTIMENT: <POSITIVE|NEGATIVE|NEUTRAL>

            EMAIL SUBJECT: {subject}
            EMAIL FROM: {sender}
            EMAIL CONTENT:
            {content}
            """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        // Explicitly cast to Map<String, Object> to avoid overload confusion
        String content = email.bodyContent();
        if (content.length() > 4000) {
            content = content.substring(0, 4000) + "... (truncated)";
        }
        
        Map<String, Object> modelMap = Map.of(
            "subject", email.subject(),
            "sender", email.sender(),
            "content", content
        );
        Prompt prompt = promptTemplate.create(modelMap);

        String response = chatModel.call(prompt).getResult().getOutput().getText();

        // Primitive parsing
        String summaryText = parseSection(response, "SUMMARY:");
        String actionsText = parseSection(response, "ACTIONS:");
        String sentiment = parseSection(response, "SENTIMENT:");

        List<String> actions = Arrays.stream(actionsText.split(","))
                                  .map(String::strip)
                                  .filter(s -> !s.isEmpty())
                                  .toList();

        return new EmailSummary(
            UUID.randomUUID().toString(),
            email.gmailId(),
            email.sourceEmail(),
            summaryText,
            actions,
            sentiment,
            LocalDateTime.now()
        );
    }

    private String parseSection(String raw, String marker) {
        try {
            int start = raw.indexOf(marker);
            if (start == -1) return "N/A";
            start += marker.length();
            int end = raw.indexOf("\n", start);
            if (end == -1) end = raw.length();
            return raw.substring(start, end).trim();
        } catch (Exception e) {
            return "N/A";
        }
    }
}
