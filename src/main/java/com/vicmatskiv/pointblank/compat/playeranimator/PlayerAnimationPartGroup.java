package com.vicmatskiv.pointblank.compat.playeranimator;

import com.vicmatskiv.pointblank.item.GunItem;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier;
import dev.kosmx.playerAnim.api.layered.modifier.AdjustmentModifier.PartModifier;
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
   ARMS("arms", (p) -> {
      return new ModifierLayer();
   }),
   LEGS("legs", (p) -> {
      return new ModifierLayer();
   }),
   TORSO("torso", (p) -> {
      return new ModifierLayer();
   }),
   HEAD("head", (p) -> {
      return new ModifierLayer();
   }),
   BODY("body", (p) -> {
      return new ModifierLayer();
   }),
   AUX("aux", (p) -> {
      return new ModifierLayer((IAnimation)null, new AbstractModifier[]{new AdjustmentModifier((bodyPart) -> {
         return correctRotation(p, bodyPart);
      })});
   });

   private final String name;
   private final ResourceLocation resourceLocation;
   private final PlayerAnimationFactory playerAnimationFactory;
   private static final float MAX_BODY_HEAD_YAW = 75.0F;
   private static final Set<String> ROTATED_PARTS = Set.of("head", "leftArm", "rightArm");

   private PlayerAnimationPartGroup(String name, PlayerAnimationFactory playerAnimationFactory) {
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

   private static Optional<PartModifier> correctRotation(Player player, String partName) {
      Minecraft mc = Minecraft.m_91087_();
      Player mainPlayer = ClientUtils.getClientPlayer();
      return (player != mainPlayer || mc.f_91066_.m_92176_() != CameraType.FIRST_PERSON) && mc.f_91080_ == null && ROTATED_PARTS.contains(partName) && mainPlayer.m_21205_().m_41720_() instanceof GunItem ? Optional.of(new PartModifier(new Vec3f(player.m_5686_(mc.getPartialTick()) * 0.017453292F, Mth.m_14036_(Mth.m_14177_(Mth.m_14189_(mc.getPartialTick(), player.f_20886_ - player.f_20884_, player.f_20885_ - player.f_20883_)), -75.0F, 75.0F) * 0.017453292F, 0.0F), Vec3f.ZERO)) : Optional.empty();
   }

   // $FF: synthetic method
   private static PlayerAnimationPartGroup[] $values() {
      return new PlayerAnimationPartGroup[]{ARMS, LEGS, TORSO, HEAD, BODY, AUX};
   }
}
