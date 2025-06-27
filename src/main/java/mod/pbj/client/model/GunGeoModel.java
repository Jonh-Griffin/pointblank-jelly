package mod.pbj.client.model;

import mod.pbj.client.controller.BlendingAnimationProcessor;
import mod.pbj.item.FireModeInstance;
import mod.pbj.item.GunItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.GeckoLibException;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.molang.MolangParser;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.model.DefaultedItemGeoModel;

import java.util.ArrayList;
import java.util.List;
import static mod.pbj.Constants.*;

import static mod.pbj.util.ClientUtil.getClientPlayer;

public class GunGeoModel extends DefaultedItemGeoModel<GunItem> {
   private BakedGeoModel currentModel = null;
   private final List<ResourceLocation> fallbackAnimations = new ArrayList<>();
   private final BlendingAnimationProcessor<GunItem> anotherAnimationProcessor = new BlendingAnimationProcessor<>(this);

   public GunGeoModel(ResourceLocation assetSubpath, List<ResourceLocation> fallbackAnimations) {
      super(assetSubpath);

       for (ResourceLocation fallbackAnimation : fallbackAnimations) {
           this.fallbackAnimations.add(this.buildFormattedAnimationPath(fallbackAnimation));
       }

   }

    @Override
    public void applyMolangQueries(GunItem animatable, double animTime) {
        super.applyMolangQueries(animatable, animTime);
        MolangParser.INSTANCE.setValue(AMMO, () -> GunItem.getClientSideAmmo(getClientPlayer(), getClientPlayer().getMainHandItem(), getClientPlayer().getInventory().selected).orElse(0));
        MolangParser.INSTANCE.setValue(FIREMODE, GunGeoModel::getFireModeIndex);
    }

    public static int getFireModeIndex() {
        var player = getClientPlayer();
        var stack = player != null ? player.getMainHandItem() : null;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            int i = 0;
            for(FireModeInstance fireMode : GunItem.getFireModes(stack)) {
                if(GunItem.getFireModeInstance(stack) == fireMode) return i;
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
