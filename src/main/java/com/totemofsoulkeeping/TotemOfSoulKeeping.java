package com.totemofsoulkeeping;

import com.mojang.logging.LogUtils;
import com.totemofsoulkeeping.event.DeathEventHandler;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TotemOfSoulKeeping.MOD_ID)
public class TotemOfSoulKeeping {

    public static final String MOD_ID = "totem_of_soul_keeping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TotemOfSoulKeeping() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modBus);
        modBus.addListener(this::onBuildCreativeTab);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ModConfigs.SPEC, "totem-of-soul-keeping-common.toml");

        MinecraftForge.EVENT_BUS.register(DeathEventHandler.class);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.TOTEM_OF_SOUL_KEEPING.get());
        }
    }
}
