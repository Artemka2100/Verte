package com.verte.net;

import com.verte.client.HallucinationRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Tells the client to start a hallucination.
 * type: 0 = distortion, 1 = blood, 2 = huge Verte. durationTicks ~= 100 (5s).
 */
public class HallucinationPacket {
    private final int type;
    private final int durationTicks;

    public HallucinationPacket(int type, int durationTicks) {
        this.type = type;
        this.durationTicks = durationTicks;
    }

    public static void encode(HallucinationPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.type);
        buf.writeInt(msg.durationTicks);
    }

    public static HallucinationPacket decode(FriendlyByteBuf buf) {
        return new HallucinationPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(HallucinationPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HallucinationRenderer.start(msg.type, msg.durationTicks)));
        context.setPacketHandled(true);
    }
}
