package com.anima.villagers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Локальный чат жителей. Слышно только в радиусе N блоков.
 * По умолчанию — 50 блоков. Можно менять.
 */
public class VillagerChat {

    public static final int DEFAULT_RADIUS = 50;   // блоков
    public static final int SHOUT_RADIUS = 100;    // если кричит
    public static final int WHISPER_RADIUS = 10;   // если шепчет

    public enum Volume {
        WHISPER, NORMAL, SHOUT
    }

    /**
     * Житель говорит что-то в радиусе.
     */
    public static void say(VillagerCharacter speaker, String message, ServerWorld world) {
        say(speaker, message, world, Volume.NORMAL);
    }

    public static void say(VillagerCharacter speaker, String message,
                           ServerWorld world, Volume volume) {
        if (speaker == null || message == null || message.isEmpty()) return;

        int radius = switch (volume) {
            case WHISPER -> WHISPER_RADIUS;
            case SHOUT -> SHOUT_RADIUS;
            default -> DEFAULT_RADIUS;
        };

        // Формат: <Бран> Привет, путник
        String prefix = switch (volume) {
            case WHISPER -> "[шёпот] ";
            case SHOUT -> "[крик] ";
            default -> "";
        };
        Text chatMessage = Text.literal("§e<" + speaker.name + "> §f" + prefix + message);

        // Отправляем всем игрокам в радиусе
        List<ServerPlayerEntity> nearby = world.getPlayers(p ->
            p.squaredDistanceTo(speaker.x, speaker.y, speaker.z) <= radius * radius
        );

        for (ServerPlayerEntity player : nearby) {
            player.sendMessage(chatMessage, false);
        }

        AnimaVillagers.LOGGER.info("[Чат/{}] <{}> {}{}",
            volume, speaker.name, prefix, message);
    }

    /**
     * Житель говорит другому жителю. Слышат только те, кто рядом с обоими.
     */
    public static void sayTo(VillagerCharacter speaker, VillagerCharacter listener,
                             String message, ServerWorld world) {
        int radius = 30;
        Text chatMessage = Text.literal(
            "§e<" + speaker.name + " → " + listener.name + "> §f" + message);

        world.getPlayers(p -> p.squaredDistanceTo(speaker.x, speaker.y, speaker.z)
                <= radius * radius)
            .forEach(p -> p.sendMessage(chatMessage, false));
    }

    /**
     * Житель реагирует на игрока — отвечает в чат.
     */
    public static void replyTo(ServerPlayerEntity player,
                               VillagerCharacter speaker,
                               String message) {
        Text msg = Text.literal("§e<" + speaker.name + "> §f" + message);
        player.sendMessage(msg, false);
    }
}
