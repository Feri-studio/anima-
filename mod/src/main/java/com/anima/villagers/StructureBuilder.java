package com.anima.villagers;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class StructureBuilder {

    /**
     * Строит здание по карте от LLM.
     * Блоки ставятся один за другим, с логом.
     */
    public static void buildFromLlm(ServerWorld world, BlockPos origin,
                                     StructureGenerator.GeneratedStructure s) {
        if (s == null) return;

        int placed = 0;
        for (int x = 0; x < s.width; x++) {
            for (int y = 0; y < s.height; y++) {
                for (int z = 0; z < s.depth; z++) {
                    String blockId = s.blocks.get(x).get(y).get(z);
                    if (blockId.equals("air")) continue;

                    Block block = parseBlock(blockId);
                    BlockPos p = origin.add(x, y, z);
                    world.setBlockState(p, block.getDefaultState());
                    placed++;
                }
            }
        }

        AnimaVillagers.LOGGER.info("[Строитель] {} готов на {} ({} блоков)",
            s.name, origin, placed);
        TheKeeper.INSTANCE.alert("🏗️ " + s.name + " построен (" + placed + " блоков)");
    }

    private static Block parseBlock(String id) {
        try {
            Identifier ident = new Identifier("minecraft", id);
            Block b = Registries.BLOCK.get(ident);
            return b != null ? b : Blocks.STONE;
        } catch (Exception e) {
            return Blocks.STONE;
        }
    }

    // === Старые методы-шаблоны оставим для быстрых случаев ===

    public static void buildHut(ServerWorld world, BlockPos c) {
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) for (int y = 0; y < 4; y++) {
            BlockPos p = c.add(x, y, z);
            boolean wall = x==0||x==4||z==0||z==4;
            if (y == 0) world.setBlockState(p, Blocks.OAK_PLANKS.getDefaultState());
            else if (y == 3) world.setBlockState(p, Blocks.OAK_SLAB.getDefaultState());
            else if (wall) world.setBlockState(p, Blocks.OAK_LOG.getDefaultState());
        }
    }
}
