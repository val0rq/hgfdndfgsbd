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
        // Dark background
        HudRenderer.fillSafe(context, 0, 0, width, height, 0xAA000000); 
        
        // Grid
        if (config.hudEditorGridSnap) {
            int s = 20; 
            for (int x = 0; x < width; x+=s) HudRenderer.fillSafe(context, x, 0, x+1, height, 0x1FFFFFFF);
            for (int y = 0; y < height; y+=s) HudRenderer.fillSafe(context, 0, y, width, y+1, 0x1FFFFFFF);
        }

        // Render HUD
        renderer.setEditMode(true);
        renderer.render(context, delta);
        renderer.setEditMode(false);

        // Help Text
        String help = "Click & Drag elements. Current Mouse: " + mouseX + ", " + mouseY;
        HudRenderer.drawTextSafe(context, textRenderer, help, 10, 10, 0xFFFFFFFF, true);
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left Click
            int mx = (int)mouseX;
            int my = (int)mouseY;
            float scale = config.hudScale;
            if (scale <= 0) scale = 1.0f; // Safety
            
            // Unscale mouse coordinates to match config coordinates
            int hudX = (int)(mx / scale);
            int hudY = (int)(my / scale);

            // FPS Box (approx 60x16)
            if (isInside(hudX, hudY, config.fpsX, config.fpsY, 60, 16)) {
                startDrag("FPS", hudX, hudY, config.fpsX, config.fpsY);
                return true;
            }
            
            // Coords Box (approx 150x40)
            if (isInside(hudX, hudY, config.coordsX, config.coordsY, 150, 40)) {
                startDrag("COORDS", hudX, hudY, config.coordsX, config.coordsY);
                return true;
            }
            
            // Minimap Box
            int mmX = config.minimapX;
            // Handle negative X (right alignment)
            if (mmX < 0) {
                 int scaledWidth = (int)(width / scale);
                 mmX = scaledWidth + config.minimapX;
            }
            
            if (isInside(hudX, hudY, mmX, config.minimapY, config.minimapSize, config.minimapSize)) {
                startDrag("MINIMAP", hudX, hudY, mmX, config.minimapY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean isInside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void startDrag(String elem, int mx, int my, int elemX, int elemY) {
        isDragging = true;
        draggedElement = elem;
        dragOffsetX = mx - elemX;
        dragOffsetY = my - elemY;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && draggedElement != null) {
            float scale = config.hudScale;
            if (scale <= 0) scale = 1.0f;
            
            int hudX = (int)(mouseX / scale);
            int hudY = (int)(mouseY / scale);
            
            int newX = hudX - dragOffsetX;
            int newY = hudY - dragOffsetY;
            
            // Snap to grid (10px)
            if (config.hudEditorGridSnap) {
                newX = (newX / 10) * 10;
                newY = (newY / 10) * 10;
            }

            switch(draggedElement) {
                case "FPS" -> { config.fpsX = newX; config.fpsY = newY; }
                case "COORDS" -> { config.coordsX = newX; config.coordsY = newY; }
                case "MINIMAP" -> {
                    // Logic for right-side alignment
                    int scaledW = (int)(width / scale);
                    if (newX > scaledW / 2) {
                        config.minimapX = newX - scaledW; // Store as negative offset
                    } else {
                        config.minimapX = newX;
                    }
                    config.minimapY = newY;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDragging) {
            isDragging = false;
            draggedElement = null;
            manager.saveConfig(config); // Save on release
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
