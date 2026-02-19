package com.yamiknight.structurecapsule.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration manager for StructureCapsule mod.
 * Handles loading/saving of config settings from structurecapsule.json
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("structurecapsule.json");
    
    private static ModConfig INSTANCE;
    
    // Configuration fields
    public int maxStructureSize = 5000;
    public int blocksPerTick = 500;
    public List<String> allowedDimensions = new ArrayList<>();
    
    public ModConfig() {
        // Default allowed dimensions
        allowedDimensions.add("minecraft:overworld");
        allowedDimensions.add("minecraft:the_nether");
        allowedDimensions.add("minecraft:the_end");
    }
    
    /**
     * Load config from disk or create default
     */
    public static ModConfig load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, ModConfig.class);
            } else {
                INSTANCE = new ModConfig();
                save();
            }
        } catch (IOException e) {
            System.err.println("Failed to load StructureCapsule config, using defaults: " + e.getMessage());
            INSTANCE = new ModConfig();
        }
        
        return INSTANCE;
    }
    
    /**
     * Save current config to disk
     */
    public static void save() {
        if (INSTANCE == null) return;
        
        try {
            String json = GSON.toJson(INSTANCE);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            System.err.println("Failed to save StructureCapsule config: " + e.getMessage());
        }
    }
    
    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }
}
