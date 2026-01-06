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
        // Draw a dark background so you know you are in edit mode
        HudRenderer.fillSafe(context, 0, 0, width, height, 0x80000000);
        
        // Render the HUD elements (so you see what you are moving)
        renderer.setEditMode(true);
        renderer.render(context, delta);
        renderer.setEditMode(false); // Disable internally so we don't double-draw borders later
        
        // Draw Grid if snapping is on
        if (config.hudEditorGridSnap) {
            drawGrid(context);
        }

        // Draw instructions
        HudRenderer.drawTextSafe(context, textRenderer, "Drag elements to move", width/2 - 50, 10, 0xFFFFFFFF, true);
        HudRenderer.drawTextSafe(context, textRenderer, "Press ESC to save", width/2 - 40, height - 20, 0xFFAAAAAA, true);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void drawGrid(DrawContext context) {
        int gridSize = config.hudEditorGridSize;
        for (int x = 0; x < width; x += gridSize) {
            HudRenderer.fillSafe(context, x, 0, x + 1, height, 0x20FFFFFF);
        }
        for (int y = 0; y < height; y += gridSize) {
            HudRenderer.fillSafe(context, 0, y, width, y + 1, 0x20FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left Click
            // Check collision with HUD elements
            // FPS Box
            if (isHovering(mouseX, mouseY, config.fpsX, config.fpsY, 60, 16)) {
                startDrag("FPS", (int)mouseX, (int)mouseY, config.fpsX, config.fpsY);
                return true;
            }
            // Coords Box
            if (isHovering(mouseX, mouseY, config.coordsX, config.coordsY, 150, 40)) {
                startDrag("COORDS", (int)mouseX, (int)mouseY, config.coordsX, config.coordsY);
                return true;
            }
            // Minimap Box
            int mmX = config.minimapX < 0 ? width + config.minimapX : config.minimapX;
            if (isHovering(mouseX, mouseY, mmX, config.minimapY, config.minimapSize, config.minimapSize)) {
                startDrag("MINIMAP", (int)mouseX, (int)mouseY, mmX, config.minimapY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(double mx, double my, int x, int y, int w, int h) {
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
            int newX = (int)mouseX - dragOffsetX;
            int newY = (int)mouseY - dragOffsetY;

            // Grid Snap
            if (config.hudEditorGridSnap) {
                newX = (newX / config.hudEditorGridSize) * config.hudEditorGridSize;
                newY = (newY / config.hudEditorGridSize) * config.hudEditorGridSize;
            }

            // Update Config
            switch (draggedElement) {
                case "FPS" -> { config.fpsX = newX; config.fpsY = newY; }
                case "COORDS" -> { config.coordsX = newX; config.coordsY = newY; }
                case "MINIMAP" -> { 
                    // Handle right-side alignment if dragged past half screen
                    if (newX > width / 2) config.minimapX = newX - width;
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
        isDragging = false;
        draggedElement = null;
        manager.saveConfig(config); // Auto-save on drop
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
