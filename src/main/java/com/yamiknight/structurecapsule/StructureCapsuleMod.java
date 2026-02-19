package com.yamiknight.structurecapsule;

import com.yamiknight.structurecapsule.command.CapsuleCommand;
import com.yamiknight.structurecapsule.config.ModConfig;
import com.yamiknight.structurecapsule.items.ModItems;
import com.yamiknight.structurecapsule.network.StructureSavePacket;
import com.yamiknight.structurecapsule.placement.AsyncStructurePlacer;
import com.yamiknight.structurecapsule.structure.StructureManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod initializer for StructureCapsule.
 * Handles server-side registration and initialization.
 */
public class StructureCapsuleMod implements ModInitializer {
    public static final String MOD_ID = "structurecapsule";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // Creative tab for our items
    public static final RegistryKey<ItemGroup> ITEM_GROUP = RegistryKey.of(
        RegistryKeys.ITEM_GROUP,
        Identifier.of(MOD_ID, "general")
    );
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Structure Capsule Mod");
        
        // Load configuration
        ModConfig.load();
        
        // Initialize structure manager and create directories
        StructureManager.init();
        
        // Register items
        ModItems.register();
        
        // Register network packets
        StructureSavePacket.registerReceiver();
        
        // Register creative tab
        registerCreativeTab();
        
        // Register commands
        CommandRegistrationCallback.EVENT.register(CapsuleCommand::register);
        
        // Register server tick event for async structure placement
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            AsyncStructurePlacer.tick(server);
        });
        
        LOGGER.info("Structure Capsule Mod initialized successfully");
    }
    
    /**
     * Register the creative mode tab for our items
     */
    private void registerCreativeTab() {
        Registry.register(Registries.ITEM_GROUP, ITEM_GROUP,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(ModItems.EMPTY_CAPSULE))
                .displayName(Text.translatable("itemGroup.structurecapsule.general"))
                .entries((context, entries) -> {
                    entries.add(ModItems.EMPTY_CAPSULE);
                    entries.add(ModItems.FILLED_CAPSULE);
                })
                .build()
        );
    }
    
    /**
     * Helper to create namespaced identifiers
     */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
