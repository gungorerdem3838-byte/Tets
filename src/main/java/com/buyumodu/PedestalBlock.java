package com.buyumodu;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
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

public class PedestalBlock extends Block {
    // Yarıçapı 0.1 blok (1.6 piksel) daraltılmış özel VoxelShape
    private static final VoxelShape SHAPE = VoxelShapes.union(
            Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 6.0, 11.0),
            Block.createCuboidShape(6.0, 6.0, 6.0, 10.0, 12.0, 10.0),
            Block.createCuboidShape(3.6, 12.0, 3.6, 12.4, 16.0, 12.4)
    );

    // Yeşil (Zümrüt Sütunu) konumları: Merkezden 4 blok uzaklıkta (arada 3 boşluk)
    private static final int[][] EMERALD_OFFSETS = {
            {0, -4},
            {-4, 0},
            {4, 0},
            {0, 4}
    };

    // Mavi (İnci Sütunu) konumları: Merkezden çapraz 3 blok uzaklıkta
    private static final int[][] PEARL_OFFSETS = {
            {-3, -3},
            {3, -3},
            {-3, 3},
            {3, 3}
    };

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
            if (!stack.isEmpty()) {
                return stack;
            }
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

    // Herhangi bir sütuna eşya konulduğunda veya alındığında yakınlardaki Göz Sütununu (Sarı) bulup ritüeli kontrol eder
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

    private static void tryPerformRitual(World world, BlockPos centerPos) {
        // Merkezdeki sarı sütunun üstü boş olmalı (Elmas orada oluşacak)
        if (!getDisplayedItem(world, centerPos).isEmpty()) {
            return;
        }

        // 1. 4 Yeşil (Zümrüt) sütunun doğru yerde olduğunu ve üstlerinde Demir Külçesi olduğunu doğrula
        for (int[] offset : EMERALD_OFFSETS) {
            BlockPos targetPos = centerPos.add(offset[0], 0, offset[1]);
            if (!world.getBlockState(targetPos).isOf(BuyuModu.EMERALD_PEDESTAL)) {
                return;
            }
            if (!getDisplayedItem(world, targetPos).isOf(Items.IRON_INGOT)) {
                return;
            }
        }

        // 2. 4 Mavi (İnci) sütunun doğru yerde olduğunu ve üstlerinde Demir Külçesi olduğunu doğrula
        for (int[] offset : PEARL_OFFSETS) {
            BlockPos targetPos = centerPos.add(offset[0], 0, offset[1]);
            if (!world.getBlockState(targetPos).isOf(BuyuModu.PEARL_PEDESTAL)) {
                return;
            }
            if (!getDisplayedItem(world, targetPos).isOf(Items.IRON_INGOT)) {
                return;
            }
        }

        // Tüm diziliş ve 8 demir külçesi tamam! Şimdi 8 demiri tüket:
        for (int[] offset : EMERALD_OFFSETS) {
            BlockPos p = centerPos.add(offset[0], 0, offset[1]);
            clearPedestalItem(world, p);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 12, 0.15, 0.15, 0.15, 0.02);
            }
        }

        for (int[] offset : PEARL_OFFSETS) {
            BlockPos p = centerPos.add(offset[0], 0, offset[1]);
            clearPedestalItem(world, p);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.SMOKE, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 12, 0.15, 0.15, 0.15, 0.02);
            }
        }

        // Ortadaki Sarı (Göz) sütunun üstünde Elmas oluştur!
        setPedestalItem(world, centerPos, new ItemStack(Items.DIAMOND));

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.END_ROD, centerPos.getX() + 0.5, centerPos.getY() + 1.2, centerPos.getZ() + 0.5, 30, 0.25, 0.25, 0.25, 0.05);
        }
        world.playSound(null, centerPos, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.BLOCKS, 0.7f, 1.4f);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        List<ItemFrameEntity> frames = getPedestalFrames(world, pos);

        // Sütunun üstünde zaten eşya varsa onu oyuncuya geri ver
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

        // Sütun boşsa ve oyuncunun elinde eşya varsa sütunun üstüne yerleştir
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
