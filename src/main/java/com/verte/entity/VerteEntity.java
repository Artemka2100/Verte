package com.verte.entity;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class VerteEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BIG =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int STAGE_KIND = 0;
    public static final int STAGE_ANGRY = 1;
    public static final int STAGE_MONSTER = 2;
    public static final int STAGE_RAMPAGE = 3;

    private static final String[] CREEPY = {
            "\u042f \u0432\u0438\u0436\u0443 \u0442\u0435\u0431\u044f.",
            "\u0422\u044b \u043d\u0435 \u043e\u0434\u0438\u043d.",
            "\u0421\u043a\u043e\u0440\u043e.",
            "\u041e\u0431\u0435\u0440\u043d\u0438\u0441\u044c."
    };

    private int anger;
    private long angerDay = -1L;
    private long bornDay = -1L;
    private int sleeps;
    private boolean wasSleeping;
    private boolean stalking;
    private int lastGameType = -1;
    private int actionCooldown = 60;
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
        this.entityData.define(DATA_STAGE, 0);
        this.entityData.define(DATA_BIG, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.25D, 2.5F, 6.0F));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
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

    public void setStage(int stage) {
        this.entityData.set(DATA_STAGE, stage);
        if (stage >= STAGE_MONSTER) {
            this.setBig(true);
            AttributeInstance hp = this.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(500.0D);
                this.setHealth(this.getMaxHealth());
            }
            AttributeInstance spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) {
                spd.setBaseValue(stage >= STAGE_RAMPAGE ? 0.6D : 0.38D);
            }
            this.setCustomName(Component.literal("VERTE"));
        }
        this.refreshDimensions();
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    public void provoke(int amount, long currentDay) {
        this.anger += amount;
        if (this.getStage() == STAGE_KIND) {
            this.setStage(STAGE_ANGRY);
            this.angerDay = currentDay;
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float scale = (this.entityData != null && this.isBig()) ? 6.0F : 1.0F;
        return super.getDimensions(pose).scale(scale);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && this.getStage() == STAGE_KIND) {
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
        if (!this.level().isClientSide && source.getEntity() instanceof Player) {
            this.provoke(3, this.level().getDayTime() / 24000L);
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

    @