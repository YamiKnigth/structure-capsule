package com.yamiknight.structurecapsule.network;

import com.yamiknight.structurecapsule.StructureCapsuleMod;
import com.yamiknight.structurecapsule.config.ModConfig;
import com.yamiknight.structurecapsule.items.FilledCapsuleItem;
import com.yamiknight.structurecapsule.items.ModItems;
import com.yamiknight.structurecapsule.structure.StructureData;
import com.yamiknight.structurecapsule.structure.StructureManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Network packet for saving structures
 * Sent from client to server when player clicks "Save" in the GUI
 */
public class StructureSavePacket {
    public static final Identifier ID = StructureCapsuleMod.id("structure_save");
    
    /**
     * Register packet handler on the server
     */
    public static void registerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, responseSender) -> {
            // Read packet data
            BlockPos pos1 = buf.readBlockPos();
            BlockPos pos2 = buf.readBlockPos();
            String name = buf.readString();
            int color = buf.readInt();
            
            // Process on server thread
            server.execute(() -> {
                handleSave(player, pos1, pos2, name, color);
            });
        });
    }
    
    /**
     * Handle structure save on server
     */
    private static void handleSave(ServerPlayerEntity player, BlockPos pos1, BlockPos pos2, String name, int color) {
        ServerWorld world = player.getServerWorld();
        
        // Validate name
        if (name.isEmpty() || name.length() > 32) {
            player.sendMessage(Text.translatable("structurecapsule.message.invalid_name"), false);
            return;
        }
        
        // Calculate size
        int sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        int totalBlocks = sizeX * sizeY * sizeZ;
        
        // Check size limit
        ModConfig config = ModConfig.getInstance();
        if (totalBlocks > config.maxStructureSize) {
            player.sendMessage(
                Text.translatable("structurecapsule.message.too_large", config.maxStructureSize),
                false
            );
            return;
        }
        
        // Save structure (this blocks the server thread, but should be quick for reasonable sizes)
        StructureData data = StructureManager.saveStructure(world, pos1, pos2, name);
        
        if (data == null) {
            player.sendMessage(Text.literal("Failed to save structure!"), false);
            return;
        }
        
        // Replace empty capsule with filled capsule
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        
        ItemStack filledCapsule = FilledCapsuleItem.create(
            name, color, sizeX, sizeY, sizeZ, data.getBlockCount()
        );
        
        if (mainHand.getItem() == ModItems.EMPTY_CAPSULE) {
            player.setStackInHand(Hand.MAIN_HAND, filledCapsule);
        } else if (offHand.getItem() == ModItems.EMPTY_CAPSULE) {
            player.setStackInHand(Hand.OFF_HAND, filledCapsule);
        }
        
        player.sendMessage(
            Text.translatable("structurecapsule.message.structure_saved", name, data.getBlockCount()),
            false
        );
    }
    
    /**
     * Send packet to server (client-side)
     */
    public static void send(BlockPos pos1, BlockPos pos2, String name, int color) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos1);
        buf.writeBlockPos(pos2);
        buf.writeString(name);
        buf.writeInt(color);
        
        ClientPlayNetworking.send(ID, buf);
    }
}
