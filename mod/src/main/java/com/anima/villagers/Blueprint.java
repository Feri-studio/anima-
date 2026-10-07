package com.anima.villagers;

import java.util.*;

/**
 * Чертёж постройки. Генерируется один раз (обычно через LLM),
 * потом жители строят по нему без участия LLM.
 */
public class Blueprint {
    public final String name;         // "замок", "собор"
    public final Era era;             // минимальная эпоха
    public final boolean fromLlm;     // сгенерирован ИИ или базовый
    public Map<String, Integer> cost = new LinkedHashMap<>();  // материалы
    public int requiredWorkers = 1;
    public int buildDays = 1;
    public String description = "";

    // Размеры (для будущей генерации структуры)
    public int width = 5;
    public int height = 5;
    public int depth = 5;

    public Blueprint(String name, Era era, boolean fromLlm) {
        this.name = name;
        this.era = era;
        this.fromLlm = fromLlm;
        applyDefaultCost();
    }

    private void applyDefaultCost() {
        switch (name) {
            case "хижина" -> { cost.put("дерево", 20); width=5; height=4; depth=5; }
            case "ферма" -> { cost.put("дерево", 10); cost.put("семена", 5); width=10; depth=10; }
            case "забор" -> { cost.put("дерево", 30); }
            case "кузница" -> { cost.put("камень", 40); cost.put("дерево", 20); requiredWorkers=2; }
            case "стена" -> { cost.put("камень", 200); requiredWorkers=5; buildDays=3; }
            case "замок" -> { cost.put("камень", 1000); cost.put("дерево", 300);
                              requiredWorkers=10; buildDays=10; width=30; height=20; depth=30; }
            case "собор" -> { cost.put("камень", 1500); cost.put("золото", 200);
                              requiredWorkers=15; buildDays=15; height=40; }
            case "рынок" -> { cost.put("дерево", 100); cost.put("камень", 50); requiredWorkers=3; }
            default -> cost.put("дерево", 10);
        }
    }

    public boolean canBuild(int availableResources) {
        int total = cost.values().stream().mapToInt(Integer::intValue).sum();
        return availableResources >= total;
    }

    @Override
    public String toString() {
        return name + " [" + era.displayName + "] стоимость: " + cost;
    }
}
