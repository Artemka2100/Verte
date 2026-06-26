package com.verte.client;

import com.verte.Verte;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Watches a single-player world being \"Opened to LAN\". The moment it is
 * published, Verte tells the host which port and IPs a friend should use to
 * connect (including ZeroTier-style addresses), so they don't have to dig
 * through menus.
 */
@Mod.EventBusSubscriber(modid = Verte.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LanWatcher {

    private static boolean announced = false;

    private LanWatcher() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || !server.isPublished()) {
            announced = false;
            return;
        }
        if (announced || mc.player == null) {
            return;
        }
        announced = true;
        announce(mc, server.getPort());
    }

    private static void announce(Minecraft mc, int port) {
        mc.player.sendSystemMessage(Component.literal(
                "\u041e! \u0442\u044b \u043e\u0442\u043a\u0440\u044b\u043b \u043c\u0438\u0440 \u0434\u043b\u044f \u0441\u0435\u0442\u0438 \u2014 \u0437\u043d\u0430\u0447\u0438\u0442 \u0445\u043e\u0447\u0435\u0448\u044c \u043f\u043e\u0438\u0433\u0440\u0430\u0442\u044c \u0441 \u0434\u0440\u0443\u0433\u043e\u043c!")
                .withStyle(ChatFormatting.GREEN));
        mc.player.sendSystemMessage(Component.literal("\u041f\u043e\u0440\u0442: " + port)
                .withStyle(ChatFormatting.AQUA));

        List<String> ips = localIps();
        if (ips.isEmpty()) {
            mc.player.sendSystemMessage(Component.literal(
                    "\u041d\u0435 \u043d\u0430\u0448\u0451\u043b \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0439 IP \u2014 \u043f\u043e\u0441\u043c\u043e\u0442\u0440\u0438 \u0432 ZeroTier \u0430\u0434\u0440\u0435\u0441 \u0441\u0435\u0442\u0438.")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            for (String ip : ips) {
                mc.player.sendSystemMessage(Component.literal(
                        "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u0434\u043b\u044f \u0434\u0440\u0443\u0433\u0430: " + ip + ":" + port)
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        mc.player.sendSystemMessage(Component.literal(
                "(\u0432 ZeroTier \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u0443\u0439\u0442\u0435 IP \u0432\u0438\u0434\u0430 10.x.x.x \u0438\u043b\u0438 192.168.x.x \u0438\u0437 \u043e\u043a\u043d\u0430 ZeroTier)")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static List<String> localIps() {
        List<String> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && addr.isSiteLocalAddress() && !addr.isLoopbackAddress()) {
                        String host = addr.getHostAddress();
                        if (!result.contains(host)) {
                            result.add(host);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Collections.sort(result);
        return result;
    }
}
