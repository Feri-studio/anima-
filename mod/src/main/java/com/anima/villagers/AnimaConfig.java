package com.anima.villagers;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Конфиг мода. Хранится в .minecraft/config/anima.properties
 * Ключ API НЕ в коде — только здесь, на ПК игрока.
 */
public class AnimaConfig {
    private static final Path CONFIG_PATH = Paths.get("config", "anima.properties");
    private static Properties props = new Properties();

    // Значения по умолчанию
    public static String LLM_PROVIDER = "groq";
    public static String LLM_API_KEY = "";
    public static String LLM_BASE_URL = "https://api.groq.com/openai/v1";
    public static String LLM_MODEL = "llama-3.3-70b-versatile";
    public static int LLM_TIMEOUT_SEC = 30;

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) {
                saveDefault();
            }
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
            }

            LLM_PROVIDER = props.getProperty("provider", LLM_PROVIDER);
            LLM_API_KEY = props.getProperty("api_key", "");
            LLM_BASE_URL = props.getProperty("base_url", LLM_BASE_URL);
            LLM_MODEL = props.getProperty("model", LLM_MODEL);
            LLM_TIMEOUT_SEC = Integer.parseInt(props.getProperty("timeout_sec", "30"));

            AnimaVillagers.LOGGER.info("Конфиг загружен: провайдер={}, модель={}",
                LLM_PROVIDER, LLM_MODEL);

            if (LLM_API_KEY.isEmpty()) {
                AnimaVillagers.LOGGER.warn("API-ключ не задан! Мод будет использовать заглушку.");
            }
        } catch (Exception e) {
            AnimaVillagers.LOGGER.error("Ошибка загрузки конфига: {}", e.getMessage());
        }
    }

    private static void saveDefault() throws IOException {
        props.setProperty("provider", "groq");
        props.setProperty("api_key", "ВСТАВЬ_СВОЙ_КЛЮЧ_СЮДА");
        props.setProperty("base_url", "https://api.groq.com/openai/v1");
        props.setProperty("model", "llama-3.3-70b-versatile");
        props.setProperty("timeout_sec", "30");

        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "Anima Villagers config. Вставь свой API-ключ в api_key");
        }
        AnimaVillagers.LOGGER.info("Создан конфиг по умолчанию: {}", CONFIG_PATH);
    }
}
