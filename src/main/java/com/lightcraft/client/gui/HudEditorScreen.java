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
    
    // Track click offset relative to element
    private int offX, offY;

    public HudEditorScreen(ModConfig config, ConfigManager manager, HudRenderer renderer) {
        super(Text.of("HUD Editor"));
        this.config = config;
        this.manager = manager;
        this.renderer = renderer;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        HudRenderer.fillSafe(context, 0, 0, width, height, 0xAA000000); // Dark BG
        
        if (config.hudEditorGridSnap) {
            int s = 10; // Grid size
            for (int x = 0; x < width; x+=s) HudRenderer.fillSafe(context, x, 0, x+1, height, 0x1FFFFFFF);
            for (int y = 0; y < height; y+=s) HudRenderer.fillSafe(context, 0, y, width, y+1, 0x1FFFFFFF);
        }

        renderer.setEditMode(true);
        renderer.render(context, delta);
        renderer.setEditMode(false);

        HudRenderer.drawTextSafe(context, textRenderer, "DRAG ELEMENTS TO MOVE", width/2 - 50, 10, 0xFF00FF00, true);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int)mouseX; int my = (int)mouseY;
            float scale = config.hudScale;
            
            // Check FPS
            if (check(mx, my, config.fpsX, config.fpsY, 60, 15, scale)) {
                startDrag("FPS", mx, my, config.fpsX, config.fpsY); return true;
            }
            // Check Coords
            if (check(mx, my, config.coordsX, config.coordsY, 150, 40, scale)) {
                startDrag("COORDS", mx, my, config.coordsX, config.coordsY); return true;
            }
            // Check Minimap
            int mmX = config.minimapX < 0 ? (int)(width/scale) + config.minimapX : config.minimapX;
            if (check(mx, my, mmX, config.minimapY, config.minimapSize, config.minimapSize, scale)) {
                startDrag("MINIMAP", mx, my, mmX, config.minimapY); return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    private boolean check(int mx, int my, int x, int y, int w, int h, float scale) {
        int bx = (int)(x * scale); int by = (int)(y * scale);
        int bw = (int)(w * scale); int bh = (int)(h * scale);
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private void startDrag(String elem, int mx, int my, int ex, int ey) {
        isDragging = true;
        draggedElement = elem;
        // Store offset from the top-left of the element
        offX = (int)(mx/config.hudScale) - ex;
        offY = (int)(my/config.hudScale) - ey;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging && draggedElement != null) {
            int nx = (int)(mouseX / config.hudScale) - offX;
            int ny = (int)(mouseY / config.hudScale) - offY;
            
            // Snap
            if (config.hudEditorGridSnap) {
                nx = (nx / 10) * 10;
                ny = (ny / 10) * 10;
            }

            switch(draggedElement) {
                case "FPS" -> { config.fpsX = nx; config.fpsY = ny; }
                case "COORDS" -> { config.coordsX = nx; config.coordsY = ny; }
                case "MINIMAP" -> {
                    int sw = (int)(width / config.hudScale);
                    if (nx > sw/2) config.minimapX = nx - sw; else config.minimapX = nx;
                    config.minimapY = ny;
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
