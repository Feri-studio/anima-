package com.anima.villagers;

/**
 * Эпохи развития деревни. Деревня проходит их последовательно.
 * Каждая эпоха открывает новые профессии, постройки и поведение.
 */
public enum Era {
    STONE("Каменный век", 0, new String[]{
        "хижина", "костёр", "тотем"
    }),
    BRONZE("Бронзовый век", 100, new String[]{
        "ферма", "колодец", "забор", "амбар"
    }),
    IRON("Железный век", 300, new String[]{
        "кузница", "стена", "казарма", "шахта"
    }),
    MEDIEVAL("Средневековье", 700, new String[]{
        "замок", "рынок", "башня", "таверна", "церковь"
    }),
    RENAISSANCE("Ренессанс", 1500, new String[]{
        "собор", "академия", "статуя", "библиотека", "театр"
    }),
    INDUSTRIAL("Индустриальная эпоха", 3000, new String[]{
        "завод", "мастерская", "железная дорога", "машина"
    });

    public final String displayName;
    public final int requiredKnowledge;
    public final String[] buildings;

    Era(String displayName, int requiredKnowledge, String[] buildings) {
        this.displayName = displayName;
        this.requiredKnowledge = requiredKnowledge;
        this.buildings = buildings;
    }

    public Era next() {
        int ord = ordinal();
        if (ord + 1 < values().length) return values()[ord + 1];
        return this;
    }

    public static Era fromKnowledge(int knowledge) {
        Era result = STONE;
        for (Era e : values()) {
            if (knowledge >= e.requiredKnowledge) result = e;
        }
        return result;
    }
}
