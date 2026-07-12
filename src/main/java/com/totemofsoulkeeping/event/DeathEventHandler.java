package com.totemofsoulkeeping.event;

import com.totemofsoulkeeping.ModConfigs;
import com.totemofsoulkeeping.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class DeathEventHandler {

    private static final String NBT_KEY = "TotemOfSoulKeeping_Rescue";
    private static final String TAG_COUNT = "Count";
    private static final String TAG_ITEM = "Item";
    private static final String TAG_XP = "Xp";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 动态构建槽位列表（内置）
        int[] slots = new int[36 + 4 + 1];
        int idx = 0;
        for (int i = 0; i <= 35; i++) slots[idx++] = i;
        for (int i = 100; i <= 103; i++) slots[idx++] = i;
        slots[idx] = -106;

        boolean found = false;
        for (int slot : slots) {
            ItemStack stack = ItemStack.EMPTY;
            if (slot == -106) {
                stack = player.getOffhandItem();
            } else if (slot < player.getInventory().getContainerSize()) {
                stack = player.getInventory().getItem(slot);
            }
            if (!stack.isEmpty() && stack.getItem() == ModItems.TOTEM_OF_SOUL_KEEPING.get()) {
                found = true;
                break;
            }
        }

        if (found) {
            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            CompoundTag hastotem = persisted.getCompound(NBT_KEY);
            hastotem.putBoolean("hastotem", true);
            persisted.put(NBT_KEY, hastotem);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!ModConfigs.KEEP_EXPERIENCE.get()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(NBT_KEY).getBoolean("hastotem")) {
            event.setCanceled(true);   // 保留经验
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.isCanceled() || event.getDrops().isEmpty()) {
            return;
        }
        if (!player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(NBT_KEY).getBoolean("hastotem")) {
            return;
        }

        List<ItemStack> rescued = new ArrayList<>();
        for (ItemEntity entity : event.getDrops()) {
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) {
                rescued.add(stack.copy());
            }
        }
        if (rescued.isEmpty()) {
            return;
        }

        CompoundTag payload = new CompoundTag();
        payload.putInt(TAG_COUNT, rescued.size());
        for (int i = 0; i < rescued.size(); i++) {
            payload.put(TAG_ITEM + i, rescued.get(i).save(new CompoundTag()));
        }
        payload.putInt(TAG_XP, player.totalExperience);

        CompoundTag persistent = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
        persistent.put(NBT_KEY, payload);

        event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persistent.contains(NBT_KEY)) {
            return;
        }
        CompoundTag payload = persistent.getCompound(NBT_KEY);
        int count = payload.getInt(TAG_COUNT);

        boolean totemConsumed = false;
        for (int i = 0; i < count; i++) {
            CompoundTag tag = payload.getCompound(TAG_ITEM + i);
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            if (!totemConsumed && stack.getItem() == ModItems.TOTEM_OF_SOUL_KEEPING.get()) {
                stack.shrink(1);
                totemConsumed = true;
                if (stack.isEmpty()) {
                    continue;
                }
            }
            returnToPlayer(player, stack);
        }

        if (ModConfigs.KEEP_EXPERIENCE.get()) {
            player.giveExperiencePoints(payload.getInt(TAG_XP));
        }

        player.sendSystemMessage(Component.translatable("message.totem_of_soul_keeping.rescued"));
        persistent.remove(NBT_KEY);
    }

    private static void returnToPlayer(Player player, ItemStack stack) {
        if (stack.getItem() instanceof ArmorItem) {
            EquipmentSlot slot = Mob.getEquipmentSlotForItem(stack);
            if (player.getItemBySlot(slot).isEmpty() && slot.getType() == EquipmentSlot.Type.ARMOR) {
                player.setItemSlot(slot, stack.copy());
                return;
            }
        } else if (stack.getItem() instanceof ShieldItem) {
            if (player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
                player.setItemSlot(EquipmentSlot.OFFHAND, stack.copy());
                return;
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, true);
        }
    }
}