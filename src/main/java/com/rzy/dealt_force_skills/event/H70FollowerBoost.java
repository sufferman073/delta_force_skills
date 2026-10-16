package com.rzy.dealt_force_skills.event;

import com.rzy.dealt_force_skills.item.DfsEquipmentItem;
import com.rzy.dealt_force_skills.registry.ModGameRules;
import com.rzy.dealt_force_skills.util.TargetingUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

/** H70 bonuses for the same owned companions protected by skill targeting. */
@EventBusSubscriber(modid = "dealt_force_skills")
public final class H70FollowerBoost {
    private static final ResourceLocation BOOST_ID = ResourceLocation.fromNamespaceAndPath("dealt_force_skills", "h70_follower_boost");
    // Only attributes where a larger positive value benefits the follower.
    private static final List<Holder<Attribute>> BOOSTED_ATTRIBUTES = List.of(
            Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED,
            Attributes.ATTACK_KNOCKBACK, Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED,
            Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE,
            Attributes.FOLLOW_RANGE, Attributes.LUCK);

    private H70FollowerBoost() {
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) {
            update(living);
        }
    }

    private static void update(LivingEntity follower) {
        if (follower.level().isClientSide || !follower.isAlive()
                || follower.tickCount % 20 != 0 || !(follower instanceof OwnableEntity ownable)) {
            return;
        }
        double amount = 0.0D;
        Player owner = null;
        if (ownable.getOwnerUUID() != null) {
            owner = follower.level().getPlayerByUUID(ownable.getOwnerUUID());
            if (owner == null && follower.getServer() != null) {
                owner = follower.getServer().getPlayerList().getPlayer(ownable.getOwnerUUID());
            }
        }
        if (owner == null && ownable.getOwner() instanceof Player resolvedOwner) {
            owner = resolvedOwner;
        }
        if (owner != null && owner.isAlive() && !owner.isRemoved()
                && TargetingUtil.isPlayerOwnedSummonOf(owner, follower)
                && ModGameRules.areArmorSpecialsEnabled(owner)) {
            DfsEquipmentItem.Profile head = DfsEquipmentItem.profile(owner.getItemBySlot(EquipmentSlot.HEAD));
            if (head != null) {
                amount = switch (head.ability()) {
                    case H70_FOLLOWER_BOOST -> 1.0D;
                    case H70_NIGHT_FOLLOWER_BOOST -> 2.5D;
                    default -> 0.0D;
                };
            }
        }

        float oldMaxHealth = follower.getMaxHealth();
        float oldHealth = follower.getHealth();
        for (var attribute : BOOSTED_ATTRIBUTES) {
            var instance = follower.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            double desired = instance.getValue() > 0.0D ? amount : 0.0D;
            var modifier = instance.getModifier(BOOST_ID);
            if (modifier != null && (desired == 0.0D || modifier.amount() != desired)) {
                instance.removeModifier(BOOST_ID);
                modifier = null;
            }
            if (modifier == null && desired > 0.0D) {
                // Persist with the entity so boosted health is not clamped away on load.
                // This fixed ID is reconciled on every refresh, including owner/helmet loss.
                instance.addPermanentModifier(new AttributeModifier(
                        BOOST_ID, desired, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        float newMaxHealth = follower.getMaxHealth();
        if (oldMaxHealth > 0.0F && oldMaxHealth != newMaxHealth) {
            follower.setHealth(Math.min(newMaxHealth, oldHealth * newMaxHealth / oldMaxHealth));
        }
    }
}
