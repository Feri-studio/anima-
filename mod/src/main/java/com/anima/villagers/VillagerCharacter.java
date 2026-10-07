package com.anima.villagers;

import java.util.*;

public class VillagerCharacter {
    public String name;
    public int age;
    public String profession;
    public String backstory;
    public Map<String, Float> traits = new HashMap<>();
    public List<String> fears = new ArrayList<>();
    public List<String> dreams = new ArrayList<>();
    public String mood = "нейтральный";
    public UUID entityUuid;
    public double x, y, z;   // позиция в мире

    public VillagerCharacter(String name, int age, String profession) {
        this.name = name;
        this.age = age;
        this.profession = profession;
    }

    public float getTrait(String key) {
        return traits.getOrDefault(key, 0.5f);
    }

    public void setTrait(String key, float value) {
        traits.put(key, Math.max(0f, Math.min(1f, value)));
    }

    @Override
    public String toString() {
        return name + " (" + profession + ", " + age + " лет) — " + mood;
    }
}
