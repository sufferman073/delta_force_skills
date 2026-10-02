package com.rzy.dealt_force_skills.client.config;

import com.rzy.dealt_force_skills.config.ConfigEntryData;
import com.rzy.dealt_force_skills.config.ConfigFileId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chinese-friendly labels and purpose descriptions for TOML paths.
 * Prefer lang keys; fall back to a built-in Chinese dictionary and path rules.
 */
public final class ConfigLabelHelper {
    /** Common path segments / leaves → Chinese. */
    private static final Map<String, String> ZH = new LinkedHashMap<>();
    /** Design-record aliases: config path fragment -> localized skill id. */
    private static final Map<String, Map<String, String>> SKILL_ALIASES = new LinkedHashMap<>();

    static {
        // categories / files
        put("characters", "角色");
        put("summons", "召唤物");
        put("entities", "实体");
        put("deployables", "部署物");
        put("consumables", "消耗品");
        put("items", "物品");
        put("equipment", "装备");
        put("shops", "商店");
        put("shop", "商店");
        put("match", "对局规则");
        put("general", "全局");
        put("events", "事件");
        put("client", "客户端");
        put("work_blocks", "工作方块");
        put("loot", "战利品");
        put("effects", "状态效果");
        put("beacon", "信标Boss");
        put("fear", "恐惧");
        put("global", "全局");
        put("hud", "HUD");
        put("attributes", "属性");
        put("experience_growth", "经验成长");
        put("facilities", "特勤处设施");
        put("redemption", "普通特勤处赎回");
        put("gti", "GTI特勤处");
        put("trading_hall", "GTI交易行");
        put("warehouse", "仓库");
        put("command_center", "指挥中心");
        put("shooting_range", "靶场");
        put("training_center", "训练中心");
        put("diving_center", "潜水中心");
        put("decontamination_room", "洗消室");
        put("max_level", "最高可升级等级");
        put("effects_enabled", "设施效果开关");
        put("redemption_enabled", "赎回功能开关");
        // character ids — must match zh_cn character.*.name (design names, NOT machine translation)
        put("dwolf", "红狼");
        put("d_wolf", "红狼");
        put("vyron", "威龙");
        put("sineva", "深蓝");
        put("hackclaw", "骇爪");
        put("luna", "露娜");
        put("morse", "回响");
        put("raptor", "银翼");
        put("toxik", "蛊");
        put("stinger", "蜂医");
        put("uluru", "乌鲁鲁");
        put("tempest", "疾风");
        put("shepherd", "牧羊人");
        put("gizmo", "比特");
        put("nox", "无名");
        put("manba", "牢大");
        put("saeed", "赛伊德");
        put("undead", "不死人");
        put("lexninjia", "蕾忍");
        put("lex_ninjia", "蕾忍");
        put("catdad", "耄耋");
        put("corps", "军团");
        put("department", "交通厅-量产型");
        put("gambler", "赌徒");
        put("ghroth", "典狱长");
        put("vlinder", "蝶");
        put("chamber", "尚博勒");
        put("nikaidou", "二阶堂希罗");
        put("nikaidou_hiro", "二阶堂希罗");
        put("nikaidou_hiro_witchification", "二阶堂希罗魔女化");
        put("ntwo", "液氮");
        put("n_two", "液氮");
        put("tacz", "TaCZ");
        // class/module fragments — keep short, not “机翻长句”
        put("state_manager", "数值");
        put("skills", "技能");
        put("placement_helper", "部署");
        put("art", "术式");
        put("arts", "术式");
        put("effect", "状态效果");
        put("hurt", "伤害");
        put("energy", "能量");
        put("round_start_freeze", "回合开局冻结");
        put("client_events", "客户端");
        put("preset", "预设");
        put("support_manager", "支援");
        put("upgrade_manager", "升级");
        put("soul_manager", "魂值");
        // raptor / recon specific
        put("footprint_life_ticks", "脚印存活时间");
        put("footprint_interval_ticks", "脚印生成间隔");
        put("footprint_sync_range", "脚印同步半径");
        put("footprint_read_range", "脚印读取距离");
        put("footprint_read_distance", "脚印准星判定距离");
        put("footprint_read_reveal_range", "读脚印暴露半径");
        put("footprint_read_reveal_ticks", "读脚印暴露时长");
        put("falcon_cooldown_ticks", "猎鹰无人机冷却");
        put("pulse_max_charges", "脉冲手雷最大充能");
        put("pulse_recharge_ticks", "脉冲手雷充能时间");
        put("hummingbird_cooldown_ticks", "蜂鸟冷却");
        put("hummingbird_attach_ticks", "蜂鸟安插延迟");
        put("hummingbird_duration_ticks", "蜂鸟持续时间");
        put("hummingbird_reveal_ticks", "蜂鸟暴露时长");
        put("hummingbird_range", "蜂鸟目标范围");
        // beacon boss leaves
        put("strafe_speed", "侧向走位速度倍率");
        put("strafe_interval_ticks", "侧向走位刷新间隔");
        put("movement_speed", "移动速度(格/秒)");
        put("attack_range", "攻击射程");
        put("follow_range", "索敌范围");
        // common words
        put("cooldown", "冷却");
        put("recharge", "充能");
        put("duration", "持续");
        put("interval", "间隔");
        put("delay", "延迟");
        put("max", "最大");
        put("min", "最小");
        put("base", "基础");
        put("active", "主动");
        put("passive", "被动");
        put("core", "大招/核心");
        put("damage", "伤害");
        put("heal", "治疗");
        put("healing", "治疗");
        put("radius", "半径");
        put("range", "范围");
        put("distance", "距离");
        put("speed", "速度");
        put("ticks", "tick");
        put("seconds", "秒");
        put("amplifier", "效果等级");
        put("enabled", "启用");
        put("enable", "启用");
        put("charges", "充能次数");
        put("charge", "充能/蓄力");
        put("stack", "层数");
        put("stacks", "层数");
        put("cost", "消耗");
        put("energy_cost", "能量消耗");
        put("fraction", "比例");
        put("percent", "百分比");
        put("permille", "千分比");
        put("multiplier", "倍率");
        put("health", "生命");
        put("armor", "护甲");
        put("stun", "眩晕");
        put("blind", "致盲");
        put("flash", "闪光");
        put("smoke", "烟雾");
        put("grenade", "手雷");
        put("drone", "无人机");
        put("missile", "导弹");
        put("trap", "陷阱");
        put("shield", "盾牌");
        put("reload", "装填");
        put("teleport", "传送");
        put("follow", "跟随");
        put("attack", "攻击");
        put("movement", "移动");
        put("horizontal", "水平");
        put("vertical", "垂直");
        put("self", "自身");
        put("owner", "主人");
        put("player", "玩家");
        put("mob", "生物");
        put("kill", "击杀");
        put("death", "死亡");
        put("revive", "救援");
        put("rescue", "救援");
        put("downed", "倒地");
        put("lock", "锁定");
        put("reveal", "揭示");
        put("scan", "扫描");
        put("deploy", "部署");
        put("throw", "投掷");
        put("fuse", "引信");
        put("life", "存活");
        put("lifetime", "存活时间");
        put("window", "窗口");
        put("threshold", "阈值");
        put("bonus", "加成");
        put("penalty", "惩罚");
        put("reduction", "减伤/减免");
        put("vulnerability", "易伤");
        put("resistance", "抗性");
        put("knockback", "击退");
        put("stamina", "耐力");
        put("price", "价格");
        put("cost", "消耗");
        put("level", "等级");
        put("upgrade", "升级");
        put("purchase", "购买");
        put("x_offset", "水平偏移");
        put("y_offset", "垂直偏移");
        put("button_scale", "按钮缩放");
        put("base_max_energy", "基础最大能量");
        put("base_energy_regen_per_tick", "每tick基础能量恢复");
        put("cooldown_ticks", "冷却时间");
        put("recharge_ticks", "充能时间");
        put("duration_ticks", "持续时间");
        put("interval_ticks", "间隔时间");
        put("delay_ticks", "延迟时间");
        put("max_charges", "最大充能次数");
        put("max_health", "最大生命");
        put("movement_speed", "移动速度(格/秒)");
        put("follow_range", "索敌范围");
        put("attack_range", "攻击射程");
        put("shoot_interval_ticks", "射击间隔");
        put("reload_ticks", "装填时间");
        put("aura_radius", "光环半径");
        put("teleport_cooldown_ticks", "传送冷却");
        put("teleport_min_distance", "传送最小距离");
        put("teleport_max_distance", "传送最大距离");
        put("damage_phase1", "一阶段伤害");
        put("damage_phase2", "二阶段伤害");
        put("armor_phase1", "一阶段护甲");
        put("armor_phase2", "二阶段护甲");
        put("fire_burst_ticks", "开火连射时长");
        put("ideal_distance_min", "理想交战距离下限");
        put("ideal_distance_max", "理想交战距离上限");
        // extra common tokens from gameplay paths
        put("hold", "长按");
        put("long", "长");
        put("short", "短");
        put("prep", "准备");
        put("startup", "前摇");
        put("channel", "引导");
        put("cast", "施法");
        put("burst", "连射/爆发");
        put("fire", "开火/燃烧");
        put("aim", "瞄准");
        put("bolt", "枪机");
        put("ammo", "弹药");
        put("magazine", "弹匣");
        put("headshot", "爆头");
        put("sniper", "狙击");
        put("attachment", "配件");
        put("property", "属性");
        put("limit", "上限");
        put("floor", "下限");
        put("cap", "上限");
        put("scale", "缩放");
        put("offset", "偏移");
        put("button", "按钮");
        put("regen", "回复");
        put("decay", "衰减");
        put("drain", "消耗");
        put("gain", "获得");
        put("loss", "损失");
        put("initial", "初始");
        put("final", "最终");
        put("required", "所需");
        put("success", "成功");
        put("failure", "失败");
        put("chance", "几率");
        put("probability", "概率");
        put("random", "随机");
        put("target", "目标");
        put("targets", "目标数");
        put("count", "数量");
        put("amount", "数值");
        put("total", "总计");
        put("per", "每");
        put("tick", "tick");
        put("second", "秒");
        put("block", "方块");
        put("blocks", "方块");
        put("chunk", "区块");
        put("chunks", "区块");
        put("field", "力场/区域");
        put("cloud", "云雾");
        put("arrow", "箭矢");
        put("bomb", "炸弹");
        put("cannon", "炮");
        put("rocket", "火箭");
        put("blade", "刀刃");
        put("wire", "线");
        put("grapple", "钩索");
        put("shield", "盾");
        put("bash", "盾击");
        put("slide", "滑铲");
        put("dash", "冲刺");
        put("roll", "翻滚");
        put("jump", "跳跃");
        put("climb", "攀爬");
        put("zipline", "滑索");
        put("stim", "兴奋剂");
        put("adrenaline", "肾上腺素");
        put("tear", "催泪");
        put("gas", "毒气");
        put("firefly", "萤火虫");
        put("falcon", "猎鹰无人机");
        put("hummingbird", "蜂鸟");
        put("spider", "蜘蛛");
        put("nest", "巢");
        put("web", "网");
        put("webbed", "被网住");
        put("sonic", "声波");
        put("pulse", "脉冲");
        put("shock", "电击");
        put("composite", "复合");
        put("incendiary", "燃烧");
        put("loitering", "巡飞");
        put("cover", "掩体");
        put("quick", "快速");
        put("package", "投掷包");
        put("medical", "医疗");
        put("waste", "废料");
        put("plasma", "等离子");
        put("dust", "尘雾");
        put("vital", "维生");
        put("protection", "保护");
        put("correction", "修正");
        put("doomed", "厄运");
        put("rift", "裂隙");
        put("ritual", "仪式");
        put("sword", "剑");
        put("iron", "烙铁");
        put("hot", "热");
        put("crippled", "致残");
        put("stealth", "潜行");
        put("decoy", "诱饵");
        put("rotor", "旋翼");
        put("guard", "卫兵");
        put("hakim", "哈基姆");
        put("recruit", "招募");
        put("monitor", "监控");
        put("command", "指挥");
        put("point", "点");
        put("ray", "射线");
        put("laser", "激光");
        put("calibration", "校准");
        put("concealment", "隐蔽");
        put("vulnerability", "易伤");
        put("reduction", "减免");
        put("execution", "处决");
        put("ascend", "上升");
        put("countdown", "倒计时");
        put("patrol", "巡逻");
        put("tap", "点按");
        put("hiss", "嘶鸣");
        put("empowered", "强化");
        put("strike", "打击");
        put("fatal", "致命");
        put("uses", "次数");
        put("lives", "命数");
        put("truck", "卡车");
        put("highway", "公路");
        put("spine", "脊刺");
        put("block", "格挡");
        put("overload", "过载");
        put("hand", "手");
        put("cannon", "炮");
        put("smoke", "烟");
        put("tactical", "战术");
        put("boost", "加速/强化");
        put("forced", "强制");
        put("loading", "加载");
        put("streaming", "流式传输");
        put("immune", "免疫");
        put("justice", "正义");
        put("noon", "正午");
        put("ceasefire", "停火");
        put("gaze", "凝视");
        put("granted", "给予");
        put("output", "输出");
        put("copies", "拷贝数");
        put("fallback", "回退");
        put("freeze", "冻结");
        put("duel", "决斗");
        put("affection", "好感/连结");
        put("bgm", "背景音乐");
        put("track", "音轨");
        put("stale", "过期");
        put("alignment", "对齐度");
        put("axis", "轴线");
        put("off", "偏");
        put("direct", "直接");
        put("guided", "制导");
        put("homing", "追踪");
        put("seek", "搜寻");
        put("stop", "停止");
        put("arrival", "抵达");
        put("guidance", "制导");
        put("coast", "滑行");
        put("damped", "阻尼");
        put("terminal", "末端");
        put("drop", "下落");
        put("gravity", "重力");
        put("acceleration", "加速度");
        put("bounce", "弹跳");
        put("factor", "系数");
        put("armed", "已激活");
        put("arming", "激活中");
        put("manual", "手动");
        put("auto", "自动");
        put("detonation", "引爆");
        put("detonate", "引爆");
        put("explosion", "爆炸");
        put("splash", "溅射");
        put("flat", "固定");
        put("true", "真实");
        put("incoming", "受到的");
        put("outgoing", "造成的");
        put("non", "非");
        put("art", "术");
        put("reflect", "反射");
        put("projectile", "抛体");
        put("near", "近");
        put("far", "远");
        put("miss", "擦过");
        put("hit", "命中");
        put("rehit", "再次命中");
        put("injury", "受伤");
        put("wound", "创伤");
        put("delayed", "延迟");
        put("silence", "静默");
        put("warning", "预警");
        put("hearing", "听觉");
        put("footprint", "足迹");
        put("read", "读取");
        put("sync", "同步");
        put("stream", "流式");
        put("respawn", "重生");
        put("spawn", "生成");
        put("spawns", "生成数");
        put("entity", "实体");
        put("entities", "实体");
        put("ghost", "幽灵");
        put("visual", "视觉");
        put("marker", "标记");
        put("markers", "标记数");
        put("ttl", "存活");
        put("light", "光照");
        put("beam", "光束");
        put("progress", "进度");
        put("full", "满");
        put("opportunity", "机会");
        put("viewport", "视口");
        put("pressure", "压力");
        put("power", "力度/能力");
        put("steel", "钢铁");
        put("body", "身体");
        put("brave", "勇猛");
        put("disarmed", "缴械");
        put("explosive", "爆炸");
        put("spine", "脊");
        put("emergency", "紧急");
        put("recall", "召回");
        put("device", "装置");
        put("wall", "墙");
        put("drill", "钻");
        put("stinger", "刺针");
        put("equip", "装备");
        put("stow", "收起");
        put("throw", "投");
        put("lift", "上抛");
        put("release", "释放");
        put("prime", "预热");
        put("pullout", "拔出");
        put("pull", "拉拽");
        put("escape", "挣脱");
        put("self", "自身");
        put("reward", "自伤/自偿");
        put("server", "服务端");
        put("drift", "漂移");
        put("camera", "镜头");
        put("refresh", "刷新");
        put("owner", "所属");
        put("ally", "友方");
        put("forces", "部队");
        put("global", "全局");
        put("local", "本地");
        put("fov", "视野");
        put("maximum", "最大");
        put("degrees", "角度");

        alias("d_wolf", "hand_cannon", "hand_cannon");
        alias("d_wolf", "overload", "overload");
        alias("d_wolf", "smoke", "smoke");
        alias("d_wolf", "slide", "tactical_slide");
        alias("nox", "delayed_wound", "delayed_wound");
        alias("nox", "flash", "flash_grenade");
        alias("nox", "rotor", "rotor");
        alias("nox", "stealth", "silent_step");
        alias("nox", "decoy", "silent_step");
        alias("tempest", "wall_drill", "wall_drill_stinger");
        alias("tempest", "recall", "emergency_recall");
        alias("tempest", "roll", "tactical_roll");
        alias("tempest", "spine", "explosive_spine");
        alias("vyron", "tiger", "tiger_cannon");
        alias("vyron", "magnetic", "magnetic_bomb");
        alias("vyron", "dash", "jet_dash");
        alias("vyron", "landing", "powered_landing");
        alias("gizmo", "spider", "spider_nest");
        alias("gizmo", "smoke", "smoke_trap");
        alias("gizmo", "t_boy", "t_boy");
        alias("gizmo", "passive", "defensive_vanguard");
        alias("hackclaw", "flash_drone", "flash_drone");
        alias("hackclaw", "knife", "hacking_knife");
        alias("hackclaw", "core", "advanced_hack");
        alias("hackclaw", "silent", "silent_movement");
        alias("luna", "composite", "composite_grenade");
        alias("luna", "shock", "shock_arrow");
        alias("luna", "recon", "recon_arrow");
        alias("luna", "passive_reveal", "target_trace");
        alias("morse", "flash", "composite_flash");
        alias("morse", "shock", "shock_orb");
        alias("morse", "sonar", "sonar_detector");
        alias("morse", "sound_mark", "alert_hearing");
        alias("raptor", "footprint", "field_experience");
        alias("raptor", "falcon", "falcon_drone");
        alias("raptor", "pulse", "pulse_grenade");
        alias("raptor", "hummingbird", "hummingbird_camera");
        alias("stinger", "smoke_drone", "smoke_drone");
        alias("stinger", "stim", "stim_gun");
        alias("stinger", "smoke", "smoke_grenade");
        alias("stinger", "rescue", "professional_rescue");
        alias("stinger", "downed", "professional_rescue");
        alias("toxik", "firefly", "firefly_swarm");
        alias("toxik", "tear_gas", "tear_gas");
        alias("toxik", "adrenaline", "adrenaline");
        alias("toxik", "effect_multiplier", "efficient_treatment");
        alias("uluru", "loitering", "loitering_missile");
        alias("uluru", "incendiary", "incendiary");
        alias("uluru", "quick_cover", "quick_cover");
        alias("uluru", "negative_effect", "veteran");
        alias("shepherd", "sonic_trap", "sonic_trap");
        alias("shepherd", "frag", "frag_grenade");
        alias("shepherd", "drone", "drone_stun");
        alias("shepherd", "explosion", "reflex_field");
        alias("sineva", "blade_wire", "blade_wire");
        alias("sineva", "grapple", "grapple");
        alias("sineva", "shield", "bomb_suit");
        alias("sineva", "rear", "rear_guard");
        alias("saeed", "fire_arrow", "fire_arrow");
        alias("saeed", "roll", "tactical_roll");
        alias("saeed", "recruit", "command_order");
        alias("saeed", "tactical_point", "red_sky_hunt");
        alias("ghroth", "noon", "noon");
        alias("ghroth", "justice", "justice");
        alias("ghroth", "stars", "stars_orbit");
        alias("ghroth", "tactical", "demigod_brain");
        alias("catdad", "hiss", "triple_hiss");
        alias("catdad", "block", "spine_block");
        alias("catdad", "highway", "highway");
        alias("catdad", "bully", "bully");
        alias("department", "laser", "overheat_laser");
        alias("department", "trap", "armor");
        alias("department", "core", "contingency");
        alias("department", "reduction", "traffic_booth");
        alias("gambler", "active1", "final_bet");
        alias("gambler", "active2", "hakko_ichiu");
        alias("gambler", "core", "killing_gambler");
        alias("gambler", "shield", "super_shield");
        alias("manba", "elbow", "elbow");
        alias("manba", "flashlight", "flashlight");
        alias("manba", "duel", "la_legend");
        alias("manba", "talent", "evil_talent");
        alias("nikaidou_hiro", "correction", "correct_error");
        alias("nikaidou_hiro", "hot_iron", "erase_sin");
        alias("nikaidou_hiro", "ritual_sword", "only_i");
        alias("nikaidou_hiro", "rift", "remove_gap");
        alias("nikaidou_hiro_witchification", "rewind", "time_rewind");
        alias("nikaidou_hiro_witchification", "error", "erase_error");
        alias("nikaidou_hiro_witchification", "core", "save_everyone");
        alias("nikaidou_hiro_witchification", "health", "witchification");
        alias("vlinder", "active_defense", "active_defense_drone");
        alias("vlinder", "medical", "medical_drone");
        alias("vlinder", "smoke", "remote_smoke");
        alias("vlinder", "downed", "vital_monitoring");
    }

