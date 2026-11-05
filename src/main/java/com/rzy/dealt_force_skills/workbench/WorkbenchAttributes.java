package com.rzy.dealt_force_skills.workbench;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="dealt_force_skills")
public final class WorkbenchAttributes {
    private WorkbenchAttributes() { }
    @SubscribeEvent
    public static void attributes(ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();
        if(WorkbenchData.tag(stack).isEmpty())return;
        EquipmentSlot slot=stack.getItem() instanceof ArmorItem armor?armor.getType().getSlot():EquipmentSlot.MAINHAND;
        int[] values=WorkbenchData.allocation(stack);
        for(var stat:WorkbenchStat.values()) {
            net.minecraft.core.Holder<Attribute> attribute=switch(stat) {
                case DAMAGE -> Attributes.ATTACK_DAMAGE;
                case ATTACK_SPEED -> Attributes.ATTACK_SPEED;
                case ENTITY_REACH -> Attributes.ENTITY_INTERACTION_RANGE;
                case BLOCK_REACH -> Attributes.BLOCK_INTERACTION_RANGE;
                case ARMOR -> Attributes.ARMOR;
                case TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
                case KNOCKBACK -> Attributes.KNOCKBACK_RESISTANCE;
                default -> null;
            };
            if(attribute==null||values[stat.ordinal()]==0)continue;
            double delta=stat.change(WorkbenchData.baseline(stack,stat),values[stat.ordinal()]);
            // Convert the advertised final delta through both kinds of existing item multiplier.
            double multiplier=1,baseMultiplier=1;
            for(var entry:event.getDefaultModifiers().modifiers()) {
                if(!entry.attribute().equals(attribute)||!entry.slot().test(slot))continue;
                if(entry.modifier().operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)baseMultiplier+=entry.modifier().amount();
                if(entry.modifier().operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)multiplier*=1+entry.modifier().amount();
            }
            multiplier*=baseMultiplier;
            if(!Double.isFinite(multiplier)||multiplier<=0)continue;
            var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("dealt_force_skills","workbench/"+slot.getName()+"/"+stat.key());
            event.addModifier(attribute,new AttributeModifier(id,delta/multiplier,AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot));
        }
    }
}
