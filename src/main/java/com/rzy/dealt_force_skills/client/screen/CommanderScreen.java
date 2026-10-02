package com.rzy.dealt_force_skills.client.screen;

import com.rzy.dealt_force_skills.character.CharacterDefinition;
import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.commander.CommanderSkillChoice;
import com.rzy.dealt_force_skills.character.commander.CommanderBranchUpgrade;
import com.rzy.dealt_force_skills.character.commander.CommanderStateManager;
import com.rzy.dealt_force_skills.character.commander.CommanderSupport;
import com.rzy.dealt_force_skills.character.commander.CommanderUpgrade;
import com.rzy.dealt_force_skills.client.character.ClientCommanderState;
import com.rzy.dealt_force_skills.client.CommanderInputHandler;
import com.rzy.dealt_force_skills.network.C2S_BuyCommanderUpgrade;
import com.rzy.dealt_force_skills.network.C2S_BuyCommanderBranchUpgrade;
import com.rzy.dealt_force_skills.network.C2S_CommanderSupport;
import com.rzy.dealt_force_skills.network.C2S_SetCommanderSkill;
import com.rzy.dealt_force_skills.network.NetworkHandler;
import com.rzy.dealt_force_skills.compat.SuperbWarfareCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class CommanderScreen extends Screen {
    private static String lastSelectedVehicleId = "";
    private final boolean support;
    private int branchPage;
    private CommanderSkillChoice openChoice;
    private int choicePage;
    private boolean branchView;
    private boolean vehicleView;
    private int vehiclePage;
    private long stateRevision;
    private String selectedVehicleId = lastSelectedVehicleId;
    private static final int BRANCHES_PER_PAGE = 12;

    public CommanderScreen(boolean support) {
        super(Component.translatable(support
                ? "screen.dealt_force_skills.commander.support_title"
                : "screen.dealt_force_skills.commander.skill_title"));
        this.support = support;
    }

    @Override
    protected void init() {
        stateRevision = ClientCommanderState.revision();
        if (support) {
            initSupport();
        } else {
            initSkills();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (stateRevision != ClientCommanderState.revision()) {
            stateRevision = ClientCommanderState.revision();
            rebuildWidgets();
        }
    }

    private void initSkills() {
        int panelWidth = panelWidth();
        int left = panelLeft(panelWidth);
        int top = panelTop(panelHeight());
        if (openChoice != null) {
            initChoicePopup(left, top, panelWidth);
            return;
        }
        CommanderSkillChoice[] choices = {
                CommanderSkillChoice.ACTIVE_1, CommanderSkillChoice.ACTIVE_2,
                CommanderSkillChoice.CORE_1, CommanderSkillChoice.CORE_2
        };
        int columnWidth = Math.max(120, (panelWidth - 36) / 2);
        for (int i = 0; i < choices.length; i++) {
            CommanderSkillChoice choice = choices[i];
            int column = i % 2;
            int row = i / 2;
            addRenderableWidget(Button.builder(choiceLabel(choice), button -> cycleChoice(choice))
                    .bounds(left + 12 + column * (columnWidth + 12), top + 42 + row * 34,
                            columnWidth, 28)
                    .tooltip(choiceTooltip(choice)).build());
        }
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.dealt_force_skills.commander.passive.configure"),
                        button -> Minecraft.getInstance().setScreen(new CommanderPassiveScreen(this)))
                .bounds(left + 12, top + 116, panelWidth - 24, 28)
                .tooltip(Tooltip.create(Component.translatable(
                        "screen.dealt_force_skills.commander.passive.configure.desc"))).build());
        CommanderUpgrade[] upgrades = CommanderUpgrade.values();
        for (int i = 0; i < upgrades.length; i++) {
            CommanderUpgrade upgrade = upgrades[i];
            addRenderableWidget(Button.builder(upgradeLabel(upgrade), button ->
                            NetworkHandler.sendToServer(new C2S_BuyCommanderUpgrade(upgrade)))
                    .bounds(left + 12 + i * ((panelWidth - 24) / 3), top + 158,
                            Math.max(1, (panelWidth - 36) / 3), 28)
                    .tooltip(Tooltip.create(Component.translatable(upgrade.translationKey() + ".desc"))).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + (panelWidth - 160) / 2, top + panelHeight() - 32, 160, 24).build());
    }

    private void initChoicePopup(int left, int top, int panelWidth) {
        List<CharacterDefinition> candidates = CommanderStateManager.availableSources(openChoice);
        int pageSize = 12;
        int page = Math.max(0, Math.min(choicePage, Math.max(0, (candidates.size() - 1) / pageSize)));
        int start = page * pageSize;
        int end = Math.min(candidates.size(), start + pageSize);
        int buttonWidth = Math.max(120, (panelWidth - 36) / 2);
        for (int i = start; i < end; i++) {
            CharacterDefinition candidate = candidates.get(i);
            int local = i - start;
            int column = local % 2;
            int row = local / 2;
            addRenderableWidget(Button.builder(candidate.displayName(), button -> {
                        NetworkHandler.sendToServer(new C2S_SetCommanderSkill(openChoice, candidate.id()));
                        ClientCommanderState.setSource(openChoice, candidate.id());
                        openChoice = null;
                        rebuildWidgets();
                    }).bounds(left + 12 + column * (buttonWidth + 12), top + 48 + row * 30,
                            buttonWidth, 24)
                    .tooltip(Tooltip.create(candidate.skill(openChoice.skillSlot())
                            .map(skill -> skill.displayDescription()).orElse(Component.empty())))
                    .build());
        }
        int pageCount = Math.max(1, (candidates.size() + pageSize - 1) / pageSize);
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    choicePage = Math.max(0, choicePage - 1);
                    rebuildWidgets();
                }).bounds(left + 12, top + panelHeight() - 32, 40, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.dealt_force_skills.commander.choice_page",
                        page + 1, pageCount), button -> {}).bounds(left + 58, top + panelHeight() - 32, 100, 24).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    choicePage = Math.min(pageCount - 1, choicePage + 1);
                    rebuildWidgets();
                }).bounds(left + 164, top + panelHeight() - 32, 40, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
                    openChoice = null;
                    choicePage = 0;
                    rebuildWidgets();
                }).bounds(left + panelWidth - 132, top + panelHeight() - 32, 120, 24).build());
    }

    private void initSupport() {
        int panelWidth = panelWidth();
        int left = panelLeft(panelWidth);
        int top = panelTop(panelHeight());
        int supportWidth = Math.max(92, (panelWidth - 36) / 3);
        if (branchView) {
            CommanderBranchUpgrade[] branches = CommanderBranchUpgrade.values();
            int pageCount = (branches.length + BRANCHES_PER_PAGE - 1) / BRANCHES_PER_PAGE;
            branchPage = Math.max(0, Math.min(branchPage, pageCount - 1));
            int start = branchPage * BRANCHES_PER_PAGE;
            int end = Math.min(branches.length, start + BRANCHES_PER_PAGE);
            for (int i = start; i < end; i++) {
                CommanderBranchUpgrade upgrade = branches[i];
                int local = i - start;
                int column = local % 3;
                int row = local / 3;
                addRenderableWidget(Button.builder(branchUpgradeLabel(upgrade), button ->
                                NetworkHandler.sendToServer(new C2S_BuyCommanderBranchUpgrade(upgrade)))
                        .bounds(left + 12 + column * (supportWidth + 6), top + 42 + row * 28,
                                supportWidth, 24)
                        .tooltip(branchTooltip(upgrade)).build());
            }
            addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                        branchPage = Math.max(0, branchPage - 1);
                        rebuildWidgets();
                    }).bounds(left + 12, top + panelHeight() - 32, 40, 24).build());
            addRenderableWidget(Button.builder(Component.translatable(
                            "screen.dealt_force_skills.commander.branch_page", branchPage + 1, pageCount), button -> {})
                    .bounds(left + 58, top + panelHeight() - 32, 120, 24).build());
            addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                        branchPage = Math.min(pageCount - 1, branchPage + 1);
                        rebuildWidgets();
                    }).bounds(left + 184, top + panelHeight() - 32, 40, 24).build());
        } else if (vehicleView) {
            initVehiclePopup(left, top, panelWidth);
            return;
        } else {
            CommanderSupport[] supports = CommanderSupport.values();
            if (selectedVehicleId.isBlank()) {
                selectedVehicleId = SuperbWarfareCompat.registeredVehicleIds(Minecraft.getInstance().level).stream()
                        .findFirst().map(Object::toString).orElse("");
                lastSelectedVehicleId = selectedVehicleId;
            }
            for (int i = 0; i < supports.length; i++) {
                CommanderSupport action = supports[i];
                int column = i % 3;
                int row = i / 3;
                addRenderableWidget(Button.builder(supportLabel(action), button -> useSupport(action))
                        .bounds(left + 12 + column * (supportWidth + 6), top + 42 + row * 28,
                                supportWidth, 24)
                        .tooltip(supportTooltip(action)).build());
            }
            List<net.minecraft.resources.ResourceLocation> vehicles = SuperbWarfareCompat.registeredVehicleIds(
                    Minecraft.getInstance().level);
            if (!selectedVehicleId.isBlank()) {
                boolean available = vehicles.stream().anyMatch(id -> id.toString().equals(selectedVehicleId));
                if (!available) {
                    selectedVehicleId = vehicles.stream().findFirst().map(Object::toString).orElse("");
                    lastSelectedVehicleId = selectedVehicleId;
                }
            }
            if (!vehicles.isEmpty()) {
                addRenderableWidget(Button.builder(Component.translatable(
                                 "screen.dealt_force_skills.commander.vehicle_select", selectedVehicleName(vehicles)), button -> {
                             vehicleView = true;
                             vehiclePage = Math.max(0, vehicles.indexOf(
                                     net.minecraft.resources.ResourceLocation.parse(selectedVehicleId))) / 12;
                             rebuildWidgets();
                        }).bounds(left + 12, top + 42 + ((supports.length + 2) / 3) * 28,
                                panelWidth - 24, 24).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable(branchView
                        ? "screen.dealt_force_skills.commander.show_supports"
                        : "screen.dealt_force_skills.commander.show_branches"), button -> {
                    branchView = !branchView;
                    branchPage = 0;
                    rebuildWidgets();
                }).bounds(left + panelWidth - 220, top + panelHeight() - 32, 100, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(left + panelWidth - 112, top + panelHeight() - 32, 100, 24).build());
    }

    private void initVehiclePopup(int left, int top, int panelWidth) {
        List<net.minecraft.resources.ResourceLocation> vehicles = SuperbWarfareCompat.registeredVehicleIds(
                Minecraft.getInstance().level);
        int buttonWidth = Math.max(120, (panelWidth - 36) / 2);
        int pageSize = 12;
        int pageCount = Math.max(1, (vehicles.size() + pageSize - 1) / pageSize);
        vehiclePage = Math.max(0, Math.min(vehiclePage, pageCount - 1));
        int start = vehiclePage * pageSize;
        int end = Math.min(vehicles.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            net.minecraft.resources.ResourceLocation id = vehicles.get(i);
            int local = i - start;
            int column = local % 2;
            int row = local / 2;
            addRenderableWidget(Button.builder(vehicleName(id), button -> {
                        selectedVehicleId = id.toString();
                        lastSelectedVehicleId = selectedVehicleId;
                        vehicleView = false;
                        rebuildWidgets();
                    }).bounds(left + 12 + column * (buttonWidth + 12), top + 46 + row * 28,
                            buttonWidth, 24).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    vehiclePage = Math.max(0, vehiclePage - 1);
                    rebuildWidgets();
                }).bounds(left + 12, top + panelHeight() - 32, 40, 24).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "screen.dealt_force_skills.commander.vehicle_page", vehiclePage + 1, pageCount), button -> {})
                .bounds(left + 58, top + panelHeight() - 32, 120, 24).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    vehiclePage = Math.min(pageCount - 1, vehiclePage + 1);
                    rebuildWidgets();
                }).bounds(left + 184, top + panelHeight() - 32, 40, 24).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
                    vehicleView = false;
                    vehiclePage = 0;
                    rebuildWidgets();
                }).bounds(left + panelWidth - 132, top + panelHeight() - 32, 120, 24).build());
    }

    private Component selectedVehicleName(List<net.minecraft.resources.ResourceLocation> vehicles) {
        if (selectedVehicleId.isBlank()) {
            return Component.translatable("screen.dealt_force_skills.commander.vehicle_none");
        }
        try {
            return vehicleName(net.minecraft.resources.ResourceLocation.parse(selectedVehicleId));
        } catch (RuntimeException ignored) {
            return Component.translatable("screen.dealt_force_skills.commander.vehicle_none");
        }
    }

    private Component vehicleName(net.minecraft.resources.ResourceLocation id) {
        if (Minecraft.getInstance().level == null) {
            return Component.literal(id.toString());
        }
        net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id);
        return type == null ? Component.literal(id.toString()) : type.getDescription();
    }

    private void cycleChoice(CommanderSkillChoice choice) {
        openChoice = choice;
        choicePage = 0;
        rebuildWidgets();
    }

    private void useSupport(CommanderSupport action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Vec3 position = minecraft.player.position().add(minecraft.player.getLookAngle().scale(16.0D));
        if (minecraft.hitResult instanceof BlockHitResult hit) {
            position = Vec3.atCenterOf(hit.getBlockPos());
        }
        int entityId = minecraft.crosshairPickEntity == null ? -1 : minecraft.crosshairPickEntity.getId();
        if (action.targeted()) {
            CommanderInputHandler.armSupport(action, selectedVehicleId);
            onClose();
        } else {
            NetworkHandler.sendToServer(new C2S_CommanderSupport(action, position, entityId, selectedVehicleId));
            onClose();
        }
    }

    private Component choiceLabel(CommanderSkillChoice choice) {
        String source = ClientCommanderState.source(choice);
        Component name = com.rzy.dealt_force_skills.character.ModCharacters.get(source)
                .map(CharacterDefinition::displayName).orElse(Component.literal("-").withStyle(net.minecraft.ChatFormatting.GRAY));
        return Component.translatable(choice.translationKey(), name);
    }

    private Tooltip choiceTooltip(CommanderSkillChoice choice) {
        String source = ClientCommanderState.source(choice);
        return Tooltip.create(ModCharacters.get(source)
                .flatMap(character -> character.skill(choice.skillSlot()))
                .map(skill -> skill.displayDescription())
                .orElse(Component.translatable("screen.dealt_force_skills.commander.skill_not_selected")));
    }

    private Component upgradeLabel(CommanderUpgrade upgrade) {
        int level = ClientCommanderState.upgradeLevel(upgrade);
        long cost = upgrade.cost(level);
        return Component.translatable(upgrade.translationKey(), level, cost < 0L ? "MAX" : cost);
    }

    private Component supportLabel(CommanderSupport action) {
        return Component.translatable(action.translationKey(), action.cost(),
                ClientCommanderState.cooldownTicks(action) / 20);
    }

    private Component branchUpgradeLabel(CommanderBranchUpgrade upgrade) {
        return Component.translatable(upgrade.translationKey(),
                ClientCommanderState.ownsBranchUpgrade(upgrade)
                        ? Component.translatable("screen.dealt_force_skills.commander.owned")
                        : Component.literal(Long.toString(upgrade.cost())));
    }

    private Tooltip supportTooltip(CommanderSupport action) {
        return Tooltip.create(Component.translatable("screen.dealt_force_skills.commander.support."
                + action.name().toLowerCase(java.util.Locale.ROOT) + ".desc"));
    }

    private Tooltip branchTooltip(CommanderBranchUpgrade upgrade) {
        return Tooltip.create(Component.translatable(upgrade.translationKey() + ".desc")
                .append("\n")
                .append(Component.translatable("screen.dealt_force_skills.commander.branch.generic_desc")));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = panelLeft(panelWidth);
        int top = panelTop(panelHeight);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xE6101820);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF55AAFF);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(
                "screen.dealt_force_skills.commander.points", ClientCommanderState.tacticalPoints()),
                width / 2, top + 27, 0xFFD866);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int panelWidth() {
        return Math.min(760, Math.max(320, width - 20));
    }

    private int panelHeight() {
        return Math.min(height - 12, support ? 420 : 250);
    }

    private int panelLeft(int panelWidth) {
        return (width - panelWidth) / 2;
    }

    private int panelTop(int panelHeight) {
        return Math.max(6, (height - panelHeight) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}