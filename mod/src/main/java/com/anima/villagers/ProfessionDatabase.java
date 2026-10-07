package com.anima.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * База профессий. 40 базовых в коде + остальные через LLM.
 * Кэшируется в config/anima_professions.json — LLM вызывается один раз.
 */
public class ProfessionDatabase {
    private static final Path CACHE = Paths.get("config", "anima_professions.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Random RNG = new Random();

    public static final String[] BASE = {
        "фермер", "кузнец", "шахтёр", "строитель", "торговец",
        "стражник", "жрец", "охотник", "рыбак", "учёный",
        "пекарь", "мясник", "кожевник", "ткач", "гончар",
        "плотник", "каменщик", "лесоруб", "пастух", "садовник",
        "травник", "лекарь", "писарь", "сказитель", "шут",
        "музыкант", "танцор", "актёр", "художник", "скульптор",
        "ювелир", "оружейник", "бронник", "лучник", "конюх",
        "сокольничий", "повар", "винодел", "пивовар", "мельник"
    };

    private static final List<String> ALL = new ArrayList<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        // базовые всегда есть
        ALL.addAll(Arrays.asList(BASE));

        // читаем кэш
        try {
            if (Files.exists(CACHE)) {
                String json = Files.readString(CACHE);
                List<String> cached = GSON.fromJson(json,
                    new TypeToken<List<String>>(){}.getType());
                if (cached != null) {
                    for (String p : cached) {
                        if (!ALL.contains(p)) ALL.add(p);
                    }
                }
                AnimaVillagers.LOGGER.info("Загружено {} профессий из кэша", ALL.size());
            }
        } catch (Exception e) {
            AnimaVillagers.LOGGER.error("Ошибка чтения кэша профессий: {}", e.getMessage());
        }
        loaded = true;
    }

    /** LLM генерирует пачку новых профессий и добавляет в базу. */
    public static void generateBatch(int count) {
        load();
        String system = "Ты — генератор профессий для средневековой деревни Minecraft. "
            + "Отвечай ТОЛЬКО списком через запятую, без нумерации, без пояснений.";

        String user = String.format(
            "Придумай %d уникальных профессий для деревни. "
            + "Пусть будут редкие и необычные: 'ловец снов', 'хранитель пчёл', "
            + "'звонарь', 'алхимик', 'травница', 'костоправ', 'мельник', 'кожемяка', "
            + "'гончар', 'солевар', 'дегтярь', 'смолокур', 'бондарь', 'колесник'. "
            + "Не повторяй уже известные: %s", count, String.join(", ", ALL));

        String result = LlmClient.ask(system, user);
        if (result == null) {
            AnimaVillagers.LOGGER.warn("LLM недоступен, база остаётся на {} профессиях", ALL.size());
            return;
        }

        int added = 0;
        for (String raw : result.split("[,\\n]")) {
            String p = raw.trim().toLowerCase()
                .replaceAll("^[0-9\\-.*\\s]+", "")
                .replaceAll("[«»\"']", "");
            if (p.length() >= 3 && p.length() < 40
                && !ALL.contains(p)) {
                ALL.add(p);
                added++;
            }
        }

        AnimaVillagers.LOGGER.info("Добавлено {} профессий (всего: {})", added, ALL.size());
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(CACHE.getParent());
            Files.writeString(CACHE, GSON.toJson(ALL));
        } catch (Exception e) {
            AnimaVillagers.LOGGER.error("Не сохранил кэш: {}", e.getMessage());
        }
    }

    public static String random() {
        load();
        return ALL.get(RNG.nextInt(ALL.size()));
    }

    public static int size() {
        load();
        return ALL.size();
    }

    public static List<String> all() {
        load();
        return Collections.unmodifiableList(ALL);
    }
}