    private static void put(String key, String zh) {
        ZH.put(key.toLowerCase(Locale.ROOT), zh);
    }

    private static void alias(String characterId, String pathFragment, String skillId) {
        SKILL_ALIASES.computeIfAbsent(characterId, ignored -> new LinkedHashMap<>())
                .put(pathFragment, skillId);
    }

    private ConfigLabelHelper() {
    }

    public static boolean preferChinese() {
        try {
            String code = Minecraft.getInstance().options.languageCode;
            return code != null && code.toLowerCase(Locale.ROOT).startsWith("zh");
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public static Component categoryTitle(String categoryKey) {
        String key = "config.dealt_force_skills.category." + categoryKey;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.literal(translateSegment(categoryKey));
    }

    public static Component subcategoryTitle(String categoryKey, String subcategoryKey) {
        String resolved = resolveCharacterDisplayName(subcategoryKey);
        if (resolved != null) {
            return Component.literal(resolved);
        }
        // Items / consumables / equipment ids → item lang
        String itemKey = "item.dealt_force_skills." + subcategoryKey;
        if (I18n.exists(itemKey)) {
            return Component.translatable(itemKey);
        }
        String key = "config.dealt_force_skills.sub." + categoryKey + "." + subcategoryKey;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        key = "config.dealt_force_skills.segment." + subcategoryKey;
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.literal(translateSegment(subcategoryKey));
    }

    public static Component entryLabel(ConfigEntryData entry) {
        String fullKey = "config.dealt_force_skills.entry." + entry.file().name().toLowerCase(Locale.ROOT)
                + "." + entry.path().replace('.', '_');
        if (I18n.exists(fullKey)) {
            return Component.translatable(fullKey);
        }
        String leafKey = "config.dealt_force_skills.leaf." + leaf(entry.path());
        if (I18n.exists(leafKey)) {
            // Prefer "角色 · 参数" context when available
            String contextual = buildLabel(entry);
            if (preferChinese() && contextual.contains("·")) {
                return Component.literal(contextual);
            }
            return Component.translatable(leafKey);
        }
        return Component.literal(buildLabel(entry));
    }

    public static Component entryTooltip(ConfigEntryData entry) {
        List<String> lines = new ArrayList<>();
        lines.add(entryLabel(entry).getString());
        lines.add(buildPurpose(entry));
        lines.add(preferChinese()
                ? "文件: " + entry.file().fileName()
                : "File: " + entry.file().fileName());
        lines.add(preferChinese()
                ? "键路径: " + entry.path()
                : "Key: " + entry.path());
        lines.add(preferChinese()
                ? "类型: " + kindChinese(entry.kind().name()) + "  当前: " + entry.valueText()
                : "Type: " + entry.kind().name() + "  Value: " + entry.valueText());
        String unit = unitHint(leaf(entry.path()));
        if (!unit.isEmpty()) {
            lines.add(unit);
        }
        // Skill name hint when path contains a recognizable skill token
        String skillHint = skillNameHint(entry.path());
        if (skillHint != null) {
            lines.add(preferChinese() ? "关联技能: " + skillHint : "Skill: " + skillHint);
        }
        if (entry.comment() != null && !entry.comment().isBlank()
                && !entry.comment().startsWith("Controls ")) {
            lines.add(entry.comment());
        }
        return Component.literal(String.join("\n", lines));
    }

    public static Component fileBadge(ConfigFileId file) {
        String key = "config.dealt_force_skills.file." + file.name().toLowerCase(Locale.ROOT);
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.literal(file.fileName());
    }

    private static String buildLabel(ConfigEntryData entry) {
        String path = entry.path();
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return path;
        }
        if (!preferChinese()) {
            return humanizeEnglish(parts);
        }
        String leaf = parts[parts.length - 1];
        String leafZh = translateLeaf(leaf);
        // Prefer short readable: 角色中文名 · 参数中文
        String context;
        if (parts.length >= 2 && "characters".equals(parts[0])) {
            String charName = resolveCharacterDisplayName(parts[1]);
            context = charName != null ? charName : translateSegment(parts[1]);
            String skill = skillNameHint(path);
            if (skill != null) {
                context = context + " · " + skill;
            } else if (parts.length >= 3) {
                context = context + " · " + shortenModule(parts[2]);
            }
        } else if (entry.file() == ConfigFileId.BOSSES) {
            context = "信标".equals(translateSegment(parts[0])) || "beacon".equalsIgnoreCase(parts[0])
                    ? "信标Boss"
                    : translateSegment(parts[0]);
        } else if (parts.length >= 2 && ("consumables".equals(parts[0]) || "items".equals(parts[0])
                || "equipment".equals(parts[0]))) {
            String itemKey = "item.dealt_force_skills." + parts[1];
            context = I18n.exists(itemKey) ? I18n.get(itemKey) : translateSegment(parts[1]);
        } else if (parts.length >= 2) {
            context = translateSegment(parts[0]);
            if (parts.length >= 3 && !parts[1].equals(leaf)) {
                context = context + " · " + translateSegment(parts[1]);
            }
        } else {
            context = "";
        }
        if (context.isEmpty()) {
            return leafZh;
        }
        return context + " · " + leafZh;
    }

    private static String buildPurpose(ConfigEntryData entry) {
        if (!preferChinese()) {
            return "Controls " + entry.path().replace('.', ' ');
        }
        String leaf = leaf(entry.path());
        String[] parts = entry.path().split("\\.");
        String who = "";
        if (parts.length >= 2 && "characters".equals(parts[0])) {
            String name = resolveCharacterDisplayName(parts[1]);
            who = "角色「" + (name != null ? name : translateSegment(parts[1])) + "」";
            String skill = skillNameHint(entry.path());
            if (skill != null) {
                who = who + "技能「" + skill + "」";
            }
        } else if (entry.file() == ConfigFileId.BOSSES) {
            who = "Boss「" + ("beacon".equalsIgnoreCase(parts[0]) ? "信标" : translateSegment(parts[0])) + "」";
        } else if (parts.length >= 2 && ("consumables".equals(parts[0]) || "items".equals(parts[0])
                || "equipment".equals(parts[0]))) {
            String itemKey = "item.dealt_force_skills." + parts[1];
            who = "物品「" + (I18n.exists(itemKey) ? I18n.get(itemKey) : translateSegment(parts[1])) + "」";
        } else if (parts.length >= 2) {
            who = "「" + translateSegment(parts[0]) + " / " + translateSegment(parts[1]) + "」";
        } else if (parts.length == 1) {
            who = "「" + translateSegment(parts[0]) + "」";
        }
        String purpose = purposeForLeaf(leaf);
        StringBuilder sb = new StringBuilder();
        sb.append("参数作用: ");
        if (!who.isEmpty()) {
            sb.append("调整").append(who).append("的");
        } else {
            sb.append("调整");
        }
        sb.append(purpose).append("。");
        SkillReference skill = skillReferenceHint(entry.path());
        if (skill != null && !skill.description().isBlank()) {
            sb.append(" 机制说明: ").append(skill.description());
        }
        String itemDescription = itemDescriptionHint(entry.path());
        if (itemDescription != null) {
            sb.append(" 物品说明: ").append(itemDescription);
        }
        sb.append(" 生效方式: 保存后立即应用；也可用 /dealtreload 从磁盘重读。");
        return sb.toString();
    }

    /**
     * Resolve config path character segment to official Chinese name from lang (design source of truth).
     */
    private static String resolveCharacterDisplayName(String configId) {
        if (configId == null || configId.isBlank()) {
            return null;
        }
        String langId = normalizeCharacterId(configId);
        String key = "character.dealt_force_skills." + langId + ".name";
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        // Alternate ids used in config trees
        for (String alt : characterLangCandidates(configId)) {
            String altKey = "character.dealt_force_skills." + alt + ".name";
            if (I18n.exists(altKey)) {
                return I18n.get(altKey);
            }
        }
        String lower = configId.toLowerCase(Locale.ROOT);
        if (ZH.containsKey(lower)) {
            return ZH.get(lower);
        }
        return null;
    }

    private static List<String> characterLangCandidates(String configId) {
        String lower = configId.toLowerCase(Locale.ROOT);
        List<String> list = new ArrayList<>();
        list.add(lower);
        list.add(normalizeCharacterId(lower));
        switch (lower) {
            case "dwolf" -> list.add("d_wolf");
            case "d_wolf" -> list.add("dwolf");
            case "lexninjia" -> list.add("lex_ninjia");
            case "lex_ninjia" -> list.add("lexninjia");
            case "ntwo", "n_two" -> {
                list.add("ntwo");
                list.add("n_two");
            }
            case "nikaidou" -> {
                list.add("nikaidou_hiro");
                list.add("nikaidou_hiro_witchification");
            }
            case "department" -> list.add("department");
            default -> {
            }
        }
        return list;
    }

    /**
     * Try to map path tokens to a skill display name via character.*.skill.*.
     */
    private static String skillNameHint(String path) {
        SkillReference reference = skillReferenceHint(path);
        if (reference != null) {
            return reference.name();
        }
        if (path == null || !path.startsWith("characters.")) {
            return null;
        }
        String[] parts = path.split("\\.");
        if (parts.length < 3) {
            return null;
        }
        String charLang = normalizeCharacterId(parts[1]);
        // Scan path tokens for known skill keys under this character.
        for (int i = 2; i < parts.length; i++) {
            String token = parts[i].toLowerCase(Locale.ROOT);
            // strip common suffixes
            String skillToken = token
                    .replace("_cooldown_ticks", "")
                    .replace("_recharge_ticks", "")
                    .replace("_duration_ticks", "")
                    .replace("_max_charges", "")
                    .replace("_ticks", "")
                    .replace("_range", "")
                    .replace("_radius", "")
                    .replace("_damage", "")
                    .replace("_speed", "");
            if (skillToken.isEmpty() || skillToken.equals(token) && token.contains("state_manager")) {
                continue;
            }
            // Direct skill id
            String skillKey = "character.dealt_force_skills." + charLang + ".skill." + skillToken;
            if (I18n.exists(skillKey)) {
                return I18n.get(skillKey);
            }
            // Footprint / falcon / pulse / hummingbird aliases for raptor
            if ("raptor".equals(charLang) || "raptor".equals(parts[1])) {
                if (token.contains("footprint")) {
                    return skillOr("character.dealt_force_skills.raptor.skill.field_experience", "经验之谈");
                }
                if (token.contains("falcon")) {
                    return skillOr("character.dealt_force_skills.raptor.skill.falcon_drone", "猎鹰无人机");
                }
                if (token.contains("pulse")) {
                    return skillOr("character.dealt_force_skills.raptor.skill.pulse_grenade", "脉冲手雷");
                }
                if (token.contains("hummingbird")) {
                    return skillOr("character.dealt_force_skills.raptor.skill.hummingbird_camera", "蜂鸟无人机");
                }
            }
        }
        return null;
    }

    private static SkillReference skillReferenceHint(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String lowerPath = path.toLowerCase(Locale.ROOT);
        String[] parts = lowerPath.split("\\.");
        if (parts.length < 2 || !"characters".equals(parts[0])) {
            return null;
        }
        String characterId = normalizeCharacterId(parts[1]);
        if (lowerPath.contains("witchification")) {
            characterId = "nikaidou_hiro_witchification";
        }
        Map<String, String> aliases = SKILL_ALIASES.get(characterId);
        if (aliases == null) {
            return null;
        }
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            if (!lowerPath.contains(alias.getKey())) {
                continue;
            }
            String baseKey = "character.dealt_force_skills." + characterId + ".skill." + alias.getValue();
            if (!I18n.exists(baseKey)) {
                continue;
            }
            String descriptionKey = baseKey + ".desc";
            return new SkillReference(I18n.get(baseKey), I18n.exists(descriptionKey) ? I18n.get(descriptionKey) : "");
        }
        return null;
    }

