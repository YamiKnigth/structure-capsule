package com.yamiknight.structurecapsule.screen;

import com.yamiknight.structurecapsule.config.ModConfig;
import com.yamiknight.structurecapsule.items.FilledCapsuleItem;
import com.yamiknight.structurecapsule.items.ModItems;
import com.yamiknight.structurecapsule.network.StructureSavePacket;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * GUI screen for saving structures
 * Allows player to input structure name and preview color
 */
public class StructureSaveScreen extends Screen {
    private static final int PADDING = 20;
    private static final int WIDGET_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    
    private final ItemStack capsuleStack;
    private TextFieldWidget nameField;
    private TextFieldWidget colorField;
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    
    public StructureSaveScreen(ItemStack capsuleStack) {
        super(Text.translatable("structurecapsule.gui.title"));
        this.capsuleStack = capsuleStack;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;
        
        // Structure name field
        nameField = new TextFieldWidget(
            this.textRenderer,
            centerX - 100,
            startY,
            200,
            WIDGET_HEIGHT,
            Text.translatable("structurecapsule.gui.name")
        );
        nameField.setMaxLength(32);
        nameField.setPlaceholder(Text.translatable("structurecapsule.gui.name"));
        addSelectableChild(nameField);
        
        // Color field (hex input)
        colorField = new TextFieldWidget(
            this.textRenderer,
            centerX - 100,
            startY + 30,
            200,
            WIDGET_HEIGHT,
            Text.translatable("structurecapsule.gui.color")
        );
        colorField.setMaxLength(7); // #RRGGBB
        colorField.setText("#00FF00"); // Default green
        colorField.setPlaceholder(Text.translatable("structurecapsule.gui.color"));
        addSelectableChild(colorField);
        
        // Save button
        saveButton = ButtonWidget.builder(
            Text.translatable("structurecapsule.gui.save"),
            button -> onSave()
        )
        .dimensions(centerX - BUTTON_WIDTH - 5, startY + 60, BUTTON_WIDTH, WIDGET_HEIGHT)
        .build();
        addDrawableChild(saveButton);
        
        // Cancel button
        cancelButton = ButtonWidget.builder(
            Text.translatable("structurecapsule.gui.cancel"),
            button -> close()
        )
        .dimensions(centerX + 5, startY + 60, BUTTON_WIDTH, WIDGET_HEIGHT)
        .build();
        addDrawableChild(cancelButton);
        
        setInitialFocus(nameField);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        
        // Draw title
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            this.title,
            this.width / 2,
            this.height / 2 - 80,
            0xFFFFFF
        );
        
        // Draw field labels
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("structurecapsule.gui.name"),
            this.width / 2 - 100,
            this.height / 2 - 60,
            0xA0A0A0
        );
        
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("structurecapsule.gui.color"),
            this.width / 2 - 100,
            this.height / 2 - 30,
            0xA0A0A0
        );
        
        // Render widgets
        nameField.render(context, mouseX, mouseY, delta);
        colorField.render(context, mouseX, mouseY, delta);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * Handle save button click
     */
    private void onSave() {
        String name = nameField.getText().trim();
        String colorHex = colorField.getText().trim();
        
        if (name.isEmpty()) {
            return;
        }
        
        // Parse color
        int color;
        try {
            if (colorHex.startsWith("#")) {
                colorHex = colorHex.substring(1);
            }
            color = Integer.parseInt(colorHex, 16);
        } catch (NumberFormatException e) {
            color = 0x00FF00; // Default green
        }
        
        // Get positions from NBT
        NbtCompound nbt = capsuleStack.getOrCreateNbt();
        if (!nbt.contains("pos1") || !nbt.contains("pos2")) {
            close();
            return;
        }
        
        BlockPos pos1 = BlockPos.fromLong(nbt.getLong("pos1"));
        BlockPos pos2 = BlockPos.fromLong(nbt.getLong("pos2"));
        
        // Calculate size for validation
        int sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
        int sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        int totalBlocks = sizeX * sizeY * sizeZ;
        
        // Check size limit
        ModConfig config = ModConfig.getInstance();
        if (totalBlocks > config.maxStructureSize) {
            if (client != null && client.player != null) {
                client.player.sendMessage(
                    Text.translatable("structurecapsule.message.too_large", config.maxStructureSize),
                    false
                );
            }
            close();
            return;
        }
        
        // Send packet to server
        StructureSavePacket.send(pos1, pos2, name, color);
        
        close();
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
