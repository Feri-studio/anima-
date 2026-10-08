package com.anima.villagers;

import java.util.*;

/**
 * Глубокий разум жителя.
 * Отвечает за: цели, планы, желания, рефлексию.
 */
public class VillagerMind {
    private static final Random RNG = new Random();

    public String currentGoal = "выжить";
    public String longTermGoal = "найти своё место в деревне";
    public final List<String> plan = new ArrayList<>();     // текущие шаги
    public final List<String> desires = new ArrayList<>();  // чего хочет
    public final List<String> regrets = new ArrayList<>();  // о чём жалеет

    public int reflectionDay = 0;

    /** Раз в день житель думает о своих целях. */
    public void dailyTick(VillagerCharacter v, int day, VillageProgress progress) {
        // Раз в 5 дней — глубокая рефлексия
        if (day - reflectionDay >= 5) {
            reflectionDay = day;
            reflect(v, day, progress);
        }

        // Простые желания — обновляем каждый день
        updateDesires(v);
    }

    /** Рефлексия — через LLM житель осмысляет свою жизнь. */
    private void reflect(VillagerCharacter v, int day, VillageProgress progress) {
        String system = "Ты — житель Minecraft. Отвечай ТОЛЬКО JSON без пояснений.";
        String user = String.format(
            "Житель %s, %s, %d лет. Настроение: %s. Эпоха: %s. " +
            "Черты: смелость=%.2f, доброта=%.2f, агрессия=%.2f, ум=%.2f. " +
            "День %d. " +
            "Верни JSON: {\"goal\":\"краткосрочная цель\",\"longTerm\":\"цель на жизнь\"," +
            "\"desire\":\"чего хочет сейчас\",\"regret\":\"о чём сожалеет\"}.",
            v.name, v.profession, v.age, v.mood,
            progress.getCurrentEra().displayName,
            v.getTrait("смелость"), v.getTrait("доброта"),
            v.getTrait("агрессия"), v.getTrait("ум"), day);

        String raw = LlmClient.ask(system, user);
        if (raw == null) return;

        try {
            raw = raw.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            String goal = extract(raw, "goal");
            String longTerm = extract(raw, "longTerm");
            String desire = extract(raw, "desire");
            String regret = extract(raw, "regret");

            if (goal != null) currentGoal = goal;
            if (longTerm != null) longTermGoal = longTerm;
            if (desire != null && !desires.contains(desire)) desires.add(desire);
            if (regret != null && !regrets.contains(regret)) regrets.add(regret);

            AnimaVillagers.LOGGER.info("[Разум] {}: цель — {}, желает — {}",
                v.name, currentGoal, desire);

            // Синхронизируем с памятью для диалогов
            v.mood = moodFromReflection(v, raw);
        } catch (Exception e) {
            AnimaVillagers.LOGGER.debug("Битый JSON рефлексии: {}", e.getMessage());
        }
    }

    private String moodFromReflection(VillagerCharacter v, String raw) {
        float val = v.getTrait("доброта") + v.getTrait("смелость") - v.getTrait("агрессия");
        if (val > 1.2f) return "радостный";
        if (val < 0.3f) return "подавленный";
        return v.mood;
    }

    private void updateDesires(VillagerCharacter v) {
        if (desires.size() > 10) return;
        String[] base = {
            "найти друга", "разбогатеть", "построить дом",
            "стать лидером", "жениться", "прославиться",
            "отомстить обидчику", "научиться магии", "увидеть море"
        };
        if (RNG.nextInt(10) == 0) {
            String d = base[RNG.nextInt(base.length)];
            if (!desires.contains(d)) desires.add(d);
        }
    }

    private String extract(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]{1,200})\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : null;
    }

    /** Проверка — хочет ли житель что-то сделать прямо сейчас. */
    public boolean wantsTo(String action) {
        return desires.stream().anyMatch(d -> d.contains(action));
    }

    @Override
    public String toString() {
        return "Цель: " + currentGoal + " | Желания: " + desires.size() + " | Сожалений: " + regrets.size();
    }
}
