package com.anima.villagers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BuildingPlanner {
    public static final BuildingPlanner INSTANCE = new BuildingPlanner();
    private final Map<String, Integer> inProgress = new LinkedHashMap<>();
    private final Random RNG = new Random();

    private BuildingPlanner() {}

    /**
     * Раз в день: продолжаем стройки, начинаем новые.
     * Получает мир, чтобы реально ставить блоки.
     */
    public void dailyPlan(ServerWorld world, BlockPos villageCenter,
                          int availableResources, Collection<VillagerCharacter> villagers) {
        // Продолжаем старые стройки — просто ускоряем прогресс
        for (Map.Entry<String, Integer> e : new ArrayList<>(inProgress.entrySet())) {
            int progress = e.getValue() + 25 + villagers.size() * 2;
            if (progress >= 100) {
                finishBuild(world, villageCenter, e.getKey());
                inProgress.remove(e.getKey());
            } else {
                e.setValue(progress);
            }
        }

        // Начинаем новую
        if (inProgress.size() < 2) {
            String next = pickNext(availableResources);
            if (next != null) {
                inProgress.put(next, 0);
                AnimaVillagers.LOGGER.info("[План] Начинаем стройку: {}", next);
            }
        }
    }

    private void finishBuild(ServerWorld world, BlockPos center, String name) {
        // Строим рядом с центром, со сдвигом
        BlockPos target = center.add(
            RNG.nextInt(40) - 20, 0, RNG.nextInt(40) - 20);

        switch (name) {
            case "хижина" -> StructureBuilder.buildHut(world, target);
            case "ферма" -> StructureBuilder.buildFarm(world, target);
            case "храм" -> StructureBuilder.buildTemple(world, target);
            case "замок" -> StructureBuilder.buildCastle(world, target);
            default -> AnimaVillagers.LOGGER.info("[План] {} — без реальной стройки", name);
        }
        VillageProgress.INSTANCE.structureBuilt(name);
    }

    private String pickNext(int resources) {
        return VillageProgress.INSTANCE.getBlueprints().values().stream()
            .filter(bp -> !inProgress.containsKey(bp.name))
            .filter(bp -> bp.era.ordinal() <= VillageProgress.INSTANCE.getCurrentEra().ordinal())
            .filter(bp -> bp.canBuild(resources))
            .sorted((a, b) -> {
                Map<String, Integer> prio = Map.of(
                    "хижина", 10, "ферма", 9, "храм", 7, "стена", 5, "замок", 3);
                return Integer.compare(
                    prio.getOrDefault(b.name, 1),
                    prio.getOrDefault(a.name, 1));
            })
            .map(bp -> bp.name)
            .findFirst().orElse(null);
    }

    public Map<String, Integer> getInProgress() { return inProgress; }
}
