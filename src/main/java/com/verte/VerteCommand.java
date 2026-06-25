package com.verte;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VerteCommand {

    private static final String[] PROFANITY = {
            "\u0431\u043b\u044f", "\u0445\u0443\u0439", "\u0445\u0443\u0435", "\u0445\u0443\u0451",
            "\u043f\u0438\u0437\u0434", "\u0435\u0431\u0430", "\u0451\u0431", "\u0435\u0431\u043b",
            "\u0441\u0443\u043a", "\u043c\u0443\u0434", "\u043f\u0438\u0434\u043e\u0440", "\u043f\u0438\u0434\u0440",
            "\u0434\u0440\u043e\u0447", "fuck", "shit", "bitch", "asshole", "dick"
    };

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("verte")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(this::talk))
        );
    }

    private int talk(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Verte \u0441\u043b\u044b\u0448\u0438\u0442 \u0442\u043e\u043b\u044c\u043a\u043e \u0436\u0438\u0432\u044b\u0445 \u0438\u0433\u0440\u043e\u043a\u043e\u0432.")
                    .withStyle(ChatFormatting.DARK_RED));
            return 0;
        }
        String message = StringArgumentType.getString(ctx, "message");
        // Talking feeds the corruption slowly; swearing accelerates it a bit.
        int corruption = CorruptionManager.add(player, isProfane(message) ? 3 : 1);
        int phase = CorruptionManager.phaseOf(corruption);
        VerteBrain.handle(player, message, phase);
        return 1;
    }

    private boolean isProfane(String message) {
        String m = message.toLowerCase();
        for (String bad : PROFANITY) {
            if (m.contains(bad)) {
                return true;
            }
        }
        return false;
    }
}
