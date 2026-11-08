package com.buyumodu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Function;

public class BuyuModu implements ModInitializer {
    public static final String MOD_ID = "buyumodu";

    public static final Block EYE_PEDESTAL = registerBlock("eye_pedestal",
            settings -> new PedestalBlock(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final Block PEARL_PEDESTAL = registerBlock("pearl_pedestal",
            settings -> new PedestalBlock(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final Block EMERALD_PEDESTAL = registerBlock("emerald_pedestal",
            settings -> new PedestalBlock(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final RegistryEntry<StatusEffect> BLEEDING_EFFECT = Registry.registerReference(
            Registries.STATUS_EFFECT,
            Identifier.of(MOD_ID, "bleeding"),
            new BleedingEffect()
    );

    public static final RegistryKey<Enchantment> KANAMA_ENCHANTMENT = RegistryKey.of(
            RegistryKeys.ENCHANTMENT,
            Identifier.of(MOD_ID, "kanama")
    );

    public static final RegistryKey<Enchantment> CAN_CALMA_ENCHANTMENT = RegistryKey.of(
            RegistryKeys.ENCHANTMENT,
            Identifier.of(MOD_ID, "can_calma")
    );

    public static final RegistryKey<Enchantment> DIKEY_COKLU_ATIS_ENCHANTMENT = RegistryKey.of(
            RegistryKeys.ENCHANTMENT,
            Identifier.of(MOD_ID, "dikey_coklu_atis")
    );

    public static final Item KANAMA_BOOK = registerItem("kanama_book",
            settings -> new Item(settings.maxCount(1).rarity(Rarity.RARE).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final Item CAN_CALMA_BOOK = registerItem("can_calma_book",
            settings -> new Item(settings.maxCount(1).rarity(Rarity.RARE).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final Item DIKEY_COKLU_ATIS_BOOK = registerItem("dikey_coklu_atis_book",
            settings -> new Item(settings.maxCount(1).rarity(Rarity.RARE).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

    public static final RegistryKey<ItemGroup> BUYU_ITEM_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(MOD_ID, "buyu_group")
    );

    private static Item registerItem(String name, Function<Item.Settings, Item> factory) {
        Identifier id = Identifier.of(MOD_ID, name);
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings settings = new Item.Settings().registryKey(itemKey);
        return Registry.register(Registries.ITEM, itemKey, factory.apply(settings));
    }

    private static Block registerBlock(String name, Function<AbstractBlock.Settings, Block> factory) {
        Identifier id = Identifier.of(MOD_ID, name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        AbstractBlock.Settings settings = AbstractBlock.Settings.copy(Blocks.BLACKSTONE).registryKey(blockKey);
        Block block = Registry.register(Registries.BLOCK, blockKey, factory.apply(settings));

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        Item.Settings itemSettings = new Item.Settings().useBlockPrefixedTranslationKey().registryKey(itemKey);
        Registry.register(Registries.ITEM, itemKey, new BlockItem(block, itemSettings));

        return block;
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM_GROUP, BUYU_ITEM_GROUP,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(KANAMA_BOOK))
                        .displayName(Text.translatable("itemGroup.buyumodu.buyu_group"))
                        .entries((context, entries) -> {
                            entries.add(EYE_PEDESTAL);
                            entries.add(PEARL_PEDESTAL);
                            entries.add(EMERALD_PEDESTAL);

                            ItemStack customBook = new ItemStack(KANAMA_BOOK);
                            context.lookup().getOptional(RegistryKeys.ENCHANTMENT)
                                    .flatMap(reg -> reg.getOptional(KANAMA_ENCHANTMENT))
                                    .ifPresent(kanamaRef -> {
                                        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                                        builder.add(kanamaRef, 1);
                                        customBook.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
                                    });
                            entries.add(customBook);

                            for (int lvl = 1; lvl <= 3; lvl++) {
                                final int level = lvl;
                                ItemStack canCalmaBook = new ItemStack(CAN_CALMA_BOOK);
                                context.lookup().getOptional(RegistryKeys.ENCHANTMENT)
                                        .flatMap(reg -> reg.getOptional(CAN_CALMA_ENCHANTMENT))
                                        .ifPresent(canCalmaRef -> {
                                            ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                                            builder.add(canCalmaRef, level);
                                            canCalmaBook.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
                                        });
                                entries.add(canCalmaBook);
                            }

                            ItemStack dikeyBook = new ItemStack(DIKEY_COKLU_ATIS_BOOK);
                            context.lookup().getOptional(RegistryKeys.ENCHANTMENT)
                                    .flatMap(reg -> reg.getOptional(DIKEY_COKLU_ATIS_ENCHANTMENT))
                                    .ifPresent(dikeyRef -> {
                                        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                                        builder.add(dikeyRef, 1);
                                        dikeyBook.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
                                    });
                            entries.add(dikeyBook);
                        })
                        .build());

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (blocked) return;
            if (source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandStack();
                if (weapon.isEmpty()) return;

                Registry<Enchantment> enchRegistry = entity.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

                // 1. Kanama Buyusu Kontrolu
                Optional<RegistryEntry.Reference<Enchantment>> bleedingOpt = enchRegistry.getOptional(KANAMA_ENCHANTMENT);
                if (bleedingOpt.isPresent() && EnchantmentHelper.getLevel(bleedingOpt.get(), weapon) > 0) {
                    int sharpnessLevel = 0;
                    Optional<RegistryEntry.Reference<Enchantment>> sharpOpt = enchRegistry.getOptional(Enchantments.SHARPNESS);
                    if (sharpOpt.isPresent()) {
                        sharpnessLevel = EnchantmentHelper.getLevel(sharpOpt.get(), weapon);
                    }

                    double chance = 0.50;
                    if (sharpnessLevel == 1) chance = 0.55;
                    else if (sharpnessLevel == 2) chance = 0.65;
                    else if (sharpnessLevel == 3) chance = 0.75;
                    else if (sharpnessLevel == 4) chance = 0.85;
                    else if (sharpnessLevel >= 5) chance = 0.95;

                    String armorMaterial = getFullArmorMaterial(entity);
                    if (armorMaterial.equals("leather")) chance -= 0.00;
                    else if (armorMaterial.equals("gold")) chance -= 0.04;
                    else if (armorMaterial.equals("iron")) chance -= 0.10;
                    else if (armorMaterial.equals("diamond")) chance -= 0.25;
                    else if (armorMaterial.equals("netherite")) chance -= 0.45;

                    if (Math.random() < chance) {
                        entity.addStatusEffect(new StatusEffectInstance(
                                BLEEDING_EFFECT,
                                100, 0, false, true, true
                        ));
                    }
                }

                // 2. Can Calma Buyusu Kontrolu
                Optional<RegistryEntry.Reference<Enchantment>> lifestealOpt = enchRegistry.getOptional(CAN_CALMA_ENCHANTMENT);
                if (lifestealOpt.isPresent()) {
                    int lifestealLevel = EnchantmentHelper.getLevel(lifestealOpt.get(), weapon);
                    if (lifestealLevel > 0 && damageTaken > 0) {
                        double chance = 0.0;
                        float stealPercent = 0.0f;

                        if (lifestealLevel == 1) {
                            chance = 0.10;
                            stealPercent = 0.30f;
                        } else if (lifestealLevel == 2) {
                            chance = 0.20;
                            stealPercent = 0.40f;
                        } else if (lifestealLevel >= 3) {
                            chance = 0.30;
                            stealPercent = 0.50f;
                        }

                        if (Math.random() < chance) {
                            float healAmount = damageTaken * stealPercent;
                            if (healAmount > 0.0f) {
                                attacker.heal(healAmount);
                            }
                        }
                    }
                }
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof ItemFrameEntity frame && frame.getCommandTags().contains("buyumodu_pedestal_item")) {
                if (!world.isClient()) {
                    BlockPos pos = frame.getAttachedBlockPos().down();
                    ItemStack held = frame.getHeldItemStack();
                    if (!held.isEmpty()) {
                        ItemStack giveBack = held.copy();
                        if (!player.getInventory().insertStack(giveBack)) {
                            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, giveBack);
                        }
                    }
                    frame.discard();
                    world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    PedestalBlock.checkRitualAround(world, pos);
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof ItemFrameEntity frame && frame.getCommandTags().contains("buyumodu_pedestal_item")) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }

    private static String getFullArmorMaterial(LivingEntity entity) {
        ItemStack boots = entity.getEquippedStack(EquipmentSlot.FEET);
        ItemStack leggings = entity.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack helmet = entity.getEquippedStack(EquipmentSlot.HEAD);

        if (boots.isEmpty() || leggings.isEmpty() || chest.isEmpty() || helmet.isEmpty()) {
            return "none";
        }

        String b = boots.getItem().toString().toLowerCase();
        String l = leggings.getItem().toString().toLowerCase();
        String c = chest.getItem().toString().toLowerCase();
        String h = helmet.getItem().toString().toLowerCase();

        if (b.contains("leather") && l.contains("leather") && c.contains("leather") && h.contains("leather")) return "leather";
        if (b.contains("golden") && l.contains("golden") && c.contains("golden") && h.contains("golden")) return "gold";
        if (b.contains("iron") && l.contains("iron") && c.contains("iron") && h.contains("iron")) return "iron";
        if (b.contains("diamond") && l.contains("diamond") && c.contains("diamond") && h.contains("diamond")) return "diamond";
        if (b.contains("netherite") && l.contains("netherite") && c.contains("netherite") && h.contains("netherite")) return "netherite";

        return "none";
    }
}
