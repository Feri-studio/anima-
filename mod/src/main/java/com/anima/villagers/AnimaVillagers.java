package com.anima.villagers;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnimaVillagers implements ModInitializer {
    public static final String MOD_ID = "anima-villagers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Anima Villagers — души пробудились");
    }
}
