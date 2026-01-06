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
        
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        // Aggressive update to fix black screen
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0) {
            updateMapData(client.world, px, pz);
            lastPlayerX = px; lastPlayerZ = pz;
        }

        // Draw Border/BG
        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(x + size/2, y + size/2, 0);
            
            float angle = config.minimapRotate ? player.getYaw() + 180.0f : 180.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            
            int zoom = Math.max(1, config.minimapZoom);
            
            // Draw Terrain
            for (int rX = -40; rX < 40; rX++) {
                for (int rZ = -40; rZ < 40; rZ++) {
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
            
            // Waypoints on Minimap
            for (ModConfig.Waypoint wp : waypointManager.getWaypoints()) {
                if (!wp.enabled) continue;
                double dx = (wp.x - player.getX()) * zoom;
                double dz = (wp.z - player.getZ()) * zoom;
                if (Math.abs(dx) < size/2 && Math.abs(dz) < size/2) {
                    HudRenderer.fillSafe(context, (int)dx - 2, (int)dz - 2, (int)dx + 2, (int)dz + 2, wp.color | 0xFF000000);
                }
            }

            // Entities
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double edx = (player.getX() - e.getX()) * zoom; // Inverted logic for rotation
                    double edz = (player.getZ() - e.getZ()) * zoom;
                    if (Math.abs(edx) < size/2 && Math.abs(edz) < size/2) {
                         HudRenderer.fillSafe(context, (int)edx - 1, (int)edz - 1, (int)edx + 1, (int)edz + 1, 0xFFFF0000);
                    }
                }
            }
            matrices.pop();
        }
        
        HudRenderer.fillSafe(context, x + size/2 - 1, y + size/2 - 1, x + size/2 + 1, y + size/2 + 1, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
        
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", x + size/2 - 2, y + 2, 0xFFFFFFFF, true);
        }
    }
    
    private void updateMapData(World world, int px, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                // FIXED: Use WORLD_SURFACE to guarantee we hit ground/water
                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                pos.set(worldX, surfaceY - 1, worldZ);
                
                BlockState state = world.getBlockState(pos);
                MapColor mapColor = state.getMapColor(world, pos);
                
                int color = (mapColor != null) ? mapColor.color : 0x7F7F7F;
                if (color == 0) color = 0x7F7F7F; // Fallback gray
                
                // Color override for water/lava if map color fails
                if (!state.getFluidState().isEmpty()) color = 0x4040FF;
                
                // Height shading
                int pY = (int) MinecraftClient.getInstance().player.getY();
                if (surfaceY < pY) color = darken(color, 20);
                if (surfaceY > pY) color = brighten(color, 20);
                
                colorCache[x + 64][z + 64] = color | 0xFF000000;
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
