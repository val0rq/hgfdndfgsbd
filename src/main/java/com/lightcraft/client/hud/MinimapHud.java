package com.lightcraft.client.hud;

import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

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
        World world = client.world;
        if (player == null || world == null) return;
        
        int size = config.minimapSize;
        int x = config.minimapX < 0 ? w + config.minimapX : config.minimapX;
        int y = config.minimapY;
        
        // Draw Background
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF222222);
        HudRenderer.drawBorderSafe(context, x-1, y-1, size+2, size+2, config.minimapBorderColor);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        int cx = x + size/2;
        int cy = y + size/2;
        int px = (int) player.getX();
        int py = (int) player.getY();
        int pz = (int) player.getZ();
        float yaw = player.getYaw();
        
        // OPTIMIZATION: Step size 2 increases FPS significantly
        int step = 2; 
        int zoom = Math.max(1, config.minimapZoom);
        int radius = (size / 2) / zoom;
        
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        // Terrain Loop
        for (int dx = -radius; dx <= radius; dx += step) {
            for (int dz = -radius; dz <= radius; dz += step) {
                int worldX = px + dx;
                int worldZ = pz + dz;
                
                int color = 0xFF222222; 
                // Simple height check
                for (int hY = py + 10; hY > world.getBottomY(); hY--) {
                    pos.set(worldX, hY, worldZ);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        MapColor mapColor = state.getMapColor(world, pos);
                        if (mapColor != null) {
                            color = mapColor.color | 0xFF000000;
                        }
                        break;
                    }
                }
                
                int mapX = dx * zoom;
                int mapZ = dz * zoom;
                
                if (config.minimapRotate) {
                    float rad = (float) Math.toRadians(-yaw);
                    int rx = (int) (mapX * Math.cos(rad) - mapZ * Math.sin(rad));
                    int rz = (int) (mapX * Math.sin(rad) + mapZ * Math.cos(rad));
                    mapX = rx; mapZ = rz;
                }
                
                int pixelX = cx + mapX;
                int pixelY = cy + mapZ;
                
                // Draw 2x2 blocks for speed
                HudRenderer.fillSafe(context, pixelX, pixelY, pixelX + step*zoom, pixelY + step*zoom, color);
            }
        }
        
        // Entities
        if (config.minimapShowEntities) {
            for (Entity e : client.world.getEntities()) {
                if (e == player) continue;
                double edx = (e.getX() - player.getX()) * zoom;
                double edz = (e.getZ() - player.getZ()) * zoom;
                
                if (config.minimapRotate) {
                    float rad = (float) Math.toRadians(-yaw);
                    double rx = edx * Math.cos(rad) - edz * Math.sin(rad);
                    double rz = edx * Math.sin(rad) + edz * Math.cos(rad);
                    edx = rx; edz = rz;
                }
                
                int ex = cx + (int)edx;
                int ey = cy + (int)edz;
                
                // Only draw if within bounds
                if (ex > x && ex < x + size && ey > y && ey < y + size) {
                    int entColor = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                    HudRenderer.fillSafe(context, ex - 1, ey - 1, ex + 1, ey + 1, entColor);
                }
            }
        }
        
        // Player Arrow
        HudRenderer.fillSafe(context, cx - 1, cy - 2, cx + 2, cy + 2, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
    }
}
