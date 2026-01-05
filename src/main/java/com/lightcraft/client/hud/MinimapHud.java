package com.lightcraft.client.hud;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    
    public MinimapHud(ModConfig config, WaypointManager wm) {
        this.config = config;
        this.waypointManager = wm;
    }
    
    public void render(DrawContext context, int w, int h, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;
        
        int size = config.minimapSize;
        int x = config.minimapX < 0 ? w + config.minimapX : config.minimapX;
        int y = config.minimapY;
        
        // Background
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF222222);
        HudRenderer.drawBorderSafe(context, x-1, y-1, size+2, size+2, 0xFF555555);
        
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        // Simple terrain representation (center is player)
        int cx = x + size/2;
        int cy = y + size/2;
        
        // Entities
        if (config.minimapShowEntities) {
            for (Entity e : client.world.getEntities()) {
                if (e == player) continue;
                double dx = (e.getX() - player.getX()) * config.minimapZoom;
                double dz = (e.getZ() - player.getZ()) * config.minimapZoom;
                
                if (config.minimapRotate) {
                    float rad = (float)Math.toRadians(-player.getYaw());
                    double rx = dx * Math.cos(rad) - dz * Math.sin(rad);
                    double rz = dx * Math.sin(rad) + dz * Math.cos(rad);
                    dx = rx; dz = rz;
                }
                
                if (Math.abs(dx) < size/2 && Math.abs(dz) < size/2) {
                    HudRenderer.fillSafe(context, cx + (int)dx - 1, cy + (int)dz - 1, cx + (int)dx + 1, cy + (int)dz + 1, 0xFFFF0000);
                }
            }
        }
        
        // Waypoints
        for (ModConfig.Waypoint wp : waypointManager.getWaypoints()) {
            if (!wp.enabled) continue;
            double dx = (wp.x - player.getX()) * config.minimapZoom;
            double dz = (wp.z - player.getZ()) * config.minimapZoom;
             if (config.minimapRotate) {
                float rad = (float)Math.toRadians(-player.getYaw());
                double rx = dx * Math.cos(rad) - dz * Math.sin(rad);
                double rz = dx * Math.sin(rad) + dz * Math.cos(rad);
                dx = rx; dz = rz;
            }
            if (Math.abs(dx) < size/2 && Math.abs(dz) < size/2) {
                HudRenderer.fillSafe(context, cx + (int)dx - 2, cy + (int)dz - 2, cx + (int)dx + 2, cy + (int)dz + 2, wp.color | 0xFF000000);
            }
        }
        
        // Player Arrow
        HudRenderer.fillSafe(context, cx - 1, cy - 2, cx + 2, cy + 2, 0xFFFFFFFF);
        HudRenderer.disableScissorSafe(context);
    }
}
