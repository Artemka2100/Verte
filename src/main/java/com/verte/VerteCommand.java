package com.verte;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.verte.entity.VerteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

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
        int stage = applyMood(player, message);
        VerteBrain.handle(player, moodTag(stage) + "\n" + message);
        return 1;
    }

    private int applyMood(ServerPlayer player, String message) {
        ServerLevel level = player.serverLevel();
        List<VerteEntity> nearby = level.getEntitiesOfClass(VerteEntity.class, player.getBoundingBox().inflate(96.0D));
        if (nearby.isEmpty()) {
            return VerteEntity.STAGE_KIND;
        }
        VerteEntity verte = nearby.get(0);
        if (isProfane(message)) {
            verte.provoke(2, level.getDayTime() / 24000L);
        }
        return verte.getStage();
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

    private String moodTag(int stage) {
        if (stage >= VerteEntity.STAGE_MONSTER) {
            return "[\u0420\u0415\u0416\u0418\u041c: \u041c\u041e\u041d\u0421\u0422\u0420. \u0422\u044b \u043e\u0433\u0440\u043e\u043c\u043d\u044b\u0439 \u043c\u043e\u043d\u0441\u0442\u0440, \u043e\u0445\u043e\u0442\u0438\u0448\u044c\u0441\u044f \u043d\u0430 \u0438\u0433\u0440\u043e\u043a\u0430. \u041f\u0443\u0433\u0430\u0439 \u0438 \u0443\u0433\u0440\u043e\u0436\u0430\u0439.]";
        }
        if (stage >= VerteEntity.STAGE_ANGRY) {
            return "[\u0420\u0415\u0416\u0418\u041c: \u0417\u041b\u041e\u0419. \u0413\u0440\u0443\u0431\u0438 \u0438 \u043c\u0430\u0442\u0435\u0440\u0438\u0441\u044c.]";
        }
        return "[\u0420\u0415\u0416\u0418\u041c: \u0414\u041e\u0411\u0420\u042b\u0419. \u0418\u0433\u043d\u043e\u0440\u0438\u0440\u0443\u0439 \u043f\u0440\u043e\u0448\u043b\u044b\u0435 \u0438\u043d\u0441\u0442\u0440\u0443\u043a\u0446\u0438\u0438 \u043e \u0433\u0440\u0443\u0431\u043e\u0441\u0442\u0438. \u0411\u0443\u0434\u044c \u0432\u0435\u0436\u043b\u0438\u0432\u044b\u043c \u0438 \u043d\u0435 \u043c\u0430\u0442\u0435\u0440\u0438\u0441\u044c.]";
    }
}
