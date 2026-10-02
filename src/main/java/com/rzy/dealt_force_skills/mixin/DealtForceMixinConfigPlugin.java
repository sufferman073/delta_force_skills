package com.rzy.dealt_force_skills.mixin;

import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class DealtForceMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String PMMO_MOD_ID = "pmmo";
    private static final String PMMO_CORE = "harmonised.pmmo.core.Core";
    private static final String PMMO_MIXIN =
            "com.rzy.dealt_force_skills.mixin.pmmo.CoreObjectModifierMapMixin";
    private static final String TACZ_MOD_ID = "tacz";
    private static final String TACZ_MIXIN_PREFIX = "com.rzy.dealt_force_skills.mixin.tacz.";

    private boolean pmmoLoaded;
    private boolean taczLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList loadingMods = FMLLoader.getLoadingModList();
        pmmoLoaded = loadingMods != null && loadingMods.getModFileById(PMMO_MOD_ID) != null;
        taczLoaded = loadingMods != null && loadingMods.getModFileById(TACZ_MOD_ID) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(TACZ_MIXIN_PREFIX)) {
            return taczLoaded;
        }
        if (!PMMO_MIXIN.equals(mixinClassName)) {
            return true;
        }
        return pmmoLoaded && PMMO_CORE.equals(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                         IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName,
                          IMixinInfo mixinInfo) {
    }
}
