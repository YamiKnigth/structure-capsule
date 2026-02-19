package com.yamiknight.structurecapsule;

import com.yamiknight.structurecapsule.renderer.StructureGhostRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Client-side mod initializer for StructureCapsule.
 * Handles rendering and client-only features.
 */
public class StructureCapsuleClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        StructureCapsuleMod.LOGGER.info("Initializing Structure Capsule Client");
        
        // Register ghost structure renderer
        WorldRenderEvents.LAST.register(context -> {
            StructureGhostRenderer.render(context);
        });
        
        StructureCapsuleMod.LOGGER.info("Structure Capsule Client initialized");
    }
}
