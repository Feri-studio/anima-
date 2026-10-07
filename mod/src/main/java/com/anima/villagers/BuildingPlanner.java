package com.anima.villagers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BuildingPlanner {
    public static final BuildingPlanner INSTANCE = new BuildingPlanner();
    private final Map<String, Integer> inProgress = new LinkedHashMap<>();
    private final Random RNG = new Random();

    private BuildingPlanner() {}

    public void dailyPlan(ServerWorld world, BlockPos center,
                          int resources, Collection<VillagerCharacter> villagers) {
        // Прогресс
        for (Map.Entry<String, Integer> e : new ArrayList<>(inProgress.entrySet())) {
            int p = e.getValue() + 25 + villagers.size() * 2;
            if (p >= 100) {
                finish(world, center, e.getKey(), villagers);
                inProgress.remove(e.getKey());
            } else e.setValue(p);
        }

        if (inProgress.size() < 2) {
            String next = pickNext(resources);
            if (next != null) {
                inProgress.put(next, 0);
                AnimaVillagers.LOGGER.info("[План] Начинаем: {}", next);
            }
        }
    }

    private void finish(ServerWorld world, BlockPos center, String name,
                         Collection<VillagerCharacter> villagers) {
        BlockPos target = center.add(RNG.nextInt(60) - 30, 0, RNG.nextInt(60) - 30);

        // Подбираем "заказчика" — жителя нужной профессии
        String profession = guessProfession(name);
        String description = String.format(
            "здание для %s в стиле %s, %s", profession,
            VillageProgress.INSTANCE.getCurrentEra().displayName, name);

        // LLM проектирует
        StructureGenerator.GeneratedStructure s = StructureGenerator.request(description);
        if (s != null) {
            StructureBuilder.buildFromLlm(world, target, s);
        } else {
            AnimaVillagers.LOGGER.warn("[План] LLM не дал чертёж для {}", name);
        }
        VillageProgress.INSTANCE.structureBuilt(name);
    }

    private String guessProfession(String building) {
        return switch (building) {
            case "кузница" -> "кузнец";
            case "храм" -> "жрец";
            case "рынок" -> "торговец";
            case "казарма" -> "стражник";
            case "замок" -> "король";
            case "собор", "академия" -> "учёный";
            default -> "строитель";
        };
    }

    private String pickNext(int resources) {
        return VillageProgress.INSTANCE.getBlueprints().values().stream()
            .filter(bp -> !inProgress.containsKey(bp.name))
            .filter(bp -> bp.era.ordinal() <= VillageProgress.INSTANCE.getCurrentEra().ordinal())
            .filter(bp -> bp.canBuild(resources))
            .map(bp -> bp.name)
            .findFirst().orElse(null);
    }

    public Map<String, Integer> getInProgress() { return inProgress; }
}
