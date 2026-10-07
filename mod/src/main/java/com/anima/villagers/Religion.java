package com.anima.villagers;

import java.util.*;

/**
 * Религия деревни. Строится вокруг бога (God.java).
 * Проходит стадии: суеверие → культ → организованная религия → фанатизм.
 */
public class Religion {
    public static final Religion INSTANCE = new Religion();
    private static final Random RNG = new Random();

    public enum Stage {
        SUPERSTITION("Суеверие", "жители просто верят в духов"),
        CULT("Культ", "есть алтарь и обряды"),
        ORGANIZED("Организованная религия", "есть храм и жрецы"),
        FANATIC("Фанатизм", "жители готовы умереть за веру"),
        SCHISM("Раскол", "две враждующие ветви");

        public final String displayName;
        public final String description;
        Stage(String n, String d) { displayName = n; description = d; }
    }

    private Stage stage = Stage.SUPERSTITION;
    private int believers = 0;
    private int temples = 0;
    private int priests = 0;
    private int fanaticism = 0;       // 0-100
    private int lastRitualDay = 0;

    // Заповеди (генерируются LLM один раз)
    private final List<String> commandments = new ArrayList<>();
    // Святые места (координаты алтарей)
    private final List<int[]> holyPlaces = new ArrayList<>();

    private Religion() {}

    /**
     * Вызывается раз в день.
     */
    public void dailyTick(int day, Collection<VillagerCharacter> villagers, int faith) {
        // Считаем верующих (у кого эмпатия > 0.4)
        believers = 0;
        for (VillagerCharacter v : villagers) {
            if (v.getTrait("эмпатия") > 0.4f) believers++;
        }

        // Считаем жрецов
        priests = 0;
        for (VillagerCharacter v : villagers) {
            if (v.profession.equals("жрец")) priests++;
        }

        // Фанатизм растёт если вера высокая + много храмов
        if (faith > 70 && temples >= 1) {
            fanaticism = Math.min(100, fanaticism + 2);
        } else if (faith < 30) {
            fanaticism = Math.max(0, fanaticism - 1);
        }

        // Проверка смены стадии
        updateStage();

        // Ритуалы раз в 7 дней
        if (day - lastRitualDay >= 7 && believers >= 3) {
            ritual(day);
            lastRitualDay = day;
        }
    }

    private void updateStage() {
        Stage newStage = stage;
        if (temples >= 3 && priests >= 5 && fanaticism > 60) newStage = Stage.FANATIC;
        else if (temples >= 1 && priests >= 2) newStage = Stage.ORGANIZED;
        else if (believers >= 5) newStage = Stage.CULT;

        if (newStage != stage) {
            stage = newStage;
            AnimaVillagers.LOGGER.info("=== РЕЛИГИЯ: {} ===", stage.displayName);
            TheKeeper.INSTANCE.alert("🛐 Религия деревни: " + stage.displayName);
        }
    }

    private void ritual(int day) {
        String[] rituals = {"молитва", "жертвоприношение", "праздник", "пост", "крестный ход"};
        String r = rituals[RNG.nextInt(rituals.length)];
        AnimaVillagers.LOGGER.info("[Религия] Ритуал дня {}: {}", day, r);
    }

    /** Строит храм. Один из жителей становится жрецом. */
    public void buildTemple() {
        temples++;
        AnimaVillagers.LOGGER.info("[Религия] Храм построен (всего: {})", temples);
    }

    /** LLM генерирует заповеди религии — один раз. */
    public void generateCommandments() {
        if (!commandments.isEmpty()) return;
        String system = "Ты — основатель религии. Отвечай ТОЛЬКО списком заповедей, по одной в строке.";
        String user = "Придумай 5 заповедей для религии средневековой деревни. Коротко, властно.";
        String result = LlmClient.ask(system, user);
        if (result == null) return;

        for (String line : result.split("\n")) {
            String c = line.trim().replaceAll("^[0-9\\-.*\\s]+", "");
            if (!c.isEmpty() && c.length() < 120) commandments.add(c);
        }
        AnimaVillagers.LOGGER.info("[Религия] Получено {} заповедей", commandments.size());
        for (String c : commandments) AnimaVillagers.LOGGER.info("  • {}", c);
    }

    public Stage getStage() { return stage; }
    public int getBelievers() { return believers; }
    public int getTemples() { return temples; }
    public int getFanaticism() { return fanaticism; }
    public List<String> getCommandments() { return Collections.unmodifiableList(commandments); }
    public List<int[]> getHolyPlaces() { return Collections.unmodifiableList(holyPlaces); }
}
