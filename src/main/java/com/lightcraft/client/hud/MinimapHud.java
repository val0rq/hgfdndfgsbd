package com.lightcraft.client.hud;

import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.client.gui.HudRenderer;
import com.lightcraft.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.Blocks;
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
import java.lang.reflect.Constructor;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    
    private NativeImage mapImage;
    private NativeImageBackedTexture mapTexture;
    private Identifier mapTextureId;
    private final int MAP_SIZE = 128;
    private boolean initialized = false;
    
    private int lastPlayerX = Integer.MAX_VALUE;
    private int lastPlayerZ = Integer.MAX_VALUE;
    
    public MinimapHud(ModConfig config, WaypointManager wm) {
        this.config = config;
        this.waypointManager = wm;
        
        try {
            this.mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, false);
            
            // CRASH FIX: Try to find valid constructor via reflection
            try {
                // Try standard first
                this.mapTexture = new NativeImageBackedTexture(mapImage);
            } catch (Throwable t1) {
                try {
                    // Try to find ANY constructor that takes NativeImage
                    for (Constructor<?> c : NativeImageBackedTexture.class.getConstructors()) {
                        if (c.getParameterCount() == 1 && c.getParameterTypes()[0] == NativeImage.class) {
                            this.mapTexture = (NativeImageBackedTexture) c.newInstance(mapImage);
                            break;
                        }
                    }
                } catch (Throwable t2) {
                    // Fail silently
                }
            }
            
            if (this.mapTexture != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                this.mapTextureId = client.getTextureManager().registerDynamicTexture("lightcraft_minimap", this.mapTexture);
                this.initialized = true;
            }
        } catch (Throwable t) {
            this.initialized = false;
        }
    }
    
    public void render(DrawContext context, int w, int h, float tickDelta) {
        if (!initialized) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        
        int size = config.minimapSize;
        int halfSize = size / 2;
        int x = config.minimapX < 0 ? w + config.minimapX : config.minimapX;
        int y = config.minimapY;
        
        int px = (int) player.getX();
        int pz = (int) player.getZ();
        
        if (Math.abs(px - lastPlayerX) > 0 || Math.abs(pz - lastPlayerZ) > 0 || client.player.age % 20 == 0) {
            updateTexture(client.world, px, (int)player.getY(), pz);
            lastPlayerX = px; lastPlayerZ = pz;
        }

        HudRenderer.fillSafe(context, x - 2, y - 2, x + size + 2, y + size + 2, config.minimapBorderColor);
        HudRenderer.fillSafe(context, x, y, x + size, y + size, 0xFF000000);
        HudRenderer.enableScissorSafe(context, x, y, x+size, y+size);
        
        MatrixStack matrices = HudRenderer.getMatricesSafe(context);
        if (matrices != null) {
            matrices.push();
            matrices.translate(x + halfSize, y + halfSize, 0);
            
            float angle = config.minimapRotate ? player.getYaw() + 180.0f : 180.0f;
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
            
            float scale = (float)size / (float)(MAP_SIZE / Math.max(1, config.minimapZoom));
            matrices.scale(scale, scale, 1.0f);
            
            if (mapTextureId != null) {
                HudRenderer.drawTextureSafe(context, mapTextureId, -MAP_SIZE/2, -MAP_SIZE/2, 0, 0, MAP_SIZE, MAP_SIZE);
            }
            
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double edx = (player.getX() - e.getX());
                    double edz = (player.getZ() - e.getZ());
                    if (Math.abs(edx) < 60 && Math.abs(edz) < 60) {
                        int c = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                        HudRenderer.fillSafe(context, (int)edx - 1, (int)edz - 1, (int)edx + 1, (int)edz + 1, c);
                    }
                }
            }
            matrices.pop();
        }
        
        HudRenderer.fillSafe(context, x + halfSize - 1, y + halfSize - 1, x + halfSize + 1, y + halfSize + 1, 0xFF00FF00);
        HudRenderer.disableScissorSafe(context);
        
        if (!config.minimapRotate) {
            HudRenderer.drawTextSafe(context, client.textRenderer, "N", x + halfSize - 2, y + 4, 0xFFFFFFFF, true);
        }
    }
    
    private void updateTexture(World world, int px, int py, int pz) {
        if (mapImage == null) return;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int offset = MAP_SIZE / 2;
        
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                int worldX = px + (x - offset);
                int worldZ = pz + (z - offset);
                
                int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING, worldX, worldZ);
                pos.set(worldX, topY - 1, worldZ);
                
                BlockState state = world.getBlockState(pos);
                int color = 0;
                
                if (!state.isAir()) {
                    // Manual Colors to ensure it works
                    if (state.getBlock() == Blocks.GRASS_BLOCK) color = 0x2E8B57;
                    else if (state.getBlock() == Blocks.WATER) color = 0x4040FF;
                    else if (state.getBlock() == Blocks.STONE) color = 0x707070;
                    else if (state.getBlock() == Blocks.SAND) color = 0xF0E68C;
                    else {
                        MapColor mapColor = state.getMapColor(world, pos);
                        if (mapColor != null) color = mapColor.color;
                    }
                    
                    if (color == 0) color = 0x555555;
                    
                    // Height shading
                    if (topY < py) color = darken(color, 20);
                    else if (topY > py) color = brighten(color, 20);
                    
                    int r = (color >> 16) & 0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = (color) & 0xFF;
                    int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                    
                    mapImage.setColor(x, z, abgr);
                } else {
                    mapImage.setColor(x, z, 0xFF000000);
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
