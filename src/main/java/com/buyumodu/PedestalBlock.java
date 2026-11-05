package com.buyumodu;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
                new Box(above).expand(0.1),
                frame -> frame.getAttachedBlockPos().equals(above) && frame.getCommandTags().contains("buyumodu_pedestal_item")
        );
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
            return ActionResult.SUCCESS;
        }

        // Sütun boşsa ve oyuncunun elinde eşya varsa sütunun üstüne yerleştir
        if (!stack.isEmpty()) {
            BlockPos above = pos.up();
            ItemFrameEntity frame = new ItemFrameEntity(world, above, Direction.UP);
            ItemStack toPlace = stack.copyWithCount(1);
            frame.setHeldItemStack(toPlace, false);
            frame.setInvisible(true);
            frame.setInvulnerable(true);
            frame.setSilent(true);
            frame.addCommandTag("buyumodu_pedestal_item");
            world.spawnEntity(frame);

            if (!player.isCreative()) {
                stack.decrement(1);
            }
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1.0f, 1.0f);
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
