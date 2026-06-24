package com.verte.entity;

import com.verte.Verte;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Verte.MOD_ID);

    public static final RegistryObject<EntityType<VerteEntity>> VERTE =
            ENTITY_TYPES.register("verte", () -> EntityType.Builder.of(VerteEntity::new, MobCategory.CREATURE)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(10)
                    .build("verte"));
}
