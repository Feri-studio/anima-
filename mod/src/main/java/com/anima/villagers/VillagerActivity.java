package com.anima.villagers;

import java.util.*;

/**
 * Ежедневная деятельность жителя.
 * Кто-то пишет книги, кто-то патрулирует, кто-то строит, кто-то торгует.
 */
public class VillagerActivity {
    private static final Random RNG = new Random();

    public String currentActivity = "отдыхает";
    public int activityDay = 0;
    public String holdingItem = "empty";

    // Возможные занятия по роли
    private static final Map<String, String[]> ACTIVITIES = new HashMap<>();
    static {
        ACTIVITIES.put("воин", new String[]{
            "патрулирует границы", "тренируется с мечом", "охраняет лидера",
            "охотится на монстров", "строит дозорную башню"
        });
        ACTIVITIES.put("рабочий", new String[]{
            "рубит лес", "добывает камень", "строит дом", "чинит стены",
            "копает шахту", "ухаживает за фермой"
        });
        ACTIVITIES.put("жрец", new String[]{
            "пишет священную книгу", "проводит ритуал", "молится богу",
            "записывает историю деревни", "толкует сны"
        });
        ACTIVITIES.put("лидер", new String[]{
            "проводит собрание", "издаёт указ", "принимает послов",
            "инспектирует стены", "пишет летопись"
        });
        ACTIVITIES.put("крестьянин", new String[]{
            "работает в поле", "торгует на рынке", "готовит еду",
            "делает записи о погоде", "ухаживает за скотом", "плетёт корзины"
        });
    }

    // Предметы, соответствующие занятию
    private static final Map<String, String> ACTIVITY_ITEM = new HashMap<>();
    static {
        ACTIVITY_ITEM.put("патрулирует границы", "minecraft:iron_sword");
        ACTIVITY_ITEM.put("тренируется с мечом", "minecraft:iron_sword");
        ACTIVITY_ITEM.put("охраняет лидера", "minecraft:shield");
        ACTIVITY_ITEM.put("охотится на монстров", "minecraft:bow");
        ACTIVITY_ITEM.put("строит дозорную башню", "minecraft:stone_bricks");
        ACTIVITY_ITEM.put("рубит лес", "minecraft:iron_axe");
        ACTIVITY_ITEM.put("добывает камень", "minecraft:iron_pickaxe");
        ACTIVITY_ITEM.put("строит дом", "minecraft:oak_planks");
        ACTIVITY_ITEM.put("чинит стены", "minecraft:cobblestone");
        ACTIVITY_ITEM.put("копает шахту", "minecraft:iron_pickaxe");
        ACTIVITY_ITEM.put("ухаживает за фермой", "minecraft:iron_hoe");
        ACTIVITY_ITEM.put("пишет священную книгу", "minecraft:writable_book");
        ACTIVITY_ITEM.put("проводит ритуал", "minecraft:torch");
        ACTIVITY_ITEM.put("молится богу", "minecraft:lantern");
        ACTIVITY_ITEM.put("записывает историю деревни", "minecraft:book");
        ACTIVITY_ITEM.put("толкует сны", "minecraft:amethyst_shard");
        ACTIVITY_ITEM.put("проводит собрание", "minecraft:golden_helmet");
        ACTIVITY_ITEM.put("издаёт указ", "minecraft:paper");
        ACTIVITY_ITEM.put("принимает послов", "minecraft:emerald");
        ACTIVITY_ITEM.put("инспектирует стены", "minecraft:shield");
        ACTIVITY_ITEM.put("пишет летопись", "minecraft:book");
        ACTIVITY_ITEM.put("работает в поле", "minecraft:iron_hoe");
        ACTIVITY_ITEM.put("торгует на рынке", "minecraft:emerald");
        ACTIVITY_ITEM.put("готовит еду", "minecraft:bread");
        ACTIVITY_ITEM.put("делает записи о погоде", "minecraft:paper");
        ACTIVITY_ITEM.put("ухаживает за скотом", "minecraft:wheat");
        ACTIVITY_ITEM.put("плетёт корзины", "minecraft:stick");
    }

    /** Раз в день выбираем занятие по роли. */
    public void dailyTick(VillagerCharacter v, Settlement s) {
        String role = s != null ? s.getRole(v.entityUuid) : "крестьянин";
        String[] pool = ACTIVITIES.getOrDefault(role, ACTIVITIES.get("крестьянин"));

        // Иногда житель сам придумывает занятие через LLM
        if (RNG.nextInt(20) == 0) {
            String custom = askLlmForActivity(v, role);
            if (custom != null) {
                currentActivity = custom;
                holdingItem = guessItem(custom);
                AnimaVillagers.LOGGER.info("[Занятие] {} придумал: {}", v.name, custom);
                return;
            }
        }

        currentActivity = pool[RNG.nextInt(pool.length)];
        holdingItem = ACTIVITY_ITEM.getOrDefault(currentActivity, "minecraft:air");
        activityDay = 0;
    }

    private String askLlmForActivity(VillagerCharacter v, String role) {
        String system = "Ты — житель Minecraft. Отвечай ОДНОЙ короткой фразой на русском.";
        String user = String.format(
            "Житель %s, роль: %s, профессия: %s, черты: смелость=%.1f, ум=%.1f. " +
            "Чем он может заняться сегодня? Одно занятие, 2-4 слова.",
            v.name, role, v.profession,
            v.getTrait("смелость"), v.getTrait("ум"));
        String result = LlmClient.ask(system, user);
        return result != null ? result.trim().replaceAll("[\"']", "") : null;
    }

    private String guessItem(String activity) {
        String a = activity.toLowerCase();
        if (a.contains("меч") || a.contains("патрул")) return "minecraft:iron_sword";
        if (a.contains("коп") || a.contains("шахт")) return "minecraft:iron_pickaxe";
        if (a.contains("руб") || a.contains("лес")) return "minecraft:iron_axe";
        if (a.contains("стро") || a.contains("дом")) return "minecraft:oak_planks";
        if (a.contains("книг") || a.contains("запис")) return "minecraft:book";
        if (a.contains("торг") || a.contains("рынк")) return "minecraft:emerald";
        if (a.contains("моли") || a.contains("ритуал")) return "minecraft:lantern";
        return "minecraft:air";
    }
}
