package com.infestationpotion;

import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * Called from the coremod (see coremods/armorstand.js) at the start of ArmorStandEntity.hurt.
 * When this returns true, vanilla's armor stand damage code is skipped.
 *
 * Fire behaviour of an armor stand (20 health = 10 hearts):
 *  - Touching a fire block / campfire (IN_FIRE) deals NO direct damage, it only sets the stand on fire.
 *  - The damage comes from the burning itself (ON_FIRE): 1 health per second, like a player.
 *  - Water extinguishes the stand every tick, so a stand that keeps being re-ignited under water only
 *    gets a brief burn: 1 health every few seconds (config), so it dies much slower.
 *  - Lava and magma hurt directly like for a player (at most one hit every 0.5 s).
 * Every time the stand takes damage, an infested stand has its chance to spawn silverfish.
 * Every other kind of damage keeps the vanilla behaviour.
 */
public class ArmorStandDamage {
    private static final String LAST_HIT_KEY = "InfestationLastFireHit";
    private static final String LAST_WATER_BURN_KEY = "InfestationLastWaterBurn";

    public static boolean handleFire(ArmorStandEntity stand, DamageSource source, float amount) {
        if (stand.level.isClientSide || !source.isFire() || !stand.isAlive()) {
            return false;
        }
        if (!enabled()) {
            return false;
        }
        if (stand.isMarker() || stand.isInvulnerableTo(source)) {
            return true; // vanilla ignores damage here too
        }

        long now = stand.level.getGameTime();
        CompoundNBT data = stand.getPersistentData();

        if (source == DamageSource.IN_FIRE) {
            if (stand.isInWater()) {
                // Water puts the fire out every tick: only a brief burn every few seconds.
                long interval = waterBurnIntervalTicks();
                if (!data.contains(LAST_WATER_BURN_KEY) || now - data.getLong(LAST_WATER_BURN_KEY) >= interval) {
                    data.putLong(LAST_WATER_BURN_KEY, now);
                    damage(stand, 1.0F);
                }
            } else if (stand.getRemainingFireTicks() <= 0) {
                stand.setSecondsOnFire(5); // contact only ignites, no direct damage
            }
            return true;
        }

        // ON_FIRE (burning), LAVA, HOT_FLOOR (magma): damage like a player, one hit per 0.5 s.
        if (data.contains(LAST_HIT_KEY) && now - data.getLong(LAST_HIT_KEY) < 10L) {
            return true;
        }
        data.putLong(LAST_HIT_KEY, now);
        damage(stand, amount);
        return true;
    }

    private static void damage(ArmorStandEntity stand, float amount) {
        float newHealth = stand.getHealth() - amount;
        if (newHealth > 0.0F) {
            stand.setHealth(newHealth);
            InfestationEvents.trySpawnSilverfish(stand); // infested stands spawn silverfish when hurt
        } else {
            breakStand(stand);
        }
    }

    private static boolean enabled() {
        try {
            return InfestationConfig.ARMOR_STAND_PLAYER_LIKE_FIRE.get();
        } catch (IllegalStateException e) {
            return true; // config not loaded yet: use the default
        }
    }

    private static long waterBurnIntervalTicks() {
        try {
            return InfestationConfig.ARMOR_STAND_WATER_BURN_SECONDS.get() * 20L;
        } catch (IllegalStateException e) {
            return 100L;
        }
    }

    private static void breakStand(ArmorStandEntity stand) {
        World level = stand.level;
        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack drop = new ItemStack(Items.ARMOR_STAND);
            if (stand.hasCustomName()) {
                drop.setHoverName(stand.getCustomName());
            }
            stand.spawnAtLocation(drop);
            for (EquipmentSlotType slot : EquipmentSlotType.values()) {
                ItemStack held = stand.getItemBySlot(slot);
                if (!held.isEmpty()) {
                    stand.spawnAtLocation(held);
                    stand.setItemSlot(slot, ItemStack.EMPTY);
                }
            }
        }
        level.playSound(null, stand.getX(), stand.getY(), stand.getZ(),
                SoundEvents.ARMOR_STAND_BREAK, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        stand.remove();
    }
}
