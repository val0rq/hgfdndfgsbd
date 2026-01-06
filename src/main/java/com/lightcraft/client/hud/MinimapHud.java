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
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    
    private int[][] colorCache;
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    private long lastUpdate = 0;
    
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
        int halfSize = size / 2;
        int x = config.minimapX < 0 ? w + config.minimapX : config.minimapX;
        int y = config.minimapY;
        int cx = x + halfSize;
        int cy = y + halfSize;
        
        // Update Data
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || System.currentTimeMillis() - lastUpdate > 250) {
            updateMapData(client.world, px, pz);
            lastPlayerX = px; lastPlayerZ = pz; lastUpdate = System.currentTimeMillis();
        }

        // Draw Border/BG
        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        // Matrix Rotation
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(cx, cy, 0);
            
            float angle = config.minimapRotate ? player.getYaw() + 180.0f : 180.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            
            int zoom = Math.max(1, config.minimapZoom);
            int renderRange = 60; // Cache limit
            
            for (int rX = -renderRange; rX < renderRange; rX++) {
                for (int rZ = -renderRange; rZ < renderRange; rZ++) {
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
            
            // Entities
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double edx = (player.getX() - e.getX()) * zoom;
                    double edz = (player.getZ() - e.getZ()) * zoom;
                    if (Math.sqrt(edx*edx + edz*edz) < halfSize) {
                         HudRenderer.fillSafe(context, (int)edx - 1, (int)edz - 1, (int)edx + 1, (int)edz + 1, 0xFFFF0000);
                    }
                }
            }
            matrices.pop();
        }
        
        HudRenderer.fillSafe(context, cx - 1, cy - 1, cx + 1, cy + 1, 0xFF00FF00); // Player
        HudRenderer.disableScissorSafe(context);
        
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", cx - 2, y + 2, 0xFFFFFFFF, true);
        }
    }
    
    private void updateMapData(World world, int px, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int range = 64;
        
        for (int x = -range; x < range; x++) {
            for (int z = -range; z < range; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                // Native surface height check - reliable!
                int surfaceY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, worldX, worldZ);
                pos.set(worldX, surfaceY - 1, worldZ);
                
                BlockState state = world.getBlockState(pos);
                MapColor mapColor = state.getMapColor(world, pos);
                
                if (mapColor != null) {
                    int color = mapColor.color;
                    
                    // Fallback for weird 1.21 map colors if they return 0
                    if (color == 0) {
                        // FIXED: Replaced getMaterial().isLiquid() with getFluidState check
                        if (!state.getFluidState().isEmpty()) color = 0x4040FF;
                        else color = 0x7F7F7F; 
                    }
                    
                    color = color | 0xFF000000; // Full alpha
                    
                    // Height shading
                    int pY = (int) MinecraftClient.getInstance().player.getY();
                    if (surfaceY < pY) color = darken(color, 30);
                    if (surfaceY > pY) color = brighten(color, 30);
                    
                    colorCache[x + 64][z + 64] = color;
                } else {
                    colorCache[x + 64][z + 64] = 0xFF222222; // Fallback
                }
            }
        }
    }
    
    private int darken(int color, int amount) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((color >> 8) & 0xFF) - amount);
        int b = Math.max(0, (color & 0xFF) - amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    private int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