    private static String itemDescriptionHint(String path) {
        if (path == null) {
            return null;
        }
        String[] parts = path.split("\\.");
        if (parts.length < 2 || !("consumables".equals(parts[0]) || "items".equals(parts[0])
                || "equipment".equals(parts[0]))) {
            return null;
        }
        String itemId = parts[1];
        String key = "tooltip.dealt_force_skills." + itemId;
        if (!I18n.exists(key) && itemId.endsWith("_item")) {
            key = "tooltip.dealt_force_skills." + itemId.substring(0, itemId.length() - 5);
        }
        return I18n.exists(key) ? I18n.get(key) : null;
    }

    private static String skillOr(String key, String fallback) {
        return I18n.exists(key) ? I18n.get(key) : fallback;
    }

    private static String purposeForLeaf(String leaf) {
        String lower = leaf.toLowerCase(Locale.ROOT);
        switch (lower) {
            case "durability" -> {
                return "物品最大耐久值";
            }
            case "durability_per_second" -> {
                return "持续使用时每秒消耗的耐久值";
            }
            case "heal_percent" -> {
                return "每次治疗恢复的最大生命值百分比";
            }
            case "pain_relief_extra_cost" -> {
                return "补充止痛效果时额外消耗的耐久值";
            }
            case "use_ticks", "use_duration" -> {
                return "完成一次物品使用所需时间";
            }
            case "life_ticks", "lifetime_ticks", "max_lifetime_ticks" -> {
                return "实体或部署物生成后的最长存在时间";
            }
            case "fuse_ticks", "default_fuse_ticks" -> {
                return "投掷物从激活到爆炸的引信时间";
            }
            case "max_health" -> {
                return "实体最大生命值";
            }
            case "max_active", "active_limit" -> {
                return "同一施法者可同时保留的最大实体数量";
            }
            default -> {
            }
        }
        if (ZH.containsKey(lower)) {
            return ZH.get(lower);
        }
        if (lower.endsWith("_cooldown_ticks") || lower.equals("cooldown_ticks")) {
            return "冷却时间";
        }
        if (lower.endsWith("_recharge_ticks") || lower.equals("recharge_ticks")) {
            return "充能恢复时间";
        }
        if (lower.endsWith("_duration_ticks") || lower.equals("duration_ticks")) {
            return "持续时间";
        }
        if (lower.endsWith("_interval_ticks") || lower.equals("interval_ticks")) {
            return "触发/刷新间隔";
        }
        if (lower.endsWith("_delay_ticks") || lower.equals("delay_ticks")) {
            return "延迟时间";
        }
        if (lower.endsWith("_max_charges") || lower.equals("max_charges")) {
            return "最大充能次数";
        }
        if (lower.endsWith("_damage") || lower.equals("damage")) {
            return "造成的伤害数值";
        }
        if (lower.endsWith("_radius") || lower.equals("radius")) {
            return "作用半径（方块）";
        }
        if (lower.endsWith("_range") || lower.equals("range")) {
            return "作用/索敌范围（方块）";
        }
        if (lower.endsWith("_distance") || lower.equals("distance")) {
            return "距离相关参数（方块）";
        }
        if (lower.endsWith("_speed") || lower.equals("speed")) {
            return "速度相关参数";
        }
        if (lower.endsWith("_amplifier") || lower.equals("amplifier")) {
            return "药水/状态效果等级（0=I）";
        }
        if (lower.endsWith("_fraction") || lower.endsWith("_percent") || lower.endsWith("_permille")) {
            return "比例/百分比相关参数";
        }
        if (lower.endsWith("_multiplier")) {
            return "倍率（1.0=不变）";
        }
        if (lower.endsWith("_cost") || lower.contains("energy_cost") || lower.contains("health_cost")) {
            return "释放/维持消耗";
        }
        if (lower.equals("enabled")) {
            return "功能开关";
        }
        if (lower.equals("x_offset") || lower.equals("y_offset")) {
            return "界面位置偏移（像素）";
        }
        if (lower.equals("button_scale")) {
            return "技能按钮缩放倍率";
        }
        if (lower.equals("stamina_x_offset") || lower.equals("stamina_y_offset")) {
            return "体力条位置偏移（通用偏移与角色偏移相加）";
        }
        if (lower.equals("stamina_scale")) {
            return "体力条缩放倍率（通用倍率与角色倍率相乘）";
        }
        return translateLeaf(leaf);
    }

