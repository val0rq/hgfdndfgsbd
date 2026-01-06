package com.lightcraft.client.hud;

import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.Blocks;
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
    
    private int[][] colorCache;
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    private int lastPlayerY = Integer.MAX_VALUE;
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
        
        // 1. Update Map Data (Check distance or time)
        int px = (int) player.getX();
        int py = (int) player.getY();
        int pz = (int) player.getZ();
        long now = System.currentTimeMillis();
        
        // Update more frequently to ensure chunks load in
        if (Math.abs(px - lastPlayerX) > 1 || Math.abs(pz - lastPlayerZ) > 1 || Math.abs(py - lastPlayerY) > 2 || now - lastUpdate > 200) {
            updateMapData(client.world, px, py, pz);
            lastPlayerX = px; lastPlayerZ = pz; lastPlayerY = py; lastUpdate = now;
        }

        // 2. Draw Background
        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        // 3. Matrix Rotation
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
            int renderRadius = (halfSize / zoom) + 2;
            
            // Limit render radius to cache size (64)
            if (renderRadius > 60) renderRadius = 60;
            
            // 4. Draw Terrain
            for (int rX = -renderRadius; rX < renderRadius; rX++) {
                for (int rZ = -renderRadius; rZ < renderRadius; rZ++) {
                    int cX = rX + 64; // Cache center offset
                    int cZ = rZ + 64;
                    
                    if (cX >= 0 && cX < 128 && cZ >= 0 && cZ < 128) {
                        int color = colorCache[cX][cZ];
                        if (color != 0) {
                            // Expand pixel by zoom level
                            HudRenderer.fillSafe(context, rX * zoom, rZ * zoom, (rX + 1) * zoom, (rZ + 1) * zoom, color);
                        }
                    }
                }
            }
            
            // 5. Draw Entities
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double edx = (player.getX() - e.getX()) * zoom;
                    double edz = (player.getZ() - e.getZ()) * zoom;
                    
                    // Simple distance check
                    if (Math.sqrt(edx*edx + edz*edz) < halfSize) {
                        int entColor = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                        HudRenderer.fillSafe(context, (int)edx - 1, (int)edz - 1, (int)edx + 1, (int)edz + 1, entColor);
                    }
                }
            }
            
            matrices.pop();
        }
        
        // 6. Draw Player Marker
        HudRenderer.fillSafe(context, cx - 1, cy - 1, cx + 1, cy + 1, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
        
        // 7. Draw N Direction
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", cx - 2, y + 2, 0xFFFFFFFF, true);
        }
    }
    
    private void updateMapData(World world, int px, int py, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int range = 64; 
        
        // Safety bounds
        int worldBottom = world.getBottomY();
        
        for (int x = -range; x < range; x++) {
            for (int z = -range; z < range; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                // Scan Start Height: Player Eyes + 20 blocks up (catch trees/hills)
                int startY = py + 20;
                // Don't go below world bottom
                int endY = Math.max(worldBottom, py - 30);
                
                int color = 0xFF000000; // Default black
                boolean found = false;

                for (int y = startY; y >= endY; y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = world.getBlockState(pos);
                    
                    // FIXED: Removed strict fluid check, now renders water/lava surfaces
                    if (!state.isAir()) {
                        MapColor mapColor = state.getMapColor(world, pos);
                        
                        if (mapColor != null) {
                            // Raw color int
                            int rawColor = mapColor.color;
                            
                            // If water, use blueish
                            if (state.getBlock() == Blocks.WATER) rawColor = 0x4040FF;
                            // If lava, use orange
                            if (state.getBlock() == Blocks.LAVA) rawColor = 0xFF8000;
                            // If grass/foliage, standard map color is usually fine
                            
                            if (rawColor == 0) rawColor = 0x7F7F7F; // Fallback grey if map color missing
                            
                            // Force Full Opacity
                            color = rawColor | 0xFF000000;
                            
                            // Depth Shading
                            if (y < py) color = darken(color, 40); // Below feet = darker
                            if (y > py) color = brighten(color, 30); // Above feet = lighter
                            
                            found = true;
                            break;
                        }
                    }
                }
                
                // If nothing found (void or sky), leave as 0 (transparent) or dark gray
                if (!found) {
                     colorCache[x + 64][z + 64] = 0; 
                } else {
                     colorCache[x + 64][z + 64] = color;
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
