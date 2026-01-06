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
        
        // Update check
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || client.player.age % 10 == 0) {
            updateMapData(client.world, px, (int)player.getY(), pz);
            lastPlayerX = px; lastPlayerZ = pz;
        }

        // Draw Background
        HudRenderer.fillSafe(context, x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF222222);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(x + size/2, y + size/2, 0);
            
            if (config.minimapRotate) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(player.getYaw() + 180.0f));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0f));
            }
            
            int zoom = Math.max(1, config.minimapZoom);
            int range = 40;
            
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
            
            // Entities
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double edx = (player.getX() - e.getX()) * zoom;
                    double edz = (player.getZ() - e.getZ()) * zoom;
                    if (Math.abs(edx) < size/2 && Math.abs(edz) < size/2) {
                         int c = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                         HudRenderer.fillSafe(context, (int)edx - 1, (int)edz - 1, (int)edx + 1, (int)edz + 1, c);
                    }
                }
            }
            matrices.pop();
        }
        
        HudRenderer.fillSafe(context, x + size/2 - 1, y + size/2 - 1, x + size/2 + 1, y + size/2 + 1, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
    }
    
    private void updateMapData(World world, int px, int py, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        
        for (int x = -64; x < 64; x++) {
            for (int z = -64; z < 64; z++) {
                int worldX = px + x;
                int worldZ = pz + z;
                
                // Use OCEAN_FLOOR_WG to find top solid block (includes under water)
                int topY = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, worldX, worldZ);
                pos.set(worldX, topY, worldZ);
                
                BlockState state = world.getBlockState(pos);
                // If it's air at topY (unlikely for ocean floor), search down
                if (state.isAir()) {
                    for (int y = topY; y > -64; y--) {
                        pos.set(worldX, y, worldZ);
                        state = world.getBlockState(pos);
                        if (!state.isAir()) {
                            topY = y;
                            break;
                        }
                    }
