package com.rzy.dealt_force_skills.network;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.shop.GtiTradingHallManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.ArrayList;
import java.util.List;
import io.netty.handler.codec.DecoderException;

public record S2C_OpenGtiTradingHall(long coins, List<GtiTradingHallManager.Entry> entries) implements CustomPacketPayload {
    private static final int MAX_ENTRIES = 4096;
    public static final Type<S2C_OpenGtiTradingHall> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DealtForceSkillsMod.MODID, "s2c_open_gti_trading_hall"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenGtiTradingHall> STREAM_CODEC = StreamCodec.of(S2C_OpenGtiTradingHall::encode, S2C_OpenGtiTradingHall::decode);
    public S2C_OpenGtiTradingHall {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many GTI entries: " + entries.size());
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    private static void encode(RegistryFriendlyByteBuf buf, S2C_OpenGtiTradingHall msg) {
        buf.writeLong(msg.coins);
        buf.writeVarInt(msg.entries.size());
        for (GtiTradingHallManager.Entry entry : msg.entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
            buf.writeLong(entry.price());
            buf.writeUtf(entry.category().id(), 64);
        }
    }
    private static S2C_OpenGtiTradingHall decode(RegistryFriendlyByteBuf buf) {
        long coins = buf.readLong();
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) throw new DecoderException("Invalid GTI entry count: " + size);
        List<GtiTradingHallManager.Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long price = buf.readLong();
            GtiTradingHallManager.Category category = GtiTradingHallManager.Category.byId(buf.readUtf(64))
                    .orElse(GtiTradingHallManager.Category.MISC);
            entries.add(new GtiTradingHallManager.Entry(stack, price, category));
        }
        return new S2C_OpenGtiTradingHall(coins, entries);
    }
    public static void handle(S2C_OpenGtiTradingHall msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPacketHandlers.openGtiTradingHall(msg.coins(), msg.entries()));
    }
}
