package com.lightcraft.client.hud;

import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    private int[][] colorCache;
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    
    public MinimapHud(ModConfig config, WaypointManager wm) {
        this.config = config;
        this.waypointManager = wm;
        this.colorCache = new int[128][128]; 
    }
    
    public void render(DrawContext context, int w, int h, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        
        int size = config.minimapSize;
        int x = config.minimapX < 0 ? w + config.minimapX : config.minimapX;
        int y = config.minimapY;
        int half = size / 2;
        
        // Update Scan
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        // Update every few blocks or frames
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || client.player.age % 20 == 0) {
            updateMapData(client.world, px, (int)player.getY(), pz);
            lastPlayerX = px; lastPlayerZ = pz;
        }

        // Draw Modern Border
        HudRenderer.fillSafe(context, x - 2, y - 2, x + size + 2, y + size + 2, 0xFF000000); // Black Outline
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF111111); // Dark BG
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(x + half, y + half, 0);
            
            float angle = config.minimapRotate ? player.getYaw() + 180.0f : 180.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            
            int zoom = Math.max(1, config.minimapZoom);
            int range = 60; 
            
            // Draw Pixels
            for (int rX = -range; rX < range; rX++) {
                for (int rZ = -range; rZ < range; rZ++) {
                    int cX = rX + 64;
                    int cZ = rZ + 64;
                    if (cX >= 0 && cX < 128 && cZ >= 0 && cZ < 128) {
                        int color = colorCache[cX][cZ];
                        if (color != 0) {
                            HudRenderer.fillSafe(context, rX * zoom, rZ * zoom, (rX + 1) * zoom, (rZ + 1) * zoom, color);
                        }
                    }
                }
            }
            
            // Waypoints (Dots on map)
            for (ModConfig.Waypoint wp : waypointManager.getWaypoints()) {
                if (!wp.enabled) continue;
                double dx = (wp.x - player.getX()) * zoom;
                double dz = (wp.z - player.getZ()) * zoom;
                if (Math.abs(dx) < half && Math.abs(dz) < half) {
                    HudRenderer.fillSafe(context, (int)dx - 2, (int)dz - 2, (int)dx + 2, (int)dz + 2, wp.color | 0xFF000000);
                }
            }

            matrices.pop();
        }
        
        // Player Marker (Green Dot)
        HudRenderer.fillSafe(context, x + half - 2, y + half - 2, x + half + 2, y + half + 2, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
        
        // N Indicator
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", x + half - 2, y + 4, 0xFFFFFFFF, true);
        }
    }
    
    private void updateMapData(World world, int px, int py, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                // FORCE SCAN DOWN from player + 10 height
                boolean found = false;
                for (int y = py + 20; y > -64; y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = world.getBlockState(pos);
                    
                    if (!state.isAir()) {
                        MapColor mapColor = state.getMapColor(world, pos);
                        if (mapColor != null) {
                            int color = mapColor.color;
                            if (color == 0) color = 0x7F7F7F; // Fallback gray
                            if (!state.getFluidState().isEmpty()) color = 0x4040FF; // Water fix
                            
                            // Brightness based on height relative to player
                            if (y < py) color = darken(color, 30);
                            else if (y > py) color = brighten(color, 20);
                            
                            colorCache[x+64][z+64] = color | 0xFF000000;
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) colorCache[x+64][z+64] = 0xFF000000; // Void is black
            }
        }
    }
    
    private int darken(int color, int amount) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((color >> 8) & 0xFF) - amount);
        int b = Math.max(0, (color & 0xFF) - amount);
        return (r << 16) | (g << 8) | b;
    }
    
    private int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (r << 16) | (g << 8) | b;
    }
}
