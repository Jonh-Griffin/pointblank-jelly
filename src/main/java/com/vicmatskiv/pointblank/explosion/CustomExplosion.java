package com.vicmatskiv.pointblank.explosion;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.vicmatskiv.pointblank.Config;
import com.vicmatskiv.pointblank.client.effect.Effect;
import com.vicmatskiv.pointblank.client.effect.EffectBuilder;
import com.vicmatskiv.pointblank.item.ExplosionDescriptor;
import com.vicmatskiv.pointblank.item.ExplosionProvider;
import com.vicmatskiv.pointblank.registry.SoundRegistry;
import com.vicmatskiv.pointblank.util.MiscUtil;
import com.vicmatskiv.pointblank.util.SimpleHitResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.GameRules.Key;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import software.bernie.geckolib.util.ClientUtils;

public class CustomExplosion extends Explosion {
   private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
   private static final float DEFAULT_SOUND_VOLUME = 4.0F;
   private Item item;
   private Level level;
   private float radius;
   private double x;
   private double y;
   private double z;
   private boolean fire;
   private final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList();
   private BlockInteraction blockInteraction;
   private Random random;
   private Entity source;
   private ExplosionDamageCalculator damageCalculator;
   private final Map<Player, Vec3> hitPlayers = Maps.newHashMap();

   public CustomExplosion(Level level, Item item, Entity entity, DamageSource damageSource, ExplosionDamageCalculator calc, double posX, double posY, double posZ, float power, boolean fire, BlockInteraction blockInteraction) {
      super(level, entity, damageSource, calc, posX, posY, posZ, power, fire, blockInteraction);
      this.level = level;
      this.item = item;
      this.source = entity;
      this.x = posX;
      this.y = posY;
      this.z = posZ;
      this.radius = power;
      this.fire = fire;
      this.blockInteraction = blockInteraction;
      this.random = new Random();
      this.damageCalculator = calc == null ? this.m_46062_(entity) : calc;
   }

   public CustomExplosion(Level level, Item gunItem, Entity entity, double x, double y, double z, float power, List<BlockPos> toBlow) {
      super(level, entity, x, y, z, power, toBlow);
      this.level = level;
      this.item = gunItem;
      this.source = entity;
      this.x = x;
      this.y = y;
      this.z = z;
      this.radius = power;
      this.toBlow.addAll(toBlow);
   }

   private ExplosionDamageCalculator m_46062_(@Nullable Entity entity) {
      return (ExplosionDamageCalculator)(entity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(entity));
   }

   public Item getItem() {
      return this.item;
   }

   @OnlyIn(Dist.CLIENT)
   public void finalizeClientExplosion() {
      Item var2 = this.item;
      if (var2 instanceof ExplosionProvider) {
         ExplosionProvider explosionProvider = (ExplosionProvider)var2;
         ExplosionDescriptor explosionDescriptor = explosionProvider.getExplosion();
         MinecraftForge.EVENT_BUS.post(new ExplosionEvent(new Vec3(this.x, this.y, this.z), explosionDescriptor));
         SoundEvent soundEvent = null;
         float soundVolume = 4.0F;
         if (explosionDescriptor != null) {
            if (explosionDescriptor.soundName() != null) {
               soundEvent = SoundRegistry.getSoundEvent(explosionDescriptor.soundName());
            }

            soundVolume = explosionDescriptor.soundVolume();
            this.applyExplosionEffects(explosionDescriptor);
         }

         if (soundEvent == null) {
            soundEvent = SoundEvents.f_11913_;
         }

         if (!MiscUtil.isNearlyZero((double)soundVolume)) {
            this.playSound(soundEvent, soundVolume);
         }
      }

      this.m_46075_(false);
   }

   private void playSound(SoundEvent soundEvent, float volume) {
      this.level.m_7785_(this.x, this.y, this.z, soundEvent, SoundSource.BLOCKS, volume, (1.0F + (this.level.f_46441_.m_188501_() - this.level.f_46441_.m_188501_()) * 0.2F) * 0.7F, false);
   }

   private void applyExplosionEffects(ExplosionDescriptor explosionDescriptor) {
      List<Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>>> effects = explosionDescriptor.effects();
      Iterator var3 = effects.iterator();

      while(var3.hasNext()) {
         Supplier<EffectBuilder<? extends EffectBuilder<?, ?>, ?>> effectBuilderSupplier = (Supplier)var3.next();
         EffectBuilder<? extends EffectBuilder<?, ?>, ?> effectBuilder = (EffectBuilder)effectBuilderSupplier.get();
         EffectBuilder.Context context = (new EffectBuilder.Context()).withHitResult(new SimpleHitResult(this.getPosition(), Type.BLOCK, Direction.UP, -1));
         Effect effect = effectBuilder.build(context);
         effect.launch(ClientUtils.getClientPlayer());
      }

   }

