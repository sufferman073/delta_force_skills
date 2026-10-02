package com.rzy.dealt_force_skills.client;

import com.rzy.dealt_force_skills.network.S2C_FoxMusicPlayback;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@EventBusSubscriber(modid = DealtForceSkillsMod.MODID, value = Dist.CLIENT)
public final class FoxMusicClient {
    private record Key(UUID owner, UUID device) { }
    private static final Map<Key, FollowingSound> SOUNDS = new HashMap<>();
    private static ClientLevel level;
    private FoxMusicClient() { }

    public static void update(S2C_FoxMusicPlayback message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (level != mc.level) clear();
        level = mc.level;
        Key key = new Key(message.owner(), message.device());
        FollowingSound old = SOUNDS.get(key);
        if (message.removed()) {
            if (old != null) { old.finish(); mc.getSoundManager().stop(old); SOUNDS.remove(key); }
            return;
        }
        if (old != null && old.generation == message.generation() && old.getLocation().equals(message.sound()) && !old.isStopped()) {
            old.paused = !message.playing(); old.lastUpdate = Util.getMillis();
            return;
        }
        if (old != null) { old.finish(); mc.getSoundManager().stop(old); }
        FollowingSound sound = new FollowingSound(message, mc.level);
        SOUNDS.put(key, sound);
        mc.getSoundManager().play(sound);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != level) clear();
        SOUNDS.entrySet().removeIf(entry -> entry.getValue().isStopped());
    }
    private static void clear() {
        for (FollowingSound sound : SOUNDS.values()) { sound.finish(); Minecraft.getInstance().getSoundManager().stop(sound); }
        SOUNDS.clear(); level = null;
    }

    public static final class FollowingSound extends AbstractTickableSoundInstance {
        private final UUID owner;
        private final int entityId;
        private final int generation;
        private final int offsetTicks;
        private final ClientLevel sourceLevel;
        private boolean paused;
        private long lastUpdate = Util.getMillis();

        private FollowingSound(S2C_FoxMusicPlayback message, ClientLevel sourceLevel) {
            super(SoundEvent.createVariableRangeEvent(message.sound()), SoundSource.RECORDS,
                    RandomSource.create(message.device().getMostSignificantBits() ^ message.generation()));
            owner = message.owner(); entityId = message.entityId(); generation = message.generation();
            offsetTicks = Math.max(0, message.elapsed()); paused = !message.playing(); this.sourceLevel = sourceLevel;
            volume = paused ? 0.0F : 1.0F; pitch = 1.0F; looping = false; relative = false;
            Entity entity = sourceLevel.getEntity(entityId);
            if (entity != null) { x = entity.getX(); y = entity.getY(); z = entity.getZ(); }
        }
        public boolean paused() { return paused; }
        public void finish() { stop(); }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            Minecraft mc = Minecraft.getInstance();
            Entity entity = sourceLevel.getEntity(entityId);
            if (mc.level != sourceLevel || entity == null || !owner.equals(entity.getUUID()) || !entity.isAlive()
                    || (!mc.isPaused() && Util.getMillis() - lastUpdate > 5000L)) { stop(); return; }
            x = entity.getX(); y = entity.getY(); z = entity.getZ(); volume = paused ? 0.0F : 1.0F;
            if (mc.isPaused()) lastUpdate = Util.getMillis();
        }
        @Override public WeighedSoundEvents resolve(SoundManager manager) {
            WeighedSoundEvents event = super.resolve(manager);
            if (event != null && sound != SoundManager.EMPTY_SOUND) {
                sound = new Sound(sound.getLocation(), sound.getVolume(), sound.getPitch(), sound.getWeight(),
                        sound.getType(), true, false, 64);
            }
            return event;
        }
        @Override public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
            return buffers.getStream(sound.getPath(), false).thenApplyAsync(stream -> {
                try { return new OffsetStream(stream, offsetTicks); }
                catch (IOException error) {
                    try { stream.close(); } catch (IOException ignored) { }
                    throw new CompletionException(error);
                }
            }, Util.backgroundExecutor());
        }
    }

    /** Seek by decoded PCM frames and keep decoder overshoot, so late listeners join the same point. */
    private static final class OffsetStream implements AudioStream {
        private final AudioStream delegate;
        private ByteBuffer pending = ByteBuffer.allocate(0);
        private OffsetStream(AudioStream delegate, int ticks) throws IOException {
            this.delegate = delegate;
            var format = delegate.getFormat();
            long bytes = (long) (format.getSampleRate() * ticks / 20.0) * format.getFrameSize();
            while (bytes > 0) {
                ByteBuffer decoded = delegate.read((int) Math.min(65536, bytes));
                if (!decoded.hasRemaining()) break;
                int skip = (int) Math.min(bytes, decoded.remaining());
                decoded.position(decoded.position() + skip); bytes -= skip;
                if (decoded.hasRemaining()) pending = decoded.slice();
            }
        }
        @Override public javax.sound.sampled.AudioFormat getFormat() { return delegate.getFormat(); }
        @Override public ByteBuffer read(int size) throws IOException {
            if (!pending.hasRemaining()) return delegate.read(size);
            ByteBuffer first = pending; pending = ByteBuffer.allocate(0);
            if (first.remaining() >= size) return first;
            ByteBuffer next = delegate.read(size - first.remaining());
            ByteBuffer result = ByteBuffer.allocateDirect(first.remaining() + next.remaining());
            result.put(first).put(next).flip(); return result;
        }
        @Override public void close() throws IOException { delegate.close(); }
    }
}
