package com.verte.entity;

import com.verte.AtmosphereManager;
import com.verte.CorruptionManager;
import com.verte.ModItems;
import com.verte.StoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.item.ItemStack;
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

    private static final int TASK_NONE = 0;
    private static final int TASK_FOLLOW = 1;
    private static final int TASK_COME = 2;
    private static final int TASK_CHOP = 3;

    private static final String[] CREEPY = {
            "\u042f \u0432\u0438\u0436\u0443 \u0442\u0435\u0431\u044f.",
            "\u0422\u044b \u043d\u0435 \u043e\u0434\u0438\u043d.",
            "\u0421\u043a\u043e\u0440\u043e.",
            "\u041e\u0431\u0435\u0440\u043d\u0438\u0441\u044c."
    };

    private long lastDay = -1L;
    private int sleeps;
    private boolean wasSleeping;
    private boolean stalking;
    private boolean rampaging;
    private int lastGameType = -1;
    private int nearTicks;
    private int currentPhase = -1;
    private int storyStep;
    private int task = TASK_NONE;
    private BlockPos taskTarget;
    private int vanishUntil = -1;
    private int emergeTicks;
    private UUID ownerUUID;

    public VerteEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setCustomName(Component.literal("verte"));
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

    /** True while Verte is doing his own thing, so the follow goal should step aside. */
    public boolean isBusy() {
        return this.task != TASK_NONE || this.stalking || this.rampaging || this.getPhase() >= PHASE_MONSTER;
    }

    public void startEmerge() {
        this.emergeTicks = 30;
        this.setNoAi(true);
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
        }
        this.refreshDimensions();
    }

    private void announcePhase(ServerPlayer sp, int phase, int prev) {
        if (phase <= prev) {
            return;
        }
        switch (phase) {
            case PHASE_STRANGE -> this.speakAsPlayer("\u044f \u043d\u0430\u0447\u0438\u043d\u0430\u044e \u0442\u0435\u0431\u044f \u0437\u0430\u043c\u0435\u0447\u0430\u0442\u044c...");
            case PHASE_HOSTILE -> this.speakAsPlayer("\u0442\u044b \u043c\u043d\u0435 \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435 \u043d\u0440\u0430\u0432\u0438\u0448\u044c\u0441\u044f.");
            case PHASE_MONSTER -> this.speakAsPlayer("\u044f \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435 \u043f\u043e\u043c\u043e\u0449\u043d\u0438\u043a. \u0431\u0435\u0433\u0438.");
            default -> {
            }
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.entityData != null && this.isBig()) {
            return EntityDimensions.scalable(1.4F, 7.0F);
        }
        return super.getDimensions(pose);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        // Sneak-interact picks Verte back up into a box \u2014 if he still tolerates you.
        if (player.isShiftKeyDown()) {
            if (this.getPhase() >= PHASE_HOSTILE) {
                this.speakAsPlayer(this.getPhase() >= PHASE_MONSTER ? "\u043d\u0435 \u0442\u0440\u043e\u0433\u0430\u0439 \u043c\u0435\u043d\u044f." : "\u0440\u0443\u043a\u0438 \u0443\u0431\u0440\u0430\u043b.");
                return InteractionResult.CONSUME;
            }
            ItemStack box = new ItemStack(ModItems.VERTE_BOX.get());
            if (!player.addItem(box)) {
                player.drop(box, false);
            }
            if (this.level() instanceof ServerLevel sl) {
                this.poof(sl, this.position());
            }
            this.discard();
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer sp) {
            CorruptionManager.add(sp, 2);
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

        // Climbing out of the box.
        if (this.emergeTicks > 0) {
            this.emergeTicks--;
            level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3D, this.getZ(), 6, 0.3D, 0.2D, 0.3D, 0.01D);
            if (this.emergeTicks == 0) {
                this.setNoAi(false);
            }
            return;
        }

        // Vanish/reappear cycle after being spotted while stalking.
        if (this.vanishUntil > 0) {
            if (this.tickCount >= this.vanishUntil) {
                this.reappear(level);
            } else {
                return;
            }
        }

        // Tasks run every tick for smooth movement.
        this.tickTask(level);

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
            if (day > this.lastDay) {
                CorruptionManager.add(sp, (int) Math.min(5L, day - this.lastDay) * 3);
                this.lastDay = day;
            }
            if (this.distanceToSqr(sp) < 24.0D * 24.0D) {
                if (++this.nearTicks >= 15) {
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

            this.storyStep = StoryManager.progress(level, sp, corruption, this.storyStep);

            int gt = sp.gameMode.getGameModeForPlayer().getId();
            if (this.lastGameType != -1 && gt != this.lastGameType && gt == GameType.CREATIVE.getId()) {
                this.speakAsPlayer(phase >= PHASE_HOSTILE ? "\u043a\u0440\u0435\u0430\u0442\u0438\u0432? \u0442\u0440\u0443\u0441." : "\u044d\u0439, \u044d\u0442\u043e \u043d\u0435\u0447\u0435\u0441\u0442\u043d\u043e :(");
                CorruptionManager.add(sp, 1);
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
                }
            }
            this.wasSleeping = sleeping;

            this.handleStalking(level, sp, night, phase);

            if (!this.stalking && this.task == TASK_NONE) {
                this.followOwner(sp, phase);
            }

            this.atmosphere(level, sp, phase, corruption);

            if (phase == PHASE_STRANGE && this.task == TASK_NONE && this.random.nextInt(30) == 0) {
                this.teleportNear(sp, 18.0D + this.random.nextInt(8), true);
                level.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.6F, 0.5F);
            }
            if (phase >= PHASE_HOSTILE && this.random.nextInt(15) == 0) {
                this.obstruct(level, sp);
            }
            if (phase >= PHASE_MONSTER) {
                // Huge phase: silent relentless pursuit, very few messages or effects.
                this.chase(level, sp);
                if (this.random.nextInt(12) == 0) {
                    this.scareSilent(level, sp);
                }
                if (this.rampaging) {
                    this.rampage(level, sp);
                }
            }
        } else if (level.isDay() && this.currentPhase < PHASE_MONSTER) {
            this.setBig(false);
            this.stalking = false;
        }
    }

    // ---- Player-like behaviour: chat commands & tasks ----

    public boolean handleChatCommand(ServerPlayer player, String message, int phase) {
        String m = message.toLowerCase();

        if (containsAny(m, "\u0441\u0442\u043e\u043f", "\u0441\u0442\u043e\u0439", "\u0445\u0432\u0430\u0442\u0438\u0442", "\u043e\u0442\u043c\u0435\u043d", "stop")) {
            this.clearTask();
            this.speakAsPlayer(phase >= PHASE_HOSTILE ? "\u043d\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u0443\u0439 \u043c\u043d\u043e\u0439." : "\u043b\u0430\u0434\u043d\u043e, \u0441\u0442\u043e\u044e.");
            return true;
        }

        int intent;
        if (containsAny(m, "\u0440\u0443\u0431", "\u0434\u0435\u0440\u0435\u0432", "chop", "tree", "wood")) {
            intent = TASK_CHOP;
        } else if (containsAny(m, "\u043a\u043e \u043c\u043d\u0435", "\u0438\u0434\u0438 \u0441\u044e\u0434\u0430", "\u043f\u043e\u0434\u043e\u0439\u0434\u0438", "come")) {
            intent = TASK_COME;
        } else if (containsAny(m, "\u0441\u043b\u0435\u0434\u0443\u0439", "\u0437\u0430 \u043c\u043d\u043e\u0439", "\u0438\u0434\u0438 \u0437\u0430", "follow")) {
            intent = TASK_FOLLOW;
        } else if (containsAny(m, "\u0434\u0440\u0443\u0436", "\u043f\u0440\u0438\u0432\u0435\u0442", "\u0437\u0434\u0430\u0440\u043e\u0432", "\u0445\u0430\u0439", "hello", "friend")) {
            this.speakAsPlayer(this.greetByPhase(phase));
            return true;
        } else {
            return false;
        }

        if (!this.willObey(phase)) {
            this.speakAsPlayer(this.refuseByPhase(phase));
            return true;
        }

        this.setOwnerUUID(player.getUUID());
        switch (intent) {
            case TASK_CHOP -> this.beginChop(player);
            case TASK_COME -> {
                this.task = TASK_COME;
                this.speakAsPlayer("\u0438\u0434\u0443 \u043a \u0442\u0435\u0431\u0435.");
            }
            case TASK_FOLLOW -> {
                this.task = TASK_FOLLOW;
                this.speakAsPlayer("\u043e\u043a\u0435\u0439, \u0438\u0434\u0443 \u0437\u0430 \u0442\u043e\u0431\u043e\u0439.");
            }
            default -> {
            }
        }
        return true;
    }

    private boolean willObey(int phase) {
        if (phase >= PHASE_MONSTER) {
            return false;
        }
        if (phase >= PHASE_HOSTILE) {
            return this.random.nextInt(4) == 0;
        }
        if (phase >= PHASE_STRANGE) {
            return this.random.nextInt(10) < 7;
        }
        return true;
    }

    private String greetByPhase(int phase) {
        if (phase >= PHASE_MONSTER) {
            return "\u043f\u043e\u0437\u0434\u043d\u043e. \u0431\u0435\u0433\u0438.";
        }
        if (phase >= PHASE_HOSTILE) {
            return "\u0434\u0440\u0443\u0436\u0438\u0442\u044c? \u0443\u0436\u0435 \u043f\u043e\u0437\u0434\u043d\u043e.";
        }
        if (phase >= PHASE_STRANGE) {
            return "\u0434\u0440\u0443\u0436\u0438\u0442\u044c? \u0442\u044b \u0432\u0435\u0434\u044c \u043c\u0435\u043d\u044f \u0431\u043e\u0438\u0448\u044c\u0441\u044f.";
        }
        return "\u043f\u0440\u0438\u0432\u0435\u0442! \u0434\u0430\u0432\u0430\u0439 \u0434\u0440\u0443\u0436\u0438\u0442\u044c :)";
    }

    private String refuseByPhase(int phase) {
        if (phase >= PHASE_HOSTILE) {
            return "\u0441\u0430\u043c \u0434\u0435\u043b\u0430\u0439.";
        }
        return "\u043c\u043e\u0436\u0435\u0442, \u043f\u043e\u0437\u0436\u0435...";
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }

    public void clearTask() {
        this.task = TASK_NONE;
        this.taskTarget = null;
        this.getNavigation().stop();
    }

    private void beginChop(ServerPlayer player) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos log = this.findNearestLog(level, this.blockPosition(), 20);
        if (log == null) {
            log = this.findNearestLog(level, player.blockPosition(), 20);
        }
        if (log == null) {
            this.speakAsPlayer("\u043d\u0435 \u0432\u0438\u0436\u0443 \u0434\u0435\u0440\u0435\u0432\u044c\u0435\u0432 \u0440\u044f\u0434\u043e\u043c.");
            return;
        }
        this.taskTarget = log;
        this.task = TASK_CHOP;
        this.speakAsPlayer("\u0438\u0434\u0443 \u0440\u0443\u0431\u0438\u0442\u044c \u0434\u0435\u0440\u0435\u0432\u043e!");
    }

    private void tickTask(ServerLevel level) {
        if (this.task == TASK_NONE) {
            return;
        }
        Player owner = this.findOwner(level);
        switch (this.task) {
            case TASK_FOLLOW -> {
                if (owner != null) {
                    if (this.distanceToSqr(owner) > 9.0D) {
                        this.getNavigation().moveTo(owner, 1.1D);
                    } else {
                        this.getNavigation().stop();
                    }
                }
            }
            case TASK_COME -> {
                if (owner != null) {
                    if (this.distanceToSqr(owner) > 6.25D) {
                        this.getNavigation().moveTo(owner, 1.2D);
                    } else {
                        this.clearTask();
                        this.speakAsPlayer("\u044f \u0442\u0443\u0442.");
                    }
                }
            }
            case TASK_CHOP -> this.doChop(level);
            default -> {
            }
        }
    }

    private void doChop(ServerLevel level) {
        if (this.taskTarget == null || !level.getBlockState(this.taskTarget).is(BlockTags.LOGS)) {
            BlockPos next = this.findNearestLog(level, this.blockPosition(), 18);
            if (next == null) {
                this.clearTask();
                this.speakAsPlayer("\u0433\u043e\u0442\u043e\u0432\u043e, \u0434\u0435\u0440\u0435\u0432\u043e \u0441\u0440\u0443\u0431\u043b\u0435\u043d\u043e.");
                return;
            }
            this.taskTarget = next;
        }
        double cx = this.taskTarget.getX() + 0.5D;
        double cy = this.taskTarget.getY() + 0.5D;
        double cz = this.taskTarget.getZ() + 0.5D;
        if (this.distanceToSqr(cx, cy, cz) > 6.25D) {
            this.getNavigation().moveTo(cx, cy, cz, 1.15D);
            this.getLookControl().setLookAt(cx, cy, cz);
        } else {
            level.destroyBlock(this.taskTarget, true);
            BlockPos above = this.taskTarget.above();
            int guard = 0;
            while (level.getBlockState(above).is(BlockTags.LOGS) && guard++ < 12) {
                level.destroyBlock(above, true);
                above = above.above();
            }
            this.taskTarget = null;
        }
    }

    private BlockPos findNearestLog(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -6; dy <= 8; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (level.getBlockState(cursor).is(BlockTags.LOGS)) {
                        double d = cursor.distSqr(center);
                        if (d < bestD) {
                            bestD = d;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---- Stalking, vanishing and atmosphere ----

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
                    // Spotted: puff of clouds, gone for 2-3 seconds, then reappears.
                    this.vanish(level, sp);
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

    private void vanish(ServerLevel level, ServerPlayer sp) {
        this.poof(level, this.position());
        this.getNavigation().stop();
        this.teleportNear(sp, 40.0D, true);
        this.setInvisible(true);
        this.setNoAi(true);
        this.setSilent(true);
        this.vanishUntil = this.tickCount + 40 + this.random.nextInt(21);
    }

    private void reappear(ServerLevel level) {
        this.vanishUntil = -1;
        this.setInvisible(false);
        this.setNoAi(false);
        this.setSilent(false);
        Player owner = this.findOwner(level);
        if (owner != null) {
            this.teleportNear(owner, 12.0D + this.random.nextInt(6), true);
        }
        this.poof(level, this.position());
    }

    private void poof(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.6D, pos.z, 25, 0.35D, 0.5D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.6D, pos.z, 18, 0.3D, 0.4D, 0.3D, 0.01D);
        level.playSound(null, BlockPos.containing(pos.x, pos.y, pos.z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.7F, 0.7F);
    }

    private void scareSilent(ServerLevel level, ServerPlayer sp) {
        if (this.random.nextBoolean()) {
            level.playSound(null, sp.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.7F);
        } else {
            sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        }
    }

    private void followOwner(ServerPlayer sp, int phase) {
        if (this.distanceToSqr(sp) > 28.0D * 28.0D) {
            this.teleportNear(sp, 6.0D, phase >= PHASE_HOSTILE);
        }
    }

    private void atmosphere(ServerLevel level, ServerPlayer sp, int phase, int corruption) {
        if (phase >= PHASE_MONSTER) {
            // Huge Verte: almost no noise. The silent presence is the horror.
            if (this.random.nextInt(8) == 0) {
                AtmosphereManager.ambient(level, sp, phase, this.random);
            }
            return;
        }
        // Subtle early dread even while Verte is still \"friendly\".
        if (phase < PHASE_STRANGE && corruption >= 12 && this.random.nextInt(40) == 0) {
            AtmosphereManager.ambient(level, sp, phase, this.random);
        }
        int ambientChance = Math.max(2, 10 - corruption / 12);
        if (phase >= PHASE_STRANGE && this.random.nextInt(ambientChance) == 0) {
            AtmosphereManager.ambient(level, sp, phase, this.random);
        }
        int distortChance = Math.max(3, 14 - corruption / 8);
        if (phase >= PHASE_HOSTILE && this.random.nextInt(distortChance) == 0) {
            AtmosphereManager.distort(sp, phase, this.random);
        }
        int wallChance = Math.max(6, 26 - corruption / 5);
        if (phase >= PHASE_STRANGE && this.random.nextInt(wallChance) == 0) {
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
                this.say(player, "\u0427\u0443\u0432\u0441\u0442\u0432\u0443\u0435\u0448\u044c \u043c\u043e\u0439 \u0433\u043d\u0435\u0432?");
            }
            case 1 -> {
                level.setWeatherParameters(0, 6000, true, true);
                this.say(player, "\u042f \u043f\u0440\u0438\u043d\u043e\u0448\u0443 \u0431\u0443\u0440\u044e.");
            }
            case 2 -> {
                level.setDayTime(18000L);
                this.say(player, "\u041d\u043e\u0447\u044c \u043f\u043e\u0434\u0447\u0438\u043d\u044f\u0435\u0442\u0441\u044f \u043c\u043d\u0435.");
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
                this.say(player, "\u0422\u044c\u043c\u0430 \u2014 \u043c\u043e\u0439 \u0434\u043e\u043c.");
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
        player.sendSystemMessage(Component.literal("Verte \u00bb ")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.RED)));
    }

    private void speakAsPlayer(String text) {
        MinecraftServer server = this.level().getServer();
        if (server == null) {
            return;
        }
        server.getPlayerList().broadcastSystemMessage(Component.literal("<verte> " + text), false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VertePhase", this.getPhase());
        tag.putLong("VerteLastDay", this.lastDay);
        tag.putInt("VerteSleeps", this.sleeps);
        tag.putInt("VerteStory", this.storyStep);
        tag.putInt("VerteTask", this.task);
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
        this.storyStep = tag.getInt("VerteStory");
        this.task = tag.getInt("VerteTask");
        this.rampaging = tag.getBoolean("VerteRampaging");
        if (tag.hasUUID("VerteOwner")) {
            this.ownerUUID = tag.getUUID("VerteOwner");
        }
        this.entityData.set(DATA_PHASE, tag.getInt("VertePhase"));
        this.currentPhase = -1;
    }
}
