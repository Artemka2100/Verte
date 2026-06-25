package com.verte.net;

import com.verte.Verte;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Tiny network channel used to tell a specific client to play a short
 * hallucination (screen distortion, blood, or a looming huge Verte).
 */
public final class VerteNetwork {
    private static final String VERSION = "1";
    public static SimpleChannel CHANNEL;

    private VerteNetwork() {
    }

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(Verte.MOD_ID, "main"),
                () -> VERSION,
                VERSION::equals,
                VERSION::equals);
        int id = 0;
        CHANNEL.registerMessage(id++, HallucinationPacket.class,
                HallucinationPacket::encode,
                HallucinationPacket::decode,
                HallucinationPacket::handle);
    }

    public static void sendHallucination(ServerPlayer player, int type, int durationTicks) {
        if (CHANNEL == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HallucinationPacket(type, durationTicks));
    }
}
