package com.example.flintnohitbox;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTNT;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Normally, right-clicking with Flint and Steel while aiming at a spot occupied by
 * a player/mob hitbox fires an "interact with entity" packet instead of a
 * "use item on block" packet -- the entity's hitbox intercepts the raytrace before
 * it ever reaches the block behind it. That's why the vanilla ignite logic never runs.
 *
 * This handler catches that entity-interact event when the held item is Flint and
 * Steel, cancels the normal (do-nothing) entity interaction, and performs its own
 * raytrace that only considers blocks -- completely ignoring every entity hitbox in
 * the way -- then manually replicates vanilla's ignite behavior on whatever block
 * that raytrace lands on.
 */
public class FlintInteractHandler {

    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
       EntityPlayer player = event.entityPlayer;
        ItemStack held = player.getHeldItem();

        if (held == null || !(held.getItem() instanceof ItemFlintAndSteel)) {
            return;
        }

        // Stop the normal "do nothing" entity interaction from happening.
        event.setCanceled(true);

        World world = player.worldObj;

        double reach = player.capabilities.isCreativeMode ? 5.0D : 4.5D;

        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        Vec3 eyePos = new Vec3(eyeX, eyeY, eyeZ);
        Vec3 lookVec = player.getLook(1.0F);
        Vec3 endPos = eyePos.addVector(
                lookVec.xCoord * reach,
                lookVec.yCoord * reach,
                lookVec.zCoord * reach);

        // stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false,
        // returnLastUncollidableBlock = true -- this raytrace only ever looks at
        // blocks, entities are never considered.
        MovingObjectPosition result = world.rayTraceBlocks(eyePos, endPos, false, false, true);

        if (result == null || result.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }

        BlockPos hitPos = result.getBlockPos();
        EnumFacing side = result.sideHit;

        // Mirror vanilla: if the block you're directly looking at is TNT, ignite it in place.
        Block hitBlock = world.getBlockState(hitPos).getBlock();
        if (hitBlock == Blocks.tnt) {
            if (!player.canPlayerEdit(hitPos, side, held)) {
                return;
            }
            world.setBlockToAir(hitPos);
            spawnTnt(world, hitPos);
            damageItem(held, player);
            return;
        }

        // Otherwise, mirror vanilla: place fire in the empty space adjacent to the
        // clicked face.
        BlockPos targetPos = hitPos.offset(side);

        if (!player.canPlayerEdit(targetPos, side, held)) {
            return;
        }

        Block targetBlock = world.getBlockState(targetPos).getBlock();

        if (targetBlock == Blocks.air) {
            world.playSoundEffect(
                    targetPos.getX() + 0.5D,
                    targetPos.getY() + 0.5D,
                    targetPos.getZ() + 0.5D,
                    "fire.ignite", 1.0F,
                    (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
            world.setBlockState(targetPos, Blocks.fire.getDefaultState());
            damageItem(held, player);
        }
    }

    private void spawnTnt(World world, BlockPos pos) {
        net.minecraft.entity.item.EntityTNTPrimed tnt = new net.minecraft.entity.item.EntityTNTPrimed(
                world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, null);
        world.playSoundEffect(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                "game.tnt.primed", 1.0F, 1.0F);
        world.spawnEntityInWorld(tnt);
    }

    private void damageItem(ItemStack stack, EntityPlayer player) {
        stack.damageItem(1, player);
    }
}
