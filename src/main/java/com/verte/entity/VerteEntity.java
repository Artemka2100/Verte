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
    private int sleeps;
    private boolean wasSleeping;
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
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STAGE, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowPlayerGoal(this, 1.25D, 2.5F, 10.0F));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public int getStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public void setStage(int stage) {
        this.entityData.set(DATA_STAGE, stage);
        this.refreshDimensions();
        if (stage >= STAGE_MONSTER) {
            AttributeInstance hp = this.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) {
                hp.setBaseValue(300.0D);
                this.setHealth(this.getMaxHealth());
            }
            AttributeInstance spd = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (spd != null) {
                spd.setBaseValue(stage >= STAGE_RAMPAGE ? 0.55D : 0.32D);
            }
            this.setCustomName(Component.literal("VERTE"));
        }
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
        float scale = (this.entityData != null && this.getStage() >= STAGE_MONSTER) ? 6.0F : 1.0F;
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
        Player owner = this.findOwner(level);

        if (this.getStage() == STAGE_ANGRY && this.angerDay >= 0 && day - this.angerDay >= 3) {
            this.setStage(STAGE_MONSTER);
            this.say(owner, "\u0422\u044b \u0440\u0430\u0437\u043e\u0437\u043b\u0438\u043b \u043c\u0435\u043d\u044f. \u0422\u0435\u043f\u0435\u0440\u044c \u044f \u0440\u044f\u0434\u043e\u043c.");
        }

        if (owner instanceof ServerPlayer sp) {
            int gt = sp.gameMode.getGameModeForPlayer().getId();
            if (this.lastGameType != -1 && gt != this.lastGameType && gt == GameType.CREATIVE.getId()) {
                this.say(sp, "\u041d\u0435\u0447\u0435\u0441\u0442\u043d\u043e. \u0412\u044b\u043a\u043b\u044e\u0447\u0438 \u043a\u0440\u0435\u0430\u0442\u0438\u0432, \u0442\u0440\u0443\u0441.");
                this.provoke(1, day);
            }
            this.lastGameType = gt;

            boolean sleeping = sp.isSleeping();
            if (sleeping && !this.wasSleeping) {
                this.sleeps++;
                if (this.getStage() >= STAGE_MONSTER && this.getStage() < STAGE_RAMPAGE && this.sleeps >= 3) {
                    this.setStage(STAGE_RAMPAGE);
                    this.say(sp, "\u042f \u043f\u0440\u0438\u0448\u0451\u043b.");
                }
            }
            this.wasSleeping = sleeping;

            if (this.getStage() >= STAGE_ANGRY && --this.actionCooldown <= 0) {
                this.actionCooldown = 30 + this.random.nextInt(60);
                this.scare(level, sp);
            }

            if (this.getStage() >= STAGE_RAMPAGE) {
                this.rampage(level, sp);
            }
        }
    }

    private Player findOwner(ServerLevel level) {
        if (this.ownerUUID != null) {
            Player p = level.getPlayerByUUID(this.ownerUUID);
            if (p != null) {
                return p;
            }
        }
        return level.getNearestPlayer(this, 128.0D);
    }

    private void scare(ServerLevel level, ServerPlayer player) {
        int r = this.random.nextInt(4);
        if (r == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.0F, 0.7F);
        } else if (r == 1) {
            this.say(player, CREEPY[this.random.nextInt(CREEPY.length)]);
        } else if (r == 2) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.8F, 0.5F);
        } else if (this.getStage() >= STAGE_MONSTER) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
        }
    }

    private void rampage(ServerLevel level, ServerPlayer player) {
        if (this.distanceToSqr(player) > 64.0D * 64.0D) {
            Vec3 pos = player.position();
            this.teleportTo(pos.x + 6.0D, pos.y, pos.z + 6.0D);
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
        player.sendSystemMessage(Component.literal("Verte \u00bb ")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.RED)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VerteStage", this.getStage());
        tag.putInt("VerteAnger", this.anger);
        tag.putLong("VerteAngerDay", this.angerDay);
        tag.putInt("VerteSleeps", this.sleeps);
        if (this.ownerUUID != null) {
            tag.putUUID("VerteOwner", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {