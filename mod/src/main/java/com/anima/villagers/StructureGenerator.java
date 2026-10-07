package com.anima.villagers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/**
 * LLM-генератор зданий.
 * Просит у LLM карту блоков -> сохраняет в кэш -> возвращает.
 * Один уникальный дом = один запрос к LLM.
 */
public class StructureGenerator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CACHE_DIR = Paths.get("config", "anima_structures");
    private static final Map<String, GeneratedStructure> MEMORY = new HashMap<>();

    public static class GeneratedStructure {
        public String name;
        public int width, height, depth;
        public List<List<List<String>>> blocks;  // [x][y][z] -> blockId
    }

    /**
     * Заказать здание у LLM.
     * @param description "дом кузнеца, средний, с трубой"
     */
    public static GeneratedStructure request(String description) {
        String hash = md5(description);

        // 1. Из кэша памяти
        if (MEMORY.containsKey(hash)) return MEMORY.get(hash);

        // 2. Из файла
        Path file = CACHE_DIR.resolve(hash + ".json");
        if (Files.exists(file)) {
            try {
                GeneratedStructure s = GSON.fromJson(Files.readString(file),
                    GeneratedStructure.class);
                MEMORY.put(hash, s);
                AnimaVillagers.LOGGER.info("[Генератор] Из кэша: {}", s.name);
                return s;
            } catch (Exception ignored) {}
        }

        // 3. Запрос к LLM
        String system = "Ты — архитектор Minecraft. Верни ТОЛЬКО JSON без пояснений.";
        String user = String.format(
            "Спроектируй здание: %s. " +
            "Размеры максимум 10x10x10. " +
            "Верни JSON: {\"name\":\"...\",\"width\":N,\"height\":N,\"depth\":N," +
            "\"layers\":[[[блоки для слоя y=0]],[[...y=1]],...]}. " +
            "Блоки: oak_planks, oak_log, stone, stone_bricks, glass, oak_door, " +
            "oak_stairs, oak_fence, air, torch, crafting_table, furnace, bookshelf. " +
            "Каждый слой — 2D массив [x][z]. Все размеры точно соответствуют width/height/depth.",
            description
        );

        String raw = LlmClient.ask(system, user);
        if (raw == null) return null;

        try {
            // Чистим от markdown-обёрток
            raw = raw.replaceAll("```json\\s*", "")
                     .replaceAll("```\\s*", "").trim();
            GeneratedStructure s = GSON.fromJson(raw, GeneratedStructure.class);
            if (s == null || s.blocks == null) return null;

            // Валидация
            s.blocks = validate(s);

            MEMORY.put(hash, s);
            try {
                Files.createDirectories(CACHE_DIR);
                Files.writeString(file, GSON.toJson(s));
            } catch (Exception e) {
                AnimaVillagers.LOGGER.warn("Не сохранил кэш здания: {}", e.getMessage());
            }

            AnimaVillagers.LOGGER.info("[Генератор] LLM спроектировал: {} ({}x{}x{})",
                s.name, s.width, s.height, s.depth);
            TheKeeper.INSTANCE.alert("🎨 ИИ спроектировал: " + s.name);
            return s;
        } catch (Exception e) {
            AnimaVillagers.LOGGER.error("LLM вернул битый JSON: {}", e.getMessage());
            return null;
        }
    }

    /** Промпт конкретно под эпоху и профессию */
    public static GeneratedStructure requestFor(String profession, Era era) {
        String desc = String.format(
            "здание для %s, стиль %s", profession, era.displayName);
        return request(desc);
    }

    /** Проверка и коррекция размеров/блоков. */
    private static List<List<List<String>>> validate(GeneratedStructure s) {
        List<List<List<String>>> out = new ArrayList<>();
        for (int x = 0; x < s.width; x++) {
            List<List<String>> plane = new ArrayList<>();
            for (int y = 0; y < s.height; y++) {
                List<String> row = new ArrayList<>();
                for (int z = 0; z < s.depth; z++) {
                    String block = "air";
                    try {
                        block = s.blocks.get(x).get(y).get(z);
                    } catch (Exception ignored) {}
                    row.add(sanitize(block));
                }
                plane.add(row);
            }
            out.add(plane);
        }
        return out;
    }

    private static String sanitize(String block) {
        if (block == null || block.isEmpty()) return "air";
        String b = block.toLowerCase().replace("minecraft:", "").trim();
        return switch (b) {
            case "oak_planks", "oak_log", "stone", "stone_bricks", "glass",
                 "oak_door", "oak_stairs", "oak_fence", "torch",
                 "crafting_table", "furnace", "bookshelf",
                 "cobblestone", "dirt", "grass_block", "spruce_planks",
                 "spruce_log", "birch_planks", "birch_log", "air",
                 "coal_ore", "iron_ore", "gold_ore", "quartz_block" -> b;
            default -> "stone";
        };
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "default"; }
    }
}