    private static String unitHint(String leaf) {
        String lower = leaf.toLowerCase(Locale.ROOT);
        if (!preferChinese()) {
            if (lower.endsWith("_ticks")) return "Unit: ticks (20 = 1s)";
            if (lower.endsWith("_seconds")) return "Unit: seconds";
            if (lower.endsWith("_fraction")) return "Unit: fraction (1.0 = 100%)";
            if (lower.endsWith("_percent")) return "Unit: percent (100 = 100%)";
            if (lower.endsWith("_permille")) return "Unit: permille (1000 = 100%)";
            if (lower.endsWith("_multiplier")) return "Unit: multiplier (1.0 = unchanged)";
            if (lower.endsWith("_radius") || lower.endsWith("_range") || lower.endsWith("_distance")) {
                return "Unit: blocks";
            }
            if (lower.endsWith("_damage") || lower.equals("damage") || lower.contains("heal")) {
                return "Unit: health points (2 = 1 heart)";
            }
            return "";
        }
        if (lower.endsWith("_ticks")) return "单位: tick（20 tick = 1 秒）";
        if (lower.endsWith("_seconds")) return "单位: 秒";
        if (lower.endsWith("_fraction")) return "单位: 比例（1.0 = 100%）";
        if (lower.endsWith("_percent")) return "单位: 百分比（100 = 100%）";
        if (lower.endsWith("_permille")) return "单位: 千分比（1000 = 100%）";
        if (lower.endsWith("_multiplier")) return "单位: 倍率（1.0 = 不变）";
        if (lower.endsWith("_radius") || lower.endsWith("_range") || lower.endsWith("_distance")) {
            return "单位: 方块";
        }
        if (lower.endsWith("_damage") || lower.equals("damage") || lower.contains("heal")) {
            return "单位: 生命值（2点=1心）";
        }
        if (lower.endsWith("_amplifier")) return "单位: 效果等级（0=I级）";
        if (lower.equals("x_offset") || lower.equals("y_offset")
                || lower.equals("stamina_x_offset") || lower.equals("stamina_y_offset")) return "单位: 像素";
        if (lower.equals("button_scale") || lower.equals("stamina_scale")) return "单位: 倍率（0.25~3.0）";
        return "";
    }

