package com.buyumodu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import java.util.function.Function;

public class BuyuModu implements ModInitializer {
    public static final String MOD_ID = "buyumodu";

    public static final Block EYE_PEDESTAL = registerBlock("eye_pedestal",
  settings -> new Block(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final Block PEARL_PEDESTAL = registerBlock("pearl_pedestal",
  settings -> new Block(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

    public static final Block EMERALD_PEDESTAL = registerBlock("emerald_pedestal",
  settings -> new Block(settings.strength(2.0f, 6.0f).requiresTool().nonOpaque()));

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
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
  entries.add(EYE_PEDESTAL);
  entries.add(PEARL_PEDESTAL);
  entries.add(EMERALD_PEDESTAL);
        });
    }
}
