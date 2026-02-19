package com.yamiknight.structurecapsule.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yamiknight.structurecapsule.items.FilledCapsuleItem;
import com.yamiknight.structurecapsule.items.ModItems;
import com.yamiknight.structurecapsule.structure.StructureData;
import com.yamiknight.structurecapsule.structure.StructureManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side renderer for ghost structure preview
 * 
 * Rendering Strategy:
 * - Hooks into WorldRenderEvents.LAST to render after the world
 * - Checks if player is holding a filled capsule
 * - Renders translucent boxes at the target location
 * - Uses the color stored in the item's NBT
 * - Caches loaded structures to avoid re-loading from disk every frame
 * - Does NOT spawn entities - purely visual rendering
 */
public class StructureGhostRenderer {
    private static final Map<String, StructureData> STRUCTURE_CACHE = new HashMap<>();
    private static final float GHOST_ALPHA = 0.3f;
    
    /**
     * Main render method called from WorldRenderEvents.LAST
     */
    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        
        if (player == null || client.world == null) {
            return;
        }
        
        // Check if player is holding a filled capsule
        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        
        ItemStack capsule = null;
        if (mainHand.getItem() instanceof FilledCapsuleItem) {
            capsule = mainHand;
        } else if (offHand.getItem() instanceof FilledCapsuleItem) {
            capsule = offHand;
        }
        
        if (capsule == null) {
            return;
        }
        
        // Get structure data from NBT
        NbtCompound nbt = capsule.getNbt();
        if (nbt == null || !nbt.contains("structureName")) {
            return;
        }
        
        String structureName = nbt.getString("structureName");
        int color = nbt.getInt("color");
        
        // Load structure (with caching)
        StructureData data = getOrLoadStructure(structureName);
        if (data == null) {
            return;
        }
        
        // Get target position from raycast
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        
        BlockHitResult blockHit = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHit.getBlockPos().offset(blockHit.getSide());
        
        // Render ghost structure
        renderGhostStructure(context, data, targetPos, player.getHorizontalFacing(), color);
    }
    
    /**
     * Render the ghost structure at the target position
     */
    private static void renderGhostStructure(WorldRenderContext context, StructureData data, 
                                            BlockPos origin, Direction facing, int colorInt) {
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;
        
        Vec3d camera = context.camera().getPos();
        
        matrices.push();
        
        // Translate to world position relative to camera
        matrices.translate(
            origin.getX() - camera.x,
            origin.getY() - camera.y,
            origin.getZ() - camera.z
        );
        
        // Setup rendering state for translucent rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        
        // Extract RGB from color int
        float r = ((colorInt >> 16) & 0xFF) / 255.0f;
        float g = ((colorInt >> 8) & 0xFF) / 255.0f;
        float b = (colorInt & 0xFF) / 255.0f;
        
        // Render each block as a colored box
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        for (StructureData.BlockEntry entry : data.getBlocks()) {
            // Rotate position based on facing
            BlockPos rotated = rotatePosition(entry.pos, facing, data.getSizeX(), data.getSizeZ());
            
            // Draw wireframe box for this block
            drawBlockOutline(buffer, matrix, rotated, r, g, b, GHOST_ALPHA);
        }
        
        tessellator.draw();
        
        // Restore rendering state
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        
        matrices.pop();
    }
    
    /**
     * Draw a wireframe box outline for a single block
     */
    private static void drawBlockOutline(BufferBuilder buffer, Matrix4f matrix, BlockPos pos, 
                                        float r, float g, float b, float a) {
        float x1 = pos.getX();
        float y1 = pos.getY();
        float z1 = pos.getZ();
        float x2 = x1 + 1;
        float y2 = y1 + 1;
        float z2 = z1 + 1;
        
        // Bottom face
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        
        // Top face
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        
        // Vertical edges
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
    }
    
    /**
     * Rotate a position based on player facing direction
     */
    private static BlockPos rotatePosition(BlockPos pos, Direction facing, int sizeX, int sizeZ) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case SOUTH -> new BlockPos(-x + sizeX - 1, y, -z + sizeZ - 1);
            case WEST -> new BlockPos(z, y, -x + sizeX - 1);
            case EAST -> new BlockPos(-z + sizeZ - 1, y, x);
            default -> pos;
        };
    }
    
    /**
     * Get structure from cache or load from disk
     * This prevents loading the structure from disk every frame
     */
    private static StructureData getOrLoadStructure(String name) {
        if (!STRUCTURE_CACHE.containsKey(name)) {
            StructureData data = StructureManager.loadStructure(name);
            if (data != null) {
                STRUCTURE_CACHE.put(name, data);
            }
            return data;
        }
        return STRUCTURE_CACHE.get(name);
    }
    
    /**
     * Clear the structure cache (useful if structures are modified)
     */
    public static void clearCache() {
        STRUCTURE_CACHE.clear();
    }
}
