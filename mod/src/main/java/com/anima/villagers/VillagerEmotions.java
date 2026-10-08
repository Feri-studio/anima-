package com.anima.villagers;

import java.util.*;

/**
 * Эмоциональная система жителя.
 * 7 базовых эмоций + сложные состояния.
 */
public class VillagerEmotions {
    private static final Random RNG = new Random();

    public enum Emotion {
        NEUTRAL("😐", "§7", "нейтрально"),
        JOY("😊", "§e", "радость"),
        SADNESS("😢", "§9", "грусть"),
        ANGER("😡", "§c", "злость"),
        FEAR("😨", "§5", "страх"),
        SURPRISE("😲", "§b", "удивление"),
        LOVE("😍", "§d", "любовь"),
        DISGUST("🤢", "§2", "отвращение"),
        HOPE("🤩", "§a", "надежда"),
        PRIDE("😎", "§6", "гордость");

        public final String emoji;
        public final String color;
        public final String name;

        Emotion(String emoji, String color, String name) {
            this.emoji = emoji;
            this.color = color;
            this.name = name;
        }
    }

    public Emotion current = Emotion.NEUTRAL;
    public Emotion previous = Emotion.NEUTRAL;
    public int emotionTicks = 0;    // сколько дней эмоция держится

    /**
     * Реагирует на событие → меняет эмоцию.
     */
    public void react(VillagerCharacter v, String event) {
        Emotion newEmotion = calculate(v, event);
        if (newEmotion != current) {
            previous = current;
            current = newEmotion;
            emotionTicks = 0;
            AnimaVillagers.LOGGER.debug("[Эмоция] {} → {}", v.name, current.name);
        }
    }

    private Emotion calculate(VillagerCharacter v, String event) {
        float courage = v.getTrait("смелость");
        float kindness = v.getTrait("доброта");
        float aggression = v.getTrait("агрессия");
        float empathy = v.getTrait("эмпатия");

        switch (event.toLowerCase()) {
            case "подарок", "помощь":
                return kindness > 0.5f ? Emotion.JOY : Emotion.SURPRISE;
            case "удар", "атака":
                return aggression > 0.6f ? Emotion.ANGER : Emotion.FEAR;
            case "смерть друга":
                return empathy > 0.6f ? Emotion.SADNESS : Emotion.FEAR;
            case "победа", "успех":
                return courage > 0.6f ? Emotion.PRIDE : Emotion.JOY;
            case "предательство":
                return aggression > 0.6f ? Emotion.ANGER : Emotion.SADNESS;
            case "еда", "праздник":
                return Emotion.JOY;
            case "красивая девушка", "красивый парень":
                return Emotion.LOVE;
            case "грязь", "тухлятина":
                return Emotion.DISGUST;
            case "новая идея", "изобретение":
                return Emotion.HOPE;
            default:
                return Emotion.NEUTRAL;
        }
    }

    /** Эмоция постепенно угасает. */
    public void dailyTick(VillagerCharacter v) {
        emotionTicks++;
        // Через 3-5 дней эмоция уходит в нейтрал
        if (emotionTicks > 3 && RNG.nextInt(3) == 0) {
            previous = current;
            current = Emotion.NEUTRAL;
            emotionTicks = 0;
        }
    }

    public String getEmoji() { return current.emoji; }
    public String getColor() { return current.color; }
    public String getName() { return current.name; }
}
