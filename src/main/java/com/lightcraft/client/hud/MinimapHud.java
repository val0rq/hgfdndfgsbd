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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    
    // Performance Caching
    private int[][] colorCache;
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    private int lastPlayerY = Integer.MAX_VALUE;
    private long lastUpdate = 0;
    
    public MinimapHud(ModConfig config, WaypointManager wm) {
        this.config = config;
        this.waypointManager = wm;
        // Initialize cache (64x64 is a good balance for FPS)
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
        
        // 1. Update Map Data (Only if moved or every 10 ticks)
        // This saves massive FPS by not scanning blocks every frame
        int px = (int) player.getX();
        int py = (int) player.getY();
        int pz = (int) player.getZ();
        long now = System.currentTimeMillis();
        
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || Math.abs(py - lastPlayerY) > 2 || now - lastUpdate > 500) {
            updateMapData(client.world, px, py, pz);
            lastPlayerX = px;
            lastPlayerZ = pz;
            lastPlayerY = py;
            lastUpdate = now;
        }

        // 2. Draw Background & Border
        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        // 3. Setup Matrix for Rotation (The "Big Bug" Fix)
        // Instead of calculating math for pixels, we rotate the whole screen context
        // We use reflectionsafe matrix access from HudRenderer if needed, 
        // but DrawContext usually exposes the stack directly or via our wrapper.
        
        // Save current state
        context.getMatrices().push();
        
        // Move to center of minimap
        context.getMatrices().translate(cx, cy, 0);
        
        // Rotate the world opposite to player
        if (config.minimapRotate) {
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(player.getYaw() + 180.0f));
        } else {
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
        }
        
        // Move back (but now we are in local map coordinates)
        // We draw the map centered at 0,0
        
        int renderRadius = 40; // Draw 40 blocks in each direction (80x80 total)
        int zoom = config.minimapZoom;
        
        // 4. Draw Cached Terrain (Fast Loop)
        for (int rX = -renderRadius; rX < renderRadius; rX++) {
            for (int rZ = -renderRadius; rZ < renderRadius; rZ++) {
                // Get color from cache (offset by 64 to find center of array)
                int cX = rX + 64;
                int cZ = rZ + 64;
                
                if (cX >= 0 && cX < 128 && cZ >= 0 && cZ < 128) {
                    int color = colorCache[cX][cZ];
                    if (color != 0) {
                        // Draw pixels. Because we rotated the matrix, these draw straight but appear rotated!
                        HudRenderer.fillSafe(context, rX * zoom, rZ * zoom, (rX + 1) * zoom, (rZ + 1) * zoom, color);
                    }
                }
            }
        }
        
        // 5. Draw Entities (Rotated automatically by matrix)
        if (config.minimapShowEntities) {
            for (Entity e : client.world.getEntities()) {
                if (e == player) continue;
                // Relative position
                double edx = (player.getX() - e.getX()) * zoom; // Inverted because 180 rot
                double edz = (player.getZ() - e.getZ()) * zoom;
                
                if (Math.abs(edx) < halfSize && Math.abs(edz) < halfSize) {
                    int entColor = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                    int ex = (int)edx;
                    int ez = (int)edz;
                    HudRenderer.fillSafe(context, ex - 1, ez - 1, ex + 1, ez + 1, entColor);
                }
            }
        }
        
        // Restore matrix (undo rotation)
        context.getMatrices().pop();
        
        // 6. Draw Player (Always in center, on top of everything)
        // Static center marker
        HudRenderer.fillSafe(context, cx - 1, cy - 1, cx + 1, cy + 1, 0xFF00FF00); // Green dot
        if (!config.minimapRotate) {
             // If map doesn't rotate, draw an arrow pointing in player direction
             // (Simplified as a dot for stability in this version)
        }

        HudRenderer.disableScissorSafe(context);
        
        // 7. Cardinal Directions
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", cx - 2, y + 2, 0xFFFFFFFF, true);
        }
    }
    
    private void updateMapData(World world, int px, int py, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int range = 64; // Max cache range
        
        for (int x = -range; x < range; x++) {
            for (int z = -range; z < range; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                // Find surface
                int surfaceY = world.getTopY();
                // Simple fast scan
                for (int y = py + 20; y > world.getBottomY(); y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir() && state.getFluidState().isEmpty()) { // Skip water surface logic for speed
                        MapColor mapColor = state.getMapColor(world, pos);
                        if (mapColor != null) {
                            int color = mapColor.color | 0xFF000000;
                            
                            // SHADOW LOGIC: Compare with block to the "North" (z-1)
                            // We approximate by checking height relative to player
                            if (y < py) color = darken(color, 30);
                            if (y > py) color = brighten(color, 20);
                            
                            // Store in array (offset to positive indices)
                            colorCache[x + 64][z + 64] = color;
                            surfaceY = y;
                            break;
                        }
                    }
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
