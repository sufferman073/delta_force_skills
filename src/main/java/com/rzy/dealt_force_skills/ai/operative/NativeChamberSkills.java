package com.rzy.dealt_force_skills.ai.operative;

import com.rzy.dealt_force_skills.character.ModCharacters;
import com.rzy.dealt_force_skills.character.SkillSlot;
import com.rzy.dealt_force_skills.character.chamber.*;
import com.rzy.dealt_force_skills.entity.*;
import com.rzy.dealt_force_skills.registry.ModEntities;
import com.rzy.dealt_force_skills.registry.ModSounds;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Native cards, retreat and renewable TaCZ core reserve. Player currency is not an AI resource. */
public final class NativeChamberSkills implements OperativeSkills {
    private UUID anchor,trap;
    private Vec3 anchorPosition,trapPosition;
    private int anchorMissing,trapMissing,teleportCooldown,trapCooldown,pistolCooldown,sniperCooldown;
    private long slowReadyAt;
    private CompoundTag carriedWeapon=new CompoundTag();
    private int idleWeaponTicks;
    private ChamberGunKind equipped=ChamberGunKind.NONE;
    public boolean owns(UUID id){return id!=null && (id.equals(anchor)||id.equals(trap));}
    public ChamberGunKind equipped(){return equipped;}

