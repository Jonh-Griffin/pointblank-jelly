package com.vicmatskiv.pointblank.entity;

import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.HitboxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;

public class ProjectileBulletEntity extends AbstractArrow {
    public static final EntityType<ProjectileBulletEntity> TYPE;
    public float speed;
    public float damage;
    public int time = 0;
    public int shotCount;
    public Vec3 initPos = new Vec3(0, 0, 0);
    public float maxDistance = 1000f;
    public float headshotMultiplier = 1.0f;

    public ProjectileBulletEntity(EntityType<ProjectileBulletEntity> entityType, Level world) {
        super(entityType, world);
    }

    public ProjectileBulletEntity(EntityType<ProjectileBulletEntity> entityType, double x, double y, double z, Level world) {
        super(entityType, x, y, z, world);
    }

    public ProjectileBulletEntity(EntityType<ProjectileBulletEntity> entityType, LivingEntity shooter, Level world) {
        super(entityType, shooter, world);
    }

    public ProjectileBulletEntity(LivingEntity shooter, Level world, float damage, float speed, int shotCount, float maxDistance) {
        this(TYPE, shooter.getX(), shooter.getEyeY() - (double) 0.1F, shooter.getZ(), world);
        this.setOwner(shooter);
        this.damage = damage;
        this.speed = speed;
        this.shotCount = shotCount;
        this.initPos = new Vec3(shooter.getX(), shooter.getEyeY() - (double) 0.1F, shooter.getZ());
        this.maxDistance = maxDistance;
        this.headshotMultiplier = 1f;
        this.setSoundEvent(SoundEvents.EMPTY);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    @Override
    public double getBaseDamage() {
        return this.damage;
    }

    @Override
    public void tick() {
        time++;
        if(time > 100) {
            discard();
            return;
        }

        if(Math.abs(initPos.distanceTo(this.position().add(this.getDeltaMovement()))) >= maxDistance) {
            this.discard();
            return;
        }

        boolean flag = false;
        Vec3 vec3 = this.getDeltaMovement();
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double d0 = vec3.horizontalDistance();
            this.setYRot((float) (Mth.atan2(vec3.x, vec3.z) * 57.2957763671875));
            this.setXRot((float) (Mth.atan2(vec3.y, d0) * 57.2957763671875));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        BlockPos blockpos = this.blockPosition();
        BlockState blockstate = this.level().getBlockState(blockpos);
        Vec3 vec33;
        if (!blockstate.isAir() && !(blockstate.getBlock() instanceof LeavesBlock || blockstate.getBlock() instanceof AbstractGlassBlock) && !flag) {
            VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
            if (!voxelshape.isEmpty()) {
                vec33 = this.position();

                for (AABB aabb : voxelshape.toAabbs()) {
                    if (aabb.move(blockpos).contains(vec33)) {
                        this.inGround = true;
                        break;
                    }
                }
            }
        }

        if (this.shakeTime > 0) {
            --this.shakeTime;
        }

        if (this.isInWaterOrRain() || blockstate.is(Blocks.POWDER_SNOW) || this.isInFluidType((fluidType, height) -> {
            return this.canFluidExtinguish(fluidType);
        })) {
            this.clearFire();
        }

        if (this.inGround && !flag) {
            ++this.inGroundTime;
        } else {
            this.inGroundTime = 0;
            Vec3 vec32 = this.position();
            vec33 = vec32.add(vec3);
            HitResult hitresult;
            if (blockstate.getBlock() instanceof LeavesBlock) {
                hitresult = this.level().clip(new ClipContext(vec32.add(0, 1000, 0), vec32.add(0, 1000, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            } else {
                hitresult = this.level().clip(new ClipContext(vec32, vec33, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            }
            if (hitresult.getType() != HitResult.Type.MISS) {
                vec33 = hitresult.getLocation();
            }

            while (!this.isRemoved()) {
                EntityHitResult entityhitresult = this.findHitEntity(vec32, vec33);
                if (entityhitresult != null) {
                    hitresult = entityhitresult;
                }

                if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult) hitresult).getEntity();
                    Entity entity1 = this.getOwner();
                    if (entity instanceof Player && entity1 instanceof Player && !((Player) entity1).canHarmPlayer((Player) entity)) {
                        hitresult = null;
                        entityhitresult = null;
                    }
                }

                if (hitresult != null && hitresult.getType() != HitResult.Type.MISS) {
                    switch (ForgeEventFactory.onProjectileImpactResult(this, hitresult)) {
                        case SKIP_ENTITY:
                            if (hitresult.getType() != HitResult.Type.ENTITY) {
                                this.onHit(hitresult);
                                this.hasImpulse = true;
                            } else {
                                entityhitresult = null;
                            }
                            break;
                        case STOP_AT_CURRENT_NO_DAMAGE:
                            this.discard();
                            entityhitresult = null;
                            break;
                        case STOP_AT_CURRENT:
                            this.setPierceLevel((byte) 0);
                        case DEFAULT:
                            this.onHit(hitresult);
                            this.hasImpulse = true;
                    }
                }

                if (entityhitresult == null || this.getPierceLevel() <= 0) {
                    break;
                }

                hitresult = null;
            }

            if (this.isRemoved()) {
                return;
            }

            vec3 = this.getDeltaMovement();
            double d5 = vec3.x;
            double d6 = vec3.y;
            double d1 = vec3.z;
            if (this.isCritArrow()) {
                for (int i = 0; i < 4; ++i) {
                    this.level().addParticle(ParticleTypes.CRIT, this.getX() + d5 * (double) i / 4.0, this.getY() + d6 * (double) i / 4.0, this.getZ() + d1 * (double) i / 4.0, -d5, -d6 + 0.2, -d1);
                }
            }

            double d7 = this.getX() + d5;
            double d2 = this.getY() + d6;
            double d3 = this.getZ() + d1;
            double d4 = vec3.horizontalDistance();
            //if (flag) {
            //    this.setYRot((float)(Mth.atan2(-d5, -d1) * 57.2957763671875));
            //} else {
            //    this.setYRot((float)(Mth.atan2(d5, d1) * 57.2957763671875));
            //}

            this.setXRot((float) (Mth.atan2(d6, d4) * 57.2957763671875));
            this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
            this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
            float f = 0.99F;
            float f1 = 0.05F;
            if (this.isInWater()) {
                for (int j = 0; j < 4; ++j) {
                    float f2 = 0.25F;
                    this.level().addParticle(ParticleTypes.BUBBLE, d7 - d5 * 0.25, d2 - d6 * 0.25, d3 - d1 * 0.25, d5, d6, d1);
                }

                f = this.getWaterInertia();
            }

            this.setDeltaMovement(vec3.scale(f));
            if (!this.isNoGravity()) {
                Vec3 vec34 = this.getDeltaMovement();
                this.setDeltaMovement(vec34.x, vec34.y - 0.05000000074505806, vec34.z);
            }

            //this.setPos(d7, d2, d3);
            lerpTo(d7, d2, d3, this.getYRot(), this.getXRot(), 0, false);
            if (!(blockstate.getBlock() instanceof LeavesBlock || blockstate.getBlock() instanceof AbstractGlassBlock)) {
                this.checkInsideBlocks();
            }
        }


        this.firstTick = false;
        this.level().getProfiler().pop();
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return new ItemStack(Items.AIR);
    }

    static {
        TYPE = EntityType.Builder.of((EntityType.EntityFactory<ProjectileBulletEntity>) ProjectileBulletEntity::new, MobCategory.MISC).noSummon().noSave().fireImmune().sized(0.0225F, 0.0225F).clientTrackingRange(256).canSpawnFarFromPlayer().setTrackingRange(512).updateInterval(0).setShouldReceiveVelocityUpdates(true).build("bullet");
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        this.playSound(level().getBlockState(pResult.getBlockPos()).getSoundType().getHitSound(), 1f / shotCount, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.EMPTY;
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity entity = pResult.getEntity();
        AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, this.getOwner());
        Vec3 startVec = this.position();
        Vec3 endVec = startVec.add(this.getDeltaMovement());
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        double headshotmulti = 1.0;
        if (hitPos != null) {
            Vec3 hitBoxPos = hitPos.subtract(entity.position());

            boolean headshot = false;
            float eyeHeight = entity.getEyeHeight();
            if(entity instanceof Player && Config.PLAYERHEADSHOTS.get()) {
                if ((double) eyeHeight - 0.25 < hitBoxPos.y && hitBoxPos.y < (double) eyeHeight + 0.25) {
                    headshot = true;
                    headshotmulti = headshotMultiplier;
                }
            }

            if(!(entity instanceof Player) && Config.MOBHEADSHOTS.get()) {
                if ((double) eyeHeight - 0.25 < hitBoxPos.y && hitBoxPos.y < (double) eyeHeight + 0.25) {
                    headshot = true;
                    headshotmulti = headshotMultiplier;
                }
            }

            DamageSource damageSource = pResult.getEntity().damageSources().arrow(this, this.getOwner());
            if (headshot)
                level().playSound(null, entity.blockPosition(), SoundRegistry.HIT_HEADSHOT.get(), SoundSource.HOSTILE, 0.8f / shotCount, 1.0f);

            pResult.getEntity().hurt(damageSource, (float) (damage * headshotmulti));
            Entity entity1 = this.getOwner();
            //Lower invulnerability for gun firing (Full auto more viable)
            pResult.getEntity().invulnerableTime = 2;
            this.discard();
        }
    }
}
