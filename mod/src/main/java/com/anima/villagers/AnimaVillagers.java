package com.anima.villagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AnimaVillagers implements ModInitializer {
    public static final String MOD_ID = "anima-villagers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int ticksSinceLastDay = 0;
    private static final int TICKS_PER_DAY = 24000;
    private boolean started = false;

    @Override
    public void onInitialize() {
        LOGGER.info("Anima Villagers — души пробуждаются");
        AnimaConfig.load();
        ProfessionDatabase.load();
        SelfImprovement.INSTANCE.load();

        // Стартовые жители
        for (int i = 0; i < 5; i++) {
            VillagerCharacter v = CharacterGenerator.generate();
            UUID uuid = UUID.randomUUID();
            v.entityUuid = uuid;
            v.profession = ProfessionGenerator.randomProfession();
            TheKeeper.INSTANCE.observeVillager(uuid, v);
            LOGGER.info("[Житель] {} ({}, {} лет) — {}",
                v.name, v.profession, v.age, v.emotions.getEmoji());
        }

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

        LOGGER.info("Хранитель проснулся. {} душ, эпоха: {}, бог: {}, изобретений: {}",
            TheKeeper.INSTANCE.getPopulation(),
            VillageProgress.INSTANCE.getCurrentEra().displayName,
            God.INSTANCE.currentName(),
            SelfImprovement.INSTANCE.getCount());
    }

    private void onNewDay(ServerWorld world) {
        TheKeeper.INSTANCE.tick();

        float avgInt = 0f; int count = 0;
        for (VillagerCharacter v : TheKeeper.INSTANCE.getVillagers()) {
            avgInt += v.getTrait("ум"); count++;
        }
        if (count > 0) avgInt /= count;

        int day = VillageProgress.INSTANCE.getEraProgressDays();

        VillageProgress.INSTANCE.dailyTick(
            TheKeeper.INSTANCE.getPopulation(), avgInt,
            TheKeeper.INSTANCE.getResources());

        BuildingPlanner.INSTANCE.dailyPlan(
            world, world.getSpawnPos(),
            TheKeeper.INSTANCE.getResources(),
            TheKeeper.INSTANCE.getVillagers());

        God.INSTANCE.dailyTick(day,
            TheKeeper.INSTANCE.getPopulation(),
            TheKeeper.INSTANCE.getMood(),
            TheKeeper.INSTANCE.getCrisisType());

        Religion.INSTANCE.dailyTick(day,
            TheKeeper.INSTANCE.getVillagers(),
            God.INSTANCE.getFaith());

        Monarchy.INSTANCE.dailyTick(day, TheKeeper.INSTANCE.getVillagers());

        // Разум и эмоции каждого жителя
        for (VillagerCharacter v : TheKeeper.INSTANCE.getVillagers()) {
            v.mind.dailyTick(v, day, VillageProgress.INSTANCE);
            v.emotions.dailyTick(v);

            // Случайная эмоция раз в несколько дней
            if (Math.random() < 0.2) {
                String[] events = {"подарок", "удар", "победа", "еда", "новая идея"};
                v.emotions.react(v, events[(int)(Math.random() * events.length)]);
            }

            // Показываем эмоцию игрокам в мире
            EmotionDisplay.show(world, v);
        }

        SelfImprovement.INSTANCE.dailyTick(day,
            TheKeeper.INSTANCE.getPopulation(),
            VillageProgress.INSTANCE.getCurrentEra());
    }
}
