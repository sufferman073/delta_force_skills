package com.rzy.dealt_force_skills.util;

import com.rzy.dealt_force_skills.client.HelmetVisionClient;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

public final class ClientVisionHooks {
    private ClientVisionHooks() {
    }

    public static boolean isThermalVisionActive() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return HelmetVisionClient.isThermalVisionActive();
        }
        return false;
    }
}
