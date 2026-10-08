package com.anima.villagers;

import java.util.*;

/**
 * Менеджер всех поселений.
 * Создаёт 3 поселения, распределяет жителей, следит за отношениями.
 */
public class SettlementManager {
    public static final SettlementManager INSTANCE = new SettlementManager();

    private final List<Settlement> settlements = new ArrayList<>();
    private boolean initialized = false;

    private SettlementManager() {}

    /** Создаёт три поселения по сиду. */
    public void init() {
        if (initialized) return;
        initialized = true;

        // Координаты из сида 5365291080835139406
        Settlement north = new Settlement("Северный Стан", -160, 0);
        Settlement west  = new Settlement("Западная Крепость", -144, -176);
        Settlement south = new Settlement("Южное Поселение", 0, -272);

        settlements.add(north);
        settlements.add(west);
        settlements.add(south);

        // Начальные отношения — нейтральные
        north.relations.put(west.id, 0);
        north.relations.put(south.id, 0);
        west.relations.put(south.id, 0);

        AnimaVillagers.LOGGER.info("Создано 3 поселения: {}, {}, {}",
            north.name, west.name, south.name);
    }

    /** Распределяет жителя в ближайшее поселение. */
    public Settlement assignToNearest(VillagerCharacter v) {
        Settlement best = null;
        double bestDist = Double.MAX_VALUE;
        for (Settlement s : settlements) {
            double dx = v.x - s.centerX;
            double dz = v.z - s.centerZ;
            double dist = dx * dx + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = s;
            }
        }
        if (best != null) best.addCitizen(v.entityUuid);
        return best;
    }

    /** Выборы лидера в поселении. */
    public void electLeader(Settlement s, Collection<VillagerCharacter> allVillagers) {
        VillagerCharacter winner = null;
        int bestScore = Integer.MIN_VALUE;

        for (UUID uuid : s.citizens) {
            VillagerCharacter v = findVillager(uuid, allVillagers);
            if (v == null) continue;

            int score = 0;
            score += (int)(v.getTrait("ум") * 40);
            score += (int)(v.getTrait("смелость") * 30);
            score += (int)(v.getTrait("доброта") * 20);
            score -= (int)(v.getTrait("жадность") * 25);
            score -= (int)(v.getTrait("агрессия") * 15);

            if (score > bestScore) {
                bestScore = score;
                winner = v;
            }
        }

        if (winner == null) return;
        s.leaderId = winner.entityUuid;
        s.assignRole(winner.entityUuid, "лидер");

        String title = switch (s.population) {
            case 0, 1, 2, 3 -> "вождь";
            case 4, 5, 6, 7, 8, 9 -> "старейшина";
            default -> s.population >= 50 ? "император" : "король";
        };
        s.leaderTitle = title;

        AnimaVillagers.LOGGER.info("[Власть] {} избран {} в {}",
            winner.name, title, s.name);
        TheKeeper.INSTANCE.alert("👑 " + winner.name + " — " + title + " в " + s.name);
    }

    /** Лидер назначает роли: воины, рабочие, жрецы... */
    public void assignRoles(Settlement s, Collection<VillagerCharacter> all) {
        // Считаем сколько нужно
        int soldierCount = Math.max(3, s.population / 5);
        int workerCount = Math.max(5, s.population / 3);
        int priestCount = Math.max(1, s.population / 15);

        List<VillagerCharacter> sorted = new ArrayList<>();
        for (UUID uuid : s.citizens) {
            VillagerCharacter v = findVillager(uuid, all);
            if (v != null && !v.entityUuid.equals(s.leaderId)) sorted.add(v);
        }
        sorted.sort((a, b) -> {
            int sa = (int)(a.getTrait("смелость") * 50 + a.getTrait("агрессия") * 30);
            int sb = (int)(b.getTrait("смелость") * 50 + b.getTrait("агрессия") * 30);
            return Integer.compare(sb, sa);
        });

        int idx = 0;
        for (; idx < sorted.size() && idx < soldierCount; idx++) {
            s.assignRole(sorted.get(idx).entityUuid, "воин");
        }
        for (; idx < sorted.size() && idx < soldierCount + workerCount; idx++) {
            s.assignRole(sorted.get(idx).entityUuid, "рабочий");
        }
        for (; idx < sorted.size() && idx < soldierCount + workerCount + priestCount; idx++) {
            s.assignRole(sorted.get(idx).entityUuid, "жрец");
        }

        AnimaVillagers.LOGGER.info("[Роли] {}: {} воинов, {} рабочих, {} жрецов",
            s.name, s.getSoldiers(), s.getWorkers(),
            (int) s.roles.values().stream().filter(r -> r.equals("жрец")).count());
    }

    private VillagerCharacter findVillager(UUID uuid, Collection<VillagerCharacter> all) {
        for (VillagerCharacter v : all) {
            if (v.entityUuid.equals(uuid)) return v;
        }
        return null;
    }

    /** Раз в день — обновление поселений. */
    public void dailyTick(Collection<VillagerCharacter> all) {
        for (Settlement s : settlements) {
            // Если нет лидера — выбираем
            if (s.leaderId == null) {
                electLeader(s, all);
                assignRoles(s, all);
            }

            // Ресурсы
            s.food += s.getWorkers() * 3 - s.population * 2;
            s.resources += s.getWorkers() * 2 - s.population;
            s.gold += s.getWorkers();

            s.food = Math.max(0, Math.min(2000, s.food));
            s.resources = Math.max(0, Math.min(2000, s.resources));

            if (s.food < s.population * 3) {
                TheKeeper.INSTANCE.alert("⚠️ " + s.name + ": голод!");
            }
        }
    }

    public List<Settlement> getSettlements() { return Collections.unmodifiableList(settlements); }

    public Settlement getSettlementOf(UUID villagerId) {
        for (Settlement s : settlements) {
            if (s.citizens.contains(villagerId)) return s;
        }
        return null;
    }
}
