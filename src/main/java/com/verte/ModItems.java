package com.verte;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Verte.MOD_ID);

    /** A small green box. Use it on a block to let Verte climb out; sneak-interact Verte to pick him back up. */
    public static final RegistryObject<Item> VERTE_BOX =
            ITEMS.register("verte_box", () -> new VerteBoxItem(new Item.Properties().stacksTo(1)));
}
