package mod.pbj.client.model;

import mod.pbj.client.BiDirectionalInterpolator;
import mod.pbj.client.ClientEventHandler;
import mod.pbj.client.GunClientState;
import mod.pbj.client.controller.BlendingAnimationProcessor;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.GeckoLibException;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.core.molang.MolangQueries;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.util.RenderUtils;

import java.util.ArrayList;
import java.util.List;

import static mod.pbj.Constants.*;
import static mod.pbj.util.ClientUtil.getClientPlayer;

public class GunGeoModel extends DefaultedItemGeoModel<GunItem> {
	private BakedGeoModel currentModel = null;
	private final List<ResourceLocation> fallbackAnimations = new ArrayList<>();
	private final BlendingAnimationProcessor<GunItem> anotherAnimationProcessor =
		new BlendingAnimationProcessor<>(this);
	@Nullable
	public static GeoBone mainBone;

	public GunGeoModel(ResourceLocation assetSubpath, List<ResourceLocation> fallbackAnimations) {
		super(assetSubpath);

		for (ResourceLocation fallbackAnimation : fallbackAnimations) {
			this.fallbackAnimations.add(this.buildFormattedAnimationPath(fallbackAnimation));
		}
	}

	@Override
	public void applyMolangQueries(GunItem animatable, double animTime) {
		super.applyMolangQueries(animatable, animTime);
		if (Minecraft.getInstance().player == null)
			return;
		GunClientState state = GunClientState.getMainHeldState();
		if (state == null)
			return;

		var player = Minecraft.getInstance().player;
		MolangParser.INSTANCE.setValue(
			AMMO,
			()
				-> GunItem
					   .getClientSideAmmo(
						   getClientPlayer(),
						   getClientPlayer().getMainHandItem(),
						   getClientPlayer().getInventory().selected)
					   .orElse(0));
		MolangParser.INSTANCE.setValue(FIREMODE, GunGeoModel::getFireModeIndex);
		MolangParser.INSTANCE.setValue(FIRETICKS, state::getTotalUninterruptedFireTime);
		MolangParser.INSTANCE.setValue(TOTALSHOTS, state::getTotalUninterruptedShots);
		MolangParser.INSTANCE.setValue(AIMING, () -> state.isAiming() ? 0 : 1);
		BiDirectionalInterpolator aimingController = (BiDirectionalInterpolator)state.getAnimationController("aiming");
		double aimprog;
		if (aimingController == null)
			aimprog = 0;
		else
			aimprog = aimingController.getValue();
		MolangParser.INSTANCE.setValue(AIMPROG, () -> aimprog);
		MolangParser.INSTANCE.setValue(HEADBOB, () -> player.bob);
		MolangParser.INSTANCE.setValue(HEADROTX, player::getXRot);
		MolangParser.INSTANCE.setValue(HEADROTY, player::getYRot);
		MolangParser.INSTANCE.setValue(CRAWLING, () -> player.isVisuallyCrawling() ? 0 : 1);
		MolangParser.INSTANCE.setValue(CROUCHING, () -> ClientEventHandler.getInstance().crouchProg.update(player.isCrouching() && !state.isAiming() ? 1.0f : 0.0f));

		// Apply Player molang queries
		MolangParser.INSTANCE.setMemoizedValue(MolangQueries.HEALTH, player::getHealth);
		MolangParser.INSTANCE.setMemoizedValue(MolangQueries.MAX_HEALTH, player::getMaxHealth);
		MolangParser.INSTANCE.setMemoizedValue(
			MolangQueries.IS_ON_FIRE, () -> RenderUtils.booleanToFloat(player.isOnFire()));
		MolangParser.INSTANCE.setMemoizedValue(MolangQueries.GROUND_SPEED, () -> {
			Vec3 velocity = player.getDeltaMovementLerped(Minecraft.getInstance().getPartialTick());

			return Mth.sqrt((float)((velocity.x * velocity.x) + (velocity.z * velocity.z)));
		});
		MolangParser.INSTANCE.setMemoizedValue(
			MolangQueries.YAW_SPEED,
			() -> player.getViewYRot((float)animTime - player.getViewYRot((float)animTime - 0.1f)));
	}

	public static int getFireModeIndex() {
		var player = getClientPlayer();
		var stack = player != null ? player.getMainHandItem() : null;
		if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
			int i = 0;
			for (FireModeInstance fireMode : GunItem.getFireModes(stack)) {
				if (GunItem.getFireModeInstance(stack) == fireMode)
					return i;
				i++;
			}
		}
		return 0;
	}

	public AnimationProcessor<GunItem> getAnimationProcessor() {
		return this.anotherAnimationProcessor;
	}

	public BakedGeoModel getBakedModel(ResourceLocation location) {
		BakedGeoModel model = GeckoLibCache.getBakedModels().get(location);
		if (model == null) {
			throw new GeckoLibException(location, "Unable to find model");
		} else {
			if (model != this.currentModel) {
				this.anotherAnimationProcessor.setActiveModel(model);
				this.currentModel = model;
			}
			mainBone = model.getBone("main").orElse(null);
			return this.currentModel;
		}
	}

	public Animation getAnimation(GunItem animatable, String name) {
		ResourceLocation location = this.getAnimationResource(animatable);
		BakedAnimations bakedAnimations = GeckoLibCache.getBakedAnimations().get(location);
		Animation bakedAnimation = null;
		if (bakedAnimations != null) {
			bakedAnimation = bakedAnimations.getAnimation(name);
		}

		if (bakedAnimation == null) {
			for (ResourceLocation animationLocation : this.fallbackAnimations) {
				BakedAnimations altAnimations = GeckoLibCache.getBakedAnimations().get(animationLocation);
				if (altAnimations != null) {
					bakedAnimation = altAnimations.getAnimation(name);
					if (bakedAnimation != null) {
						break;
					}
				}
			}
		}

		return bakedAnimation;
	}
}
