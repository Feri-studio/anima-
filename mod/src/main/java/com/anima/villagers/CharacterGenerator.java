package com.anima.villagers;

import java.util.*;

public class CharacterGenerator {
    private static final Random RNG = new Random();

    private static final String[] NAMES = {
        "Бран", "Марта", "Торн", "Эйра", "Грим", "Лиан", "Олаф", "Сага",
        "Кестрел", "Вирн", "Хельга", "Руфус", "Аста", "Бьорн", "Нима"
    };
    private static final String[] PROFESSIONS = {
        "фермер", "кузнец", "шахтёр", "строитель", "торговец",
        "стражник", "жрец", "охотник", "рыбак", "учёный"
    };

    /**
     * Генерирует жителя.
     * Если LLM настроен — использует его для предыстории и характера.
     * Иначе — заглушка.
     */
    public static VillagerCharacter generate() {
        String name = NAMES[RNG.nextInt(NAMES.length)];
        String profession = PROFESSIONS[RNG.nextInt(PROFESSIONS.length)];
        int age = 18 + RNG.nextInt(50);

        VillagerCharacter v = new VillagerCharacter(name, age, profession);

        // Пробуем LLM
        String backstory = generateBackstoryWithLlm(name, profession, age);
        if (backstory != null) {
            v.backstory = backstory;
            AnimaVillagers.LOGGER.info("LLM сгенерировал предысторию для {}", name);
        } else {
            v.backstory = "Скромное прошлое, о котором не любит говорить.";
            AnimaVillagers.LOGGER.info("Заглушка предыстории для {}", name);
        }

        // Черты — пока локально (потом можно тоже через LLM)
        v.setTrait("смелость", RNG.nextFloat());
        v.setTrait("доброта", RNG.nextFloat());
        v.setTrait("жадность", RNG.nextFloat());
        v.setTrait("агрессия", RNG.nextFloat());
        v.setTrait("ум", RNG.nextFloat());
        v.setTrait("эмпатия", RNG.nextFloat());
        v.setTrait("любопытство", RNG.nextFloat());

        return v;
    }

    private static String generateBackstoryWithLlm(String name, String profession, int age) {
        String system = "Ты — генератор персонажей для средневековой деревни Minecraft. "
            + "Отвечай ТОЛЬКО одной фразой на русском, без кавычек.";

        String user = String.format(
            "Придумай короткую предысторию (1 предложение) для жителя: "
            + "имя %s, профессия %s, возраст %d лет. "
            + "Пусть будет что-то запоминающееся — потеря, мечта, тайна.",
            name, profession, age
        );

        String result = LlmClient.ask(system, user);
        if (result != null) {
            result = result.trim().replaceAll("^\"|\"$", "");
            if (result.length() > 200) result = result.substring(0, 200) + "...";
        }
        return result;
    }
}
