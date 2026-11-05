package com.rzy.dealt_force_skills.department;

import com.rzy.dealt_force_skills.shop.TaczShopCatalog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/** A saved, immutable offer. Only a server-created withdrawal plan can consume inventory. */
public record CollectorOffer(int variant, List<Part> requirements, List<Part> rewards, long coins, int experience) {
    public record Part(ItemStack template, int count, boolean minimumEnchantment) {
        public Part(ItemStack template,int count) { this(template,count,false); }
        public Part {
            if (template.isEmpty() || count < 1 || count > 65536) throw new IllegalArgumentException("Invalid offer item");
            template = template.copyWithCount(1);
        }
        @Override public ItemStack template() { return template.copy(); }
        public ItemStack stack() { return template.copyWithCount(count); }
    }
    public CollectorOffer {
        requirements = List.copyOf(requirements); rewards = List.copyOf(rewards);
        if (requirements.isEmpty() || requirements.size() > 32 || rewards.size() > 32 || coins < 0 || experience < 0)
            throw new IllegalArgumentException("Invalid collector offer");
    }
    public CompoundTag save(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Variant", variant); tag.putLong("Coins", coins); tag.putInt("Experience", experience);
        tag.put("Requirements", saveParts(player, requirements)); tag.put("Rewards", saveParts(player, rewards));
        return tag;
    }
    private static ListTag saveParts(ServerPlayer player, List<Part> parts) {
        ListTag tags = new ListTag();
        for (Part part : parts) {
            CompoundTag tag = new CompoundTag();
            tag.put("Item", DepartmentPlatform.save(player,part.template)); tag.putInt("Count",part.count);
            tag.putBoolean("MinimumEnchantment",part.minimumEnchantment);
            tags.add(tag);
        }
        return tags;
    }
    public static CollectorOffer load(ServerPlayer player, CompoundTag tag) {
        try {
            return new CollectorOffer(tag.getInt("Variant"), readParts(player,tag.getList("Requirements",Tag.TAG_COMPOUND)),
                    readParts(player,tag.getList("Rewards",Tag.TAG_COMPOUND)),tag.getLong("Coins"),tag.getInt("Experience"));
        } catch (RuntimeException error) { return null; }
    }
    private static List<Part> readParts(ServerPlayer player, ListTag tags) {
        if (tags.size() > 32) throw new IllegalArgumentException("Too many offer items");
        List<Part> result = new ArrayList<>();
        for (int i=0;i<tags.size();i++) {
            CompoundTag tag=tags.getCompound(i);
            result.add(new Part(DepartmentPlatform.load(player,tag.getCompound("Item")),tag.getInt("Count"),tag.getBoolean("MinimumEnchantment")));
        }
        return result;
    }
    public static boolean matches(ServerPlayer player, ItemStack stack, Part part) {
        ItemStack requested=part.template;
        if (stack.isEmpty() || !stack.is(requested.getItem())) return false;
        if (requested.is(Items.ENCHANTED_BOOK) && part.minimumEnchantment) {
            var required=DepartmentPlatform.enchantments(player,requested);
            var actual=DepartmentPlatform.enchantments(player,stack);
            return required.size()==1 && actual.size()==1 && required.entrySet().stream()
                    .allMatch(entry -> actual.getOrDefault(entry.getKey(),0) >= entry.getValue());
        }
        var tacz=TaczShopCatalog.find(requested);
        if(tacz.isPresent())return TaczShopCatalog.find(stack).map(entry -> entry.entryId().equals(tacz.get().entryId())).orElse(false);
        if(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(requested.getItem()).getNamespace().equals("tacz"))return false;
        return !requested.is(Items.ENCHANTED_BOOK) || DepartmentPlatform.enchantments(player,requested).equals(DepartmentPlatform.enchantments(player,stack));
    }
    /** Slots are allocated only once, even if multiple requirements match the same item. */
    public int[] withdrawalPlan(ServerPlayer player) {
        int[] plan=new int[player.getInventory().items.size()];
        // Higher enchantment levels first prevent a low-level requirement consuming the only high-level book.
        List<Part> ordered=new ArrayList<>(requirements);
        ordered.sort(java.util.Comparator.comparingInt((Part p) -> DepartmentPlatform.enchantments(player,p.template)
                .values().stream().mapToInt(Integer::intValue).max().orElse(0)).reversed());
        for (Part part:ordered) {
            int remaining=part.count;
            for(int i=0;i<plan.length&&remaining>0;i++) {
                ItemStack stack=player.getInventory().getItem(i);
                if(!matches(player,stack,part))continue;
                int take=Math.min(remaining,Math.max(0,stack.getCount()-plan[i]));
                plan[i]+=take;remaining-=take;
            }
            if(remaining>0)return null;
        }
        return plan;
    }
    public boolean consume(ServerPlayer player) {
        int[] plan=withdrawalPlan(player);
        if(plan==null)return false;
        for(int i=0;i<plan.length;i++)if(plan[i]>0)player.getInventory().getItem(i).shrink(plan[i]);
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();return true;
    }
}
