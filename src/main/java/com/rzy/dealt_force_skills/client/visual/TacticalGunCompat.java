package com.rzy.dealt_force_skills.client.visual;

import net.minecraft.world.entity.player.Player;

/** Public API marker inspection only; TaCZ retains ownership of gun animation. */
public final class TacticalGunCompat {
    private TacticalGunCompat() { }
    private static final ClassValue<Boolean> GUN = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) { return gunType(type); }
    };
    private static boolean gunType(Class<?> type) {
        if (type == null) return false;
        if (type.getName().equals("com.tacz.guns.api.item.IGun")) return true;
        for (Class<?> contract : type.getInterfaces()) if (gunType(contract)) return true;
        return gunType(type.getSuperclass());
    }
    public static boolean holdingGun(Player player) {
        return !player.getMainHandItem().isEmpty() && GUN.get(player.getMainHandItem().getItem().getClass());
    }
}