    private static String translateLeaf(String leaf) {
        String lower = leaf.toLowerCase(Locale.ROOT);
        String key = "config.dealt_force_skills.leaf." + lower;
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        if (ZH.containsKey(lower)) {
            return ZH.get(lower);
        }
        // strip common suffixes for word translation then re-attach unit tag
        String base = lower;
        String suffixZh = "";
        if (base.endsWith("_ticks")) {
            base = base.substring(0, base.length() - 6);
            suffixZh = preferChinese() ? "（tick）" : " (ticks)";
        } else if (base.endsWith("_seconds")) {
            base = base.substring(0, base.length() - 8);
            suffixZh = preferChinese() ? "（秒）" : " (s)";
        } else if (base.endsWith("_fraction")) {
            base = base.substring(0, base.length() - 9);
            suffixZh = preferChinese() ? "（比例）" : " (fraction)";
        } else if (base.endsWith("_percent")) {
            base = base.substring(0, base.length() - 8);
            suffixZh = preferChinese() ? "（%）" : " (%)";
        } else if (base.endsWith("_permille")) {
            base = base.substring(0, base.length() - 9);
            suffixZh = preferChinese() ? "（‰）" : " (‰)";
        } else if (base.endsWith("_multiplier")) {
            base = base.substring(0, base.length() - 11);
            suffixZh = preferChinese() ? "（倍率）" : " (x)";
        } else if (base.endsWith("_sqr")) {
            base = base.substring(0, base.length() - 4);
            suffixZh = preferChinese() ? "（平方）" : " (sqr)";
        }
        return translateWords(base) + suffixZh;
    }

