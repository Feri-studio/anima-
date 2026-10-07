package com.anima.villagers;

public class ProfessionGenerator {
    public static String randomProfession() {
        return ProfessionDatabase.random();
    }

    /** Вызывается раз в старте мира — генерим ещё 200 профессий. */
    public static void generateMore(int count) {
        ProfessionDatabase.generateBatch(count);
    }

    /** Догоняем базу до 2000 профессий за несколько вызовов. */
    public static void fillTo2000() {
        int target = 2000;
        int batches = 0;
        while (ProfessionDatabase.size() < target && batches < 20) {
            int need = Math.min(200, target - ProfessionDatabase.size());
            ProfessionDatabase.generateBatch(need);
            batches++;
        }
        AnimaVillagers.LOGGER.info("Финальная база профессий: {}", ProfessionDatabase.size());
    }
}
