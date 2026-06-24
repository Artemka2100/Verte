package com.verte;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Verte.MOD_ID)
public class Verte {
    public static final String MOD_ID = "verte";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Verte() {
        MinecraftForge.EVENT_BUS.register(new VerteCommand());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VerteConfig.SPEC);
        LOGGER.info("Verte awakens...");
    }
}
