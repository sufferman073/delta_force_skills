package mccfk.DeltaForce.java.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Display preferences belong to the requesting client, including on dedicated servers. */
@EventBusSubscriber(modid = "dealt_force_skills", value = Dist.CLIENT)
public final class UiScaleCommands {
    private UiScaleCommands() { }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        for (String alias : new String[]{"df", "deltaforce"}) {
            event.getDispatcher().register(Commands.literal(alias)
                    .then(Commands.literal("config").then(Commands.literal("scale")
                            .executes(ctx -> {
                                var cfg = DFClientConfig.getInstance();
                                ctx.getSource().sendSuccess(() -> Component.literal("UI scale: "
                                        + (cfg.isUiScaleAuto() ? "auto" : cfg.getUiScale())), false);
                                return 1;
                            })
                            .then(Commands.literal("auto").executes(ctx -> {
                                var cfg = DFClientConfig.getInstance();
                                cfg.setUiScaleAuto();
                                cfg.save();
                                ctx.getSource().sendSuccess(() -> Component.translatable("screen.deltaforce.settings.scale_auto_applied"), false);
                                return 1;
                            }))
                            .then(Commands.argument("value", DoubleArgumentType.doubleArg(DFClientConfig.MIN_UI_SCALE, DFClientConfig.MAX_UI_SCALE))
                                    .executes(ctx -> {
                                        var cfg = DFClientConfig.getInstance();
                                        cfg.setUiScale(DoubleArgumentType.getDouble(ctx, "value"));
                                        cfg.save();
                                        ctx.getSource().sendSuccess(() -> Component.translatable("screen.deltaforce.settings.scale_applied", cfg.getUiScale()), false);
                                        return 1;
                                    })))));
        }
    }
}
