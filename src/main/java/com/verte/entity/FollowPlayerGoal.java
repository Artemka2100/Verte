package com.verte.entity;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class FollowPlayerGoal extends Goal {
    private final PathfinderMob mob;
    private final double speedModifier;
    private final float stopDistance;
    private final float startDistance;
    private final PathNavigation navigation;
    private Player target;
    private int timeToRecalcPath;

    public FollowPlayerGoal(PathfinderMob mob, double speedModifier, float stopDistance, float startDistance) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.stopDistance = stopDistance;
        this.startDistance = startDistance;
        this.navigation = mob.getNavigation();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player nearest = this.mob.level().getNearestPlayer(this.mob, 64.0D);
        if (nearest == null || nearest.isSpectator()) {
            return false;
        }
        if (this.mob.distanceToSqr(nearest) < (double) (this.startDistance * this.startDistance)) {
            return false;
        }
        this.target = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        }
        if (this.target == null || this.target.isSpectator()) {
            return false;
        }
        return this.mob.distanceToSqr(this.target) > (double) (this.stopDistance * this.stopDistance);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
    }

    @Override
    public void stop() {
        this.target = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.target, 10.0F, (float) this.mob.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (this.mob.distanceToSqr(this.target) > (double) (this.stopDistance * this.stopDistance)) {
                this.navigation.moveTo(this.target, this.speedModifier);
            } else {
                this.navigation.stop();
            }
        }
    }
}
