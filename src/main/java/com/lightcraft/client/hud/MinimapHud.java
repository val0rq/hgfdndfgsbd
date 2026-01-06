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
        
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        // Only update if moved to save FPS
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || System.currentTimeMillis() - lastUpdate > 500) {
            updateMapData(client.world, px, (int)player.getY(), pz);
            lastPlayerX = px; lastPlayerZ = pz; lastUpdate = System.currentTimeMillis();
        }

        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(cx, cy, 0);
            
            if (config.minimapRotate) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(player.getYaw() + 180.0f));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
            }
            
            int zoom = Math.max(1, config.minimapZoom);
            int renderRange = 50; 
            
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
            matrices.pop();
        }
        
        HudRenderer.fillSafe(context, cx - 1, cy - 1, cx + 1, cy + 1, 0xFF00FF00); // Player
        HudRenderer.disableScissorSafe(context);
        
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", cx - 2, y + 2, 0xFFFFFFFF, true);
        }
    }
    
    private void updateMapData(World world, int px, int py, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                pos.set(worldX, topY - 1, worldZ);
                
                BlockState state = world.getBlockState(pos);
                MapColor mapColor = state.getMapColor(world, pos);
                int color = (mapColor != null) ? mapColor.color : 0x7F7F7F;
                
                if (!state.getFluidState().isEmpty()) color = 0x4040FF;
                if (color == 0) color = 0x555555;
                
                if (topY < py) color = darken(color, 20);
                if (topY > py) color = brighten(color, 20);
                
                colorCache[x + 64][z + 64] = color | 0xFF000000;
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
