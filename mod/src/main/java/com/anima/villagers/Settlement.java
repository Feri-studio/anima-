package com.anima.villagers;

import java.util.*;

/**
 * Поселение. Одно из трёх в мире.
 * Имеет координаты, лидера, население, профессии, армию.
 */
public class Settlement {
    public final String name;
    public final int centerX, centerZ;
    public final UUID id = UUID.randomUUID();

    // Власть
    public UUID leaderId = null;
    public String leaderTitle = "вождь";
    public final Map<UUID, String> roles = new HashMap<>();  // житель → роль (воин, рабочий, жрец...)

    // Население
    public final List<UUID> citizens = new ArrayList<>();
    public int population = 0;

    // Ресурсы
    public int food = 100;
    public int resources = 50;
    public int gold = 0;

    // Отношения с другими поселениями (-100..100)
    public final Map<UUID, Integer> relations = new HashMap<>();

    public Settlement(String name, int centerX, int centerZ) {
        this.name = name;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public void addCitizen(UUID uuid) {
        if (!citizens.contains(uuid)) {
            citizens.add(uuid);
            population = citizens.size();
        }
    }

    public void removeCitizen(UUID uuid) {
        citizens.remove(uuid);
        roles.remove(uuid);
        if (uuid.equals(leaderId)) leaderId = null;
        population = citizens.size();
    }

    public void assignRole(UUID uuid, String role) {
        roles.put(uuid, role);
    }

    public String getRole(UUID uuid) {
        return roles.getOrDefault(uuid, "крестьянин");
    }

    public int getSoldiers() {
        return (int) roles.values().stream().filter(r -> r.equals("воин")).count();
    }

    public int getWorkers() {
        return (int) roles.values().stream().filter(r -> r.equals("рабочий")).count();
    }

    @Override
    public String toString() {
        return name + " (" + population + " душ, лидер: " + leaderTitle + ")";
    }
}
