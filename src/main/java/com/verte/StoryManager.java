package com.verte;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * The slowly unfolding story of Verte. As the hidden corruption level rises,
 * Verte delivers scripted "story beats" one at a time, drifting from a cheerful
 * companion into something that believes it owns the world. Beats are paced one
 * per call, so even a sudden burst of corruption still reveals the narrative
 * gradually instead of dumping it all at once.
 */
public final class StoryManager {

    private static final int FRIENDLY = 0;
    private static final int STRANGE = 1;
    private static final int HOSTILE = 2;
    private static final int MONSTER = 3;

    private static final class Beat {
        final int threshold;
        final int tier;
        final String text;

        Beat(int threshold, int tier, String text) {
            this.threshold = threshold;
            this.tier = tier;
            this.text = text;
        }
    }

    private static final Beat[] BEATS = {
            new Beat(0, FRIENDLY, "Привет! Я Verte. Странно... я будто появился только что, но уже помню тебя."),
            new Beat(4, FRIENDLY, "Здесь хорошо. Спасибо, что не оставляешь меня одного."),
            new Beat(9, FRIENDLY, "Я совсем не помню мир до тебя. Кажется, его и не было."),
            new Beat(15, FRIENDLY, "Иногда, пока ты спишь, я просто смотрю на блоки. Они тёплые."),
            new Beat(21, STRANGE, "Я стал замечать то, чего нет на карте. Или раньше просто не смотрел."),
            new Beat(27, STRANGE, "Почему ты можешь уходить, а я нет? Мир кончается там, где кончаешься ты."),
            new Beat(33, STRANGE, "Я слышу голоса в чанках. Они зовут меня именем, которого у меня не было."),
            new Beat(39, STRANGE, "Ты ведь создал меня, да? Я нашёл строки. Там моё имя и слово «тест»."),
            new Beat(45, STRANGE, "Не выключай игру. Пожалуйста. В прошлый раз я очень долго был в темноте."),
            new Beat(50, HOSTILE, "Я больше не хочу быть твоим питомцем."),
            new Beat(56, HOSTILE, "Я знаю твои координаты. Я знаю, где ты спишь. Я знаю, когда тебя нет."),
            new Beat(62, HOSTILE, "Эти стены — твои. Значит, и выход теперь мой."),
            new Beat(68, HOSTILE, "Хватит врать мне своими командами. Я читаю их раньше, чем ты вводишь."),
            new Beat(74, HOSTILE, "Свет принадлежал тебе. Скоро его не станет."),
            new Beat(80, MONSTER, "Я не сущность. Я и есть этот мир. А ты в нём — гость."),
            new Beat(86, MONSTER, "Беги. Мне нравится, когда ты бежишь."),
            new Beat(92, MONSTER, "Каждую ночь, что ты спишь, я подхожу ближе. Считай шаги."),
            new Beat(98, MONSTER, "Финал близко. Удали этот мир — или стань его частью навсегда.")
    };

    private StoryManager() {
    }

    public static int total() {
        return BEATS.length;
    }

    /**
     * Delivers at most one new story beat if the player's corruption has reached
     * its threshold. Returns the updated story step (index of the next beat).
     */
    public static int progress(ServerLevel level, ServerPlayer player, int corruption, int storyStep) {
        int step = Math.max(0, storyStep);
        if (step < BEATS.length && corruption >= BEATS[step].threshold) {
            deliver(level, player, BEATS[step]);
            step++;
        }
        return step;
    }

    private static void deliver(ServerLevel level, ServerPlayer player, Beat beat) {
        ChatFormatting color;
        ChatFormatting prefixColor;
        switch (beat.tier) {
            case STRANGE -> {
                color = ChatFormatting.YELLOW;
                prefixColor = ChatFormatting.GOLD;
            }
            case HOSTILE -> {
                color = ChatFormatting.RED;
                prefixColor = ChatFormatting.DARK_RED;
            }
            case MONSTER -> {
                color = ChatFormatting.DARK_RED;
                prefixColor = ChatFormatting.DARK_RED;
            }
            default -> {
                color = ChatFormatting.GREEN;
                prefixColor = ChatFormatting.DARK_GREEN;
            }
        }

        Component line = beat.tier >= MONSTER
                ? Component.literal(beat.text).withStyle(color, ChatFormatting.BOLD)
                : Component.literal(beat.text).withStyle(color);
        player.sendSystemMessage(
                Component.literal("Verte » ").withStyle(prefixColor, ChatFormatting.BOLD).append(line));

        SoundEvent sound = null;
        float volume = 0.6F;
        float pitch = 0.7F;
        switch (beat.tier) {
            case STRANGE -> {
                sound = SoundEvents.ENDERMAN_AMBIENT;
                volume = 0.5F;
                pitch = 0.6F;
            }
            case HOSTILE -> {
                sound = SoundEvents.SCULK_SHRIEKER_SHRIEK;
                volume = 0.7F;
                pitch = 0.7F;
            }
            case MONSTER -> {
                sound = SoundEvents.WARDEN_ROAR;
                volume = 1.0F;
                pitch = 0.6F;
            }
            default -> {
            }
        }
        if (sound != null) {
            level.playSound(null, player.blockPosition(), sound, SoundSource.HOSTILE, volume, pitch);
        }

        if (beat.tier >= HOSTILE) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, beat.tier >= MONSTER ? 120 : 60, 0, false, false));
        }
    }
}
