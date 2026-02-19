package com.yamiknight.structurecapsule.items;

import com.yamiknight.structurecapsule.placement.AsyncStructurePlacer;
import com.yamiknight.structurecapsule.renderer.StructureGhostRenderer;
import com.yamiknight.structurecapsule.structure.StructureData;
import com.yamiknight.structurecapsule.structure.StructureManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

/**
 * Filled Capsule Item - Contains a saved structure
 * 
 * Interactions:
 * - Holding: Shows ghost preview at look position
 * - Right-Click: Places structure and consumes item
 * 
 * NBT Data:
 * - structureName: String
 * - color: int (RGB)
 * - sizeX, sizeY, sizeZ: int (for tooltip)
 * - blockCount: int (for tooltip)
 */
public class FilledCapsuleItem extends Item {
    
    public FilledCapsuleItem(Settings settings) {
        super(settings);
    }
    
    /**
     * Handle right-click to place structure
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return placeStructure(context);
    }
    
    /**
     * Handle right-click in air to place structure
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            // Try to place at the block the player is looking at
            // This is handled by useOnBlock, so we just pass here
        }
        return TypedActionResult.pass(player.getStackInHand(hand));
    }
    
    /**
     * Place the structure at the target location
     */
    private ActionResult placeStructure(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (player == null) return ActionResult.PASS;
        
        ItemStack stack = context.getStack();
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        
        // Server-side placement
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            NbtCompound nbt = stack.getNbt();
            if (nbt == null || !nbt.contains("structureName")) {
                return ActionResult.FAIL;
            }
            
            String structureName = nbt.getString("structureName");
            Direction facing = player.getHorizontalFacing();
            
            // Load structure data
            StructureData data = StructureManager.loadStructure(structureName);
            if (data == null) {
                player.sendMessage(
                    Text.translatable("structurecapsule.message.structure_not_found", structureName),
                    false
                );
                return ActionResult.FAIL;
            }
            
            // Queue async placement
            AsyncStructurePlacer.queuePlacement(world.getServer(), world, pos, data, facing);
            
            player.sendMessage(
                Text.translatable("structurecapsule.message.structure_placed", structureName),
                false
            );
            
            // Consume item
            stack.decrement(1);
            
            return ActionResult.SUCCESS;
        }
        
        return world.isClient ? ActionResult.SUCCESS : ActionResult.CONSUME;
    }
    
    /**
     * Add tooltip showing structure info
     */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("structureName")) {
            String name = nbt.getString("structureName");
            tooltip.add(Text.translatable("structurecapsule.tooltip.structure", name));
            
            if (nbt.contains("sizeX")) {
                int x = nbt.getInt("sizeX");
                int y = nbt.getInt("sizeY");
                int z = nbt.getInt("sizeZ");
                tooltip.add(Text.translatable("structurecapsule.tooltip.dimensions", x, y, z));
            }
            
            if (nbt.contains("blockCount")) {
                int count = nbt.getInt("blockCount");
                tooltip.add(Text.translatable("structurecapsule.tooltip.blocks", count));
            }
        }
    }
    
    /**
     * Create a filled capsule with structure data
     */
    public static ItemStack create(String structureName, int color, int sizeX, int sizeY, int sizeZ, int blockCount) {
        ItemStack stack = new ItemStack(ModItems.FILLED_CAPSULE);
        NbtCompound nbt = stack.getOrCreateNbt();
        
        nbt.putString("structureName", structureName);
        nbt.putInt("color", color);
        nbt.putInt("sizeX", sizeX);
        nbt.putInt("sizeY", sizeY);
        nbt.putInt("sizeZ", sizeZ);
        nbt.putInt("blockCount", blockCount);
        
        return stack;
    }
}
