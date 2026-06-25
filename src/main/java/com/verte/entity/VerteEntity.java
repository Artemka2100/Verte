package com.verte.entity;

import com.verte.AtmosphereManager;
import com.verte.CorruptionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class VerteEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BIG =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int PHASE_FRIENDLY = CorruptionManager.PHASE_FRIENDLY;
    public static final int PHASE_STRANGE = CorruptionManager.PHASE_STRANGE;
    public static final int PHASE_HOSTILE = CorruptionManager.PHASE_HOSTILE;
    public static final int PHASE_MONSTER = CorruptionManager.PHASE_MONSTER;

    private static final String[] CREEPY = {
            "Я вижу тебя.",
            "Ты не один.",
            "Скоро.",
            "Обернись."
    };

    private long lastDay = -1L;
    private int sleeps;
    private boolean wasSleeping;
    private boolean stalking;
    private boolean rampaging;
    private int lastGameType = -1;
    private int nearTicks;
    private int currentPhase = -1;
    private UUID ownerUUID;

    public VerteEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("Verte :)"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 256.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0);
        this.entityData.define(DATA_BIG, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.25D, 2.5F, 6.0F));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    public boolean isBig() {
        return this.entityData.get(DATA_BIG);
    }

    public void setBig(boolean big) {
        if (this.entityData.get(DATA_BIG) != big) {
            this.entityData.set(DATA_BIG, big);
            this.refreshDimensions();
        }
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    private void applyPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
        AttributeInstance hp = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (phase >= PHASE_MONSTER) {
            this.setBig(true);
            if (hp != null) {
                hp.setBaseValue(500.0D);
                this.setHealth(this.getMaxHealth());
            }
            if (spd != null) {
                spd.setBaseValue(0.55D);
            }
            this.setCustomName(Component.literal("VERTE"));
        } else {
            if (!this.stalking) {
                this.setBig(false);
            }
            if (hp != null) {
                hp.setBaseValue(20.0D);
            }
            if (spd != null) {
                spd.setBaseValue(0.45D);
            }
            this.setCustomName(Component.literal(phase >= PHASE_HOSTILE ? "Verte" : "Verte :)"));
        }
        this.refreshDimensions();
    }

    private void announcePhase(ServerPlayer sp, int phase, int prev) {
        if (phase <= prev) {
            return;
        }
        switch (phase) {
            case PHASE_STRANGE -> this.say(sp, "Я начинаю замечать тебя...");
            case PHASE_HOSTILE -> this.say(sp, "Ты мне больше не нравишься.");
            case PHASE_MONSTER -> this.say(sp, "Я больше не помощник. Я бог этого мира. Беги.");
            default -> {
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float scale = (this.entityData != null && this.isBig()) ? 6.0F : 1.0F;
        return super.getDimensions(pose).scale(scale);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && this.getPhase() == PHASE_FRIENDLY) {
            if (this.isPassenger()) {
                this.stopRiding();
            } else {
                this.startRiding(player, true);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer sp) {
            CorruptionManager.add(sp, 5);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount % 20 != 0) {
            return;
        }

        long day = level.getDayTime() / 24000L;
        if (this.lastDay < 0L) {
            this.lastDay = day;
        }
        boolean night = !level.isDay();
        Player owner = this.findOwner(level);

        if (owner instanceof ServerPlayer sp) {
            // Corruption grows over time when the player lingers near Verte.
            if (day > this.lastDay) {
                CorruptionManager.add(sp, (int) Math.min(5L, day - this.lastDay) * 10);
                this.lastDay = day;
            }
            if (this.distanceToSqr(sp) < 24.0D * 24.0D) {
                if (++this.nearTicks >= 6) {
                    this.nearTicks = 0;
                    CorruptionManager.add(sp, 1);
                }
            }

            int corruption = CorruptionManager.get(sp);
            int phase = CorruptionManager.phaseOf(corruption);
            if (phase != this.currentPhase) {
                int prev = this.currentPhase;
                this.currentPhase = phase;
                this.applyPhase(phase);
                this.announcePhase(sp, phase, prev);
            }

            int gt = sp.gameMode.getGameModeForPlayer().getId();
            if (this.lastGameType != -1 && gt != this.lastGameType && gt == GameType.CREATIVE.getId()) {
                this.say(sp, "Нечестно. Выключи креатив, трус.");
                CorruptionManager.add(sp, 3);
            }
            this.lastGameType = gt;

            boolean sleeping = sp.isSleeping();
            if (sleeping && !this.wasSleeping) {
                this.sleeps++;
                if (phase >= PHASE_STRANGE) {
                    AtmosphereManager.knowsWhereYouSleep(sp);
                }
                if (phase >= PHASE_MONSTER && this.sleeps >= 3 && !this.rampaging) {
                    this.rampaging = true;
                    this.say(sp, "Я пришёл.");
                }
            }
            this.wasSleeping = sleeping;

            this.handleStalking(level, sp, night, phase);

            if (!this.stalking) {
                this.followOwner(sp, phase);
            }

            this.atmosphere(level, sp, phase);

            if (phase == PHASE_STRANGE && this.random.nextInt(30) == 0) {
                this.teleportNear(sp, 18.0D + this.random.nextInt(8), true);
                level.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.6F, 0.5F);
            }
            if (phase >= PHASE_HOSTILE && this.random.nextInt(15) == 0) {
                this.obstruct(level, sp);
            }
            if (phase >= PHASE_MONSTER) {
                if (this.random.nextInt(3) == 0) {
                    this.godAct(level, sp);
                } else {
                    this.scare(level, sp);
                }
                this.chase(level, sp);
                if (this.rampaging) {
                    this.rampage(level, sp);
                }
            }
        } else if (level.isDay() && this.currentPhase < PHASE_MONSTER) {
            this.setBig(false);
            this.stalking = false;
        }
    }

    private void handleStalking(ServerLevel level, ServerPlayer sp, boolean night, int phase) {
        if (phase < PHASE_STRANGE) {
            if (this.stalking) {
                this.stalking = false;
                if (phase < PHASE_MONSTER) {
                    this.setBig(false);
                }
            }
            return;
        }
        if (night) {
            if (!this.stalking) {
                if (this.random.nextInt(4) == 0) {
                    this.stalking = true;
                    if (phase >= PHASE_HOSTILE) {
                        this.setBig(true);
                    }
                    this.getNavigation().stop();
                    this.teleportNear(sp, 22.0D, true);
                    level.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.6F, 0.4F);
                }
            } else {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(sp, 30.0F, 30.0F);
                if (this.playerSees(sp)) {
                    level.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 0.5F);
                    if (this.random.nextInt(3) == 0) {
                        this.teleportNear(sp, 2.5D, true);
                        sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
                    } else {
                        this.teleportNear(sp, 28.0D, true);
                    }
                } else if (this.random.nextInt(6) == 0) {
                    this.teleportNear(sp, 18.0D + this.random.nextInt(8), true);
                }
            }
        } else if (this.stalking) {
            this.stalking = false;
            if (phase < PHASE_MONSTER) {
                this.setBig(false);
            }
        }
    }

    private void followOwner(ServerPlayer sp, int phase) {
        if (this.distanceToSqr(sp) > 28.0D * 28.0D) {
            this.teleportNear(sp, 6.0D, phase >= PHASE_HOSTILE);
        }
    }

    private void atmosphere(ServerLevel level, ServerPlayer sp, int phase) {
        if (phase >= PHASE_STRANGE && this.random.nextInt(Math.max(2, 7 - phase * 2)) == 0) {
            AtmosphereManager.ambient(level, sp, phase, this.random);
        }
        if (phase >= PHASE_HOSTILE && this.random.nextInt(12 - phase * 2) == 0) {
            AtmosphereManager.distort(sp, phase, this.random);
        }
        if (phase >= PHASE_STRANGE && this.random.nextInt(20) == 0) {
            AtmosphereManager.fourthWall(sp, this.random);
        }
    }

    private void obstruct(ServerLevel level, ServerPlayer sp) {
        BlockPos base = sp.blockPosition();
        BlockPos[] around = {
                base, base.above(), base.north(), base.south(), base.east(), base.west()
        };
        BlockPos target = around[this.random.nextInt(around.length)];
        if (level.getBlockState(target).isAir()) {
            level.setBlockAndUpdate(target, Blocks.COBWEB.defaultBlockState());
        }
    }

    private void chase(ServerLevel level, ServerPlayer sp) {
        this.getNavigation().moveTo(sp, 1.2D);
        this.getLookControl().setLookAt(sp, 30.0F, 30.0F);
        if (this.distanceToSqr(sp) < 6.0D * 6.0D) {
            sp.hurt(this.damageSources().mobAttack(this), 4.0F);
            level.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private void followOwnerTeleport() {
    }

    private void teleportNear(Player player, double distance, boolean behind) {
        Vec3 dir = player.getViewVector(1.0F).normalize();
        double sign = behind ? -1.0D : 1.0D;
        double x = player.getX() + dir.x * distance * sign;
        double z = player.getZ() + dir.z * distance * sign;
        this.teleportTo(x, player.getY(), z);
        this.getNavigation().stop();
    }

    private boolean playerSees(Player player) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 diff = this.position().subtract(player.getEyePosition()).normalize();
        return look.dot(diff) > 0.55D
                && this.distanceToSqr(player) < 40.0D * 40.0D
                && this.hasLineOfSight(player);
    }

    private Player findOwner(ServerLevel level) {
        if (this.ownerUUID != null) {
            Player p = level.getPlayerByUUID(this.ownerUUID);
            if (p != null) {
                return p;
            }
        }
        return level.getNearestPlayer(this, 256.0D);
    }

    private void scare(ServerLevel level, ServerPlayer player) {
        int r = this.random.nextInt(4);
        if (r == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.7F);
        } else if (r == 1) {
            this.say(player, CREEPY[this.random.nextInt(CREEPY.length)]);
        } else if (r == 2) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.8F, 0.5F);
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
        }
    }

    private void godAct(ServerLevel level, ServerPlayer player) {
        switch (this.random.nextInt(5)) {
            case 0 -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    Vec3 dir = player.getViewVector(1.0F).normalize();
                    bolt.moveTo(player.getX() + dir.x * 4.0D, player.getY(), player.getZ() + dir.z * 4.0D);
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                this.say(player, "Чувствуешь мой гнев?");
            }
            case 1 -> {
                level.setWeatherParameters(0, 6000, true, true);
                this.say(player, "Я приношу бурю.");
            }
            case 2 -> {
                level.setDayTime(18000L);
                this.say(player, "Ночь подчиняется мне.");
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
                this.say(player, "Тьма — мой дом.");
            }
            default -> this.scare(level, player);
        }
    }

    private void rampage(ServerLevel level, ServerPlayer player) {
        if (this.distanceToSqr(player) > 48.0D * 48.0D) {
            this.teleportNear(player, 6.0D, true);
        }
        BlockPos center = player.blockPosition();
        for (int i = 0; i < 16; i++) {
            BlockPos p = center.offset(this.random.nextInt(11) - 5, this.random.nextInt(7) - 1, this.random.nextInt(11) - 5);
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && state.getDestroySpeed(level, p) >= 0.0F) {
                level.destroyBlock(p, true);
                break;
            }
        }
    }

    private void say(Player player, String text) {
        if (player == null) {
            return;
        }
        player.sendSystemMessage(Component.literal("Verte » ")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.RED)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VertePhase", this.getPhase());
        tag.putLong("VerteLastDay", this.lastDay);
        tag.putInt("VerteSleeps", this.sleeps);
        tag.putBoolean("VerteRampaging", this.rampaging);
        if (this.ownerUUID != null) {
            tag.putUUID("VerteOwner", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lastDay = tag.contains("VerteLastDay") ? tag.getLong("VerteLastDay") : -1L;
        this.sleeps = tag.getInt("VerteSleeps");
        this.rampaging = tag.getBoolean("VerteRampaging");
        if (tag.hasUUID("VerteOwner")) {
            this.ownerUUID = tag.getUUID("VerteOwner");
        }
        this.entityData.set(DATA_PHASE, tag.getInt("VertePhase"));
        this.currentPhase = -1;
    }
}
