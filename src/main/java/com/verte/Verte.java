package com.verte;

import com.mojang.logging.LogUtils;
import com.verte.entity.ModEntities;
import com.verte.entity.VerteEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Verte.MOD_ID)
public class Verte {
    public static final String MOD_ID = "verte";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Verte() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(this::registerAttributes);

        MinecraftForge.EVENT_BUS.register(new VerteCommand());
        MinecraftForge.EVENT_BUS.register(new VerteEvents());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VerteConfig.SPEC);
        LOGGER.info("Verte awakens...");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VERTE.get(), VerteEntity.createAttributes().build());
    }
}
