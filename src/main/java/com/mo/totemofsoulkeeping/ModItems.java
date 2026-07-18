package com.mo.totemofsoulkeeping;

import com.mo.totemofsoulkeeping.item.TotemOfSoulKeepingItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TotemOfSoulKeeping.MOD_ID);

    public static final RegistryObject<Item> TOTEM_OF_SOUL_KEEPING =
            ITEMS.register("totem_of_soul_keeping", TotemOfSoulKeepingItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
