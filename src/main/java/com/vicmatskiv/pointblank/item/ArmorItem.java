package com.vicmatskiv.pointblank.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.vicmatskiv.pointblank.Nameable;
import com.vicmatskiv.pointblank.attachment.Attachment;
import com.vicmatskiv.pointblank.attachment.AttachmentCategory;
import com.vicmatskiv.pointblank.attachment.AttachmentHost;
import com.vicmatskiv.pointblank.attachment.Attachments;
import com.vicmatskiv.pointblank.client.controller.GlowAnimationController;
import com.vicmatskiv.pointblank.client.effect.AbstractEffect;
import com.vicmatskiv.pointblank.client.render.ArmorInHandRenderer;
import com.vicmatskiv.pointblank.client.render.ArmorItemRenderer;
import com.vicmatskiv.pointblank.crafting.Craftable;
import com.vicmatskiv.pointblank.feature.*;
import com.vicmatskiv.pointblank.registry.ItemRegistry;
import com.vicmatskiv.pointblank.util.JsonUtil;
import groovy.lang.Script;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ArmorItem extends net.minecraft.world.item.ArmorItem implements Equipable, Nameable, ScriptHolder, Craftable, AttachmentHost, GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final String name;
    public ResourceLocation modelResourceLocation;
    private final List<Supplier<Attachment>> compatibleAttachmentsSuppliers;
    private Collection<Attachment> compatibleAttachments;

    private final List<String> compatibleAttachmentGroups;
    private final Map<Class<? extends Feature>, Feature> features;
    private final List<Supplier<AttachmentItem>> defaultAttachments;
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
    private final List<GlowAnimationController.Builder> glowEffectBuilders;

    public ArmorItem(Builder builder, String namespace) {
        super(ArmorMaterials.LEATHER,builder.armorType,new Properties().stacksTo(1).durability(builder.durability));
        this.name = builder.name;
        if (this.name.contains(":"))
            this.modelResourceLocation = ResourceLocation.parse(this.name);
        else
            this.modelResourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, this.name);

        this.glowEffectBuilders = builder.glowEffectBuilders;
        this.defense = builder.armor;
        this.type = builder.armorType;
        this.toughness = builder.armorToughness;
        this.equipSound = builder.equipSound;
        this.knockbackResistance = builder.knockbackResistance;
        this.compatibleAttachmentGroups = builder.compatibleAttachmentGroups;
        this.defaultAttachments = builder.defaultAttachments;
        this.compatibleAttachmentsSuppliers = builder.compatibleAttachments;
        this.script = builder.script;
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

    @Override
    public void inventoryTick(ItemStack pStack, Level pLevel, Entity pEntity, int pSlotId, boolean pIsSelected) {
        if (pEntity instanceof LivingEntity entity) {
                if (pStack.getEquipmentSlot() != null && pStack.getEquipmentSlot() != EquipmentSlot.MAINHAND && pStack.getEquipmentSlot() != EquipmentSlot.OFFHAND) {
                    invokeFunction("armorTick", pStack, pLevel, entity);
                    for (ItemStack attachment : Attachments.getAttachments(pStack))
                        ((AttachmentItem)attachment.getItem()).invokeFunction("armorTick$A", pStack, pLevel, entity);
                }
            invokeFunction("inventoryTick", pStack, pLevel, entity);
            for (ItemStack attachment : Attachments.getAttachments(pStack))
                ((AttachmentItem)attachment.getItem()).invokeFunction("inventoryTick$A", pStack, pLevel, entity);
        }
    }

    @Override
    public Collection<Attachment> getCompatibleAttachments() {
        if (this.compatibleAttachments == null) {
            Set<AttachmentCategory> attachmentCategories = new HashSet<>();
            Set<Attachment> compatibleAttachments = new LinkedHashSet<>();

            for(Attachment attachment : this.getDefaultAttachments()) {
                if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
                    break;
                }

                attachmentCategories.add(attachment.getCategory());
                compatibleAttachments.add(attachment);
            }

            for(Supplier<Attachment> attachmentSupplier : this.compatibleAttachmentsSuppliers) {
                Attachment attachment = attachmentSupplier.get();
                if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
                    break;
                }

                attachmentCategories.add(attachment.getCategory());
                compatibleAttachments.add(attachment);
            }

            for(String group : this.compatibleAttachmentGroups) {
                for(Supplier<? extends Item> ga : ItemRegistry.ITEMS.getAttachmentsForGroup(group)) {
                    Item item = ga.get();
                    if (item instanceof Attachment attachment) {
                        if (attachmentCategories.size() >= this.getMaxAttachmentCategories()) {
                            break;
                        }

                        compatibleAttachments.add(attachment);
                    }
                }
            }

            this.compatibleAttachments = compatibleAttachments;
        }
        return this.compatibleAttachments;
    }

    @Override
    public long getCraftingDuration() {
        return this.craftingDuration;
    }

    @Override
    public Collection<Feature> getFeatures() {
        return this.features.values();
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
      //  controllerRegistrar.add(new AnimationController<>(this, "idle",0, (state)-> PlayState.CONTINUE));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private ArmorItemRenderer renderer = null;
            public ArmorInHandRenderer inHandRenderer = null;
            @Override
            public @NotNull ArmorItemRenderer getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if(this.inHandRenderer == null)
                    this.inHandRenderer = new ArmorInHandRenderer(ArmorItem.this.modelResourceLocation, ArmorItem.this.glowEffectBuilders);
                if (this.renderer == null)
                    this.renderer = new ArmorItemRenderer(((ArmorItem)itemStack.getItem()).modelResourceLocation, this.inHandRenderer);

                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.inHandRenderer == null) {
                    this.inHandRenderer = new ArmorInHandRenderer(ArmorItem.this.modelResourceLocation, ArmorItem.this.glowEffectBuilders);
                }
                return this.inHandRenderer;
            }
        });
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

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot pEquipmentSlot, ItemStack stack) {
        float defenseMod = DefenseFeature.getDefenseModifier(stack);
        int defenseAdd = DefenseFeature.getDefenseAdditive(stack);

        float toughnessMod = DefenseFeature.getToughnessModifier(stack);
        float toughnessAdd = DefenseFeature.getToughnessAdditive(stack);

        Multimap<Attribute, AttributeModifier> multimap = LinkedListMultimap.create(this.defaultModifiers);

        int defenseFinal = (int) ((float)this.defense * defenseMod) + defenseAdd;
        float toughnessFinal = (this.toughness * toughnessMod) + toughnessAdd;

        if(hasFunction("addArmorDefense"))
            defenseFinal += (int) invokeFunction("addArmorDefense", stack);

        if(hasFunction("mulArmorDefense"))
            defenseFinal *= (int) invokeFunction("mulArmorDefense", stack);

        if(hasFunction("addArmorToughness"))
            toughnessFinal += (int) invokeFunction("addArmorToughness", stack);

        if(hasFunction("mulArmorToughness"))
            toughnessFinal *= (int) invokeFunction("mulArmorToughness", stack);

        multimap.get(Attributes.ARMOR).add(new AttributeModifier(ARMOR_MODIFIER_UUID_PER_TYPE.get(type),"Defense", defenseFinal, AttributeModifier.Operation.ADDITION));
        multimap.get(Attributes.ARMOR_TOUGHNESS).add(new AttributeModifier(ARMOR_MODIFIER_UUID_PER_TYPE.get(type),"Toughness", toughnessFinal, AttributeModifier.Operation.ADDITION));

        return pEquipmentSlot == this.type.getSlot() ? multimap : super.getDefaultAttributeModifiers(pEquipmentSlot);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
        for(AttributeModifier modifier : this.getAttributeModifiers(pStack.getEquipmentSlot(), pStack).get(Attributes.ARMOR)) {
            pTooltipComponents.add(MutableComponent.create(Component.literal(modifier.getName()).withStyle(ChatFormatting.GRAY).append(": ").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal(String.valueOf(modifier.getAmount())).withStyle(ChatFormatting.AQUA)).getContents()));
        }
        for(AttributeModifier modifier : this.getAttributeModifiers(pStack.getEquipmentSlot(), pStack).get(Attributes.ARMOR_TOUGHNESS)) {
            pTooltipComponents.add(MutableComponent.create(Component.literal(modifier.getName()).withStyle(ChatFormatting.GRAY).append(": ").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal(String.valueOf(modifier.getAmount())).withStyle(ChatFormatting.AQUA)).getContents()));
        }
        for(AttributeModifier modifier : this.getAttributeModifiers(pStack.getEquipmentSlot(), pStack).get(Attributes.KNOCKBACK_RESISTANCE)) {
            pTooltipComponents.add(MutableComponent.create(Component.literal(modifier.getName()).withStyle(ChatFormatting.GRAY).append(": ").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal(String.valueOf(modifier.getAmount())).withStyle(ChatFormatting.AQUA)).getContents()));
        }
    }

    @Override
    public int getDefaultTooltipHideFlags(@NotNull ItemStack stack) {
        return 2;
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

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        for(Supplier<AttachmentItem> attachmentSupplier : this.defaultAttachments)
            Attachments.addAttachment(stack, attachmentSupplier.get().getDefaultInstance(), true);
        return stack;
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
        private final List<Supplier<AttachmentItem>> defaultAttachments;
        private final List<GlowAnimationController.Builder> glowEffectBuilders = new ArrayList<>();
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

        public Builder withGlow(String glowingPartName) {
            return this.withGlow(glowingPartName, null);
        }

        public Builder withGlow(String glowingPartName, String textureName) {
            return this.withGlow(Collections.singleton(GunItem.FirePhase.ANY), Collections.singleton(glowingPartName), textureName);
        }

        public Builder withGlow(Collection<GunItem.FirePhase> firePhases, String glowingPartName) {
            return this.withGlow(firePhases, Collections.singleton(glowingPartName), null);
        }

        public Builder withGlow(Collection<GunItem.FirePhase> firePhases, Collection<String> glowingPartNames, String texture) {
            GlowAnimationController.Builder builder = (new GlowAnimationController.Builder()).withFirePhases(firePhases);
            if (texture != null) {
                builder.withTexture(ResourceLocation.fromNamespaceAndPath("pointblank", texture));
            }

            builder.withGlowingPartNames(glowingPartNames);
            this.glowEffectBuilders.add(builder);
            return this;
        }

        public Builder withGlow(Collection<GunItem.FirePhase> firePhases, String glowingPartName, String texture, AbstractEffect.SpriteAnimationType spriteAnimationType, int spriteRows, int spriteColumns, int spritesPerSecond, Direction... directions) {
            GlowAnimationController.Builder builder = (new GlowAnimationController.Builder()).withFirePhases(firePhases);
            if (texture != null)
                builder.withTexture(ResourceLocation.fromNamespaceAndPath("pointblank", texture));
            builder.withGlowingPartNames(Collections.singleton(glowingPartName));
            builder.withSprites(spriteRows, spriteColumns, spritesPerSecond, spriteAnimationType);
            builder.withDirections(directions);
            this.glowEffectBuilders.add(builder);
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
            for(String compatibleAttachmentName : JsonUtil.getStrings(obj, "compatibleAttachments")) {
                Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
                if (ri != null) {
                    this.compatibleAttachments.add(() -> (Attachment)ri.get());
                }
            }
            for(String compatibleAttachmentName : JsonUtil.getStrings(obj, "defaultAttachments")) {
                Supplier<Item> ri = ItemRegistry.ITEMS.getDeferredRegisteredObject(compatibleAttachmentName);
                if (ri != null) {
                    this.defaultAttachments.add(() -> (AttachmentItem) ri.get());
                }
            }
            for(JsonObject glowingPart : JsonUtil.getJsonObjects(obj, "glowingParts")) {
                String partName = JsonUtil.getJsonString(glowingPart, "name");
                List<GunItem.FirePhase> firePhases = Collections.singletonList(GunItem.FirePhase.ANY);

                String textureName = JsonUtil.getJsonString(glowingPart, "texture", null);
                Direction direction = (Direction)JsonUtil.getEnum(glowingPart, "direction", Direction.class, null, true);
                JsonObject spritesObj = glowingPart.getAsJsonObject("sprites");
                if (spritesObj != null) {
                    int rows = JsonUtil.getJsonInt(spritesObj, "rows", 1);
                    int columns = JsonUtil.getJsonInt(spritesObj, "columns", 1);
                    int fps = JsonUtil.getJsonInt(spritesObj, "fps", 60);
                    AbstractEffect.SpriteAnimationType spriteAnimationType = (AbstractEffect.SpriteAnimationType)JsonUtil.getEnum(spritesObj, "type", AbstractEffect.SpriteAnimationType.class, AbstractEffect.SpriteAnimationType.LOOP, true);
                    if (direction != null) {
                        this.withGlow(firePhases, partName, textureName, spriteAnimationType, rows, columns, fps, direction);
                    } else {
                        this.withGlow(firePhases, partName, textureName, spriteAnimationType, rows, columns, fps);
                    }
                } else {
                    this.withGlow(firePhases, Collections.singletonList(partName), textureName);
                }
            }
            List<String> compatibleAttachmentGroups = JsonUtil.getStrings(obj, "compatibleAttachmentGroups");
            this.compatibleAttachmentGroups.addAll(compatibleAttachmentGroups);
            return this;
        }

        @Override
        public ArmorItem build() {
            return new ArmorItem(this, "pointblank");
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
