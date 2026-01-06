package com.lightcraft.client.gui;

import com.lightcraft.client.hud.*;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class HudRenderer {
    private final ModConfig config;
    private final FpsHud fpsHud;
    private final CoordsHud coordsHud;
    private final MinimapHud minimapHud;
    private final WaypointManager waypointManager;
    private boolean editMode = false;
    
    private static Method matrixMethod, drawTextMethod, fillMethod, borderMethod, scissorOnMethod, scissorOffMethod;
    private static Field matrixField;
    private static Field xField, yField, zField;

    public HudRenderer(ModConfig config, WaypointManager waypointManager) {
        this.config = config;
        this.waypointManager = waypointManager;
        this.fpsHud = new FpsHud(config);
        this.coordsHud = new CoordsHud(config);
        this.minimapHud = new MinimapHud(config, waypointManager);
    }
    
    public void render(DrawContext context, float tickDelta) {
        if (!config.hudEnabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        // 1. Waypoints
        try {
            if (config.renderWaypointsInWorld && client.player != null) {
                renderWaypoints2D(context, client, width, height, tickDelta);
            }
        } catch (Throwable t) {}

        // 2. HUD
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
        } catch (Throwable t) {}
        
        if (matrices != null) matrices.pop();
    }

    private void renderWaypoints2D(DrawContext context, MinecraftClient client, int w, int h, float tickDelta) {
        Entity cam = client.getCameraEntity();
        if (cam == null) cam = client.player;
        
        // Manual Camera Position
        double cx = MathHelper.lerp(tickDelta, cam.prevX, cam.getX());
        double cy = MathHelper.lerp(tickDelta, cam.prevY, cam.getY()) + cam.getStandingEyeHeight();
        double cz = MathHelper.lerp(tickDelta, cam.prevZ, cam.getZ());
        
        // Camera Rotation (Radians)
        float yaw = (float) Math.toRadians(cam.getYaw(tickDelta));
        float pitch = (float) Math.toRadians(cam.getPitch(tickDelta));

        String dim = client.world.getRegistryKey().getValue().toString();

        for (ModConfig.Waypoint wp : waypointManager.getWaypoints()) {
            if (!wp.enabled || !wp.dimension.equals(dim)) continue;

            // Relative Position
            double dx = wp.x - cx + 0.5;
            double dy = wp.y - cy + 1.0; // Above block
            double dz = wp.z - cz + 0.5;
            
            // 3D Rotation Math to find Screen Position
            // Rotate Yaw
            double x1 = dx * Math.cos(yaw) + dz * Math.sin(yaw);
            double z1 = dz * Math.cos(yaw) - dx * Math.sin(yaw);
            // Rotate Pitch
            double y2 = dy * Math.cos(pitch) + z1 * Math.sin(pitch);
            double z2 = z1 * Math.cos(pitch) - dy * Math.sin(pitch);

            // Z > 0 means the point is IN FRONT of the camera
            if (z2 > 0) {
                // Projection Scale (FOV approx 90ish, factor ~ 1.0)
                double scale = (double)h / (2.0 * z2); 
                
                int screenX = (int) (w / 2.0 - x1 * scale);
                int screenY = (int) (h / 2.0 - y2 * scale);
                
                // Keep on screen? No, strictly projection.
                // Distance Text
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                String label = wp.name + " (" + (int)dist + "m)";
                int textW = client.textRenderer.getWidth(label);
                
                // Draw floating text
                if (dist < 5000) {
                    fillSafe(context, screenX - textW/2 - 2, screenY - 2, screenX + textW/2 + 2, screenY + 10, 0x88000000);
                    drawTextSafe(context, client.textRenderer, label, screenX - textW/2, screenY, wp.color, true);
                }
            }
        }
    }
    
    // Safe Reflection Methods remain same as previous step...
    // (Ensure drawTextSafe, fillSafe, drawBorderSafe, enableScissorSafe, disableScissorSafe, getMatricesSafe are present)
    
    public static void drawTextSafe(DrawContext context, TextRenderer tr, String text, int x, int y, int color, boolean shadow) {
        try {
            if (drawTextMethod == null) {
                for (Method m : DrawContext.class.getMethods()) {
                    if (m.getName().equals("drawText") || m.getName().equals("method_25303") || 
                       (m.getParameterCount() == 6 && m.getParameterTypes()[1] == String.class)) {
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
                    if (m.getName().equals("fill") || m.getName().equals("method_25294") ||
                       (m.getParameterCount() == 5 && m.getParameterTypes()[0] == int.class && m.getReturnType() == void.class)) {
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

    public static MatrixStack getMatricesSafe(DrawContext context) {
        try {
            if (matrixMethod != null) return (MatrixStack) matrixMethod.invoke(context);
            if (matrixField != null) return (MatrixStack) matrixField.get(context);

            for (Method m : DrawContext.class.getMethods()) {
                if ((m.getName().equals("getMatrices") || m.getName().equals("method_51446")) && m.getParameterCount() == 0) {
                    matrixMethod = m; return (MatrixStack) m.invoke(context);
                }
            }
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
