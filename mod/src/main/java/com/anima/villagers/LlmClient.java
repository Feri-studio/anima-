package com.anima.villagers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP-клиент для OpenAI-совместимых API (Groq, Gemini, OpenRouter, Cerebras).
 * Использует java.net.http.HttpClient — встроен в Java, не требует библиотек.
 */
public class LlmClient {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // Простой парсер JSON — ищем "content":"..."
    private static final Pattern CONTENT_PATTERN =
        Pattern.compile("\"content\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    /**
     * Отправляет промпт в LLM и возвращает ответ.
     * @param systemPrompt системный промпт (кто такой житель)
     * @param userPrompt   что просим
     * @return ответ или null при ошибке
     */
    public static String ask(String systemPrompt, String userPrompt) {
        if (AnimaConfig.LLM_API_KEY.isEmpty()
            || AnimaConfig.LLM_API_KEY.equals("ВСТАВЬ_СВОЙ_КЛЮЧ_СЮДА")) {
            AnimaVillagers.LOGGER.warn("LLM не настроен — возвращаю заглушку");
            return null;
        }

        try {
            String body = buildRequestBody(systemPrompt, userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AnimaConfig.LLM_BASE_URL + "/chat/completions"))
                .header("Authorization", "Bearer " + AnimaConfig.LLM_API_KEY)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(AnimaConfig.LLM_TIMEOUT_SEC))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = HTTP.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                AnimaVillagers.LOGGER.error("LLM ошибка {}: {}",
                    response.statusCode(), response.body());
                return null;
            }

            return extractContent(response.body());

        } catch (Exception e) {
            AnimaVillagers.LOGGER.error("LLM запрос упал: {}", e.getMessage());
            return null;
        }
    }

    private static String buildRequestBody(String system, String user) {
        // Экранируем кавычки и переносы строк
        String sys = escape(system);
        String usr = escape(user);

        return "{"
            + "\"model\":\"" + AnimaConfig.LLM_MODEL + "\","
            + "\"messages\":["
            + "{\"role\":\"system\",\"content\":\"" + sys + "\"},"
            + "{\"role\":\"user\",\"content\":\"" + usr + "\"}"
            + "],"
            + "\"temperature\":0.9,"
            + "\"max_tokens\":400"
            + "}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private static String extractContent(String json) {
        Matcher m = CONTENT_PATTERN.matcher(json);
        if (m.find()) {
            return m.group(1)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        }
        return null;
    }
}
