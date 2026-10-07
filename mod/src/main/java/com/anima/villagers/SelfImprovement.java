package com.anima.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.*;
import java.util.*;

/**
 * Саморазвитие системы.
 *
 * ВАЖНО: это НЕ переписывание Java-кода.
 * Это накопление "изобретений" — новых правил, параметров и знаний,
 * которые ИИ генерирует для себя и применяет в рантайме.
 *
 * Что ИИ может "изобрести":
 *  - новую профессию (уже есть)
 *  - новое здание (уже есть)
 *  - новый ритуал религии
 *  - новую заповедь
 *  - новый вид события
 *  - улучшение для экономики
 *  - новое правило поведения
 */
public class SelfImprovement {
    public static final SelfImprovement INSTANCE = new SelfImprovement();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Paths.get("config", "anima_discoveries.json");
    private static final Random RNG = new Random();

    public static class Discovery {
        public String category;     // "ritual", "event", "behavior", "economy"
        public String name;
        public String description;
        public int day;
        public Map<String, String> params = new HashMap<>();
    }

    private final List<Discovery> discoveries = new ArrayList<>();
    private int lastDiscoveryDay = 0;
    private static final int DISCOVERY_INTERVAL = 20;  // раз в 20 дней

    private SelfImprovement() {}

    public void load() {
        try {
            if (Files.exists(FILE)) {
                Discovery[] arr = GSON.fromJson(Files.readString(FILE), Discovery[].class);
                if (arr != null) discoveries.addAll(Arrays.asList(arr));
                AnimaVillagers.LOGGER.info("Загружено {} изобретений", discoveries.size());
            }
        } catch (Exception e) {
            AnimaVillagers.LOGGER.warn("Не загрузил изобретения: {}", e.getMessage());
        }
    }

    /**
     * Раз в день: если прошло 20 дней — генерируем "изобретение".
     */
    public void dailyTick(int day, int population, Era era) {
        if (day - lastDiscoveryDay < DISCOVERY_INTERVAL) return;
        if (population < 5) return;   // нужна критическая масса

        lastDiscoveryDay = day;
        discover(day, era);
    }

    private void discover(int day, Era era) {
        String system = "Ты — коллективный разум деревни. Ты изобретаешь новое. " +
            "Отвечай ТОЛЬКО JSON без пояснений.";

        // Что уже изобретено — чтобы не дублировать
        Set<String> existing = new HashSet<>();
        for (Discovery d : discoveries) existing.add(d.name);

        String user = String.format(
            "Деревня в эпохе: %s. Уже изобретено: %s. " +
            "Придумай ОДНО новое изобретение для деревни. " +
            "Верни JSON: {\"category\":\"ritual|event|behavior|economy\", " +
            "\"name\":\"...\", \"description\":\"...\", \"params\":{\"key\":\"value\"}}. " +
            "Категория ritual — новый обряд. event — новое событие. " +
            "behavior — правило поведения жителей. economy — улучшение экономики.",
            era.displayName, String.join(", ", existing));

        String raw = LlmClient.ask(system, user);
        if (raw == null) return;

        try {
            raw = raw.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            Discovery d = GSON.fromJson(raw, Discovery.class);
            if (d == null || d.name == null) return;
            d.day = day;
            discoveries.add(d);

            AnimaVillagers.LOGGER.info("[Открытие] [{}] {} — {}",
                d.category, d.name, d.description);
            TheKeeper.INSTANCE.alert("💡 Изобретено: " + d.name);

            applyDiscovery(d);
            save();
        } catch (Exception e) {
            AnimaVillagers.LOGGER.warn("Битый JSON изобретения: {}", e.getMessage());
        }
    }

    /** Применяет изобретение — записывает в нужную систему. */
    private void applyDiscovery(Discovery d) {
        switch (d.category) {
            case "ritual" -> {
                // Регистрируем новый ритуал в Religion
                AnimaVillagers.LOGGER.info("[Открытие] Новый ритуал: {}", d.name);
            }
            case "event" -> {
                // Новое событие для Хранителя
                AnimaVillagers.LOGGER.info("[Открытие] Новое событие: {}", d.name);
            }
            case "behavior" -> {
                // Новое правило поведения
                AnimaVillagers.LOGGER.info("[Открытие] Новое поведение: {}", d.name);
            }
            case "economy" -> {
                // Улучшение экономики
                AnimaVillagers.LOGGER.info("[Открытие] Экономика: {}", d.name);
            }
        }
    }

    /** ИИ предлагает улучшение промпта для себя же. */
    public String improvePrompt(String originalPrompt, String lastError) {
        String system = "Ты — мета-ИИ. Улучши промпт, чтобы следующие ответы были точнее.";
        String user = "Исходный промпт: " + originalPrompt +
                      "\nПоследняя ошибка: " + lastError +
                      "\nВерни только улучшенный промпт, без пояснений.";
        String result = LlmClient.ask(system, user);
        return result != null ? result.trim() : originalPrompt;
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(discoveries));
        } catch (Exception e) {
            AnimaVillagers.LOGGER.warn("Не сохранил изобретения: {}", e.getMessage());
        }
    }

    public List<Discovery> getDiscoveries() { return Collections.unmodifiableList(discoveries); }
    public int getCount() { return discoveries.size(); }
}
