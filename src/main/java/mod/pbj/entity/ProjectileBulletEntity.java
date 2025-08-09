package mod.pbj.entity;

import mod.pbj.Config;
import mod.pbj.network.HitScanFireResponsePacket;
import mod.pbj.network.Network;
import mod.pbj.registry.SoundRegistry;
import mod.pbj.util.HitboxHelper;
import mod.pbj.util.MiscUtil;
import mod.pbj.util.SimpleHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.PacketDistributor;

import static mod.pbj.item.GunItem.getItemStackId;

public class ProjectileBulletEntity extends AbstractHurtingProjectile {
	public static final EntityType<ProjectileBulletEntity> TYPE;
	private int correlationId;
	public float speed;
	public float damage;
	public int time = 0;
	public int shotCount;
	public Vec3 initPos = new Vec3(0, 0, 0);
	public float maxDistance = 1500f;
	public float headshotMultiplier = 1.0f;
	public ItemStack gunStack;
	public static TagKey<Block> PASSABLE = BlockTags.create(ResourceLocation.fromNamespaceAndPath("pointblank", "passable"));
	public static TagKey<Block> BREAKABLE = BlockTags.create(ResourceLocation.fromNamespaceAndPath("pointblank", "breakable"));
	private float bulletGravity = 0.03f;

	public void setBulletGravity(float gravity) {
		this.bulletGravity = gravity;
	}
	public ProjectileBulletEntity(Level world) {
		super(TYPE, world);
	}
	public ProjectileBulletEntity(EntityType<ProjectileBulletEntity> entityType, Level world) {
		super(entityType, world);
	}

	public ProjectileBulletEntity(
		EntityType<ProjectileBulletEntity> entityType, double x, double y, double z, Level world) {
		super(entityType, x, y, z, 0, 0, 0, world);
	}

	public ProjectileBulletEntity(EntityType<ProjectileBulletEntity> entityType, LivingEntity shooter, Level world) {
		super(entityType, shooter, 0, 0, 0, world);
	}

