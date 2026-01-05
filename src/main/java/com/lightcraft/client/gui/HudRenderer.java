package com.lightcraft.client.gui;

import com.lightcraft.client.hud.*;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class HudRenderer {
    private final ModConfig config;
    private final FpsHud fpsHud;
    private final CoordsHud coordsHud;
    private final MinimapHud minimapHud;
    private boolean editMode = false;
    
    // Cache the accessor to avoid performance hit
    private static Method cachedMatrixMethod = null;
    private static Field cachedMatrixField = null;
    private static boolean reflectionFailed = false;
    
    public HudRenderer(ModConfig config, WaypointManager waypointManager) {
        this.config = config;
        this.fpsHud = new FpsHud(config);
        this.coordsHud = new CoordsHud(config);
        this.minimapHud = new MinimapHud(config, waypointManager);
    }
    
    public void render(DrawContext context, float tickDelta) {
        if (!config.hudEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        // Safely get matrices via reflection to bypass version mismatches
        MatrixStack matrices = getMatricesSafe(context);
        
        if (matrices != null) {
            matrices.push();
            matrices.scale(config.hudScale, config.hudScale, 1.0f);
        }
        
        int sW = (int)(width / config.hudScale);
        int sH = (int)(height / config.hudScale);
        
        try {
            if (config.fpsEnabled) fpsHud.render(context, sW, sH);
            if (config.coordsEnabled) coordsHud.render(context, sW, sH);
            if (config.minimapEnabled) minimapHud.render(context, sW, sH, tickDelta);
            
            if (editMode) {
                 context.drawBorder(config.fpsX-2, config.fpsY-2, 60, 16, 0x80FFFFFF);
                 context.drawBorder(config.coordsX-2, config.coordsY-2, 150, 40, 0x80FFFFFF);
            }
        } catch (Exception e) {
            // Suppress rendering errors to prevent crash
        }
        
        if (matrices != null) {
            matrices.pop();
        }
    }
    
    private MatrixStack getMatricesSafe(DrawContext context) {
        if (reflectionFailed) return null;

        try {
            // 1. Try Cached Method
            if (cachedMatrixMethod != null) {
                return (MatrixStack) cachedMatrixMethod.invoke(context);
            }
            // 2. Try Cached Field
            if (cachedMatrixField != null) {
                return (MatrixStack) cachedMatrixField.get(context);
            }

            // 3. Initial Lookup: Search for a method returning MatrixStack
            for (Method m : DrawContext.class.getMethods()) {
                if (m.getReturnType() == MatrixStack.class && m.getParameterCount() == 0) {
                    cachedMatrixMethod = m;
                    return (MatrixStack) m.invoke(context);
                }
            }
            
            // 4. Fallback: Search for a field of type MatrixStack
            for (Field f : DrawContext.class.getDeclaredFields()) {
                if (f.getType() == MatrixStack.class) {
                    f.setAccessible(true);
                    cachedMatrixField = f;
                    return (MatrixStack) f.get(context);
                }
            }
            
        } catch (Exception e) {
            reflectionFailed = true; // Stop trying if it fails significantly
        }
        
        return null;
    }
    
    public void tick() { fpsHud.tick(); }
    public void setEditMode(boolean mode) { this.editMode = mode; }
}
