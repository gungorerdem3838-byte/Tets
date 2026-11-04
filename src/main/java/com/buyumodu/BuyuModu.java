package com.buyumodu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.function.Function;

public class BuyuModu implements ModInitializer {
    public static final String MOD_ID = "buyumodu";

    public static final Block EYE_PEDESTAL = registerBlock("eye_pedestal",
  props -> new Block(props.strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final Block PEARL_PEDESTAL = registerBlock("pearl_pedestal",
  props -> new Block(props.strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static final Block EMERALD_PEDESTAL = registerBlock("emerald_pedestal",
  props -> new Block(props.strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(Blocks.BLACKSTONE).setId(blockKey);
        Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(props));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        Item.Properties itemProps = new Item.Properties().useBlockDescriptionPrefix().setId(itemKey);
        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProps));

        return block;
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
  entries.accept(EYE_PEDESTAL);
  entries.accept(PEARL_PEDESTAL);
  entries.accept(EMERALD_PEDESTAL);
        });
    }
}
