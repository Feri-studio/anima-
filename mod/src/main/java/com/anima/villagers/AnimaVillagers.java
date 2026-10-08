package com.anima.villagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AnimaVillagers implements ModInitializer {
    public static final String MOD_ID = "anima-villagers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int ticksSinceLastDay = 0;
    private static final int TICKS_PER_DAY = 24000;
    private boolean started = false;

    // Держим активности отдельно
    private static final java.util.Map<UUID, VillagerActivity> ACTIVITIES = new java.util.HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Anima Villagers — души пробуждаются");
        AnimaConfig.load();
        ProfessionDatabase.load();
        SelfImprovement.INSTANCE.load();
        SettlementManager.INSTANCE.init();

        // Создаём 300 жителей (по 100 на поселение)
        Settlement[] list = SettlementManager.INSTANCE.getSettlements().toArray(new Settlement[0]);
        for (int s = 0; s < list.length; s++) {
            Settlement settlement = list[s];
            for (int i = 0; i < 100; i++) {
                VillagerCharacter v = CharacterGenerator.generate();
                UUID uuid = UUID.randomUUID();
                v.entityUuid = uuid;
                v.profession = ProfessionGenerator.randomProfession();

                // Ставим координаты около центра поселения
                v.x = settlement.centerX + (Math.random() - 0.5) * 80;
                v.z = settlement.centerZ + (Math.random() - 0.5) * 80;
                v.y = 65;

                settlement.addCitizen(uuid);
                TheKeeper.INSTANCE.observeVillager(uuid, v);
                ACTIVITIES.put(uuid, new VillagerActivity());
            }
            LOGGER.info("{}: 100 жителей создано", settlement.name);
        }

        // Выборы лидеров сразу
        SettlementManager.INSTANCE.dailyTick(TheKeeper.INSTANCE.getVillagers());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!started && server.getTicks() > 200) {
                started = true;
                new Thread(() -> {
                    ProfessionGenerator.fillTo2000();
                    Religion.INSTANCE.generateCommandments();
                }, "Anima-Bootstrap").start();
            }
            ticksSinceLastDay++;
            if (ticksSinceLastDay >= TICKS_PER_DAY) {
                ticksSinceLastDay = 0;
                onNewDay(server.getOverworld());
            }
        });

        LOGGER.info("Хранитель проснулся. {} душ в {} поселениях",
            TheKeeper.INSTANCE.getPopulation(),
            SettlementManager.INSTANCE.getSettlements().size());
    }

    private void onNewDay(ServerWorld world) {
        TheKeeper.INSTANCE.tick();

        int day = VillageProgress.INSTANCE.getEraProgressDays();

        // Поселения
        SettlementManager.INSTANCE.dailyTick(TheKeeper.INSTANCE.getVillagers());

        // Каждый житель: разум, эмоции, занятие
        for (VillagerCharacter v : TheKeeper.INSTANCE.getVillagers()) {
            Settlement s = SettlementManager.INSTANCE.getSettlementOf(v.entityUuid);
            VillagerActivity act = ACTIVITIES.computeIfAbsent(v.entityUuid,
                k -> new VillagerActivity());

            v.mind.dailyTick(v, day, VillageProgress.INSTANCE);
            v.emotions.dailyTick(v);
            act.dailyTick(v, s);

            // Показываем эмоцию и предмет
            EmotionDisplay.show(world, v);
            VillagerEquipment.showHeldItem(world, v, act);
        }

        // Глобальные системы
        VillageProgress.INSTANCE.dailyTick(
            TheKeeper.INSTANCE.getPopulation(), 0.5f,
            TheKeeper.INSTANCE.getResources());

        God.INSTANCE.dailyTick(day,
            TheKeeper.INSTANCE.getPopulation(),
            TheKeeper.INSTANCE.getMood(),
            TheKeeper.INSTANCE.getCrisisType());

        SelfImprovement.INSTANCE.dailyTick(day,
            TheKeeper.INSTANCE.getPopulation(),
            VillageProgress.INSTANCE.getCurrentEra());
    }
}
