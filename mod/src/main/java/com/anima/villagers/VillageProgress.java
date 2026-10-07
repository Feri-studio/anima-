package com.anima.villagers;

import java.util.*;

/**
 * Коллективный прогресс деревни.
 * Жители вместе копят знания и открывают технологии.
 */
public class VillageProgress {
    public static final VillageProgress INSTANCE = new VillageProgress();

    // Текущее состояние
    private int knowledge = 0;              // очки знаний
    private Era currentEra = Era.STONE;     // текущая эпоха
    private int eraProgressDays = 0;        // сколько дней в эпохе

    // Открытые технологии (название → день открытия)
    private final Map<String, Integer> unlockedTechs = new LinkedHashMap<>();

    // Открытые чертежи (Blueprint)
    private final Map<String, Blueprint> blueprints = new LinkedHashMap<>();

    // Построенные здания (название → сколько штук)
    private final Map<String, Integer> builtStructures = new LinkedHashMap<>();

    private VillageProgress() {}

    /**
     * Вызывается раз в день. Жители вносят вклад в общее дело.
     * @param workerCount сколько жителей работает
     * @param avgIntelligence средний интеллект жителей (0-1)
     * @param resources сколько ресурсов в деревне
     */
    public void dailyTick(int workerCount, float avgIntelligence, int resources) {
        // Базовый прирост знаний
        int gain = workerCount * 2;
        gain += (int)(workerCount * avgIntelligence * 5);
        gain += resources / 20;

        knowledge += Math.max(1, gain);
        eraProgressDays++;

        // Проверка на переход в новую эпоху
        Era newEra = Era.fromKnowledge(knowledge);
        if (newEra != currentEra) {
            advanceEra(newEra);
        }

        // Открытие технологий по мере накопления знаний
        checkTechUnlock();

        if (eraProgressDays % 5 == 0) {
            AnimaVillagers.LOGGER.info(
                "[Прогресс] {} — знания: {}, дней в эпохе: {}, зданий: {}",
                currentEra.displayName, knowledge, eraProgressDays, builtStructures.size()
            );
        }
    }

    private void advanceEra(Era newEra) {
        currentEra = newEra;
        eraProgressDays = 0;

        AnimaVillagers.LOGGER.info("=== ДЕРЕВНЯ ВОШЛА В {} ===", newEra.displayName.toUpperCase());
        TheKeeper.INSTANCE.alert("🏛️ Деревня перешла в " + newEra.displayName);

        // Автоматически разблокируем базовые постройки эпохи
        for (String b : newEra.buildings) {
            if (!blueprints.containsKey(b)) {
                blueprints.put(b, new Blueprint(b, newEra, false));
            }
        }
    }

    private void checkTechUnlock() {
        // Простые технологии по порогам знаний
        int[][] thresholds = {
            {50, "земледелие"},
            {150, "письменность"},
            {400, "металлургия"},
            {900, "строительное дело"},
            {1800, "архитектура"},
            {3500, "механика"}
        };
        for (int[] t : thresholds) {
            String techName = (String) techNameOf(t[0]);
            if (knowledge >= t[0] && !unlockedTechs.containsKey(techName)) {
                unlockedTechs.put(techName, eraProgressDays);
                AnimaVillagers.LOGGER.info("[Прогресс] Технология открыта: {}", techName);
                TheKeeper.INSTANCE.alert("🔬 Открыта технология: " + techName);
            }
        }
    }

    private String techNameOf(int threshold) {
        switch (threshold) {
            case 50: return "земледелие";
            case 150: return "письменность";
            case 400: return "металлургия";
            case 900: return "строительное дело";
            case 1800: return "архитектура";
            case 3500: return "механика";
            default: return "неизвестно";
        }
    }

    /** Регистрирует завершённую постройку. */
    public void structureBuilt(String name) {
        builtStructures.merge(name, 1, Integer::sum);
        AnimaVillagers.LOGGER.info("[Прогресс] Построено: {} (всего: {})",
            name, builtStructures.get(name));
        TheKeeper.INSTANCE.alert("🏗️ Построено: " + name);
    }

    /** Добавляет новый чертёж (обычно сгенерированный LLM). */
    public void addBlueprint(Blueprint bp) {
        blueprints.put(bp.name, bp);
        AnimaVillagers.LOGGER.info("[Прогресс] Новый чертёж: {} ({})", bp.name, bp.era.displayName);
    }

    // Геттеры
    public int getKnowledge() { return knowledge; }
    public Era getCurrentEra() { return currentEra; }
    public int getEraProgressDays() { return eraProgressDays; }
    public Map<String, Blueprint> getBlueprints() { return Collections.unmodifiableMap(blueprints); }
    public Map<String, Integer> getBuiltStructures() { return Collections.unmodifiableMap(builtStructures); }
    public int getStructureCount() { return builtStructures.values().stream().mapToInt(Integer::intValue).sum(); }
}
