//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.vicmatskiv.pointblank.compat.playeranimator;

import com.vicmatskiv.pointblank.item.GunItem;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.core.util.Vec3f;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib.util.ClientUtils;

public enum PlayerAnimationPartGroup {
   ARMS("arms", (p) -> new ModifierLayer<>()),
   LEGS("legs", (p) -> new ModifierLayer<>()),
   TORSO("torso", (p) -> new ModifierLayer<>()),
   HEAD("head", (p) -> new ModifierLayer<>()),
   BODY("body", (p) -> new ModifierLayer<>()),
   AUX("aux", (p) -> new ModifierLayer<>(null, new AdjustmentModifier((bodyPart) -> correctRotation(p, bodyPart))));

   private final String name;
   private final ResourceLocation resourceLocation;
   private final PlayerAnimationFactory playerAnimationFactory;
   private static final float MAX_BODY_HEAD_YAW = 75.0F;
   private static final Set<String> ROTATED_PARTS = Set.of("head", "leftArm", "rightArm");

   PlayerAnimationPartGroup(String name, PlayerAnimationFactory playerAnimationFactory) {
      this.name = name;
      this.playerAnimationFactory = playerAnimationFactory;
      this.resourceLocation = new ResourceLocation("pointblank", this.name().toLowerCase(Locale.ROOT));
   }

   String getGroupName() {
      return this.name;
   }

   ResourceLocation getLayerResource() {
      return this.resourceLocation;
   }

   public static PlayerAnimationPartGroup fromName(String name) {
      return valueOf(name.toUpperCase(Locale.ROOT));
   }

   public PlayerAnimationFactory getAnimationFactory() {
      return this.playerAnimationFactory;
   }

   private static Optional<AdjustmentModifier.PartModifier> correctRotation(Player player, String partName) {
      Minecraft mc = Minecraft.getInstance();
      Player mainPlayer = ClientUtils.getClientPlayer();
      return (player != mainPlayer || mc.options.getCameraType() != CameraType.FIRST_PERSON) && mc.screen == null && ROTATED_PARTS.contains(partName) && mainPlayer.getMainHandItem().getItem() instanceof GunItem ? Optional.of(new AdjustmentModifier.PartModifier(new Vec3f(player.getViewXRot(mc.getPartialTick()) * ((float)Math.PI / 180F), Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(mc.getPartialTick(), player.yHeadRotO - player.yBodyRotO, player.yHeadRot - player.yBodyRot)), -75.0F, 75.0F) * ((float)Math.PI / 180F), 0.0F), Vec3f.ZERO)) : Optional.empty();
   }
}
