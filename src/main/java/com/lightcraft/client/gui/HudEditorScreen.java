package com.lightcraft.client.gui;

import com.lightcraft.config.ConfigManager;
import com.lightcraft.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HudEditorScreen extends Screen {
    private final ModConfig config;
    private final ConfigManager manager;
    private final HudRenderer renderer;
    private boolean isDragging = false;
    private String draggedElement = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudEditorScreen(ModConfig config, ConfigManager manager, HudRenderer renderer) {
        super(Text.of("HUD Editor"));
        this.config = config;
        this.manager = manager;
        this.renderer = renderer;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Modern Background
        renderGradient(context);
        
        renderer.setEditMode(true);
        renderer.render(context, delta);
        renderer.setEditMode(false);
        
        if (config.hudEditorGridSnap) drawGrid(context);

        HudRenderer.drawTextSafe(context, textRenderer, "Drag to move | Scale: " + config.hudScale, width/2 - 50, 10, 0xFFFFFFFF, true);
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void renderGradient(DrawContext context) {
        HudRenderer.fillSafe(context, 0, 0, width, height, 0xAA000000);
    }
    
    private void drawGrid(DrawContext context) {
        int gridSize = config.hudEditorGridSize;
        for (int x = 0; x < width; x += gridSize) HudRenderer.fillSafe(context, x, 0, x + 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += gridSize) HudRenderer.fillSafe(context, 0, y, width, y + 1, 0x10FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // CRITICAL FIX: Scale mouse coord to match HUD coord
            int scaledX = (int)(mouseX / config.hudScale);
            int scaledY = (int)(mouseY / config.hudScale);
            
            if (isHovering(scaledX, scaledY, config.fpsX, config.fpsY, 60, 16)) {
                startDrag("FPS", scaledX, scaledY, config.fpsX, config.fpsY);
                return true;
            }
            if (isHovering(scaledX, scaledY, config.coordsX, config.coordsY, 150, 40)) {
                startDrag("COORDS", scaledX, scaledY, config.coordsX, config.coordsY);
                return true;
            }
            int mmX = config.minimapX < 0 ? (int)(width/config.hudScale) + config.minimapX : config.minimapX;
            if (isHovering(scaledX, scaledY, mmX, config.minimapY, config.minimapSize, config.minimapSize)) {
                startDrag("MINIMAP", scaledX, scaledY, mmX, config.minimapY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void startDrag(String element, int mx, int my, int elemX, int elemY) {
        isDragging = true;
        draggedElement = element;
        dragOffsetX = mx - elemX;
        dragOffsetY = my - elemY;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && draggedElement != null) {
            // Apply scale logic to dragging
            int scaledX = (int)(mouseX / config.hudScale);
            int scaledY = (int)(mouseY / config.hudScale);
            
            int newX = scaledX - dragOffsetX;
            int newY = scaledY - dragOffsetY;

            if (config.hudEditorGridSnap) {
                newX = (newX / config.hudEditorGridSize) * config.hudEditorGridSize;
                newY = (newY / config.hudEditorGridSize) * config.hudEditorGridSize;
            }

            switch (draggedElement) {
                case "FPS" -> { config.fpsX = newX; config.fpsY = newY; }
                case "COORDS" -> { config.coordsX = newX; config.coordsY = newY; }
                case "MINIMAP" -> { 
                    int scaledW = (int)(width / config.hudScale);
                    if (newX > scaledW / 2) config.minimapX = newX - scaledW;
                    else config.minimapX = newX;
                    config.minimapY = newY; 
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false; draggedElement = null;
        manager.saveConfig(config);
        return super.mouseReleased(mouseX, mouseY, button);
    }
    @Override public void close() { MinecraftClient.getInstance().setScreen(null); }
}
