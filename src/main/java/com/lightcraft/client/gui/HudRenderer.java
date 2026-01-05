package com.lightcraft.client.gui;

import com.lightcraft.client.hud.*;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HudRenderer {
    private final ModConfig config;
    private final FpsHud fpsHud;
    private final CoordsHud coordsHud;
    private final MinimapHud minimapHud;
    private boolean editMode = false;
    
    // Reflection Cache
    private static Method matrixMethod, drawTextMethod, fillMethod, borderMethod, scissorOnMethod, scissorOffMethod;
    private static Field matrixField;

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
                 drawBorderSafe(context, config.fpsX-2, config.fpsY-2, 60, 16, 0xFFFFFFFF);
                 drawBorderSafe(context, config.coordsX-2, config.coordsY-2, 150, 40, 0xFFFFFFFF);
            }
        } catch (Exception e) {
            // Prevent crash loop
        }
        
        if (matrices != null) {
            matrices.pop();
        }
    }
    
    // --- Safe Reflection Methods ---

    public static void drawTextSafe(DrawContext context, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        try {
            if (drawTextMethod == null) {
                for (Method m : DrawContext.class.getMethods()) {
                    // Try to match "drawText" or Intermediary "method_25303"
                    if (m.getName().equals("drawText") || m.getName().equals("method_25303") || 
                       (m.getParameterCount() == 6 && m.getParameterTypes()[1] == String.class && m.getParameterTypes()[5] == boolean.class)) {
                        drawTextMethod = m; break;
                    }
                }
            }
            if (drawTextMethod != null) drawTextMethod.invoke(context, tr, text, x, y, color, shadow);
        } catch (Exception e) {}
    }

    public static void fillSafe(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        try {
            if (fillMethod == null) {
                for (Method m : DrawContext.class.getMethods()) {
                    // Try to match "fill" or Intermediary "method_25294"
                    if (m.getName().equals("fill") || m.getName().equals("method_25294") ||
                       (m.getParameterCount() == 5 && m.getParameterTypes()[0] == int.class && m.getReturnType() == void.class && m.getName().length() < 10)) {
                        fillMethod = m; break;
                    }
                }
            }
            if (fillMethod != null) fillMethod.invoke(context, x1, y1, x2, y2, color);
        } catch (Exception e) {}
    }

    public static void drawBorderSafe(DrawContext context, int x, int y, int w, int h, int color) {
        try {
            if (borderMethod == null) {
                for (Method m : DrawContext.class.getMethods()) {
                    if (m.getName().equals("drawBorder") || m.getName().equals("method_51448") ||
                        (m.getParameterCount() == 5 && m.getName().toLowerCase().contains("border"))) {
                        borderMethod = m; break;
                    }
                }
            }
            if (borderMethod != null) borderMethod.invoke(context, x, y, w, h, color);
            else {
                // Manual fallback if method not found
                fillSafe(context, x, y, x + w, y + 1, color);
                fillSafe(context, x, y + h - 1, x + w, y + h, color);
                fillSafe(context, x, y + 1, x + 1, y + h - 1, color);
                fillSafe(context, x + w - 1, y + 1, x + w, y + h - 1, color);
            }
        } catch (Exception e) {}
    }

    public static void enableScissorSafe(DrawContext context, int x1, int y1, int x2, int y2) {
        try {
            if (scissorOnMethod == null) {
                for (Method m : DrawContext.class.getMethods()) {
                    if (m.getName().equals("enableScissor") || m.getName().equals("method_25298") ||
                       (m.getParameterCount() == 4 && m.getName().toLowerCase().contains("scissor"))) {
                        scissorOnMethod = m; break;
                    }
                }
            }
            if (scissorOnMethod != null) scissorOnMethod.invoke(context, x1, y1, x2, y2);
        } catch (Exception e) {}
    }

    public static void disableScissorSafe(DrawContext context) {
        try {
            if (scissorOffMethod == null) {
                for (Method m : DrawContext.class.getMethods()) {
                    if (m.getName().equals("disableScissor") || m.getName().equals("method_25299") ||
                       (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("scissor"))) {
                        scissorOffMethod = m; break;
                    }
                }
            }
            if (scissorOffMethod != null) scissorOffMethod.invoke(context);
        } catch (Exception e) {}
    }

    private MatrixStack getMatricesSafe(DrawContext context) {
        try {
            if (matrixMethod != null) return (MatrixStack) matrixMethod.invoke(context);
            if (matrixField != null) return (MatrixStack) matrixField.get(context);

            for (Method m : DrawContext.class.getMethods()) {
                if ((m.getName().equals("getMatrices") || m.getName().equals("method_51446")) && m.getParameterCount() == 0) {
                    matrixMethod = m; return (MatrixStack) m.invoke(context);
                }
            }
            // Fallback field search
            for (Field f : DrawContext.class.getDeclaredFields()) {
                if (f.getType() == MatrixStack.class) {
                    f.setAccessible(true); matrixField = f; return (MatrixStack) f.get(context);
                }
            }
        } catch (Exception e) {}
        return null;
    }
    
    public void tick() { fpsHud.tick(); }
    public void setEditMode(boolean mode) { this.editMode = mode; }
}
