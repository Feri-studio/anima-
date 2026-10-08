package com.anima.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.*;
import java.util.*;

/**
 * Саморазвитие системы.
 * ИИ анализирует свою работу и предлагает улучшения.
 */
public class SelfImprovement {
    public static final SelfImprovement INSTANCE = new SelfImprovement();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Paths.get("config", "anima_discoveries.json");
    private static final Random RNG = new Random();

    public static class Discovery {
        public String category;
        public String name;
        public String description;
        public int day;
        public Map<String, String> params = new HashMap<>();
    }

    private final List<Discovery> discoveries = new ArrayList<>();
    private final Map<String, Integer> errorStats = new HashMap<>();  // для анализа
    private int lastDiscoveryDay = 0;
    private int lastMetaDay = 0;
    private static final int DISCOVERY_INTERVAL = 20;
    private static final int META_INTERVAL = 50;   // раз в 50 дней — мета-рефлексия

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

    public void dailyTick(int day, int population, Era era) {
        if (day - lastDiscoveryDay >= DISCOVERY_INTERVAL && population >= 5) {
            lastDiscoveryDay = day;
            discover(day, era);
        }

        // Мета-рефлексия — реже, но глубже
        if (day - lastMetaDay >= META_INTERVAL && population >= 10) {
            lastMetaDay = day;
            metaReflect(day, population, era);
        }
    }

    /** Регистрируем ошибки систем — для анализа. */
    public void noteError(String source, String error) {
        String key = source + ":" + error;
        errorStats.merge(key, 1, Integer::sum);
    }

    private void discover(int day, Era era) {
        String system = "Ты — коллективный разум деревни. Ты изобретаешь новое. " +
            "Отвечай ТОЛЬКО JSON без пояснений.";

        Set<String> existing = new HashSet<>();
        for (Discovery d : discoveries) existing.add(d.name);

        String user = String.format(
            "Деревня в эпохе: %s. Уже изобретено: %s. " +
            "Придумай ОДНО новое изобретение для деревни. " +
            "Верни JSON: {\"category\":\"ritual|event|behavior|economy|emotion\"," +
            "\"name\":\"...\",\"description\":\"...\",\"params\":{\"key\":\"value\"}}.",
            era.displayName, String.join(", ", existing));

        String raw = LlmClient.ask(system, user);
        if (raw == null) return;

        try {
            raw = raw.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            Discovery d = GSON.fromJson(raw, Discovery.class);
            if (d == null || d.name == null) return;
            d.day = day;
            discoveries.add(d);

            AnimaVillagers.LOGGER.info("[Открытие] [{}] {}", d.category, d.name);
            TheKeeper.INSTANCE.alert("💡 " + d.name);
            applyDiscovery(d);
            save();
        } catch (Exception e) {
            AnimaVillagers.LOGGER.warn("Битый JSON: {}", e.getMessage());
        }
    }

    /** Мета-рефлексия — ИИ думает, как улучшить сам себя. */
    private void metaReflect(int day, int population, Era era) {
        AnimaVillagers.LOGGER.info("[Самоанализ] День {} — ИИ размышляет о своём развитии", day);

        // Собираем статистику
        StringBuilder stats = new StringBuilder();
        stats.append("День: ").append(day).append(". ");
        stats.append("Население: ").append(population).append(". ");
        stats.append("Эпоха: ").append(era.displayName).append(". ");
        stats.append("Изобретений: ").append(discoveries.size()).append(". ");
        stats.append("Ошибок систем: ").append(errorStats.size()).append(". ");

        if (!errorStats.isEmpty()) {
            stats.append("Частые ошибки: ");
            errorStats.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(3)
                .forEach(e -> stats.append(e.getKey()).append(" (").append(e.getValue()).append("), "));
        }

        String system = "Ты — мета-ИИ, который улучшает систему ИИ-жителей. " +
            "Отвечай кратко и по делу.";

        String user = stats + "\nЧто улучшить в системе? Верни 1-2 предложения с конкретными идеями.";

        String result = LlmClient.ask(system, user);
        if (result != null) {
            AnimaVillagers.LOGGER.info("[Самоанализ] Рекомендация: {}", result.trim());
            TheKeeper.INSTANCE.alert("🧠 Самоанализ: " + result.trim());

            // Записываем как "изобретение" категории "meta"
            Discovery meta = new Discovery();
            meta.category = "meta";
            meta.name = "Мета-рефлексия дня " + day;
            meta.description = result.trim();
            meta.day = day;
            discoveries.add(meta);
            save();
        }
    }

    private void applyDiscovery(Discovery d) {
        switch (d.category) {
            case "ritual" -> AnimaVillagers.LOGGER.info("[Открытие] Ритуал: {}", d.name);
            case "event" -> AnimaVillagers.LOGGER.info("[Открытие] Событие: {}", d.name);
            case "behavior" -> AnimaVillagers.LOGGER.info("[Открытие] Поведение: {}", d.name);
            case "economy" -> AnimaVillagers.LOGGER.info("[Открытие] Экономика: {}", d.name);
            case "emotion" -> AnimaVillagers.LOGGER.info("[Открытие] Эмоция: {}", d.name);
        }
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(discoveries));
        } catch (Exception e) {
            AnimaVillagers.LOGGER.warn("Не сохранил: {}", e.getMessage());
        }
    }

    public List<Discovery> getDiscoveries() { return Collections.unmodifiableList(discoveries); }
    public int getCount() { return discoveries.size(); }
}
