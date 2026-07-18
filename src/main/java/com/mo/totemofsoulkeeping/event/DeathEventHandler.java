package com.mo.totemofsoulkeeping.event;

import com.mo.totemofsoulkeeping.ModConfigs;
import com.mo.totemofsoulkeeping.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
    private static final String TAG_ARMOR = "ArmorSlots";

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
                // 消耗一个图腾（直接修改槽位引用，因此后续 LivingDropsEvent 不会再收集到该图腾）
                stack.shrink(1);
                found = true;
                break;
            }
        }

        if (found) {
            // 记录玩家盔甲槽位(100~103)和副手槽位(-106)的nbt信息，供重生时智能还原使用
            CompoundTag armorSlots = new CompoundTag();
            recordSlotNBT(armorSlots, "100", player.getItemBySlot(EquipmentSlot.FEET));
            recordSlotNBT(armorSlots, "101", player.getItemBySlot(EquipmentSlot.LEGS));
            recordSlotNBT(armorSlots, "102", player.getItemBySlot(EquipmentSlot.CHEST));
            recordSlotNBT(armorSlots, "103", player.getItemBySlot(EquipmentSlot.HEAD));
            recordSlotNBT(armorSlots, "-106", player.getItemBySlot(EquipmentSlot.OFFHAND));

            CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
            CompoundTag rescue = persisted.getCompound(NBT_KEY);
            rescue.putBoolean("hastotem", true);
            rescue.put(TAG_ARMOR, armorSlots);
            persisted.put(NBT_KEY, rescue);
        }
    }

    /** 将非空槽位的 ItemStack 序列化到指定键下 */
    private static void recordSlotNBT(CompoundTag tag, String key, ItemStack stack) {
        if (!stack.isEmpty()) {
            tag.put(key, stack.save(new CompoundTag()));
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

        CompoundTag persistent = player.getPersistentData()
                .getCompound(Player.PERSISTED_NBT_TAG);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
        // 复用①中已记录的复合标签（保留 hastotem 标记与盔甲/副手 nbt），仅追加掉落物与经验
        CompoundTag payload = persistent.getCompound(NBT_KEY);
        payload.putInt(TAG_COUNT, rescued.size());
        for (int i = 0; i < rescued.size(); i++) {
            payload.put(TAG_ITEM + i, rescued.get(i).save(new CompoundTag()));
        }
        payload.putInt(TAG_XP, player.totalExperience);
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
        CompoundTag armorSlots = payload.getCompound(TAG_ARMOR);

        for (int i = 0; i < count; i++) {
            CompoundTag tag = payload.getCompound(TAG_ITEM + i);
            ItemStack stack = ItemStack.of(tag);
            if (stack.isEmpty()) {
                continue;
            }
            returnToPlayer(player, stack, armorSlots);
        }

        if (ModConfigs.KEEP_EXPERIENCE.get()) {
            player.giveExperiencePoints(payload.getInt(TAG_XP));
        }

        player.sendSystemMessage(Component.translatable("message.totem_of_soul_keeping.rescued"));
        persistent.remove(NBT_KEY);
    }

    /**
     * 智能还原：读取①中记录的盔甲/副手 nbt，若归还物品与某记录槽位匹配，
     * 且玩家当前该槽位为空，则归还到对应槽位；否则放入背包，背包满则掉落。
     */
    private static void returnToPlayer(Player player, ItemStack stack, CompoundTag armorSlots) {
        for (String slotKey : armorSlots.getAllKeys()) {
            ItemStack recordedStack = ItemStack.of(armorSlots.getCompound(slotKey));
            if (!recordedStack.isEmpty() && ItemStack.isSameItemSameTags(stack, recordedStack)) {
                EquipmentSlot slot = slotIdToEquipmentSlot(Integer.parseInt(slotKey));
                if (slot != null && player.getItemBySlot(slot).isEmpty()) {
                    player.setItemSlot(slot, stack.copy());
                    return;
                }
            }
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, true);
        }
    }

    /** NBT 槽位编号 -> 装备槽位（与 Inventory.save 的编号一致） */
    private static EquipmentSlot slotIdToEquipmentSlot(int slotId) {
        switch (slotId) {
            case 100: return EquipmentSlot.FEET;
            case 101: return EquipmentSlot.LEGS;
            case 102: return EquipmentSlot.CHEST;
            case 103: return EquipmentSlot.HEAD;
            case -106: return EquipmentSlot.OFFHAND;
            default: return null;
        }
    }
}
