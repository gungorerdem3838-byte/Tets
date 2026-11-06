package com.buyumodu;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class PedestalBlock extends Block {
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 6.0, 11.0),
            Block.createCuboidShape(6.0, 6.0, 6.0, 10.0, 12.0, 10.0),
            Block.createCuboidShape(3.6, 12.0, 3.6, 12.4, 16.0, 12.4)
    );

    // Sira: 0: Kuzey (0, -4), 1: Bati (-4, 0), 2: Dogu (4, 0), 3: Guney (0, 4)
    // Karsilikli Ciftler: (0 ve 3) ile (1 ve 2)
    private static final int[][] EMERALD_OFFSETS = {{0, -4}, {-4, 0}, {4, 0}, {0, 4}};

    // Sira: 0: KB (-3, -3), 1: KD (3, -3), 2: GB (-3, 3), 3: GD (3, 3)
    // Capraz Karsilikli Ciftler: (0 ve 3) ile (1 ve 2)
    private static final int[][] PEARL_OFFSETS = {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}};

    public PedestalBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    public static List<ItemFrameEntity> getPedestalFrames(World world, BlockPos pos) {
        BlockPos above = pos.up();
        return world.getEntitiesByClass(
                ItemFrameEntity.class,
                new Box(above).expand(0.2),
                frame -> frame.getAttachedBlockPos().equals(above) && frame.getCommandTags().contains("buyumodu_pedestal_item")
        );
    }

    public static ItemStack getDisplayedItem(World world, BlockPos pos) {
        List<ItemFrameEntity> frames = getPedestalFrames(world, pos);
        for (ItemFrameEntity frame : frames) {
            ItemStack stack = frame.getHeldItemStack();
            if (!stack.isEmpty()) return stack;
        }
        return ItemStack.EMPTY;
    }

    public static void clearPedestalItem(World world, BlockPos pos) {
        List<ItemFrameEntity> frames = getPedestalFrames(world, pos);
        for (ItemFrameEntity frame : frames) {
            frame.discard();
        }
    }

    public static void setPedestalItem(World world, BlockPos pos, ItemStack stack) {
        clearPedestalItem(world, pos);
        if (!stack.isEmpty()) {
            BlockPos above = pos.up();
            ItemFrameEntity frame = new ItemFrameEntity(world, above, Direction.UP);
            frame.setHeldItemStack(stack.copyWithCount(1), false);
            frame.setInvisible(true);
            frame.setInvulnerable(true);
            frame.setSilent(true);
            frame.addCommandTag("buyumodu_pedestal_item");
            world.spawnEntity(frame);
        }
    }

    public static void checkRitualAround(World world, BlockPos changedPos) {
        if (world.isClient()) return;

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos centerPos = changedPos.add(dx, 0, dz);
                if (world.getBlockState(centerPos).isOf(BuyuModu.EYE_PEDESTAL)) {
                    tryPerformRitual(world, centerPos);
                }
            }
        }
    }

    public static boolean isCombatWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)) return true;
        String id = stack.getItem().toString().toLowerCase();
        return id.contains("sword") || id.contains("axe") || id.contains("trident") || id.contains("mace");
    }

    private static void tryPerformRitual(World world, BlockPos centerPos) {
        ItemStack centerItem = getDisplayedItem(world, centerPos);
        if (!isCombatWeapon(centerItem)) return;

        // Sütun bloklarının yerinde olup olmadığını doğrula ve üstlerindeki eşyaları al
        Item[] emeraldItems = new Item[4];
        for (int i = 0; i < 4; i++) {
            BlockPos p = centerPos.add(EMERALD_OFFSETS[i][0], 0, EMERALD_OFFSETS[i][1]);
            if (!world.getBlockState(p).isOf(BuyuModu.EMERALD_PEDESTAL)) return;
            emeraldItems[i] = getDisplayedItem(world, p).getItem();
        }

        Item[] pearlItems = new Item[4];
        for (int i = 0; i < 4; i++) {
            BlockPos p = centerPos.add(PEARL_OFFSETS[i][0], 0, PEARL_OFFSETS[i][1]);
            if (!world.getBlockState(p).isOf(BuyuModu.PEARL_PEDESTAL)) return;
            pearlItems[i] = getDisplayedItem(world, p).getItem();
        }

        Registry<Enchantment> enchRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> bleedingOpt = enchRegistry.getOptional(BuyuModu.KANAMA_ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> lifestealOpt = enchRegistry.getOptional(BuyuModu.CAN_CALMA_ENCHANTMENT);

        // 1. KANAMA RITUELI: Zumrut sutunlarda Lapis, Inci sutunlarda Demir Kulcesi
        if (bleedingOpt.isPresent() && EnchantmentHelper.getLevel(bleedingOpt.get(), centerItem) == 0) {
            boolean allLapis = true;
            boolean allIron = true;
            for (int i = 0; i < 4; i++) {
                if (emeraldItems[i] != Items.LAPIS_LAZULI) allLapis = false;
                if (pearlItems[i] != Items.IRON_INGOT) allIron = false;
            }
            if (allLapis && allIron) {
                completeRitual(world, centerPos, centerItem, bleedingOpt.get(), 1);
                return;
            }
        }

        // 2. CAN CALMA RITUELLERI (Seviye 1, 2 ve 3)
        if (lifestealOpt.isPresent()) {
            int currentLvl = EnchantmentHelper.getLevel(lifestealOpt.get(), centerItem);

            if (currentLvl == 0) {
                // Seviye 1: Zumrutlerin hepsi Lapis, Incilerin hepsi Fermante Orumcek Gozu
                boolean allLapis = true;
                boolean allSpiderEye = true;
                for (int i = 0; i < 4; i++) {
                    if (emeraldItems[i] != Items.LAPIS_LAZULI) allLapis = false;
                    if (pearlItems[i] != Items.FERMENTED_SPIDER_EYE) allSpiderEye = false;
                }
                if (allLapis && allSpiderEye) {
                    completeRitual(world, centerPos, centerItem, lifestealOpt.get(), 1);
                    return;
                }
            } else if (currentLvl == 1) {
                // Seviye 2: Karsilikli iki Zumrutte Lapis, diger karsilikli iki Zumrutte Lapis Blogu
                // Incilerin 1'inde Blaze Cubugu, diger 3'unde Curuk Et
                boolean emeraldOppositeMatch =
                        (emeraldItems[0] == Items.LAPIS_LAZULI && emeraldItems[3] == Items.LAPIS_LAZULI &&
                         emeraldItems[1] == Items.LAPIS_BLOCK && emeraldItems[2] == Items.LAPIS_BLOCK)
                        ||
                        (emeraldItems[0] == Items.LAPIS_BLOCK && emeraldItems[3] == Items.LAPIS_BLOCK &&
                         emeraldItems[1] == Items.LAPIS_LAZULI && emeraldItems[2] == Items.LAPIS_LAZULI);

                int blazeCount = 0;
                int fleshCount = 0;
                for (int i = 0; i < 4; i++) {
                    if (pearlItems[i] == Items.BLAZE_ROD) blazeCount++;
                    else if (pearlItems[i] == Items.ROTTEN_FLESH) fleshCount++;
                }

                if (emeraldOppositeMatch && blazeCount == 1 && fleshCount == 3) {
                    completeRitual(world, centerPos, centerItem, lifestealOpt.get(), 2);
                    return;
                }
            } else if (currentLvl == 2) {
                // Seviye 3: Zumrutlerin hepsi Lapis Blogu
                // Karsilikli iki Incide Blaze Cubugu, diger karsilikli iki Incide Altin Karpuz (Glistering Melon Slice)
                boolean allLapisBlock = true;
                for (int i = 0; i < 4; i++) {
                    if (emeraldItems[i] != Items.LAPIS_BLOCK) allLapisBlock = false;
                }

                boolean pearlOppositeMatch =
                        (pearlItems[0] == Items.BLAZE_ROD && pearlItems[3] == Items.BLAZE_ROD &&
                         pearlItems[1] == Items.GLISTERING_MELON_SLICE && pearlItems[2] == Items.GLISTERING_MELON_SLICE)
                        ||
                        (pearlItems[0] == Items.GLISTERING_MELON_SLICE && pearlItems[3] == Items.GLISTERING_MELON_SLICE &&
                         pearlItems[1] == Items.BLAZE_ROD && pearlItems[2] == Items.BLAZE_ROD);

                if (allLapisBlock && pearlOppositeMatch) {
                    completeRitual(world, centerPos, centerItem, lifestealOpt.get(), 3);
                    return;
                }
            }
        }
    }

    private static void completeRitual(World world, BlockPos centerPos, ItemStack centerItem, RegistryEntry<Enchantment> enchantment, int targetLevel) {
        for (int[] offset : EMERALD_OFFSETS) {
            BlockPos p = centerPos.add(offset[0], 0, offset[1]);
            clearPedestalItem(world, p);
            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
            }
        }
        for (int[] offset : PEARL_OFFSETS) {
            BlockPos p = centerPos.add(offset[0], 0, offset[1]);
            clearPedestalItem(world, p);
            if (world instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 10, 0.2, 0.2, 0.2, 0.02);
            }
        }

        ItemStack enchantedWeapon = centerItem.copy();
        enchantedWeapon.addEnchantment(enchantment, targetLevel);

        setPedestalItem(world, centerPos, enchantedWeapon);
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, centerPos.getX() + 0.5, centerPos.getY() + 1.2, centerPos.getZ() + 0.5, 40, 0.3, 0.3, 0.3, 0.1);
        }
        world.playSound(null, centerPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 1.0f, 1.2f);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (world.isClient()) return ActionResult.SUCCESS;

        List<ItemFrameEntity> frames = getPedestalFrames(world, pos);
        if (!frames.isEmpty()) {
            for (ItemFrameEntity frame : frames) {
                ItemStack held = frame.getHeldItemStack();
                if (!held.isEmpty()) {
                    ItemStack giveBack = held.copy();
                    if (!player.getInventory().insertStack(giveBack)) {
                        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, giveBack);
                    }
                }
                frame.discard();
            }
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
            checkRitualAround(world, pos);
            return ActionResult.SUCCESS;
        }

        if (!stack.isEmpty()) {
            setPedestalItem(world, pos, stack);
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
            checkRitualAround(world, pos);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        List<ItemFrameEntity> frames = getPedestalFrames(world, pos);
        if (!frames.isEmpty()) {
            if (!world.isClient()) {
                for (ItemFrameEntity frame : frames) {
                    ItemStack held = frame.getHeldItemStack();
                    if (!held.isEmpty()) {
                        ItemStack giveBack = held.copy();
                        if (!player.getInventory().insertStack(giveBack)) {
                            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, giveBack);
                        }
                    }
                    frame.discard();
                }
                world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
                checkRitualAround(world, pos);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            List<ItemFrameEntity> frames = getPedestalFrames(world, pos);
            for (ItemFrameEntity frame : frames) {
                ItemStack held = frame.getHeldItemStack();
                if (!held.isEmpty()) {
                    ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, held.copy());
                }
                frame.discard();
            }
        }
        return super.onBreak(world, pos, state, player);
    }
}
