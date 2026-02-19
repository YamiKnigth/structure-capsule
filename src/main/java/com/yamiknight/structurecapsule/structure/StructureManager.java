package com.yamiknight.structurecapsule.structure;

import com.yamiknight.structurecapsule.StructureCapsuleMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manager for saving and loading structure files to/from disk
 * Structures are stored in ./config/structurecapsule/saved_structures/
 */
public class StructureManager {
    private static Path STRUCTURES_DIR;
    
    /**
     * Initialize the structure manager and create directories
     */
    public static void init() {
        STRUCTURES_DIR = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("structurecapsule")
            .resolve("saved_structures");
        
        try {
            Files.createDirectories(STRUCTURES_DIR);
            StructureCapsuleMod.LOGGER.info("Structure directory initialized at: {}", STRUCTURES_DIR);
        } catch (IOException e) {
            StructureCapsuleMod.LOGGER.error("Failed to create structures directory", e);
        }
    }
    
    /**
     * Scan a region in the world and save it as a structure
     * This is a BLOCKING operation - call from a server thread
     * 
     * @param world The world to scan
     * @param pos1 First corner
     * @param pos2 Second corner
     * @param name Structure name
     * @return The saved structure data, or null on failure
     */
    public static StructureData saveStructure(ServerWorld world, BlockPos pos1, BlockPos pos2, String name) {
        // Calculate bounds
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        
        StructureData data = new StructureData(name, sizeX, sizeY, sizeZ);
        BlockPos origin = new BlockPos(minX, minY, minZ);
        
        // Scan all blocks in the region
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    
                    // Skip air blocks to save space
                    if (state.isAir()) {
                        continue;
                    }
                    
                    // Calculate relative position
                    BlockPos relative = pos.subtract(origin);
                    data.addBlock(relative, state);
                }
            }
        }
        
        // Save to disk
        try {
            File file = STRUCTURES_DIR.resolve(sanitizeName(name) + ".nbt").toFile();
            NbtCompound nbt = data.toNbt();
            NbtIo.writeCompressed(nbt, file);
            
            StructureCapsuleMod.LOGGER.info("Saved structure '{}' with {} blocks", name, data.getBlockCount());
            return data;
        } catch (IOException e) {
            StructureCapsuleMod.LOGGER.error("Failed to save structure: {}", name, e);
            return null;
        }
    }
    
    /**
     * Load a structure from disk
     * This is a BLOCKING operation - use AsyncStructurePlacer for placement
     * 
     * @param name Structure name
     * @return The loaded structure data, or null if not found
     */
    public static StructureData loadStructure(String name) {
        try {
            File file = STRUCTURES_DIR.resolve(sanitizeName(name) + ".nbt").toFile();
            if (!file.exists()) {
                StructureCapsuleMod.LOGGER.warn("Structure not found: {}", name);
                return null;
            }
            
            NbtCompound nbt = NbtIo.readCompressed(file);
            return StructureData.fromNbt(nbt);
        } catch (IOException e) {
            StructureCapsuleMod.LOGGER.error("Failed to load structure: {}", name, e);
            return null;
        }
    }
    
    /**
     * Check if a structure exists
     */
    public static boolean structureExists(String name) {
        File file = STRUCTURES_DIR.resolve(sanitizeName(name) + ".nbt").toFile();
        return file.exists();
    }
    
    /**
     * List all saved structures
     */
    public static List<String> listStructures() {
        List<String> structures = new ArrayList<>();
        
        try (Stream<Path> paths = Files.list(STRUCTURES_DIR)) {
            paths.filter(path -> path.toString().endsWith(".nbt"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String structureName = fileName.substring(0, fileName.length() - 4); // Remove .nbt
                    structures.add(structureName);
                });
        } catch (IOException e) {
            StructureCapsuleMod.LOGGER.error("Failed to list structures", e);
        }
        
        return structures;
    }
    
    /**
     * Sanitize structure name for file system
     */
    private static String sanitizeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }
}
