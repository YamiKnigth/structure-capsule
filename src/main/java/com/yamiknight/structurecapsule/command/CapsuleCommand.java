package com.yamiknight.structurecapsule.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yamiknight.structurecapsule.items.FilledCapsuleItem;
import com.yamiknight.structurecapsule.structure.StructureData;
import com.yamiknight.structurecapsule.structure.StructureManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Commands for StructureCapsule mod
 * 
 * Commands:
 * - /scapsule give <structure_name> [player] - Give a filled capsule
 * - /scapsule list - List all saved structures
 */
public class CapsuleCommand {
    
    /**
     * Register all commands
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, 
                               CommandRegistryAccess registryAccess,
                               CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
            CommandManager.literal("scapsule")
                .requires(source -> source.hasPermissionLevel(2)) // Require OP level 2
                .then(CommandManager.literal("give")
                    .then(CommandManager.argument("structure", StringArgumentType.string())
                        .suggests(structureSuggestions())
                        .executes(context -> giveToSelf(context))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .executes(context -> giveToPlayer(context))
                        )
                    )
                )
                .then(CommandManager.literal("list")
                    .executes(context -> listStructures(context))
                )
        );
    }
    
    /**
     * Give filled capsule to the command executor
     */
    private static int giveToSelf(CommandContext<ServerCommandSource> context) {
        String structureName = StringArgumentType.getString(context, "structure");
        ServerCommandSource source = context.getSource();
        
        try {
            ServerPlayerEntity player = source.getPlayerOrThrow();
            return giveCapsule(player, structureName, source);
        } catch (Exception e) {
            source.sendError(Text.literal("This command must be executed by a player!"));
            return 0;
        }
    }
    
    /**
     * Give filled capsule to a specified player
     */
    private static int giveToPlayer(CommandContext<ServerCommandSource> context) {
        String structureName = StringArgumentType.getString(context, "structure");
        ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
        ServerCommandSource source = context.getSource();
        
        return giveCapsule(targetPlayer, structureName, source);
    }
    
    /**
     * Core logic for giving a capsule
     */
    private static int giveCapsule(ServerPlayerEntity player, String structureName, ServerCommandSource source) {
        // Check if structure exists
        if (!StructureManager.structureExists(structureName)) {
            source.sendError(Text.translatable("structurecapsule.message.structure_not_found", structureName));
            return 0;
        }
        
        // Load structure to get dimensions
        StructureData data = StructureManager.loadStructure(structureName);
        if (data == null) {
            source.sendError(Text.literal("Failed to load structure data!"));
            return 0;
        }
        
        // Create filled capsule with green preview by default
        ItemStack capsule = FilledCapsuleItem.create(
            structureName,
            0x00FF00, // Green
            data.getSizeX(),
            data.getSizeY(),
            data.getSizeZ(),
            data.getBlockCount()
        );
        
        // Give to player
        player.giveItemStack(capsule);
        
        source.sendFeedback(
            () -> Text.translatable("structurecapsule.message.capsule_given", structureName),
            true
        );
        
        return 1;
    }
    
    /**
     * List all saved structures
     */
    private static int listStructures(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        List<String> structures = StructureManager.listStructures();
        
        if (structures.isEmpty()) {
            source.sendFeedback(
                () -> Text.translatable("structurecapsule.command.list.empty"),
                false
            );
            return 0;
        }
        
        source.sendFeedback(
            () -> Text.translatable("structurecapsule.command.list.header"),
            false
        );
        
        for (String structure : structures) {
            source.sendFeedback(
                () -> Text.translatable("structurecapsule.command.list.entry", structure),
                false
            );
        }
        
        return structures.size();
    }
    
    /**
     * Provide suggestions for structure names
     */
    private static SuggestionProvider<ServerCommandSource> structureSuggestions() {
        return (context, builder) -> {
            List<String> structures = StructureManager.listStructures();
            structures.forEach(builder::suggest);
            return builder.buildFuture();
        };
    }
}
