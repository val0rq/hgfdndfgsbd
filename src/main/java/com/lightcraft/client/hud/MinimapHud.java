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
    
    // Texture Data
    private NativeImage mapImage;
    private NativeImageBackedTexture mapTexture;
    private Identifier mapTextureId;
    private final int MAP_SIZE = 128; // Internal resolution
    
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    
    public MinimapHud(ModConfig config, WaypointManager wm) {
        this.config = config;
        this.waypointManager = wm;
        
        // Initialize Texture
        this.mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, false);
        this.mapTexture = new NativeImageBackedTexture(mapImage);
        
        // Register texture with a unique ID
        MinecraftClient client = MinecraftClient.getInstance();
        // Dynamic registration usually returns an Identifier or we provide a name
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
        
        // Update Logic (Chunk based)
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        // Only update if moved to save performance
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || client.player.age % 20 == 0) {
            updateTexture(client.world, px, (int)player.getY(), pz);
            lastPlayerX = px; lastPlayerZ = pz;
        }

        // Draw Border
        HudRenderer.fillSafe(context, x - 2, y - 2, x + size + 2, y + size + 2, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            
            // Move to center
            matrices.translate(x + halfSize, y + halfSize, 0);
            
            // Rotate Map
            float angle = config.minimapRotate ? player.getYaw() + 180.0f : 180.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            
            // Scale based on zoom
            float scale = (float)size / (float)(MAP_SIZE / Math.max(1, config.minimapZoom));
            matrices.scale(scale, scale, 1.0f);
            
            // Draw The Texture! (Centered at 0,0 because we translated)
            // Using DrawTextureSafe via reflection to bypass 1.21.11 changes
            int drawSize = MAP_SIZE;
            HudRenderer.drawTextureSafe(context, mapTextureId, -drawSize/2, -drawSize/2, 0, 0, drawSize, drawSize);
            
            // Draw Entities on top of map
            if (config.minimapShowEntities) {
                float entityScale = 1.0f / scale; // Keep dots consistent size
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    
                    double edx = (player.getX() - e.getX()); // Inverted
                    double edz = (player.getZ() - e.getZ());
                    
                    // Simple dot drawing relative to center
                    // We must scale coordinates to match map texture space
                    // Map is 128x128 pixels, representing 128x128 blocks (at zoom 1)
                    
                    // Check if within render range
                    if (Math.abs(edx) < 60 && Math.abs(edz) < 60) {
                        int entColor = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                        int ex = (int)(edx);
                        int ez = (int)(edz);
                        // Draw a 2x2 dot
                        HudRenderer.fillSafe(context, ex-1, ez-1, ex+1, ez+1, entColor);
                    }
                }
            }

            matrices.pop();
        }
        
        // Player Marker (Center Screen)
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
                // Calculate world position for this pixel
                int worldX = px + (x - offset);
                int worldZ = pz + (z - offset);
                
                // Get Heightmap Surface
                int surfaceY = world.getTopY(Heightmap.Type.WORLD_SURFACE, worldX, worldZ);
                pos.set(worldX, surfaceY - 1, worldZ);
                
                BlockState state = world.getBlockState(pos);
                MapColor mapColor = state.getMapColor(world, pos);
                
                int color = 0;
                
                // Try standard map color
                if (mapColor != null) {
                    color = mapColor.color;
                }
                
                // Fallback / Water handling
                if (color == 0) {
                    if (!state.getFluidState().isEmpty()) color = 0x4040FF; // Blue
                    else color = 0x000000; // Void
                }
                
                // If standard color is found, apply shading
                if (color != 0) {
                    // Shading logic
                    if (surfaceY < py) color = darken(color, 20);
                    else if (surfaceY > py) color = brighten(color, 20);
                    
                    // NativeImage expects ABGR (Alpha Blue Green Red) or ARGB depending on endianness
                    // But usually 0xAABBGGRR. We have 0xRRGGBB.
                    // Minecraft's ColorHelper handles this, but let's do manual bit shifting to be safe.
                    // NativeImage.setColor wants: Alpha (24-31), Blue (16-23), Green (8-15), Red (0-7)
                    
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = (color) & 0xFF;
                    
                    // Pack as ABGR (0xFFBBGGRR)
                    int finalColor = 0xFF000000 | (b << 16) | (g << 8) | r;
                    
                    mapImage.setColor(x, z, finalColor);
                } else {
                    mapImage.setColor(x, z, 0xFF000000); // Black
                }
            }
        }
        
        // Upload to GPU
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
