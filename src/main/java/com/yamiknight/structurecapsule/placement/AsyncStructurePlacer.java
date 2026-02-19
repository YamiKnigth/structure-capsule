package com.yamiknight.structurecapsule.placement;

import com.yamiknight.structurecapsule.config.ModConfig;
import com.yamiknight.structurecapsule.structure.StructureData;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Async structure placer that places blocks over multiple ticks
 * to avoid freezing the server.
 * 
 * Strategy:
 * - Structures are queued for placement
 * - Each tick, we place a configurable number of blocks (blocksPerTick)
 * - This spreads the placement over multiple ticks, maintaining 20 TPS
 * - Chunk loading is checked before placing each block
 */
public class AsyncStructurePlacer {
    private static final Queue<PlacementTask> PLACEMENT_QUEUE = new ConcurrentLinkedQueue<>();
    
    /**
     * Queue a structure for async placement
     * This method is called from the server thread when a player places a filled capsule
     * 
     * @param server The server instance
     * @param world The world to place in
     * @param origin The origin position (where player clicked)
     * @param data The structure data to place
     * @param facing Player's facing direction for rotation
     */
    public static void queuePlacement(MinecraftServer server, World world, BlockPos origin, 
                                      StructureData data, Direction facing) {
        // Create a new placement task
        PlacementTask task = new PlacementTask(
            server,
            (ServerWorld) world,
            origin,
            data,
            facing
        );
        
        PLACEMENT_QUEUE.offer(task);
    }
    
    /**
     * Tick function called from ServerTickEvents.END_SERVER_TICK
     * Processes the placement queue and places blocks for active tasks
     * 
     * This is called ONCE per server tick (20 times per second)
     */
    public static void tick(MinecraftServer server) {
        if (PLACEMENT_QUEUE.isEmpty()) {
            return;
        }
        
        ModConfig config = ModConfig.getInstance();
        int blocksPerTick = config.blocksPerTick;
        
        // Process the current task
        PlacementTask task = PLACEMENT_QUEUE.peek();
        if (task == null) {
            return;
        }
        
        // Place up to blocksPerTick blocks
        int placed = 0;
        while (placed < blocksPerTick && task.hasMoreBlocks()) {
            if (task.placeNextBlock()) {
                placed++;
            }
        }
        
        // If task is complete, remove it from queue
        if (!task.hasMoreBlocks()) {
            PLACEMENT_QUEUE.poll();
        }
    }
    
    /**
     * Inner class representing a single placement task
     * Holds the state for placing one structure
     */
    private static class PlacementTask {
        private final MinecraftServer server;
        private final ServerWorld world;
        private final BlockPos origin;
        private final StructureData data;
        private final Direction facing;
        private final List<StructureData.BlockEntry> blocks;
        private int currentIndex = 0;
        
        public PlacementTask(MinecraftServer server, ServerWorld world, BlockPos origin, 
                           StructureData data, Direction facing) {
            this.server = server;
            this.world = world;
            this.origin = origin;
            this.data = data;
            this.facing = facing;
            this.blocks = new ArrayList<>(data.getBlocks());
        }
        
        /**
         * Check if there are more blocks to place
         */
        public boolean hasMoreBlocks() {
            return currentIndex < blocks.size();
        }
        
        /**
         * Place the next block in the sequence
         * Returns true if a block was placed, false if skipped (e.g., unloaded chunk)
         */
        public boolean placeNextBlock() {
            if (!hasMoreBlocks()) {
                return false;
            }
            
            StructureData.BlockEntry entry = blocks.get(currentIndex);
            currentIndex++;
            
            // Rotate position based on player facing
            BlockPos rotated = rotatePosition(entry.pos, facing, data.getSizeX(), data.getSizeZ());
            BlockPos targetPos = origin.add(rotated);
            
            // Safety check: ensure chunk is loaded
            if (!world.isChunkLoaded(targetPos)) {
                // Skip this block, but don't fail the entire placement
                return false;
            }
            
            // Rotate block state
            BlockState rotatedState = rotateBlockState(entry.state, facing);
            
            // Place the block
            // Use flag 3: notify neighbors and clients
            world.setBlockState(targetPos, rotatedState, 3);
            
            return true;
        }
        
        /**
         * Rotate a position based on player facing direction
         * Applies 90-degree rotations around the Y axis
         */
        private BlockPos rotatePosition(BlockPos pos, Direction facing, int sizeX, int sizeZ) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            
            return switch (facing) {
                case NORTH -> new BlockPos(x, y, z);  // No rotation
                case SOUTH -> new BlockPos(-x + sizeX - 1, y, -z + sizeZ - 1);  // 180°
                case WEST -> new BlockPos(z, y, -x + sizeX - 1);  // 90° CCW
                case EAST -> new BlockPos(-z + sizeZ - 1, y, x);  // 90° CW
                default -> pos;  // UP/DOWN not applicable
            };
        }
        
        /**
         * Rotate block state properties based on facing direction
         * This handles directional blocks like stairs, doors, etc.
         */
        private BlockState rotateBlockState(BlockState state, Direction playerFacing) {
            // Apply rotation based on player facing
            // Each 90-degree turn clockwise
            return switch (playerFacing) {
                case NORTH -> state;  // No rotation
                case EAST -> state.rotate(net.minecraft.util.BlockRotation.CLOCKWISE_90);
                case SOUTH -> state.rotate(net.minecraft.util.BlockRotation.CLOCKWISE_180);
                case WEST -> state.rotate(net.minecraft.util.BlockRotation.COUNTERCLOCKWISE_90);
                default -> state;
            };
        }
    }
}
