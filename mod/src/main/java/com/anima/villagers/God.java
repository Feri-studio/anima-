package com.anima.villagers;

import java.util.*;

/**
 * Бог жителей. Жители верят в него, молятся, получают "ответы".
 * Технически — это спец-агент внутри Хранителя, но с отдельной логикой.
 *
 * Жители НЕ знают его настоящего имени. У каждого своё представление.
 */
public class God {
    public static final God INSTANCE = new God();

    private final Random RNG = new Random();

    // Имена, которыми жители называют бога (зависят от эпохи)
    private final Map<Era, String> names = new EnumMap<>(Era.class);
    private int faith = 50;          // общая вера деревни 0-100
    private int lastMiracleDay = 0;

    private God() {
        names.put(Era.STONE, "Дух Камня");
        names.put(Era.BRONZE, "Мать-Земля");
        names.put(Era.IRON, "Отец-Кузнец");
        names.put(Era.MEDIEVAL, "Всевышний");
        names.put(Era.RENAISSANCE, "Разум");
        names.put(Era.INDUSTRIAL, "Прогресс");
    }

    public String currentName() {
        return names.getOrDefault(VillageProgress.INSTANCE.getCurrentEra(), "Неизвестный");
    }

    /**
     * Вызывается раз в день. Бог "смотрит" на деревню.
     */
    public void dailyTick(int day, int population, int mood, String crisis) {
        // Вера растёт если всё хорошо, падает если плохо
        if (crisis == null && mood > 60) faith += 2;
        else if (crisis != null) faith -= 3;
        faith = Math.max(0, Math.min(100, faith));

        // Раз в 10 дней — "чудо" (если вера высокая)
        if (day - lastMiracleDay >= 10 && faith > 60) {
            miracle(day);
            lastMiracleDay = day;
        }

        // Раз в 15 дней — "кара" (если вера низкая)
        if (faith < 20 && RNG.nextInt(15) == 0) {
            punishment();
        }
    }

    private void miracle(int day) {
        String[] miracles = {
            "урожай", "дождь", "здоровье", "знание", "защита"
        };
        String m = miracles[RNG.nextInt(miracles.length)];

        String godName = currentName();
        AnimaVillagers.LOGGER.info("[Бог] Чудо дня {}: {} — {}", day, godName, m);
        TheKeeper.INSTANCE.alert("✨ Чудо: " + godName + " даровал " + m);

        switch (m) {
            case "урожай" -> {
                // TODO: добавить еды
            }
            case "знание" -> {
                // TODO: дать очков знаний
            }
            default -> {}
        }
    }

    private void punishment() {
        String godName = currentName();
        AnimaVillagers.LOGGER.warn("[Бог] Кара: {} разгневан", godName);
        TheKeeper.INSTANCE.alert("⚡ " + godName + " разгневан");
    }

    /**
     * Житель молится богу. Бог может ответить (или нет).
     */
    public String pray(VillagerCharacter villager) {
        if (RNG.nextInt(100) > faith) {
            return null;  // бог молчит
        }

        String system = "Ты — бог деревни. Говори загадочно, коротко (1 предложение).";
        String user = String.format(
            "Житель %s (%s) молится тебе. Ответь ему в стиле бога.",
            villager.name, villager.profession);

        return LlmClient.ask(system, user);
    }

    public int getFaith() { return faith; }
}