    private static boolean valid(OperativeHost host,LivingEntity target){return target!=null && target.level()==host.body().level() && host.isEnemy(target) && host.withinCombatRange(target.position()) && host.body().distanceToSqr(target)<=48*48 && host.canSee(target);}
    private static boolean available(OperativeHost host,SkillSlot slot){return NativeRaptorSkills.available(host) && !OperativeSupport.downed(host.body()) && !OperativeElectronics.blocked(host,ModCharacters.CHAMBER_ID,slot);}
    @Override public double score(OperativeHost host,SkillSlot slot,LivingEntity target){
        if(!available(host,slot))return 0;
        return switch(slot){
            case ACTIVE_1 -> retreat(host)?130:anchor==null && teleportCooldown==0 && valid(host,target) && cardPoint(host,target,false)!=null?78:0;
            case ACTIVE_2 -> trap==null && trapCooldown==0 && valid(host,target) && cardPoint(host,target,true)!=null?84:0;
            case CORE -> !(host.parallelNativeAi() && com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(host.body()).getSynReloadState().getStateType().isReloading()) && equipped==ChamberGunKind.NONE && valid(host,target) && chooseGun(host,target)!=ChamberGunKind.NONE?95:0;
            default -> 0;
        };
    }
    private ChamberGunKind chooseGun(OperativeHost host,LivingEntity target){
        if(host.body().distanceToSqr(target)>=16*16 && sniperCooldown==0)return ChamberGunKind.TOUR_DE_FORCE;
        return pistolCooldown==0?ChamberGunKind.HEADHUNTER:sniperCooldown==0?ChamberGunKind.TOUR_DE_FORCE:ChamberGunKind.NONE;
    }
    @Override public boolean start(OperativeHost host,SkillSlot slot,LivingEntity target){
        if(score(host,slot,target)<=0)return false;
        if(slot==SkillSlot.CORE){
            var kind=chooseGun(host,target);var stack=ChamberTaczEquipment.create(kind);var gun=IGun.getIGunOrNull(stack);
            if(gun==null)return false;
            OperativeGun.supplyReserve(stack);
            if(host.body() instanceof OperativeEntity bot)bot.gun().cancel(bot);
            var carried=host.body().getMainHandItem();
            carriedWeapon=carried.isEmpty()?new CompoundTag():carried.save(new CompoundTag());
            idleWeaponTicks=0;
            host.body().setItemInHand(InteractionHand.MAIN_HAND,stack);equipped=kind;
            host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.CHAMBER_TOUR_DE_FORCE.get(),SoundSource.NEUTRAL,.8F,1);
            return true;
        }
        if(slot==SkillSlot.ACTIVE_1 && retreat(host)){
            var marker=host.serverLevel().getEntity(anchor);Vec3 point=marker.position();
            host.body().teleportTo(point.x,point.y,point.z);host.body().setDeltaMovement(Vec3.ZERO);host.body().fallDistance=0;
            if(!host.parallelNativeAi() && host.body() instanceof net.minecraft.world.entity.Mob mob)mob.getNavigation().stop();
            marker.discard();finishMarker(marker.getUUID());
            host.serverLevel().playSound(null,host.body().blockPosition(),ModSounds.CHAMBER_TELEPORT_CARD.get(),SoundSource.NEUTRAL,.85F,1);return true;
        }
        boolean isTrap=slot==SkillSlot.ACTIVE_2;Vec3 point=cardPoint(host,target,isTrap);if(point==null)return false;
        Vec3 direction=point.subtract(host.body().getEyePosition()).normalize();
        var card=new ChamberCardProjectileEntity(ModEntities.CHAMBER_CARD.get(),host.body().level(),host.body(),isTrap?ChamberMarkerType.TRAP:ChamberMarkerType.TELEPORT_ANCHOR);
        card.setPos(host.body().getEyePosition().add(direction.scale(.65)).add(0,-.12,0));card.setDeltaMovement(direction.scale(1.65));
        if(!host.serverLevel().addFreshEntity(card))return false;
        if(isTrap){trap=card.getUUID();trapPosition=point;trapMissing=0;}else{anchor=card.getUUID();anchorPosition=point;anchorMissing=0;}
        return true;
    }
    private boolean retreat(OperativeHost host){
        LivingEntity target=host.body() instanceof net.minecraft.world.entity.Mob mob?mob.getTarget():null;
        boolean threatened=host.body().getHealth()<host.body().getMaxHealth()*.5F
                || target!=null && host.isEnemy(target) && (host.body().distanceToSqr(target)<64
                    || host.body() instanceof OperativeEntity bot && (bot.gun().reloading(bot) || !host.canSee(target)));
        return threatened && anchor!=null && host.body().onGround()
                && host.serverLevel().getEntity(anchor) instanceof ChamberTeleportAnchorEntity marker
                && marker.distanceToSqr(host.body())>4 && marker.distanceToSqr(host.body())<=ChamberStateManager.TELEPORT_RADIUS*ChamberStateManager.TELEPORT_RADIUS
                && safe(host,marker.position())
                && (target==null || marker.distanceToSqr(target)>host.body().distanceToSqr(target)+9);
    }
    private static Vec3 cardPoint(OperativeHost host,LivingEntity target,boolean trap){
        if(target==null)return null;
        Vec3 delta=target.position().subtract(host.body().position()).multiply(1,0,1);if(delta.lengthSqr()<.01)return null;
        Vec3 desired=host.body().position().add(delta.normalize().scale(trap?2.5:-2.5));
        for(int dy=1;dy>=-2;dy--){
            BlockPos feet=BlockPos.containing(desired).offset(0,dy,0);Vec3 point=Vec3.atBottomCenterOf(feet);
            if(!safe(host,point))continue;
            var hit=host.serverLevel().clip(new ClipContext(host.body().getEyePosition(),point.add(0,-.1,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,host.body()));
            if(hit.getType()==HitResult.Type.BLOCK && hit.getDirection()==Direction.UP && hit.getLocation().distanceToSqr(point)<1)return point;
        }
        return null;
    }
    public static boolean safe(OperativeHost host,Vec3 point){
        if(!Double.isFinite(point.x)||!Double.isFinite(point.y)||!Double.isFinite(point.z)||!host.withinOrders(point))return false;
        var level=host.serverLevel();var box=host.body().getBoundingBox().move(point.subtract(host.body().position()));var below=BlockPos.containing(point).below();
        return level.hasChunkAt(BlockPos.containing(point)) && box.minY>=level.getMinBuildHeight() && box.maxY<=level.getMaxBuildHeight()
                && level.getWorldBorder().isWithinBounds(box) && level.noCollision(host.body(),box) && !level.containsAnyLiquid(box)
                && level.getBlockState(below).isFaceSturdy(level,below,Direction.UP);
    }
    @Override public void tick(OperativeHost host){
        if(teleportCooldown>0)teleportCooldown--;if(trapCooldown>0)trapCooldown--;if(pistolCooldown>0)pistolCooldown--;if(sniperCooldown>0)sniperCooldown--;
        if(anchor!=null){Entity entity=host.serverLevel().getEntity(anchor);if(entity!=null){anchorPosition=entity.position();anchorMissing=0;}else if(anchorPosition==null||host.serverLevel().hasChunkAt(BlockPos.containing(anchorPosition))){if(++anchorMissing>40)finishMarker(anchor);}}
        if(trap!=null){Entity entity=host.serverLevel().getEntity(trap);if(entity!=null){trapPosition=entity.position();trapMissing=0;}else if(trapPosition==null||host.serverLevel().hasChunkAt(BlockPos.containing(trapPosition))){if(++trapMissing>40)finishMarker(trap);}}
        if(equipped!=ChamberGunKind.NONE){
            var stack=host.body().getMainHandItem();var gun=IGun.getIGunOrNull(stack);
            if(ChamberTaczEnhancement.kind(stack)!=equipped || gun==null
                    || gun.getCurrentAmmoCount(stack)<=0 && !gun.hasBulletInBarrel(stack))stow(host);
            else {
                OperativeGun.supplyReserve(stack);
                LivingEntity target=host.body() instanceof net.minecraft.world.entity.Mob mob?mob.getTarget():null;
                if(!valid(host,target))idleWeaponTicks++;else idleWeaponTicks=0;
                if(idleWeaponTicks>=100 || target!=null && equipped==ChamberGunKind.TOUR_DE_FORCE
                        && host.body().distanceToSqr(target)<100 && !carriedWeapon.isEmpty())stow(host);
            }
        }
        slowReadyAt=Math.min(slowReadyAt,host.serverLevel().getGameTime()+ChamberStateManager.TOUR_SLOW_FIELD_TRIGGER_INTERVAL_TICKS);
    }
    public void stow(OperativeHost host){
        if(equipped==ChamberGunKind.NONE)return;
        ItemStack carried=takeCarried(host);
        if(ChamberTaczEnhancement.kind(host.body().getMainHandItem())==equipped || host.body().getMainHandItem().isEmpty())
            host.body().setItemInHand(InteractionHand.MAIN_HAND,carried);
        else if(!carried.isEmpty())host.body().spawnAtLocation(carried);
        if(host.body() instanceof OperativeEntity bot)bot.gun().cancel(bot);
        idleWeaponTicks=0;
        if(equipped==ChamberGunKind.HEADHUNTER)pistolCooldown=ChamberStateManager.HEADHUNTER_COOLDOWN_TICKS;else sniperCooldown=ChamberStateManager.TOUR_DE_FORCE_COOLDOWN_TICKS;
        equipped=ChamberGunKind.NONE;
    }
    public ItemStack takeCarried(OperativeHost host){
        ItemStack stack=carriedWeapon.isEmpty()?ItemStack.EMPTY:ItemStack.of(carriedWeapon);
        carriedWeapon=new CompoundTag();OperativeGun.preparePlayerTransfer(stack);return stack;
    }
    public void finishMarker(UUID id){
        if(id!=null && id.equals(anchor)){anchor=null;anchorPosition=null;anchorMissing=0;teleportCooldown=ChamberStateManager.TELEPORT_COOLDOWN_TICKS;}
        if(id!=null && id.equals(trap)){trap=null;trapPosition=null;trapMissing=0;trapCooldown=ChamberStateManager.TRAP_COOLDOWN_TICKS;}
    }
    public static OperativeHost root(Entity entity,OperativeAllegiance allegiance){return entity.level() instanceof ServerLevel level && allegiance.caster()!=null && level.getEntity(allegiance.caster()) instanceof OperativeHost root?root:null;}
    public static void endDevice(Entity entity,OperativeAllegiance allegiance){var root=root(entity,allegiance);if(root!=null && root.skills() instanceof NativeChamberSkills skills)skills.finishMarker(entity.getUUID());entity.discard();}
    public static void tickDevice(Entity entity,OperativeAllegiance allegiance,boolean isTrap){
        var root=root(entity,allegiance);
        if(entity.tickCount>=72000){endDevice(entity,allegiance);return;}
        if(root==null)return; // Owner and marker may load in either order.
        if(!root.body().isAlive() || !(root.skills() instanceof NativeChamberSkills skills) || !skills.owns(entity.getUUID()) || !root.withinOrders(entity.position())){endDevice(entity,allegiance);return;}
        allegiance.capture(root.body());
        if(isTrap && OperativeTrapMaintenance.stale(entity,allegiance,4)){endDevice(entity,allegiance);return;}
        if(!isTrap || entity.tickCount%5!=0 || !available(root,SkillSlot.ACTIVE_2))return;
        var level=root.serverLevel();
        for(var target:level.getEntitiesOfClass(LivingEntity.class,entity.getBoundingBox().inflate(4),root::isEnemy)){
            if(target.distanceToSqr(entity)>16 || !root.withinOrders(target.position()))continue;
            var hit=level.clip(new ClipContext(entity.position().add(0,.2,0),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
            if(hit.getType()==HitResult.Type.MISS && spawnSlow(root,target.position())){endDevice(entity,allegiance);return;}
        }
    }
    public static void landCard(ChamberCardProjectileEntity card,OperativeAllegiance allegiance,ChamberMarkerType type,Vec3 point){
        var root=root(card,allegiance);
        if(root==null || !(root.skills() instanceof NativeChamberSkills skills) || !skills.owns(card.getUUID()) || !root.body().isAlive() || point==null || !safe(root,point)){endDevice(card,allegiance);return;}
        Entity marker=type==ChamberMarkerType.TRAP?new ChamberTripTrapEntity(ModEntities.CHAMBER_TRAP.get(),root.body().level(),root.body()):new ChamberTeleportAnchorEntity(ModEntities.CHAMBER_TELEPORT_ANCHOR.get(),root.body().level(),root.body());
        marker.setPos(point);
        if(!root.serverLevel().addFreshEntity(marker)){endDevice(card,allegiance);return;}
        if(type==ChamberMarkerType.TRAP){skills.trap=marker.getUUID();skills.trapPosition=point;skills.trapMissing=0;}else{skills.anchor=marker.getUUID();skills.anchorPosition=point;skills.anchorMissing=0;}
        card.discard();
    }
    private static boolean spawnSlow(OperativeHost host,Vec3 point){var field=new ChamberSlowFieldEntity(ModEntities.CHAMBER_SLOW_FIELD.get(),host.body().level(),host.body());field.setPos(point);return host.serverLevel().addFreshEntity(field);}
    public void sniperHit(OperativeHost host,LivingEntity target){long now=host.serverLevel().getGameTime();if(now>=slowReadyAt && (target.isAlive()?host.isEnemy(target):!OperativeSupport.ally(host.body(),target)) && spawnSlow(host,target.position()))slowReadyAt=now+ChamberStateManager.TOUR_SLOW_FIELD_TRIGGER_INTERVAL_TICKS;}
    @Override public boolean busy(){return false;}
    @Override public void cancel(OperativeHost host){stow(host);if(host.body() instanceof OperativeEntity root)root.gun().cancel(root);}
    public void clear(OperativeHost host){for(UUID id:new UUID[]{anchor,trap})if(id!=null){var entity=host.serverLevel().getEntity(id);if(entity!=null)entity.discard();}anchor=trap=null;anchorPosition=trapPosition=null;stow(host);}
    @Override public CompoundTag save(){var t=new CompoundTag();put(t,"Anchor",anchor,anchorPosition);put(t,"Trap",trap,trapPosition);t.putInt("TeleportCooldown",teleportCooldown);t.putInt("TrapCooldown",trapCooldown);t.putInt("PistolCooldown",pistolCooldown);t.putInt("SniperCooldown",sniperCooldown);t.putString("Gun",equipped.name());if(!carriedWeapon.isEmpty())t.put("CarriedWeapon",carriedWeapon.copy());t.putLong("SlowReadyAt",slowReadyAt);return t;}
    @Override public void load(CompoundTag t){carriedWeapon=t.getCompound("CarriedWeapon").copy();idleWeaponTicks=0;anchor=t.hasUUID("Anchor")?t.getUUID("Anchor"):null;trap=t.hasUUID("Trap")?t.getUUID("Trap"):null;anchorPosition=position(t,"Anchor");trapPosition=position(t,"Trap");anchorMissing=trapMissing=0;teleportCooldown=bound(t,"TeleportCooldown",600);trapCooldown=bound(t,"TrapCooldown",600);pistolCooldown=bound(t,"PistolCooldown",1200);sniperCooldown=bound(t,"SniperCooldown",2400);slowReadyAt=Math.max(0,t.getLong("SlowReadyAt"));try{equipped=ChamberGunKind.valueOf(t.getString("Gun"));}catch(RuntimeException ex){equipped=ChamberGunKind.NONE;}}
    private static int bound(CompoundTag t,String key,int max){return Math.max(0,Math.min(max,t.getInt(key)));}
    private static void put(CompoundTag t,String key,UUID id,Vec3 point){if(id!=null)t.putUUID(key,id);if(point!=null){var pos=new CompoundTag();pos.putDouble("X",point.x);pos.putDouble("Y",point.y);pos.putDouble("Z",point.z);t.put(key+"Position",pos);}}
    private static Vec3 position(CompoundTag t,String key){if(!t.contains(key+"Position",10))return null;var p=t.getCompound(key+"Position");double x=p.getDouble("X"),y=p.getDouble("Y"),z=p.getDouble("Z");return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)?new Vec3(x,y,z):null;}
    @Override public String describe(){return "Chamber gun="+equipped+",anchor="+anchor+",trap="+trap+",cooldowns="+teleportCooldown+"/"+trapCooldown+"/"+pistolCooldown+"/"+sniperCooldown;}

    @Override public void refresh(OperativeHost host) {
        var fresh = new NativeChamberSkills();
        pistolCooldown = fresh.pistolCooldown;
        sniperCooldown = fresh.sniperCooldown;
        teleportCooldown = fresh.teleportCooldown;
        trapCooldown = fresh.trapCooldown;
    }
}
