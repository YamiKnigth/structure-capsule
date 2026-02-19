package com.yamiknight.structurecapsule.items;

import com.yamiknight.structurecapsule.StructureCapsuleMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Registry for all mod items
 */
public class ModItems {
    
    public static final EmptyCapsuleItem EMPTY_CAPSULE = new EmptyCapsuleItem(
        new Item.Settings().maxCount(1)
    );
    
    public static final FilledCapsuleItem FILLED_CAPSULE = new FilledCapsuleItem(
        new Item.Settings().maxCount(16)
    );
    
    /**
     * Register all items
     */
    public static void register() {
        Registry.register(Registries.ITEM, StructureCapsuleMod.id("empty_capsule"), EMPTY_CAPSULE);
        Registry.register(Registries.ITEM, StructureCapsuleMod.id("filled_capsule"), FILLED_CAPSULE);
    }
}
