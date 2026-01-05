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
        
        // Draw Border & Background
        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        // Draw Terrain
        int radius = size / 2 / config.minimapZoom;
        int cx = x + size/2;
        int cy = y + size/2;
        int px = (int) player.getX();
        int py = (int) player.getY();
        int pz = (int) player.getZ();
        float yaw = player.getYaw();
        
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        // Scan area
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int worldX = px + dx;
                int worldZ = pz + dz;
                
                // Find top block
                int color = 0xFF222222; // default
                for (int hY = py + 10; hY > world.getBottomY(); hY--) {
                    pos.set(worldX, hY, worldZ);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty()) {
                        MapColor mapColor = state.getMapColor(world, pos);
                        if (mapColor != null) {
                            color = mapColor.color | 0xFF000000;
                            // Simple height shading
                            if (hY > py) color = brighten(color, 20);
                            if (hY < py) color = darken(color, 20);
                        }
                        break;
                    }
                }
                
                // Project to map
                int mapX = dx * config.minimapZoom;
                int mapZ = dz * config.minimapZoom;
                
                if (config.minimapRotate) {
                    float rad = (float) Math.toRadians(-yaw);
                    int rx = (int) (mapX * Math.cos(rad) - mapZ * Math.sin(rad));
                    int rz = (int) (mapX * Math.sin(rad) + mapZ * Math.cos(rad));
                    mapX = rx; mapZ = rz;
                }
                
                // Draw pixel (zoomed)
                int pixelX = cx + mapX;
                int pixelY = cy + mapZ;
                int zoom = config.minimapZoom;
                
                if (pixelX >= x && pixelX < x + size && pixelY >= y && pixelY < y + size) {
                    HudRenderer.fillSafe(context, pixelX, pixelY, pixelX + zoom, pixelY + zoom, color);
                }
            }
        }
        
        // Entities
        if (config.minimapShowEntities) {
            for (Entity e : client.world.getEntities()) {
                if (e == player) continue;
                double edx = (e.getX() - player.getX()) * config.minimapZoom;
                double edz = (e.getZ() - player.getZ()) * config.minimapZoom;
                
                if (config.minimapRotate) {
                    float rad = (float) Math.toRadians(-yaw);
                    double rx = edx * Math.cos(rad) - edz * Math.sin(rad);
                    double rz = edx * Math.sin(rad) + edz * Math.cos(rad);
                    edx = rx; edz = rz;
                }
                
                int ex = cx + (int)edx;
                int ey = cy + (int)edz;
                
                if (ex > x && ex < x + size && ey > y && ey < y + size) {
                    int entColor = 0xFFFF0000; // Red for mobs
                    if (e instanceof PlayerEntity) entColor = 0xFFFFFFFF; // White for players
                    HudRenderer.fillSafe(context, ex - 1, ey - 1, ex + 2, ey + 2, entColor);
                }
            }
        }
        
        // Player Arrow (Center)
        HudRenderer.fillSafe(context, cx - 1, cy - 2, cx + 2, cy + 2, 0xFF00FF00); // Green arrow
        
        HudRenderer.disableScissorSafe(context);
        
        // Cardinal Directions Overlay
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", cx - 2, y + 2, 0xFFFFFFFF, true);
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
