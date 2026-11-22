package mccfk.DeltaForce.java.client;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.rzy.dealt_force_skills.client.ClientHudLayout;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Display preferences belong to the requesting client, including on dedicated servers. */
@Mod.EventBusSubscriber(modid = "dealt_force_skills", value = Dist.CLIENT)
public final class UiScaleCommands {
    private UiScaleCommands() { }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> offset(String axis, boolean skills) {
        int limit = skills ? 1000 : 500;
        return Commands.literal(axis).then(Commands.argument("value", IntegerArgumentType.integer(-limit, limit)).executes(ctx -> {
            int value = IntegerArgumentType.getInteger(ctx, "value");
            if (skills) ClientHudLayout.setValue("global." + axis + "_offset", value);
            else {
                var cfg = DFClientConfig.getInstance();
                if (axis.equals("x")) cfg.setTabXOffset(value); else cfg.setTabYOffset(value);
                cfg.save();
            }
            ctx.getSource().sendSuccess(() -> Component.literal("本地" + (skills ? "技能按钮" : "顶栏") + " " + axis + " = " + value), false);
            return 1;
        }));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> localEntries(boolean write) {
        var command = Commands.literal(write ? "set" : "get");
        for (var entry : ClientHudLayout.snapshotEntries()) {
            var key = Commands.literal(entry.path());
            if (write) key.then(Commands.argument("value", com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(ctx -> {
                String text = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "value");
                try {
                    ClientHudLayout.setValue(entry.path(), com.rzy.dealt_force_skills.config.ConfigEntryData.parse(entry.kind(), text));
                    ctx.getSource().sendSuccess(() -> Component.literal(entry.path() + " = " + text), false);
                    return 1;
                } catch (IllegalArgumentException invalid) { ctx.getSource().sendFailure(Component.literal("无效 HUD 值: " + text)); return 0; }
            }));
            else key.executes(ctx -> {
                ClientHudLayout.snapshotEntries().stream().filter(current -> current.path().equals(entry.path())).findFirst().ifPresent(current ->
                        ctx.getSource().sendSuccess(() -> Component.literal(current.path() + " = " + current.valueText()), false));
                return 1;
            });
            command.then(key);
        }
        return command;
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        for (String alias : new String[]{"df", "deltaforce"}) {
            event.getDispatcher().register(Commands.literal(alias)
                    .then(Commands.literal("dfs").then(localEntries(false)).then(localEntries(true)).then(offset("x", true)).then(offset("y", true))
                            .then(Commands.literal("scale").then(Commands.argument("value", DoubleArgumentType.doubleArg(.25, 3))
                                    .executes(ctx -> {
                                        double value = DoubleArgumentType.getDouble(ctx, "value");
                                        ClientHudLayout.setValue("global.button_scale", value);
                                        ctx.getSource().sendSuccess(() -> Component.literal("本地技能按钮缩放: " + value), false);
                                        return 1;
                                    }))))
                    .then(Commands.literal("config").executes(ctx -> {
                        var cfg = DFClientConfig.getInstance();
                        ctx.getSource().sendSuccess(() -> Component.literal("本地顶栏偏移 X=" + cfg.getTabXOffset() + " Y=" + cfg.getTabYOffset()), false);
                        return 1;
                    }).then(offset("x", false)).then(offset("y", false)).then(Commands.literal("scale")
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
