package mccfk.DeltaForce.java.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.Eeconomy.CurrencyType;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.client.DFClientConfig;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * DF指令注册器
 * 注册 /deltaforce 和 /df 两个根指令，共用同一套子指令树
 */
public class DFCommand {

    /**
     * 注册 /deltaforce 和 /df 指令
     */
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        var deltaforceNode = dispatcher.register(Commands.literal("deltaforce")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.help"), false);
                return 1;
            })
            .then(buildEconomy())
            .then(buildConfig())
            .then(buildDfs())
        );

        // /df 也注册为相同结构，但直接执行时显示全部子指令用法
        dispatcher.register(Commands.literal("df")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.fullhelp"), false);
                return 1;
            })
            .redirect(deltaforceNode)
        );
    }

    private static boolean isDfPermitted(CommandSourceStack src) {
        return src.isPlayer() && src.hasPermission(3);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildEconomy() {
        return Commands.literal("economy")
            // 需为玩家且 OP 等级 >= 3（>2防止命令方块/矿车执行）
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                return 1;
            })
            .then(buildCurrency(CurrencyType.HAVOC_COIN))
            .then(buildCurrency(CurrencyType.DELTA_COIN));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildCurrency(CurrencyType currency) {
        return Commands.literal(currency.getId())
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                return 1;
            })
            .then(Commands.literal("add")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggester())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            EeconomyManager.getInstance().addBalance(name, currency, amount);
                            long balance = EeconomyManager.getInstance().getBalance(name, currency);
                            ctx.getSource().sendSuccess(() ->
                                net.minecraft.network.chat.Component.translatable(
                                    "command.deltaforce.economy.add",
                                    name, amount, currency.getDisplayName(), balance), true);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("take")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggester())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            long after = EeconomyManager.getInstance().subtractBalance(name, currency, amount);
                            ctx.getSource().sendSuccess(() ->
                                net.minecraft.network.chat.Component.translatable(
                                    "command.deltaforce.economy.take",
                                    name, amount, currency.getDisplayName(), after), true);
                            return 1;
                        })
                    )
                )
            );
    }

    /**
     * 从货币目录的 .json 文件中读取玩家名作为补全建议
     */
    private static SuggestionProvider<CommandSourceStack> playerSuggester() {
        return (ctx, builder) -> {
            for (String name : PlayerCharacterStore.getPlayerNamesFromFiles()) {
                builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildConfig() {
        return Commands.literal("config")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                DFClientConfig cfg = DFClientConfig.getInstance();
                // 注意：指令在服务端执行，不能引用客户端 Minecraft 类，因此这里只报配置值与模式，
                // 真正生效的倍数由客户端界面在 init() 里按当前界面尺寸解析。
                String modeText = cfg.isUiScaleAuto()
                        ? "§7自动（界面尺寸>=3 用 1.0，<3 用 1.5）"
                        : "§7手动";
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== 顶栏偏移配置 ===\n" +
                        "§6/df config x <值> (设置X偏移)\n" +
                        "§6/df config y <值> (设置Y偏移)\n" +
                        "§6/df config scale <值> (界面放大倍数 " +
                        DFClientConfig.MIN_UI_SCALE + "~" + DFClientConfig.MAX_UI_SCALE + ")\n" +
                        "§6/df config scale auto (恢复自动：按界面尺寸自动选倍数)\n" +
                        "§7当前 X偏移: " + cfg.getTabXOffset() + "  Y偏移: " + cfg.getTabYOffset() +
                        "  界面放大: " + String.format("%.2f", cfg.getUiScale()) + " " + modeText), false);
                return 1;
            })
            .then(Commands.literal("x")
                .then(Commands.argument("value", IntegerArgumentType.integer(-500, 500))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        DFClientConfig cfg = DFClientConfig.getInstance();
                        cfg.setTabXOffset(value);
                        cfg.save();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a顶栏X偏移已设置为: " + value + "（重新打开界面生效）"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("y")
                .then(Commands.argument("value", IntegerArgumentType.integer(-500, 500))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        DFClientConfig cfg = DFClientConfig.getInstance();
                        cfg.setTabYOffset(value);
                        cfg.save();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a顶栏Y偏移已设置为: " + value + "（重新打开界面生效）"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("scale")
                .executes(ctx -> {
                    // /df config scale 不带值时只打印当前配置值与模式，避免误操作
                    DFClientConfig cfg = DFClientConfig.getInstance();
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§e界面放大倍数: " + String.format("%.2f", cfg.getUiScale()) +
                                (cfg.isUiScaleAuto() ? "（自动模式：界面尺寸>=3 用 1.0，<3 用 1.5）" : "（手动）") +
                                "\n§7用 /df config scale <值> 手动设置，/df config scale auto 恢复自动"), false);
                    return 1;
                })
                .then(Commands.literal("auto")
                    .executes(ctx -> {
                        DFClientConfig cfg = DFClientConfig.getInstance();
                        cfg.setUiScaleAuto();
                        cfg.save();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a已恢复自动模式：界面尺寸>=3 用 1.0，<3 用 1.5（重新打开界面生效）"), true);
                        return 1;
                    })
                )
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(
                        DFClientConfig.MIN_UI_SCALE, DFClientConfig.MAX_UI_SCALE))
                    .executes(ctx -> {
                        double value = DoubleArgumentType.getDouble(ctx, "value");
                        DFClientConfig cfg = DFClientConfig.getInstance();
                        cfg.setUiScale(value);
                        cfg.save();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a界面放大倍数已设置为: " + String.format("%.2f", cfg.getUiScale())
                                    + "（重新打开界面生效）"), true);
                        return 1;
                    })
                )
            );
    }

    /**
     * DFS 配置管理指令（指令版配置界面）
     * 用法: /df dfs (详细帮助)
     *      /df dfs list [文件] (列出配置项及当前值)
     *      /df dfs get <键路径> (查看单项详情)
     *      /df dfs set <键路径> <值> (修改配置，自动校验类型)
     *      /df dfs x <值> / y <值> (技能按钮偏移)
     *      /df dfs scale <值> (技能按钮缩放 0.25-3.0)
     *      /df dfs reload (重新加载全部配置)
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildDfs() {
        return Commands.literal("dfs")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== DFS 配置管理（指令版） ===\n" +
                        "§6/df dfs list [文件] §7(列出全部配置项及当前值)\n" +
                        "§6/df dfs get <键路径> §7(查看单项详情)\n" +
                        "§6/df dfs set <键路径> <值> §7(修改配置，自动校验类型)\n" +
                        "§6/df dfs x <值> / y <值> §7(技能按钮偏移)\n" +
                        "§6/df dfs scale <值> §7(技能按钮缩放 0.25-3.0)\n" +
                        "§6/df dfs reload §7(重新加载全部配置)\n" +
                        "§7配置文件: config 目录下 dealt*.toml"), false);
                return 1;
            })
            .then(Commands.literal("list")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(dfsList("")), false);
                    return 1;
                })
                .then(Commands.argument("file", StringArgumentType.word())
                    .suggests(dfsFileSuggester())
                    .executes(ctx -> {
                        String file = StringArgumentType.getString(ctx, "file");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(dfsList(file)), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("get")
                .then(Commands.argument("path", StringArgumentType.word())
                    .suggests(dfsPathSuggester())
                    .executes(ctx -> {
                        String path = StringArgumentType.getString(ctx, "path");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(dfsGet(path)), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("path", StringArgumentType.word())
                    .suggests(dfsPathSuggester())
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String path = StringArgumentType.getString(ctx, "path");
                            String value = StringArgumentType.getString(ctx, "value");
                            int code = dfsSet(path, value);
                            if (code == 0) {
                                ctx.getSource().sendSuccess(() ->
                                    Component.literal("§a已设置 §b" + path + " §a= §b" + value + " §a并已生效"), true);
                                return 1;
                            }
                            if (code == -2) {
                                ctx.getSource().sendFailure(Component.literal("§c" + path + " 的值 \"" + value + "\" 格式错误，请检查类型"));
                                return 0;
                            }
                            ctx.getSource().sendFailure(Component.literal("§c未找到可修改的配置项: " + path));
                            return 0;
                        })
                    )
                )
            )
            .then(Commands.literal("x")
                .then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        com.rzy.dealt_force_skills.client.ClientHudLayout.setValue("global.x_offset", value);
                        com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                        com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a技能按钮X偏移已设置为: " + value + "，当前生效位置 X=" +
                                com.rzy.dealt_force_skills.client.ClientHudLayout.x(8)), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("y")
                .then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        com.rzy.dealt_force_skills.client.ClientHudLayout.setValue("global.y_offset", value);
                        com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                        com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a技能按钮Y偏移已设置为: " + value + "，立即生效"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("scale")
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.25, 3.0))
                    .executes(ctx -> {
                        float value = (float) DoubleArgumentType.getDouble(ctx, "value");
                        com.rzy.dealt_force_skills.client.ClientHudLayout.setValue("global.button_scale", (double) value);
                        com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                        com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(String.format("§a技能按钮缩放已设置为: %.2f，当前生效缩放=%.2f",
                                value, com.rzy.dealt_force_skills.client.ClientHudLayout.buttonScale())), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    boolean ok;
                    try {
                        com.rzy.dealt_force_skills.config.ConfigHotReload.reloadAll();
                        ok = true;
                    } catch (RuntimeException error) {
                        ok = false;
                    }
                    com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                    boolean finalOk = ok;
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(finalOk ? "§a已重新加载全部 DFS 配置" : "§c部分配置重载失败，详见日志"), true);
                    return 1;
                })
            );
    }

    /** DFS 全部配置项（HUD + 服务器配置） */
    private static java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData> dfsAllEntries() {
        java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData> all = new java.util.ArrayList<>();
        all.addAll(com.rzy.dealt_force_skills.client.ClientHudLayout.snapshotEntries());
        all.addAll(com.rzy.dealt_force_skills.config.ConfigAccess.snapshotAllServerFiles());
        return all;
    }

    private static String dfsList(String fileFilter) {
        StringBuilder sb = new StringBuilder();
        String lower = fileFilter == null ? "" : fileFilter.toLowerCase(java.util.Locale.ROOT);
        java.util.Map<String, java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData>> byFile =
            new java.util.LinkedHashMap<>();
        for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
            String fileName = entry.file().fileName();
            if (!lower.isEmpty() && !fileName.toLowerCase(java.util.Locale.ROOT).contains(lower)
                    && !entry.file().name().toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                continue;
            }
            byFile.computeIfAbsent(fileName, k -> new java.util.ArrayList<>()).add(entry);
        }
        if (byFile.isEmpty()) {
            return "§c未找到匹配的配置项";
        }
        int shown = 0;
        int total = 0;
        for (java.util.Map.Entry<String, java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData>> group : byFile.entrySet()) {
            sb.append("§e=== ").append(group.getKey()).append(" ===\n");
            for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : group.getValue()) {
                total++;
                if (shown >= 40) {
                    continue;
                }
                sb.append("§7").append(entry.path()).append(" = ")
                  .append(entry.valueText()).append(" §8(").append(entry.kind()).append(")\n");
                shown++;
            }
        }
        if (total > shown) {
            sb.append("§7... 共 ").append(total).append(" 项，仅显示前 40 项，可用 /df dfs list <文件> 筛选");
        }
        if (sb.length() > 9000) {
            sb.setLength(9000);
            sb.append("\n§7... 消息过长已截断");
        }
        return sb.toString();
    }

    private static String dfsGet(String path) {
        for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
            if (!entry.path().equals(path)) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("§e=== ").append(path).append(" ===\n");
            try {
                sb.append(com.rzy.dealt_force_skills.client.config.ConfigLabelHelper.entryTooltip(entry).getString());
            } catch (Throwable ignored) {
                sb.append("§7文件: ").append(entry.file().fileName()).append("\n");
                sb.append("§7类型: ").append(entry.kind()).append("  当前值: ").append(entry.valueText());
            }
            return sb.toString();
        }
        return "§c未找到配置项: " + path;
    }

    private static int dfsSet(String path, String value) {
        for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
            if (!entry.path().equals(path)) {
                continue;
            }
            Object parsed;
            try {
                parsed = com.rzy.dealt_force_skills.config.ConfigEntryData.parse(entry.kind(), value);
            } catch (RuntimeException error) {
                return -2;
            }
            if (entry.file() == com.rzy.dealt_force_skills.config.ConfigFileId.HUD) {
                if (com.rzy.dealt_force_skills.client.ClientHudLayout.setValue(path, parsed)) {
                    com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                    com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                    return 0;
                }
                return -1;
            }
            if (com.rzy.dealt_force_skills.config.ConfigAccess.applyChange(entry.file(), path, entry.kind(), value)) {
                com.rzy.dealt_force_skills.config.ConfigAccess.flush(entry.file());
                try {
                    com.rzy.dealt_force_skills.config.ConfigHotReload.reloadAll();
                } catch (RuntimeException ignored) {
                }
                return 0;
            }
            return -1;
        }
        return -1;
    }

    private static SuggestionProvider<CommandSourceStack> dfsPathSuggester() {
        return (ctx, builder) -> {
            for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
                builder.suggest(entry.path());
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> dfsFileSuggester() {
        return (ctx, builder) -> {
            for (com.rzy.dealt_force_skills.config.ConfigFileId file : com.rzy.dealt_force_skills.config.ConfigFileId.values()) {
                builder.suggest(file.name().toLowerCase(java.util.Locale.ROOT));
                builder.suggest(file.fileName());
            }
            return builder.buildFuture();
        };
    }
}