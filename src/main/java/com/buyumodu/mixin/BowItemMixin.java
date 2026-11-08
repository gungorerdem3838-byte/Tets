package com.buyumodu.mixin;

import com.buyumodu.BuyuModu;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BowItem.class)
public class BowItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("RETURN"))
    private void buyumodu$onBowStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (!(world instanceof ServerWorld serverWorld)) return;
        if (!(user instanceof PlayerEntity player)) return;

        Registry<Enchantment> enchRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> verticalOpt = enchRegistry.getOptional(BuyuModu.DIKEY_COKLU_ATIS_ENCHANTMENT);
        if (verticalOpt.isEmpty() || EnchantmentHelper.getLevel(verticalOpt.get(), stack) <= 0) return;

        int useTicks = stack.getMaxUseTime(user) - remainingUseTicks;
        float pullProgress = BowItem.getPullProgress(useTicks);
        if (pullProgress < 0.1f) return;

        float speed = pullProgress * 3.0f;
        // -30.0f: 30 derece yukari, 15.0f: 15 derece asagi
        float[] pitchOffsets = {-30.0f, 15.0f};

        for (float pitchOffset : pitchOffsets) {
            ItemStack projectileStack = player.getProjectileType(stack);
            if (projectileStack.isEmpty() && !player.getAbilities().creativeMode) {
                break;
            }
            if (projectileStack.isEmpty()) {
                projectileStack = new ItemStack(Items.ARROW);
            }

            ItemStack singleArrow = projectileStack.copyWithCount(1);
            if (!player.getAbilities().creativeMode) {
                projectileStack.decrement(1);
                if (projectileStack.isEmpty()) {
                    player.getInventory().removeOne(projectileStack);
                }
            }

            ArrowItem arrowItem = (ArrowItem) (singleArrow.getItem() instanceof ArrowItem ? singleArrow.getItem() : Items.ARROW);
            PersistentProjectileEntity projectile = arrowItem.createArrow(serverWorld, singleArrow, player, stack);
            if (pullProgress == 1.0f) {
                projectile.setCritical(true);
            }
            if (player.getAbilities().creativeMode) {
                projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;
            } else {
                projectile.pickupType = PersistentProjectileEntity.PickupPermission.ALLOWED;
            }

            ProjectileEntity.spawnWithVelocity(
                    projectile,
                    serverWorld,
                    singleArrow,
                    player.getPitch() + pitchOffset,
                    player.getYaw(),
                    0.0f,
                    speed,
                    1.0f
            );
        }
    }
}
