package com.mo.totemofsoulkeeping.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TotemOfSoulKeepingItem extends Item {

    public TotemOfSoulKeepingItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.totem_of_soul_keeping.totem_of_soul_keeping.line1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.totem_of_soul_keeping.totem_of_soul_keeping.line2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.totem_of_soul_keeping.totem_of_soul_keeping.line3").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.totem_of_soul_keeping.totem_of_soul_keeping").withStyle(ChatFormatting.AQUA);
    }
}
