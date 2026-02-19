package com.yamiknight.structurecapsule.structure;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a saved structure with block data
 * Stores the structure in a compact format for NBT serialization
 */
public class StructureData {
    private final String name;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<BlockEntry> blocks;
    
    public StructureData(String name, int sizeX, int sizeY, int sizeZ) {
        this.name = name;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = new ArrayList<>();
    }
    
    /**
     * Add a block to the structure
     * @param relativePos Position relative to origin (0,0,0)
     * @param state Block state to store
     */
    public void addBlock(BlockPos relativePos, BlockState state) {
        blocks.add(new BlockEntry(relativePos, state));
    }
    
    /**
     * Get all blocks in this structure
     */
    public List<BlockEntry> getBlocks() {
        return blocks;
    }
    
    public String getName() {
        return name;
    }
    
    public int getSizeX() {
        return sizeX;
    }
    
    public int getSizeY() {
        return sizeY;
    }
    
    public int getSizeZ() {
        return sizeZ;
    }
    
    public int getBlockCount() {
        return blocks.size();
    }
    
    /**
     * Serialize to NBT for disk storage
     */
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", name);
        nbt.putInt("sizeX", sizeX);
        nbt.putInt("sizeY", sizeY);
        nbt.putInt("sizeZ", sizeZ);
        
        NbtList blockList = new NbtList();
        for (BlockEntry entry : blocks) {
            NbtCompound blockNbt = new NbtCompound();
            blockNbt.putInt("x", entry.pos.getX());
            blockNbt.putInt("y", entry.pos.getY());
            blockNbt.putInt("z", entry.pos.getZ());
            
            // Store block state as string ID with properties
            Identifier id = Registries.BLOCK.getId(entry.state.getBlock());
            blockNbt.putString("block", id.toString());
            
            // Store block state properties
            NbtCompound stateNbt = new NbtCompound();
            entry.state.getEntries().forEach((property, value) -> {
                stateNbt.putString(property.getName(), value.toString());
            });
            blockNbt.put("properties", stateNbt);
            
            blockList.add(blockNbt);
        }
        nbt.put("blocks", blockList);
        
        return nbt;
    }
    
    /**
     * Deserialize from NBT
     */
    public static StructureData fromNbt(NbtCompound nbt) {
        String name = nbt.getString("name");
        int sizeX = nbt.getInt("sizeX");
        int sizeY = nbt.getInt("sizeY");
        int sizeZ = nbt.getInt("sizeZ");
        
        StructureData data = new StructureData(name, sizeX, sizeY, sizeZ);
        
        NbtList blockList = nbt.getList("blocks", 10); // 10 = NbtCompound type
        for (int i = 0; i < blockList.size(); i++) {
            NbtCompound blockNbt = blockList.getCompound(i);
            
            int x = blockNbt.getInt("x");
            int y = blockNbt.getInt("y");
            int z = blockNbt.getInt("z");
            BlockPos pos = new BlockPos(x, y, z);
            
            // Parse block state
            String blockId = blockNbt.getString("block");
            Identifier id = Identifier.tryParse(blockId);
            if (id == null || !Registries.BLOCK.containsId(id)) {
                continue; // Skip invalid blocks
            }
            
            BlockState state = Registries.BLOCK.get(id).getDefaultState();
            
            // Apply properties
            if (blockNbt.contains("properties")) {
                NbtCompound propsNbt = blockNbt.getCompound("properties");
                for (String key : propsNbt.getKeys()) {
                    String value = propsNbt.getString(key);
                    try {
                        state = parseBlockProperty(state, key, value);
                    } catch (Exception e) {
                        // Skip invalid properties
                    }
                }
            }
            
            data.addBlock(pos, state);
        }
        
        return data;
    }
    
    /**
     * Helper to parse block state properties
     */
    private static BlockState parseBlockProperty(BlockState state, String propertyName, String value) {
        var property = state.getBlock().getStateManager().getProperty(propertyName);
        if (property != null) {
            var parsedValue = property.parse(value);
            if (parsedValue.isPresent()) {
                return state.with(property, (Comparable) parsedValue.get());
            }
        }
        return state;
    }
    
    /**
     * Inner class representing a single block in the structure
     */
    public static class BlockEntry {
        public final BlockPos pos;
        public final BlockState state;
        
        public BlockEntry(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
