package com.rzy.dealt_force_skills.mixin;
import com.rzy.dealt_force_skills.workbench.WorkbenchArrowSnapshot;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
@Mixin(AbstractArrow.class)
public abstract class WorkbenchArrowMixin implements WorkbenchArrowSnapshot {
    @Unique private ItemStack dfs$weapon=ItemStack.EMPTY;
    public void dfs$weapon(ItemStack weapon){dfs$weapon=weapon;}
    public ItemStack dfs$takeWeapon(){var result=dfs$weapon;dfs$weapon=ItemStack.EMPTY;return result;}
}
