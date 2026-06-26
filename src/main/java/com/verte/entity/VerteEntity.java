package com.verte.entity;

import com.verte.CorruptionManager;
import com.verte.ModItems;
import com.verte.VerteBrain;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * CLASSIC (\"поугарать\") version of Verte. No horror. He is on his own: he does
 * NOT follow players around \u2014 he wanders, does whatever he feels like through the
 * AI brain, and once in a while teleports straight to a player and starts
 * crit-hitting them with a sword while wearing full netherite. He still obeys
 * direct orders typed in chat.
 */
public class VerteEntity extends PathfinderMob {

    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BIG =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_WAVING =
            SynchedEntityData.defineId(VerteEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int PHASE_FRIENDLY = CorruptionManager.PHASE_FRIENDLY;
    public static final int PHASE_STRANGE = CorruptionManager.PHASE_STRANGE;
    public static final int PHASE_HOSTILE = CorruptionManager.PHASE_HOSTILE;
    public static final int PHASE_MONSTER = CorruptionManager.PHASE_MONSTER;

    private static final int TASK_NONE = 0;
    private static final int TASK_FOLLOW = 1;
    private static final int TASK_COME = 2;
    private static final int TASK_CHOP = 3;

    private int task = TASK_NONE;
    private BlockPos taskTarget;
    private int emergeTicks;
    private int waveUntil = -1;
    private int sayingUntil = -1;
    private int nextActAt = -1;
    private boolean geared;
    private int aggroUntil = -1;
    private UUID targetUUID;
    private int swingCd;
    private int tpCooldown;
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
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 256.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, 0);
        this.entityData.define(DATA_BIG, false);
        this.entityData.define(DATA_WAVING, false);
    }

    @Override
    protected void registerGoals() {
        // He is on his own \u2014 he strolls around, he does not follow players.
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0D));
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

    public boolean isWaving() {
        return this.entityData.get(DATA_WAVING);
    }

    public void setOwnerUUID(UUID uuid) {
        this.ownerUUID = uuid;
    }

    /** True while Verte is busy doing his own thing, so passive goals step aside. */
    public boolean isBusy() {
        return this.task != TASK_NONE || this.aggroUntil > 0;
    }

    public void startEmerge() {
        this.emergeTicks = 30;
        this.entityData.set(DATA_WAVING, true);
        this.setNoAi(true);
    }

    /** Show a line of text floating above Verte for a few seconds (mirrors chat). */
    public void displaySpeech(String text) {
        this.setCustomName(Component.literal(text));
        this.setCustomNameVisible(true);
        this.sayingUntil = this.tickCount + 120;
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
        // Sneak-interact picks Verte back up into a box.
        if (player.isShiftKeyDown()) {
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
        // Hits never anger him; he just shrugs them off.
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

        // Always kitted out in full netherite with a sword in hand.
        if (!this.geared) {
            this.equipNetherite();
            this.geared = true;
        }

        // Revert the floating speech back to his name after a few seconds.
        if (this.sayingUntil > 0 && this.tickCount >= this.sayingUntil) {
            this.setCustomName(Component.literal("verte"));
            this.sayingUntil = -1;
        }

        // Climbing out of the box, then a silent wave.
        if (this.emergeTicks > 0) {
            this.emergeTicks--;
            level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3D, this.getZ(), 6, 0.3D, 0.2D, 0.3D, 0.01D);
            if (this.emergeTicks == 0) {
                this.setNoAi(false);
                this.waveUntil = this.tickCount + 50;
            }
            return;
        }
        if (this.isWaving() && this.tickCount >= this.waveUntil) {
            this.entityData.set(DATA_WAVING, false);
        }

        // Tasks run every tick for smooth movement.
        this.tickTask(level);

        // Combat runs every tick while he is on the warpath.
        if (this.aggroUntil > 0) {
            this.tickCombat(level);
            return;
        }

        if (this.tickCount % 20 != 0) {
            return;
        }

        Player owner = this.findOwner(level);

        // Autonomy: every ~30-60s he either does something chaotic via the brain
        // or decides to teleport in and beat a player up. He never just follows.
        if (this.nextActAt < 0) {
            this.nextActAt = this.tickCount + 300 + this.random.nextInt(300);
        }
        if (this.tickCount >= this.nextActAt) {
            this.nextActAt = this.tickCount + 600 + this.random.nextInt(600);
            if (this.task == TASK_NONE && owner instanceof ServerPlayer sp) {
                if (this.random.nextInt(3) == 0) {
                    this.startAttack(sp, 140 + this.random.nextInt(120));
                } else {
                    VerteBrain.act(sp);
                }
            }
        }
    }

    // ---- Combat: teleport in and crit with a netherite sword ----

    public void startAttack(Player target, int durationTicks) {
        if (target == null) {
            return;
        }
        this.clearTask();
        if (!this.geared) {
            this.equipNetherite();
            this.geared = true;
        }
        this.targetUUID = target.getUUID();
        this.aggroUntil = this.tickCount + durationTicks;
        this.swingCd = 0;
        this.tpCooldown = 0;
        if (this.level() instanceof ServerLevel sl) {
            this.teleportToTarget(sl, target);
        }
    }

    public void endAttack() {
        this.aggroUntil = -1;
        this.targetUUID = null;
        this.getNavigation().stop();
    }

    private void tickCombat(ServerLevel level) {
        if (this.tickCount >= this.aggroUntil) {
            this.endAttack();
            return;
        }
        Player target = (this.targetUUID != null) ? level.getPlayerByUUID(this.targetUUID) : null;
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()) {
            this.endAttack();
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double d2 = this.distanceToSqr(target);
        if (d2 > 256.0D && this.tpCooldown <= 0) {
            this.teleportToTarget(level, target);
            this.tpCooldown = 30;
        } else if (d2 > 4.0D) {
            this.getNavigation().moveTo(target, 1.8D);
        } else {
            this.getNavigation().stop();
        }
        if (this.tpCooldown > 0) {
            this.tpCooldown--;
        }
        if (this.swingCd > 0) {
            this.swingCd--;
        }
        if (d2 < 6.25D && this.swingCd <= 0) {
            this.critHit(level, target);
            this.swingCd = 12;
        }
    }

    private void critHit(ServerLevel level, Player target) {
        this.swing(InteractionHand.MAIN_HAND);
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
        boolean hit = target.hurt(this.damageSources().mobAttack(this), dmg);
        if (hit) {
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0D, target.getZ(),
                    14, 0.3D, 0.5D, 0.3D, 0.25D);
            level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private void teleportToTarget(ServerLevel level, Player target) {
        this.poof(level, this.position());
        Vec3 dir = target.getViewVector(1.0F).normalize();
        double x = target.getX() + dir.x * 1.5D;
        double z = target.getZ() + dir.z * 1.5D;
        this.teleportTo(x, target.getY(), z);
        this.getNavigation().stop();
        this.poof(level, this.position());
    }

    private void equipNetherite() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    // ---- Player-like behaviour: chat commands & tasks (he obeys) ----

    public boolean handleChatCommand(ServerPlayer player, String message, int phase) {
        String m = message.toLowerCase();

        if (containsAny(m, "\u0441\u0442\u043e\u043f", "\u0441\u0442\u043e\u0439", "\u0445\u0432\u0430\u0442\u0438\u0442", "\u043e\u0442\u043c\u0435\u043d", "stop")) {
            this.clearTask();
            this.endAttack();
            this.speakAsPlayer("\u043b\u0430\u0434\u043d\u043e, \u0441\u0442\u043e\u044e.");
            return true;
        }

        int intent;
        if (containsAny(m, "\u0440\u0443\u0431", "\u0434\u0435\u0440\u0435\u0432", "chop", "tree", "wood")) {
            intent = TASK_CHOP;
        } else if (containsAny(m, "\u043a\u043e \u043c\u043d\u0435", "\u0438\u0434\u0438 \u0441\u044e\u0434\u0430", "\u043f\u043e\u0434\u043e\u0439\u0434\u0438", "come")) {
            intent = TASK_COME;
        } else if (containsAny(m, "\u0441\u043b\u0435\u0434\u0443\u0439", "\u0437\u0430 \u043c\u043d\u043e\u0439", "\u0438\u0434\u0438 \u0437\u0430", "follow")) {
            intent = TASK_FOLLOW;
        } else {
            // Everything else (greetings, chatter, requests) goes to the AI brain.
            return false;
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

    private void poof(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.6D, pos.z, 25, 0.35D, 0.5D, 0.35D, 0.02D);
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.6D, pos.z, 18, 0.3D, 0.4D, 0.3D, 0.01D);
        level.playSound(null, BlockPos.containing(pos.x, pos.y, pos.z), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.7F, 1.0F);
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

    private void speakAsPlayer(String text) {
        MinecraftServer server = this.level().getServer();
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("<verte> " + text), false);
        }
        this.displaySpeech(text);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VerteTask", this.task);
        tag.putBoolean("VerteBig", this.isBig());
        if (this.ownerUUID != null) {
            tag.putUUID("VerteOwner", this.ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.task = tag.getInt("VerteTask");
        if (tag.getBoolean("VerteBig")) {
            this.entityData.set(DATA_BIG, true);
        }
        if (tag.hasUUID("VerteOwner")) {
            this.ownerUUID = tag.getUUID("VerteOwner");
        }
        this.entityData.set(DATA_PHASE, 0);
    }
}
