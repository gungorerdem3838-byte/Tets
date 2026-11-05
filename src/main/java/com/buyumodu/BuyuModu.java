package com.buyumodu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;

public class BuyuModu implements ModInitializer {
    public static final String MOD_ID = "buyumodu";

    public static final Block EYE_PEDESTAL = registerBlock("eye_pedestal",
            settings -> new PedestalBlock(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final Block PEARL_PEDESTAL = registerBlock("pearl_pedestal",
            settings -> new PedestalBlock(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final Block EMERALD_PEDESTAL = registerBlock("emerald_pedestal",
            settings -> new PedestalBlock(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final RegistryKey<ItemGroup> BUYU_ITEM_GROUP = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            Identifier.of(MOD_ID, "buyu_group")
    );

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
                        .icon(() -> new ItemStack(EMERALD_PEDESTAL))
                        .displayName(Text.translatable("itemGroup.buyumodu.buyu_group"))
                        .entries((context, entries) -> {
                            entries.add(EYE_PEDESTAL);
                            entries.add(PEARL_PEDESTAL);
                            entries.add(EMERALD_PEDESTAL);
                        })
                        .build());

        // Oyuncu doğrudan üstteki eşya çerçevesine sağ tıklarsa eşyayı geri ver (döndürmesini engelle)
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
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        // Oyuncu üstteki eşyaya sol tıklarsa çerçeveyi kırmasını engelle
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity instanceof ItemFrameEntity frame && frame.getCommandTags().contains("buyumodu_pedestal_item")) {
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });
    }
}