   public void m_46075_(boolean ignored) {
      boolean flag = this.m_254884_();
      if (flag) {
         ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList();
         boolean flag1 = this.m_252906_() instanceof Player;
         Util.m_214673_(this.toBlow, this.level.f_46441_);
         ObjectListIterator var5 = this.toBlow.iterator();

         while(var5.hasNext()) {
            BlockPos blockpos = (BlockPos)var5.next();
            BlockState blockstate = this.level.m_8055_(blockpos);
            if (!blockstate.m_60795_()) {
               BlockPos blockpos1 = blockpos.m_7949_();
               this.level.m_46473_().m_6180_("explosion_blocks");
               if (blockstate.canDropFromExplosion(this.level, blockpos, this)) {
                  Level $$9 = this.level;
                  if ($$9 instanceof ServerLevel) {
                     ServerLevel serverlevel = (ServerLevel)$$9;
                     BlockEntity blockentity = blockstate.m_155947_() ? this.level.m_7702_(blockpos) : null;
                     Builder lootparams$builder = (new Builder(serverlevel)).m_287286_(LootContextParams.f_81460_, Vec3.m_82512_(blockpos)).m_287286_(LootContextParams.f_81463_, ItemStack.f_41583_).m_287289_(LootContextParams.f_81462_, blockentity).m_287289_(LootContextParams.f_81455_, this.source);
                     if (this.blockInteraction == BlockInteraction.DESTROY_WITH_DECAY) {
                        lootparams$builder.m_287286_(LootContextParams.f_81464_, this.radius);
                     }

                     blockstate.m_222967_(serverlevel, blockpos, ItemStack.f_41583_, flag1);
                     blockstate.m_287290_(lootparams$builder).forEach((p_46074_) -> {
                        m_46067_(objectarraylist, p_46074_, blockpos1);
                     });
                  }
               }

               blockstate.onBlockExploded(this.level, blockpos, this);
               this.level.m_46473_().m_7238_();
            }
         }

         var5 = objectarraylist.iterator();

         while(var5.hasNext()) {
            Pair<ItemStack, BlockPos> pair = (Pair)var5.next();
            Block.m_49840_(this.level, (BlockPos)pair.getSecond(), (ItemStack)pair.getFirst());
         }
      }

      if (this.fire) {
         ObjectListIterator var13 = this.toBlow.iterator();

         while(var13.hasNext()) {
            BlockPos blockpos2 = (BlockPos)var13.next();
            if (this.random.nextInt(3) == 0 && this.level.m_8055_(blockpos2).m_60795_() && this.level.m_8055_(blockpos2.m_7495_()).m_60804_(this.level, blockpos2.m_7495_())) {
               this.level.m_46597_(blockpos2, BaseFireBlock.m_49245_(this.level, blockpos2));
            }
         }
      }

   }

   private static void m_46067_(ObjectArrayList<Pair<ItemStack, BlockPos>> blockPosToBlow, ItemStack itemStack, BlockPos blockPos) {
      int i = blockPosToBlow.size();

      for(int j = 0; j < i; ++j) {
         Pair<ItemStack, BlockPos> pair = (Pair)blockPosToBlow.get(j);
         ItemStack itemstack = (ItemStack)pair.getFirst();
         if (ItemEntity.m_32026_(itemstack, itemStack)) {
            ItemStack itemstack1 = ItemEntity.m_32029_(itemstack, itemStack, 16);
            blockPosToBlow.set(j, Pair.of(itemstack1, (BlockPos)pair.getSecond()));
            if (itemStack.m_41619_()) {
               return;
            }
         }
      }

      blockPosToBlow.add(Pair.of(itemStack, blockPos));
   }

   private static BlockInteraction getDestroyType(Level level, Key<BooleanValue> gameRulesKey) {
      return level.m_46469_().m_46207_(gameRulesKey) ? BlockInteraction.DESTROY_WITH_DECAY : BlockInteraction.DESTROY;
   }

