package com.vicmatskiv.pointblank.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.feature.Feature;
import com.vicmatskiv.pointblank.feature.FeatureBuilder;
import com.vicmatskiv.pointblank.feature.Features;
import com.vicmatskiv.pointblank.util.DoubleBiPredicate;
import com.vicmatskiv.pointblank.util.JsonUtil;
import com.vicmatskiv.pointblank.util.Tradeable;
import groovy.lang.Script;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Supplier;

public class ArmorItem extends Item implements Equipable, Nameable, ScriptHolder, Craftable, AttachmentHost, GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String name;
    private final List<Supplier<Attachment>> compatibleAttachments;
    private final List<String> compatibleAttachmentGroups;
    private final Map<Class<? extends Feature>, Feature> features;
    private final List<Supplier<Attachment>> defaultAttachments;
    private final Script script;
    private static final EnumMap<net.minecraft.world.item.ArmorItem.Type, UUID> ARMOR_MODIFIER_UUID_PER_TYPE = Util.make(new EnumMap<>(net.minecraft.world.item.ArmorItem.Type.class), (p_266744_) -> {
        p_266744_.put(net.minecraft.world.item.ArmorItem.Type.BOOTS, UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"));
        p_266744_.put(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"));
        p_266744_.put(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"));
        p_266744_.put(net.minecraft.world.item.ArmorItem.Type.HELMET, UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"));
    });
    public static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        protected ItemStack execute(BlockSource p_40408_, ItemStack p_40409_) {
            return dispenseArmor(p_40408_, p_40409_) ? p_40409_ : super.execute(p_40408_, p_40409_);
        }
    };
    protected final net.minecraft.world.item.ArmorItem.Type type;
    private final int defense;
    private final float toughness;
    protected final float knockbackResistance;
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    private final Ingredient repairMaterial;
    private final long craftingDuration;
    private final SoundEvent equipSound;

    public ArmorItem(Builder builder, String namespace) {
        super(new Properties().stacksTo(1).durability(builder.durability));
        this.defense = builder.armor;
        this.type = builder.armorType;
        this.toughness = builder.armorToughness;
        this.equipSound = builder.equipSound;
        this.knockbackResistance = builder.knockbackResistance;
        this.compatibleAttachmentGroups = builder.compatibleAttachmentGroups;
        this.defaultAttachments = builder.defaultAttachments;
        this.compatibleAttachments = builder.compatibleAttachments;
        this.script = builder.script;
        this.name = builder.name;
        this.craftingDuration = builder.craftingDuration;
        this.repairMaterial = Ingredient.of(builder.repairItems.stream());
        Map<Class<? extends Feature>, Feature> features = new HashMap<>();
        for(FeatureBuilder<?, ?> featureBuilder : builder.featureBuilders) {
            Feature feature = featureBuilder.build(this);
            features.put(feature.getClass(), feature);
        }
        this.features = Collections.unmodifiableMap(features);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> attrbuilder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIER_UUID_PER_TYPE.get(builder.armorType);
        attrbuilder.put(Attributes.ARMOR, new AttributeModifier(uuid, "Armor modifier", this.defense, AttributeModifier.Operation.ADDITION));
        attrbuilder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Armor toughness", this.toughness, AttributeModifier.Operation.ADDITION));
        if (this.knockbackResistance > 0.0F) {
            attrbuilder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, AttributeModifier.Operation.ADDITION));
        }

        this.defaultModifiers = attrbuilder.build();
    }

    public static boolean dispenseArmor(BlockSource pSource, ItemStack pStack) {
        BlockPos blockpos = pSource.getPos().relative(pSource.getBlockState().getValue(DispenserBlock.FACING));
        List<LivingEntity> list = pSource.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(blockpos), EntitySelector.NO_SPECTATORS.and(new EntitySelector.MobCanWearArmorEntitySelector(pStack)));
        if (list.isEmpty()) {
            return false;
        } else {
            LivingEntity livingentity = list.get(0);
            EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(pStack);
            ItemStack itemstack = pStack.split(1);
            livingentity.setItemSlot(equipmentslot, itemstack);
            if (livingentity instanceof Mob) {
                ((Mob)livingentity).setDropChance(equipmentslot, 2.0F);
                ((Mob)livingentity).setPersistenceRequired();
            }

            return true;
        }
    }

    @Override
    public Collection<Attachment> getCompatibleAttachments() {
        return List.of();
    }

    @Override
    public long getCraftingDuration() {
        return this.craftingDuration;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return List.of();
    }

    @Override
    public @Nullable Script getScript() {
        return this.script;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    public net.minecraft.world.item.ArmorItem.Type getType() {
        return this.type;
    }

    public int getEnchantmentValue() {
        return 0;
    }

    public boolean isValidRepairItem(ItemStack pToRepair, ItemStack pRepair) {
        return this.repairMaterial.test(pRepair) || super.isValidRepairItem(pToRepair, pRepair);
    }

    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        return this.swapWithEquipmentSlot(this, pLevel, pPlayer, pHand);
    }

    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot pEquipmentSlot) {
        return pEquipmentSlot == this.type.getSlot() ? this.defaultModifiers : super.getDefaultAttributeModifiers(pEquipmentSlot);
    }

    public int getDefense() {
        return this.defense;
    }

    public float getToughness() {
        return this.toughness;
    }

    public EquipmentSlot getEquipmentSlot() {
        return this.type.getSlot();
    }

    public SoundEvent getEquipSound() {
        return this.equipSound;
    }

    public static class Builder extends ItemBuilder<Builder> {
        private long craftingDuration = 500;
        private int armor;
        private float armorToughness;
        private float knockbackResistance;
        private int durability;
        private String name;
        private final SoundEvent equipSound = SoundEvents.ARMOR_EQUIP_GENERIC;
        private net.minecraft.world.item.ArmorItem.Type armorType;
        private final List<ItemStack> repairItems;
        private final List<Supplier<Attachment>> compatibleAttachments;
        private final List<String> compatibleAttachmentGroups;
        private final List<FeatureBuilder<?, ?>> featureBuilders;
        private final List<Supplier<Attachment>> defaultAttachments;
        private Script script;

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withArmorToughness(float toughness) {
            this.armorToughness = toughness;
            return this;
        }

        public Builder withArmor(int armor) {
            this.armor = armor;
            return this;
        }

        public Builder withEquipSound(SoundEvent soundEvent) {
            this.armor = armor;
            return this;
        }

        public Builder withType(net.minecraft.world.item.ArmorItem.Type type) {
            this.armorType = type;
            return this;
        }

        public Builder withScript(Script script) {
            this.script = script;
            return this;
        }

        public Builder withKnockbackResistance(float knockbackResistance) {
            this.knockbackResistance = knockbackResistance;
            return this;
        }

        private Builder withCraftingDuration(long craftingDuration) {
            this.craftingDuration = craftingDuration;
            return this;
        }

        private Builder withFeature(FeatureBuilder<?, ?> featureBuilder) {
            this.featureBuilders.add(featureBuilder);
            return this;
        }

        private Builder withDurability(int durability) {
            this.durability = durability;
            return this;
        }
        public Builder() {
            this.compatibleAttachments = new ArrayList<>();
            this.compatibleAttachmentGroups = new ArrayList<>();
            this.featureBuilders = new ArrayList<>();
            this.defaultAttachments = new ArrayList<>();
            this.repairItems = new ArrayList<>();
        }

        @Override
        public Builder withJsonObject(JsonObject obj) {
            this.withName(JsonUtil.getJsonString(obj, "name"))
                .withScript(JsonUtil.getJsonScript(obj))
                .withCraftingDuration(JsonUtil.getJsonInt(obj, "craftingDuration", 500))
                .withArmor(JsonUtil.getJsonInt(obj, "defense", 0))
                .withDurability(JsonUtil.getJsonInt(obj, "durability", 128))
                .withArmorToughness(JsonUtil.getJsonFloat(obj, "toughness", 0))
                .withType((net.minecraft.world.item.ArmorItem.Type) JsonUtil.getEnum(obj, "armorType", net.minecraft.world.item.ArmorItem.Type.class, net.minecraft.world.item.ArmorItem.Type.HELMET, true));

            for(JsonObject featureObj : JsonUtil.getJsonObjects(obj, "features")) {
                FeatureBuilder<?, ?> featureBuilder = Features.fromJson(featureObj);
                this.withFeature(featureBuilder);
            }

            return this;
        }

        @Override
        public Item build() {
            return new ArmorItem(this, "pointblank");
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
