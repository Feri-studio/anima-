package com.anima.villagers;

import java.util.*;

/**
 * Политическая система деревни.
 * Проходит стадии: вождь → совет → королевство → империя.
 */
public class Monarchy {
    public static final Monarchy INSTANCE = new Monarchy();
    private static final Random RNG = new Random();

    public enum Government {
        TRIBE("Племя", "вождь правит силой"),
        COUNCIL("Совет", "старейшины решают вместе"),
        KINGDOM("Королевство", "король и знать"),
        EMPIRE("Империя", "император и наместники");

        public final String displayName;
        public final String description;
        Government(String n, String d) { displayName = n; description = d; }
    }

    private Government government = Government.TRIBE;
    private UUID currentRuler = null;
    private String rulerTitle = "вождь";
    private int electionDay = 0;
    private static final int ELECTION_INTERVAL = 30;  // раз в 30 дней

    // Титулы (выдаются знати)
    private final Map<UUID, String> titles = new HashMap<>();
    private final Map<UUID, Integer> reputation = new HashMap<>();  // репутация у деревни

    private Monarchy() {}

    /**
     * Раз в день: правитель работает, раз в 30 дней — выборы.
     */
    public void dailyTick(int day, Collection<VillagerCharacter> villagers) {
        // Пересчитываем репутацию
        for (VillagerCharacter v : villagers) {
            int rep = 0;
            rep += (int)(v.getTrait("ум") * 30);
            rep += (int)(v.getTrait("доброта") * 30);
            rep += (int)(v.getTrait("смелость") * 20);
            rep -= (int)(v.getTrait("жадность") * 15);
            rep -= (int)(v.getTrait("агрессия") * 10);
            // профессия
            if (v.profession.equals("жрец")) rep += 10;
            if (v.profession.equals("кузнец")) rep += 5;
            reputation.put(v.entityUuid, rep);
        }

        // Смена стадии государства
        updateGovernment(villagers.size());

        // Выборы
        if (day - electionDay >= ELECTION_INTERVAL) {
            electionDay = day;
            holdElection(villagers);
        }
    }

    private void updateGovernment(int pop) {
        Government next = government;
        if (pop >= 50 && government == Government.KINGDOM) next = Government.EMPIRE;
        else if (pop >= 20 && government == Government.COUNCIL) next = Government.KINGDOM;
        else if (pop >= 8 && government == Government.TRIBE) next = Government.COUNCIL;

        if (next != government) {
            government = next;
            rulerTitle = switch (government) {
                case TRIBE -> "вождь";
                case COUNCIL -> "старейшина";
                case KINGDOM -> "король";
                case EMPIRE -> "император";
            };
            AnimaVillagers.LOGGER.info("=== {} ===", government.displayName.toUpperCase());
            TheKeeper.INSTANCE.alert("👑 Деревня стала: " + government.displayName);
        }
    }

    private void holdElection(Collection<VillagerCharacter> villagers) {
        VillagerCharacter winner = null;
        int bestRep = Integer.MIN_VALUE;

        for (VillagerCharacter v : villagers) {
            int rep = reputation.getOrDefault(v.entityUuid, 0);
            if (rep > bestRep) { bestRep = rep; winner = v; }
        }
        if (winner == null) return;

        boolean isReelection = winner.entityUuid.equals(currentRuler);
        currentRuler = winner.entityUuid;

        String title;
        if (government == Government.COUNCIL) title = "старейшина";
        else if (government == Government.KINGDOM) title = "король";
        else if (government == Government.EMPIRE) title = "император";
        else title = "вождь";

        titles.put(winner.entityUuid, title);

        String msg = isReelection
            ? String.format("%s переизбран %s (репутация %d)", winner.name, title, bestRep)
            : String.format("%s избран %s (репутация %d)", winner.name, title, bestRep);
        AnimaVillagers.LOGGER.info("[Власть] {}", msg);
        TheKeeper.INSTANCE.alert("👑 " + msg);
    }

    /** Назначает титул (например, жрец = "епископ") */
    public void grantTitle(VillagerCharacter v, String title) {
        titles.put(v.entityUuid, title);
    }

    public Government getGovernment() { return government; }
    public String getRulerTitle() { return rulerTitle; }
    public UUID getCurrentRuler() { return currentRuler; }
    public Map<UUID, String> getTitles() { return Collections.unmodifiableMap(titles); }
    public int getReputation(UUID uuid) { return reputation.getOrDefault(uuid, 0); }
}
