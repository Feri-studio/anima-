package com.anima.villagers;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Показывает эмоции жителей в мире:
 *  - частицами
 *  - сообщением над головой (action bar)
 *  - звуками
 */
public class EmotionDisplay {

    /** Спавнит частицы над жителем. */
    public static void spawnParticles(ServerWorld world, double x, double y, double z,
                                       VillagerEmotions.Emotion emotion) {
        ParticleEffect particle = switch (emotion) {
            case JOY, LOVE -> ParticleTypes.HEART;
            case ANGER -> ParticleTypes.ANGRY_VILLAGER;
            case SADNESS -> ParticleTypes.SPLASH;
            case FEAR -> ParticleTypes.SMOKE;
            case SURPRISE -> ParticleTypes.CLOUD;
            case HOPE -> ParticleTypes.HAPPY_VILLAGER;
            case PRIDE -> ParticleTypes.FIREWORK;
            case DISGUST -> ParticleTypes.ITEM_SLIME;
            default -> ParticleTypes.HAPPY_VILLAGER;
        };

        for (int i = 0; i < 3; i++) {
            world.spawnParticles(particle,
                x + (Math.random() - 0.5) * 0.5,
                y + 2.2,
                z + (Math.random() - 0.5) * 0.5,
                1, 0, 0.1, 0, 0.02);
        }
    }

    /** Отправляет эмодзи игрокам в радиусе как всплывающий текст. */
    public static void showEmoji(ServerWorld world, double x, double y, double z,
                                  VillagerCharacter v) {
        if (v.emotions == null) return;

        int radius = 30;
        Text message = Text.literal(
            v.emotions.getColor() + v.name + " " + v.emotions.getEmoji() +
            " §7(" + v.emotions.getName() + ")");

        world.getPlayers(p ->
            p.squaredDistanceTo(x, y, z) <= radius * radius
        ).forEach(p -> p.sendMessage(message, true));  // action bar
    }

    /** Звук в зависимости от эмоции. */
    public static void playSound(ServerWorld world, double x, double y, double z,
                                  VillagerEmotions.Emotion emotion) {
        switch (emotion) {
            case ANGER -> world.playSound(null, x, y, z,
                net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_NO,
                net.minecraft.sound.SoundCategory.NEUTRAL, 1f, 0.8f);
            case JOY, LOVE -> world.playSound(null, x, y, z,
                net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES,
                net.minecraft.sound.SoundCategory.NEUTRAL, 1f, 1.2f);
            case FEAR -> world.playSound(null, x, y, z,
                net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_HURT,
                net.minecraft.sound.SoundCategory.NEUTRAL, 1f, 1.5f);
            default -> {}  // тишина
        }
    }

    /** Применяет зелье-эффекты для визуала. */
    public static void applyEffects(VillagerCharacter v) {
        // Пока просто логируем — эффекты навесим, когда будет сущность
    }

    /**
     * Полный цикл показа: частицы + эмодзи + звук.
     */
    public static void show(ServerWorld world, VillagerCharacter v) {
        if (v.emotions == null) return;
        spawnParticles(world, v.x, v.y, v.z, v.emotions.current);
        showEmoji(world, v.x, v.y, v.z, v);
        playSound(world, v.x, v.y, v.z, v.emotions.current);
    }
}
