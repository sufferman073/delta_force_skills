package mccfk.DeltaForce.java.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 三角洲行动 · 调酒饮品
 * 从CFK移植并优化：可饮用（3秒）、赋予指定药水效果、饮用后返还玻璃瓶。
 * 最大堆叠16，饮用动画为DRINK。
 */
public class CocktailItem extends Item {

    /**
     * 单个药水效果配置
     *
     * @param effect    效果类型
     * @param duration  持续时间（tick）
     * @param amplifier 效果等级（0为I级）
     */
    public record Effect(Holder<MobEffect> effect, int duration, int amplifier) {}

    private final List<Effect> effects;

    public CocktailItem(Properties properties, List<Effect> effects) {
        super(properties.stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));
        this.effects = List.copyOf(effects);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        Player player = entity instanceof Player p ? p : null;

        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (player != null) {
            player.awardStat(Stats.ITEM_USED.get(this));

            // 应用药水效果
            if (!level.isClientSide && !effects.isEmpty()) {
                for (Effect e : effects) {
                    entity.addEffect(new MobEffectInstance(e.effect(), e.duration(), e.amplifier(), false, true));
                }
            }

            // 生存模式返还玻璃瓶
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    return bottle;
                }
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 60; // 3秒饮用时间
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // 物品介绍：参照 friend 模组对应酒类的 tooltip 文本，本地化键 item.<modid>.<注册名>.desc
        stack.getItemHolder().unwrapKey().ifPresent(key -> {
            ResourceLocation id = key.location();
            tooltip.add(Component.translatable("item." + id.getNamespace() + "." + id.getPath() + ".desc"));
        });
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
}