    private static String translateSegment(String segment) {
        if (segment == null || segment.isBlank()) {
            return "";
        }
        String lower = segment.toLowerCase(Locale.ROOT);
        String key = "config.dealt_force_skills.segment." + lower;
        if (I18n.exists(key)) {
            return I18n.get(key);
        }
        if (ZH.containsKey(lower)) {
            return ZH.get(lower);
        }
        if (lower.chars().allMatch(Character::isDigit)) {
            return "#" + lower;
        }
        // drop _state_manager / _skills noise
        if (lower.endsWith("_state_manager")) {
            return translateSegment(lower.substring(0, lower.length() - "_state_manager".length()))
                    + (preferChinese() ? "状态" : " state");
        }
        if (lower.endsWith("_skills")) {
            return translateSegment(lower.substring(0, lower.length() - "_skills".length()))
                    + (preferChinese() ? "技能" : " skills");
        }
        return translateWords(lower);
    }

    private static String shortenModule(String module) {
        String lower = module.toLowerCase(Locale.ROOT);
        if (lower.endsWith("_state_manager")) {
            return preferChinese() ? "状态" : "state";
        }
        if (lower.endsWith("_skills")) {
            return preferChinese() ? "技能" : "skills";
        }
        if (lower.endsWith("_placement_helper")) {
            return preferChinese() ? "放置" : "place";
        }
        return translateSegment(module);
    }

