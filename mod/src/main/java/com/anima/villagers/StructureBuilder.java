package com.anima.villagers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Реальный строитель. Ставит блоки в мире по чертежу.
 * Используется, когда житель получает задание "построить X".
 */
public class StructureBuilder {
    private static final Random RNG = new Random();

    /** Ставит "хижину" — 5x4x5 деревянный домик с дверью. */
    public static void buildHut(ServerWorld world, BlockPos center) {
        int w = 5, h = 4, d = 5;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                for (int y = 0; y < h; y++) {
                    BlockPos p = center.add(x, y, z);

                    // Стены
                    boolean isWall = (x == 0 || x == w - 1 || z == 0 || z == d - 1);
                    boolean isFloor = (y == 0);
                    boolean isRoof = (y == h - 1);

                    if (isFloor) {
                        world.setBlockState(p, Blocks.OAK_PLANKS.getDefaultState());
                    } else if (isRoof) {
                        world.setBlockState(p, Blocks.OAK_SLAB.getDefaultState());
                    } else if (isWall) {
                        // окно или бревно
                        if (y == 2 && RNG.nextInt(3) == 0) {
                            world.setBlockState(p, Blocks.GLASS.getDefaultState());
                        } else {
                            world.setBlockState(p, Blocks.OAK_LOG.getDefaultState());
                        }
                    }
                }
            }
        }

        // Дверь
        BlockPos doorPos = center.add(2, 1, 0);
        world.setBlockState(doorPos, Blocks.OAK_DOOR.getDefaultState());
        world.setBlockState(doorPos.up(), Blocks.OAK_DOOR.getDefaultState());

        AnimaVillagers.LOGGER.info("[Строитель] Хижина готова на {}", center);
        TheKeeper.INSTANCE.alert("🏠 Построена хижина " + center);
    }

    /** Ставит "ферму" — 10x1x10 грядки с водой в центре. */
    public static void buildFarm(ServerWorld world, BlockPos center) {
        int size = 10;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                BlockPos p = center.add(x, 0, z);

                // Канавки с водой по краям
                if (x == 0 || z == 0 || x == size - 1 || z == size - 1) {
                    world.setBlockState(p, Blocks.WATER.getDefaultState());
                } else {
                    // Чередование грядок и воздуха
                    if ((x + z) % 2 == 0) {
                        world.setBlockState(p, Blocks.FARMLAND.getDefaultState());
                        world.setBlockState(p.up(), Blocks.WHEAT.getDefaultState());
                    } else {
                        world.setBlockState(p, Blocks.WATER.getDefaultState());
                    }
                }
            }
        }
        // Забор
        for (int x = -1; x <= size; x++) {
            world.setBlockState(center.add(x, 1, -1), Blocks.OAK_FENCE.getDefaultState());
            world.setBlockState(center.add(x, 1, size), Blocks.OAK_FENCE.getDefaultState());
        }
        for (int z = -1; z <= size; z++) {
            world.setBlockState(center.add(-1, 1, z), Blocks.OAK_FENCE.getDefaultState());
            world.setBlockState(center.add(size, 1, z), Blocks.OAK_FENCE.getDefaultState());
        }

        AnimaVillagers.LOGGER.info("[Строитель] Ферма готова на {}", center);
        TheKeeper.INSTANCE.alert("🌾 Построена ферма " + center);
    }

    /** Ставит "замок" — большая крепость со стенами, башнями и воротами. */
    public static void buildCastle(ServerWorld world, BlockPos center) {
        int size = 30;
        int wallH = 8;

        // Стены по периметру
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                boolean isWall = (x == 0 || x == size - 1 || z == 0 || z == size - 1);
                if (!isWall) continue;

                for (int y = 0; y < wallH; y++) {
                    world.setBlockState(center.add(x, y, z),
                        Blocks.STONE_BRICKS.getDefaultState());
                }
                // Зубцы сверху
                if ((x + z) % 2 == 0) {
                    world.setBlockState(center.add(x, wallH, z),
                        Blocks.STONE_BRICK_WALL.getDefaultState());
                }
            }
        }

        // 4 башни по углам
        int[][] corners = {{0, 0}, {0, size - 1}, {size - 1, 0}, {size - 1, size - 1}};
        for (int[] c : corners) {
            for (int y = 0; y < wallH + 6; y++) {
                for (int dx = 0; dx < 3; dx++) {
                    for (int dz = 0; dz < 3; dz++) {
                        world.setBlockState(center.add(c[0] + dx, y, c[1] + dz),
                            Blocks.STONE_BRICKS.getDefaultState());
                    }
                }
            }
        }

        // Ворота (двойная арка)
        int gateX = size / 2;
        for (int y = 1; y <= 3; y++) {
            world.setBlockState(center.add(gateX, y, 0), Blocks.AIR.getDefaultState());
            world.setBlockState(center.add(gateX + 1, y, 0), Blocks.AIR.getDefaultState());
        }
        world.setBlockState(center.add(gateX, 4, 0), Blocks.OAK_FENCE.getDefaultState());

        AnimaVillagers.LOGGER.info("[Строитель] ЗАМОК готов на {}", center);
        TheKeeper.INSTANCE.alert("🏰 Построен ЗАМОК " + center);
    }

    /** Ставит храм — 7x7 пирамидка из камня. */
    public static void buildTemple(ServerWorld world, BlockPos center) {
        int base = 7;
        for (int level = 0; level < 4; level++) {
            int s = base - level * 2;
            for (int x = 0; x < s; x++) {
                for (int z = 0; z < s; z++) {
                    world.setBlockState(
                        center.add(x + level, level, z + level),
                        Blocks.QUARTZ_BLOCK.getDefaultState());
                }
            }
        }
        // Алтарь в центре
        world.setBlockState(center.add(3, 4, 3), Blocks.GOLD_BLOCK.getDefaultState());
        world.setBlockState(center.add(3, 5, 3), Blocks.LANTERN.getDefaultState());

        Religion.INSTANCE.buildTemple();

        AnimaVillagers.LOGGER.info("[Строитель] Храм готов на {}", center);
        TheKeeper.INSTANCE.alert("🛐 Построен храм " + center);
    }
}