   public static CustomExplosion explode(Level level, Item item, @Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator calc, double posX, double posY, double posZ, float power, boolean fire, ExplosionInteraction interaction, boolean particlesEnabled) {
      if (!Config.explosionDestroyBlocksEnabled) {
         interaction = ExplosionInteraction.NONE;
      }

      BlockInteraction blockInteraction;
      switch(interaction) {
      case NONE:
         blockInteraction = BlockInteraction.KEEP;
         break;
      case BLOCK:
         blockInteraction = getDestroyType(level, GameRules.f_254629_);
         break;
      case TNT:
         blockInteraction = getDestroyType(level, GameRules.f_254705_);
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      CustomExplosion explosion = new CustomExplosion(level, item, entity, damageSource, calc, posX, posY, posZ, power, fire, blockInteraction);
      explosion.m_46061_();
      explosion.m_46075_(false);
      return explosion;
   }

   public void m_46061_() {
      this.level.m_220400_(this.source, GameEvent.f_157812_, new Vec3(this.x, this.y, this.z));
      Set<BlockPos> set = Sets.newHashSet();

      int k;
      int l;
      for(int j = 0; j < 16; ++j) {
         for(k = 0; k < 16; ++k) {
            for(l = 0; l < 16; ++l) {
               if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                  double d0 = (double)((float)j / 15.0F * 2.0F - 1.0F);
                  double d1 = (double)((float)k / 15.0F * 2.0F - 1.0F);
                  double d2 = (double)((float)l / 15.0F * 2.0F - 1.0F);
                  double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                  d0 /= d3;
                  d1 /= d3;
                  d2 /= d3;
                  float f = this.radius * (0.7F + this.level.f_46441_.m_188501_() * 0.6F);
                  double d4 = this.x;
                  double d6 = this.y;
                  double d8 = this.z;

                  for(float var20 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                     BlockPos blockpos = BlockPos.m_274561_(d4, d6, d8);
                     BlockState blockstate = this.level.m_8055_(blockpos);
                     FluidState fluidstate = this.level.m_6425_(blockpos);
                     if (!this.level.m_46739_(blockpos)) {
                        break;
                     }

                     Optional<Float> optional = this.damageCalculator.m_6617_(this, this.level, blockpos, blockstate, fluidstate);
                     if (optional.isPresent()) {
                        f -= ((Float)optional.get() + 0.3F) * 0.3F;
                     }

                     if (f > 0.0F && this.damageCalculator.m_6714_(this, this.level, blockpos, blockstate, f)) {
                        set.add(blockpos);
                     }

                     d4 += d0 * 0.30000001192092896D;
                     d6 += d1 * 0.30000001192092896D;
                     d8 += d2 * 0.30000001192092896D;
                  }
               }
            }
         }
      }

      this.toBlow.addAll(set);
      float adjustedRadius = this.radius * 2.0F;
      k = Mth.m_14107_(this.x - (double)adjustedRadius - 1.0D);
      l = Mth.m_14107_(this.x + (double)adjustedRadius + 1.0D);
      int bbYMin = Mth.m_14107_(this.y - (double)adjustedRadius - 1.0D);
      int bbYMax = Mth.m_14107_(this.y + (double)adjustedRadius + 1.0D);
      int bbZMin = Mth.m_14107_(this.z - (double)adjustedRadius - 1.0D);
      int bbZMax = Mth.m_14107_(this.z + (double)adjustedRadius + 1.0D);
      List<Entity> list = this.level.m_45933_(this.source, new AABB((double)k, (double)bbYMin, (double)bbZMin, (double)l, (double)bbYMax, (double)bbZMax));
      ForgeEventFactory.onExplosionDetonate(this.level, this, list, (double)adjustedRadius);
      Vec3 thisPos = new Vec3(this.x, this.y, this.z);

      for(int k2 = 0; k2 < list.size(); ++k2) {
         Entity entity = (Entity)list.get(k2);
         if (!entity.m_6128_() && !MiscUtil.isProtected(entity)) {
            double normalizedDistanceToEntity = Math.sqrt(entity.m_20238_(thisPos)) / (double)adjustedRadius;
            if (normalizedDistanceToEntity <= 1.0D) {
               double xOffset = entity.m_20185_() - this.x;
               double yOffset = (entity instanceof PrimedTnt ? entity.m_20186_() : entity.m_20188_()) - this.y;
               double zOffset = entity.m_20189_() - this.z;
               double adjustedDistanceToEntity = Math.sqrt(xOffset * xOffset + yOffset * yOffset + zOffset * zOffset);
               if (adjustedDistanceToEntity != 0.0D) {
                  xOffset /= adjustedDistanceToEntity;
                  yOffset /= adjustedDistanceToEntity;
                  zOffset /= adjustedDistanceToEntity;
                  double seenPercent = (double)m_46064_(thisPos, entity);
                  double damage = (1.0D - normalizedDistanceToEntity) * seenPercent;
                  entity.m_6469_(this.m_46077_(), (float)((int)((damage * damage + damage) / 2.0D * 7.5D * (double)adjustedRadius + 1.0D)));
                  double d11;
                  if (entity instanceof LivingEntity) {
                     LivingEntity livingentity = (LivingEntity)entity;
                     d11 = ProtectionEnchantment.m_45135_(livingentity, damage);
                  } else {
                     d11 = damage;
                  }

                  xOffset *= d11;
                  yOffset *= d11;
                  zOffset *= d11;
                  Vec3 knockbackMovement = new Vec3(xOffset, yOffset, zOffset);
                  entity.m_20256_(entity.m_20184_().m_82549_(knockbackMovement));
                  if (entity instanceof Player) {
                     Player player = (Player)entity;
                     if (!player.m_5833_() && (!player.m_7500_() || !player.m_150110_().f_35935_)) {
                        this.hitPlayers.put(player, knockbackMovement);
                     }
                  }
               }
            }
         }
      }

   }
}
