package com.rzy.dealt_force_skills.workbench;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mod.EventBusSubscriber(modid="dealt_force_skills")
public final class WorkbenchAttributes {
    private WorkbenchAttributes() { }
    @SubscribeEvent
    public static void attributes(ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();
        if(WorkbenchData.tag(stack).isEmpty())return;
        EquipmentSlot slot=stack.getItem() instanceof ArmorItem armor?armor.getType().getSlot():EquipmentSlot.MAINHAND;
        if(event.getSlotType()!=slot)return;
        int[] values=WorkbenchData.allocation(stack);
        for(var stat:WorkbenchStat.values()) {
            Attribute attribute=switch(stat) {
                case DAMAGE -> Attributes.ATTACK_DAMAGE;
                case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
                case ENTITY_REACH -> net.minecraftforge.common.ForgeMod.ENTITY_REACH.get();
                case BLOCK_REACH -> net.minecraftforge.common.ForgeMod.BLOCK_REACH.get();
                case ARMOR -> Attributes.ARMOR;
                case TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
                case KNOCKBACK -> Attributes.KNOCKBACK_RESISTANCE;
                default -> null;
            };
            if(attribute==null||values[stat.ordinal()]==0)continue;
            double delta=stat.change(WorkbenchData.baseline(stack,stat),values[stat.ordinal()]);
            // Convert the advertised final delta through both kinds of existing item multiplier.
            double multiplier=1,baseMultiplier=1;
            for(var modifier:event.getOriginalModifiers().get(attribute)) {
                if(modifier.getOperation()==AttributeModifier.Operation.MULTIPLY_BASE)baseMultiplier+=modifier.getAmount();
                if(modifier.getOperation()==AttributeModifier.Operation.MULTIPLY_TOTAL)multiplier*=1+modifier.getAmount();
            }
            multiplier*=baseMultiplier;
            if(!Double.isFinite(multiplier)||multiplier<=0)continue;
            UUID id=UUID.nameUUIDFromBytes(("dealt_force_skills.workbench."+slot.getName()+"."+stat.key()).getBytes(StandardCharsets.UTF_8));
            event.addModifier(attribute,new AttributeModifier(id,"dealt_force_skills.workbench",delta/multiplier,AttributeModifier.Operation.ADDITION));
        }
    }
}
