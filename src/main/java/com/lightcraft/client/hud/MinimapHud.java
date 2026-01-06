package com.lightcraft.client.hud;

import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    
    private NativeImage mapImage;
    private NativeImageBackedTexture mapTexture;
    private Identifier mapTextureId;
    private final int MAP_SIZE = 128;
    
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    
    public MinimapHud(ModConfig config, WaypointManager wm) {
        this.config = config;
        this.waypointManager = wm;
        
        // Init Texture
        this.mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, true);
        this.mapTexture = new NativeImageBackedTexture(mapImage);
        
        MinecraftClient client = MinecraftClient.getInstance();
        this.mapTextureId = client.getTextureManager().registerDynamicTexture("lightcraft_minimap", this.mapTexture);
    }
    
    public void render(DrawContext context, int w, int h, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        
        int size = config.minimapSize;
        int halfSize = size / 2;
        int x = config.minimapX < 0 ? w + config.minimapX : config.minimapX;
        int y = config.minimapY;
        
        // 1. Update Texture
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        // Update if moved or every 10 ticks
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || client.player.age % 10 == 0) {
            updateTexture(client.world, px, (int)player.getY(), pz);
            lastPlayerX = px; lastPlayerZ = pz;
        }

        // 2. Draw Frame
        HudRenderer.fillSafe(context, x - 2, y - 2, x + size + 2, y + size + 2, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        // 3. Draw Texture (Rotated)
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(x + halfSize, y + halfSize, 0);
            
            float angle = config.minimapRotate ? player.getYaw() + 180.0f : 180.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            
            // Calculate scale to fit MAP_SIZE into config.minimapSize with zoom
            float zoom = Math.max(1, config.minimapZoom);
            float scale = (float)size / (float)(MAP_SIZE / zoom);
            matrices.scale(scale, scale, 1.0f);
            
            // Draw the texture centered
            HudRenderer.drawTextureSafe(context, mapTextureId, -MAP_SIZE/2, -MAP_SIZE/2, 0, 0, MAP_SIZE, MAP_SIZE);
            
            // 4. Draw Waypoints on Map
            float invScale = 1.0f / scale;
            for (ModConfig.Waypoint wp : waypointManager.getWaypoints()) {
                if (!wp.enabled) continue;
                double dx = (wp.x - player.getX());
                double dz = (wp.z - player.getZ());
                // Only draw if within map bounds roughly
                if (Math.abs(dx) < 64*zoom && Math.abs(dz) < 64*zoom) {
                    HudRenderer.fillSafe(context, (int)dx - 2, (int)dz - 2, (int)dx + 2, (int)dz + 2, wp.color | 0xFF000000);
                }
            }
            
            // 5. Draw Entities
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double dx = (player.getX() - e.getX());
                    double dz = (player.getZ() - e.getZ());
                    
                    if (Math.abs(dx) < 64*zoom && Math.abs(dz) < 64*zoom) {
                        int c = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                        HudRenderer.fillSafe(context, (int)dx - 1, (int)dz - 1, (int)dx + 1, (int)dz + 1, c);
                    }
                }
            }
            
            matrices.pop();
        }
        
        // 6. Center Marker
        HudRenderer.fillSafe(context, x + halfSize - 2, y + halfSize - 2, x + halfSize + 2, y + halfSize + 2, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
        
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", x + halfSize - 2, y + 4, 0xFFFFFFFF, true);
        }
    }
    
    private void updateTexture(World world, int px, int py, int pz) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int offset = MAP_SIZE / 2;
        
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                int worldX = px + (x - offset);
                int worldZ = pz + (z - offset);
                
                // Use MOTION_BLOCKING to find trees/ground
                int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, worldX, worldZ);
                pos.set(worldX, topY - 1, worldZ);
                
                BlockState state = world.getBlockState(pos);
                // Check if we hit water/glass/etc and look down if needed
                if (!state.getFluidState().isEmpty()) {
                     // Keep liquid color
                }
                
                MapColor mapColor = state.getMapColor(world, pos);
                int color = (mapColor != null) ? mapColor.color : 0;
                
                if (color == 0) {
                    if (!state.getFluidState().isEmpty()) color = 0x4040FF;
                    else color = 0x000000;
                }
                
                if (color != 0) {
                    // Height shading
                    if (topY < py) color = darken(color, 20);
                    else if (topY > py) color = brighten(color, 20);
                    
                    // Convert integer RGB to ABGR for NativeImage
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = (color) & 0xFF;
                    int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                    
                    mapImage.setColor(x, z, abgr);
                } else {
                    mapImage.setColor(x, z, 0xFF000000); // Black for void
                }
            }
        }
        mapTexture.upload();
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