    private static String translateWords(String snake) {
        if (!preferChinese()) {
            return titleCase(snake.replace('_', ' '));
        }
        if (ZH.containsKey(snake)) {
            return ZH.get(snake);
        }
        String[] words = snake.split("_");
        StringBuilder out = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) {
                continue;
            }
            if (ZH.containsKey(w)) {
                out.append(ZH.get(w));
            } else if (w.chars().allMatch(Character::isDigit)) {
                out.append(w);
            } else {
                // keep unknown English token readable
                out.append(w);
            }
        }
        // If almost nothing translated (still mostly ascii words stuck), join with ·
        if (out.length() == snake.replace("_", "").length() && words.length > 1
                && out.toString().matches("[a-zA-Z0-9]+")) {
            StringBuilder alt = new StringBuilder();
            for (String w : words) {
                if (alt.length() > 0) {
                    alt.append('·');
                }
                alt.append(ZH.getOrDefault(w, w));
            }
            return alt.toString();
        }
        return out.length() == 0 ? snake : out.toString();
    }

    private static String humanizeEnglish(String[] parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(titleCase(parts[i].replace('_', ' ')));
        }
        return builder.toString();
    }

    private static String kindChinese(String kind) {
        return switch (kind) {
            case "BOOLEAN" -> "开关(true/false)";
            case "INTEGER" -> "整数";
            case "LONG" -> "长整数";
            case "FLOAT" -> "小数(float)";
            case "DOUBLE" -> "小数(double)";
            case "STRING" -> "文本";
            default -> kind;
        };
    }

    private static String leaf(String path) {
        int index = path.lastIndexOf('.');
        return index < 0 ? path : path.substring(index + 1);
    }

    private record SkillReference(String name, String description) {
    }

    private static String normalizeCharacterId(String id) {
        if (id == null) {
            return "";
        }
        String lower = id.toLowerCase(Locale.ROOT);
        // Map config-tree ids → zh_cn character.dealt_force_skills.<id>.name keys
        return switch (lower) {
            case "dwolf" -> "d_wolf";
            case "lexninjia" -> "lex_ninjia";
            case "n_two" -> "ntwo"; // lang key is ntwo, not n_two
            case "nikaidou" -> "nikaidou_hiro";
            default -> lower;
        };
    }

    private static String titleCase(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        boolean start = true;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                builder.append(ch);
                start = true;
            } else if (start) {
                builder.append(Character.toUpperCase(ch));
                start = false;
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}