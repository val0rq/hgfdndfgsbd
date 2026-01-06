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
    private int dragStartX, dragStartY;
    private int initialElemX, initialElemY;

    public HudEditorScreen(ModConfig config, ConfigManager manager, HudRenderer renderer) {
        super(Text.of("HUD Editor"));
        this.config = config;
        this.manager = manager;
        this.renderer = renderer;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        HudRenderer.fillSafe(context, 0, 0, width, height, 0x99000000); 
        
        if (config.hudEditorGridSnap) {
            int size = config.hudEditorGridSize;
            for (int x = 0; x < width; x+=size) HudRenderer.fillSafe(context, x, 0, x+1, height, 0x1FFFFFFF);
            for (int y = 0; y < height; y+=size) HudRenderer.fillSafe(context, 0, y, width, y+1, 0x1FFFFFFF);
        }

        renderer.setEditMode(true);
        renderer.render(context, delta);
        renderer.setEditMode(false);

        HudRenderer.drawTextSafe(context, textRenderer, "HUD EDITOR", width/2 - 30, 10, 0xFF00FF00, true);
        HudRenderer.drawTextSafe(context, textRenderer, "Click & Drag to move. ESC to save.", width/2 - 90, 25, 0xFFAAAAAA, true);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int)mouseX;
            int my = (int)mouseY;
            float s = config.hudScale;
            
            // Check Hitbox (Scaled)
            if (hit(mx, my, config.fpsX, config.fpsY, 60, 15, s)) startDrag("FPS", mx, my, config.fpsX, config.fpsY);
            else if (hit(mx, my, config.coordsX, config.coordsY, 150, 40, s)) startDrag("COORDS", mx, my, config.coordsX, config.coordsY);
            else {
                int mmX = config.minimapX < 0 ? (int)(width/s) + config.minimapX : config.minimapX;
                if (hit(mx, my, mmX, config.minimapY, config.minimapSize, config.minimapSize, s)) {
                    startDrag("MINIMAP", mx, my, mmX, config.minimapY);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean hit(int mx, int my, int x, int y, int w, int h, float scale) {
        int bx = (int)(x * scale);
        int by = (int)(y * scale);
        int bw = (int)(w * scale);
        int bh = (int)(h * scale);
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private void startDrag(String elem, int mx, int my, int ex, int ey) {
        isDragging = true;
        draggedElement = elem;
        dragStartX = mx;
        dragStartY = my;
        initialElemX = ex;
        initialElemY = ey;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && draggedElement != null) {
            float scale = config.hudScale;
            
            // Movement calculated in raw screen pixels, then converted back to HUD units
            int dx = (int)((mouseX - dragStartX) / scale);
            int dy = (int)((mouseY - dragStartY) / scale);
            
            int newX = initialElemX + dx;
            int newY = initialElemY + dy;
            
            if (config.hudEditorGridSnap) {
                newX = (newX / 10) * 10;
                newY = (newY / 10) * 10;
            }

            if (draggedElement.equals("FPS")) { config.fpsX = newX; config.fpsY = newY; }
            if (draggedElement.equals("COORDS")) { config.coordsX = newX; config.coordsY = newY; }
            if (draggedElement.equals("MINIMAP")) {
                int screenW = (int)(width / scale);
                if (newX > screenW / 2) config.minimapX = newX - screenW;
                else config.minimapX = newX;
                config.minimapY = newY;
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
