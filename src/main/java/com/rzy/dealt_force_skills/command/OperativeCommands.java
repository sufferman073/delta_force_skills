package com.rzy.dealt_force_skills.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.character.CharacterAvailability;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.entity.OperativeEntity;
import com.rzy.dealt_force_skills.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID)
public final class OperativeCommands {
    private OperativeCommands() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dealtoperative")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("summon").then(Commands.argument("character", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills.characterIds().forEach(id ->
                                    builder.suggest(id.substring(id.lastIndexOf('/') + 1)));
                            return builder.buildFuture();
                        })
                        .executes(OperativeCommands::summon)))
                .then(Commands.literal("role").then(Commands.argument("operative", EntityArgument.entity())
                        .then(Commands.argument("character", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills.characterIds().forEach(id ->
                                            builder.suggest(id.substring(id.lastIndexOf('/') + 1)));
                                    return builder.buildFuture();
                                })
                                .executes(OperativeCommands::changeRole))))
                .then(Commands.literal("inspect").then(Commands.argument("operative", EntityArgument.entity())
                        .executes(context -> withOperative(context, "inspect"))))
                .then(Commands.literal("follow").then(Commands.argument("operative", EntityArgument.entity())
                        .executes(context -> withOperative(context, "follow"))))
                .then(Commands.literal("guard").then(Commands.argument("operative", EntityArgument.entity())
                        .executes(context -> withOperative(context, "guard"))))
                .then(Commands.literal("dismiss").then(Commands.argument("operative", EntityArgument.entity())
                        .executes(context -> withOperative(context, "dismiss")))));
    }

    private static int summon(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var source = context.getSource();
        var definition = ModCharacters.findByCommandName(StringArgumentType.getString(context, "character"));
        if (definition.isEmpty() || !com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills.supports(definition.get().id())) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.operative.unsupported"));
            return 0;
        }
        var player = source.getPlayerOrException();
        var blocked = CharacterAvailability.selectionBlockedMessageKey(player, definition.get());
        if (blocked.isPresent()) {
            source.sendFailure(Component.translatable(blocked.get()));
            return 0;
        }
        OperativeEntity entity = ModEntities.OPERATIVE.get().create(source.getLevel());
        if (entity == null) return 0;
        var position = source.getPosition();
        entity.moveTo(position.x, position.y, position.z, player.getYRot(), 0);
        entity.setCharacter(definition.get().id());
        entity.setOwnerUUID(player.getUUID());
        com.rzy.dealt_force_skills.ai.operative.OperativeLoadout.equip(entity);
        if (!source.getLevel().addFreshEntity(entity)) return 0;
        source.sendSuccess(() -> Component.translatable("commands.dealt_force_skills.operative.summoned", entity.getUUID().toString()), false);
        return 1;
    }

    private static int changeRole(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var source = context.getSource();
        if (!(EntityArgument.getEntity(context, "operative") instanceof OperativeEntity entity)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.operative.invalid"));
            return 0;
        }
        var definition = ModCharacters.findByCommandName(StringArgumentType.getString(context, "character"));
        if (definition.isEmpty() || !com.rzy.dealt_force_skills.ai.operative.NativeOperativeSkills.supports(definition.get().id())) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.operative.unsupported"));
            return 0;
        }
        var blocked = CharacterAvailability.selectionBlockedMessageKey(source.getPlayerOrException(), definition.get());
        if (blocked.isPresent()) {
            source.sendFailure(Component.translatable(blocked.get()));
            return 0;
        }
        if (!entity.setCharacter(definition.get().id())) return 0;
        source.sendSuccess(() -> Component.literal(entity.debugState()), false);
        return 1;
    }

    private static int withOperative(CommandContext<CommandSourceStack> context, String action)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var source = context.getSource();
        if (!(EntityArgument.getEntity(context, "operative") instanceof OperativeEntity entity)) {
            source.sendFailure(Component.translatable("commands.dealt_force_skills.operative.invalid"));
            return 0;
        }
        switch (action) {
            case "follow" -> entity.setGuarding(false);
            case "guard" -> entity.setGuarding(true);
            case "dismiss" -> { entity.skills().cancel(entity); entity.discard(); }
            default -> { }
        }
        source.sendSuccess(() -> Component.literal(entity.debugState()), false);
        return 1;
    }

    @SubscribeEvent
    public static void onDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        if (event.getAmount() <= 0 || event.getEntity().level().isClientSide) return;
        if (event.getSource().getEntity() instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost operative
                && (com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(operative.skills(), com.rzy.dealt_force_skills.ai.operative.NativeLunaSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeLunaSkills
                    || operative.skills() instanceof com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills)) {
            boolean skill = event.getSource().getDirectEntity() instanceof com.rzy.dealt_force_skills.entity.LunaShockArrowEntity
                    || event.getSource().getDirectEntity() instanceof com.rzy.dealt_force_skills.entity.LunaCompositeGrenadeEntity;
            com.rzy.dealt_force_skills.ai.operative.OperativeRecon.reveal(operative, event.getEntity(), skill
                    ? com.rzy.dealt_force_skills.character.luna.LunaStateManager.SKILL_REVEAL_TICKS
                    : com.rzy.dealt_force_skills.character.luna.LunaStateManager.PASSIVE_REVEAL_TICKS);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost operative
                && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(operative.skills(), com.rzy.dealt_force_skills.ai.operative.NativeVyronSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeVyronSkills vyron) {
            vyron.onKill();
        }
        if (event.getSource().getEntity() instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost operative
                && !com.rzy.dealt_force_skills.util.TargetingUtil.shouldSkipFriendlyControl(operative.body(), event.getEntity())
                && !com.rzy.dealt_force_skills.util.TargetingUtil.isPlayerOwnedSummonOf(operative.body(), event.getEntity())
                && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(operative.skills(), com.rzy.dealt_force_skills.ai.operative.NativeDWolfSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeDWolfSkills dwolf) dwolf.onKill(operative);
        if (event.getSource().getEntity() instanceof com.rzy.dealt_force_skills.ai.operative.OperativeHost operative
                && !com.rzy.dealt_force_skills.ai.operative.OperativeSupport.ally(operative.body(), event.getEntity())
                && com.rzy.dealt_force_skills.ai.operative.NativeCommanderSkills.inherited(operative.skills(), com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills.class) instanceof com.rzy.dealt_force_skills.ai.operative.NativeTempestSkills tempest) tempest.onKill();
        if (event.getEntity() instanceof OperativeEntity operative) operative.skills().cancel(operative);
    }
}
