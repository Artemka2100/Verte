package com.verte;

import com.mojang.logging.LogUtils;
import com.verte.entity.ModEntities;
import com.verte.entity.VerteEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(new VerteCommand());
        MinecraftForge.EVENT_BUS.register(new VerteEvents());
        MinecraftForge.EVENT_BUS.register(new VerteChatHandler());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VerteConfig.SPEC);
        LOGGER.info("Verte awakens...");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.VERTE.get(), VerteEntity.createAttributes().build());
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.VERTE_BOX.get());
        }
    }
}