	public boolean checkLeftOwner() {
		Entity entity = this.getOwner();
		if (entity != null) {
			for (Entity entity1 : this.level().getEntities(
					 this,
					 this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D),
					 (p_37272_) -> !p_37272_.isSpectator() && p_37272_.isPickable())) {
				if (entity1.getRootVehicle() == entity.getRootVehicle()) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public void tick() {
		super.tick();
		this.addDeltaMovement(new Vec3(0, -bulletGravity, 0));
	}

	public ProjectileBulletEntity(
		LivingEntity shooter,
		Level world,
		float damage,
		float speed,
		int shotCount,
		float maxDistance,
		float headshotMultiplier,
		ItemStack gunStack,
		int correlationId) {
		this(TYPE, shooter.getX(), shooter.getEyeY() - (double)0.1F, shooter.getZ(), world);
		this.setOwner(shooter);
		this.damage = damage;
		this.speed = speed;
		this.shotCount = shotCount;
		this.initPos = new Vec3(shooter.getX(), shooter.getEyeY() - (double)0.1F, shooter.getZ());
		this.maxDistance = maxDistance;
		this.headshotMultiplier = headshotMultiplier;
		this.gunStack = gunStack;
		this.correlationId = correlationId;
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public float getDamage() {
		return damage;
	}

	static {
		TYPE = EntityType.Builder
				   .of((EntityType.EntityFactory<ProjectileBulletEntity>)ProjectileBulletEntity::new, MobCategory.MISC)
				   .noSummon()
				   .noSave()
				   .fireImmune()
				   .sized(0.0225F, 0.0225F)
				   .clientTrackingRange(256)
				   .canSpawnFarFromPlayer()
				   .setTrackingRange(512)
				   .updateInterval(1)
				   .setShouldReceiveVelocityUpdates(true)
				   .build("bullet");
	}

	@Override
	protected void onHit(HitResult pResult) {
		HitResult.Type hitresult$type = pResult.getType();
		if (hitresult$type == HitResult.Type.ENTITY) {
			this.onHitEntity((EntityHitResult)pResult);
			this.level().gameEvent(GameEvent.PROJECTILE_LAND, pResult.getLocation(), GameEvent.Context.of(this, (BlockState)null));
		} else if (hitresult$type == HitResult.Type.BLOCK) {
			BlockHitResult blockhitresult = (BlockHitResult)pResult;
			this.onHitBlock(blockhitresult);
			BlockPos blockpos = blockhitresult.getBlockPos();
			BlockState blockState = level().getBlockState(blockpos);
			if(blockState.is(BREAKABLE)) {
				level().destroyBlock(blockpos, true, getOwner());
				return;
			}
			if(blockState.is(PASSABLE)) {
				return;
			}
			this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult pResult) {
		if(pResult.getLocation().distanceTo(initPos) >= this.maxDistance) {
			this.discard();
			return;
		}
		var blockState = level().getBlockState(pResult.getBlockPos());
		if(blockState.is(BREAKABLE)) {
			level().destroyBlock(pResult.getBlockPos(), true, getOwner());
			return;
		}
		if(blockState.is(PASSABLE)) {
			return;
		}

		super.onHitBlock(pResult);
		if (getOwner() != null && level() instanceof ServerLevel)
			for (ServerPlayer serverPlayer : ((ServerLevel)MiscUtil.getLevel(getOwner())).getPlayers((p) -> true)) {
				if (serverPlayer == getOwner() ||
					serverPlayer.distanceToSqr(getOwner()) < (maxDistance * maxDistance)) {
					Network.networkChannel.send(
						PacketDistributor.PLAYER.with(() -> serverPlayer),
						new HitScanFireResponsePacket(
							getOwner().getId(),
							getItemStackId(this.gunStack),
							serverPlayer.getInventory().findSlotMatchingItem(this.gunStack),
							this.correlationId,
							SimpleHitResult.fromHitResult(pResult),
							damage));
				}
			}
		this.playSound(
			level().getBlockState(pResult.getBlockPos()).getSoundType().getHitSound(),
			1f / shotCount,
			1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
	}

	@Override
	protected void onHitEntity(EntityHitResult pResult) {
		if(pResult.getLocation().distanceTo(initPos) >= this.maxDistance) {
			this.discard();
			return;
		}

		Entity entity = pResult.getEntity();
		AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, this.getOwner());
		Vec3 startVec = this.position();
		Vec3 endVec = startVec.add(this.getDeltaMovement());
		Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
		double headshotmulti = 1.0;
		if (hitPos != null) {
			if (getOwner() != null && level() instanceof ServerLevel)
				for (ServerPlayer serverPlayer : ((ServerLevel)MiscUtil.getLevel(getOwner())).getPlayers((p) -> true)) {
					if (serverPlayer == getOwner() ||
						serverPlayer.distanceToSqr(getOwner()) < (maxDistance * maxDistance)) {
						Network.networkChannel.send(
							PacketDistributor.PLAYER.with(() -> serverPlayer),
							new HitScanFireResponsePacket(
								getOwner().getId(),
								getItemStackId(this.gunStack),
								serverPlayer.getInventory().findSlotMatchingItem(this.gunStack),
								this.correlationId,
								SimpleHitResult.fromHitResult(pResult),
								damage));
					}
				}

			Vec3 hitBoxPos = hitPos.subtract(entity.position());

			boolean headshot = false;
			float eyeHeight = entity.getEyeHeight();
			if (entity instanceof Player && Config.PLAYERHEADSHOTS.get()) {
				if ((double)eyeHeight - 0.25 < hitBoxPos.y && hitBoxPos.y < (double)eyeHeight + 0.25) {
					headshot = true;
					headshotmulti = headshotMultiplier;
				}
			}

			if (!(entity instanceof Player) && Config.MOBHEADSHOTS.get()) {
				if ((double)eyeHeight - 0.25 < hitBoxPos.y && hitBoxPos.y < (double)eyeHeight + 0.25) {
					headshot = true;
					headshotmulti = headshotMultiplier;
				}
			}

			DamageSource damageSource =
				pResult.getEntity().damageSources().mobProjectile(this, (LivingEntity)this.getOwner());
			if (headshot)
				level().playSound(
					null,
					entity.blockPosition(),
					SoundRegistry.HIT_HEADSHOT.get(),
					SoundSource.HOSTILE,
					0.8f / shotCount,
					1.0f);

			pResult.getEntity().hurt(damageSource, (float)(damage * headshotmulti));
			// Lower invulnerability for gun firing (Full auto more viable)
			pResult.getEntity().invulnerableTime = Config.iframes;
			this.discard();
		}
	}
}
