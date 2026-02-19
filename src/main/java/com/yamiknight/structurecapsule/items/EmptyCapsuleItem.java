package com.yamiknight.structurecapsule.items;

import com.yamiknight.structurecapsule.screen.StructureSaveScreen;
import com.yamiknight.structurecapsule.structure.StructureManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Empty Capsule Item - Used by admins to define and save structures
 * 
 * Interactions:
 * - Left-Click Block: Set Position 1
 * - Right-Click Block: Set Position 2
 * - Shift + Right-Click Air: Open save GUI
 */
public class EmptyCapsuleItem extends Item {
    
    public EmptyCapsuleItem(Settings settings) {
        super(settings);
    }
    
    /**
     * Handle left-click on block (set Position 1)
     * This is called on the client, we use NBT to store positions temporarily
     */
    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, PlayerEntity player) {
        if (!player.getWorld().isClient) {
            // Store position 1 in NBT
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putLong("pos1", pos.asLong());
            
            player.sendMessage(
                Text.translatable("structurecapsule.message.pos1_set", 
                    String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ())),
                true
            );
        }
        return true; // Prevent actually breaking the block
    }
    
    /**
     * Handle right-click on block (set Position 2)
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;
        
        ItemStack stack = context.getStack();
        BlockPos pos = context.getBlockPos();
        
        if (!player.getWorld().isClient) {
            // Store position 2 in NBT
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putLong("pos2", pos.asLong());
            
            player.sendMessage(
                Text.translatable("structurecapsule.message.pos2_set",
                    String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ())),
                true
            );
        }
        
        return ActionResult.SUCCESS;
    }
    
    /**
     * Handle shift + right-click in air (open GUI)
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (player.isSneaking()) {
            // Check if both positions are set
            NbtCompound nbt = stack.getOrCreateNbt();
            if (!nbt.contains("pos1") || !nbt.contains("pos2")) {
                if (!world.isClient) {
                    player.sendMessage(
                        Text.translatable("structurecapsule.message.no_positions"),
                        false
                    );
                }
                return TypedActionResult.fail(stack);
            }
            
            // Open GUI on client
            if (world.isClient) {
                openSaveScreen(stack);
            }
            
            return TypedActionResult.success(stack, world.isClient);
        }
        
        return TypedActionResult.pass(stack);
    }
    
    /**
     * Open the structure save screen (client-side only)
     */
    @Environment(EnvType.CLIENT)
    private void openSaveScreen(ItemStack stack) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new StructureSaveScreen(stack));
    }
}
