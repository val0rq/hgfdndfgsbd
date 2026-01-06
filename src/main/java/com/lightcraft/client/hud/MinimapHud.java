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
import java.lang.reflect.Constructor;

public class MinimapHud {
    private final ModConfig config;
    private final WaypointManager waypointManager;
    
    // Texture Data
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
            // SAFE INITIALIZATION
            this.mapImage = new NativeImage(MAP_SIZE, MAP_SIZE, false);
            
            // Try standard constructor first
            try {
                this.mapTexture = new NativeImageBackedTexture(mapImage);
            } catch (Throwable t) {
                // Reflection Fallback for weird versions
                try {
                    Constructor<?>[] ctors = NativeImageBackedTexture.class.getConstructors();
                    for (Constructor<?> c : ctors) {
                        if (c.getParameterCount() == 1 && c.getParameterTypes()[0] == NativeImage.class) {
                            this.mapTexture = (NativeImageBackedTexture) c.newInstance(mapImage);
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("LightCraft: Minimap failed to init texture");
                }
            }

            if (this.mapTexture != null) {
                MinecraftClient client = MinecraftClient.getInstance();
                this.mapTextureId = client.getTextureManager().registerDynamicTexture("lightcraft_minimap", this.mapTexture);
                this.initialized = true;
            }
        } catch (Throwable t) {
            // Prevent launch crash if everything fails
            this.initialized = false;
        }
    }
    
    public void render(DrawContext context, int w, int h, float tickDelta) {
        if (!initialized) return; // Don't render if init failed
        
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
            
            int drawSize = MAP_SIZE;
            if (mapTextureId != null) {
                HudRenderer.drawTextureSafe(context, mapTextureId, -drawSize/2, -drawSize/2, 0, 0, drawSize, drawSize);
            }
            
            if (config.minimapShowEntities) {
                for (Entity e : client.world.getEntities()) {
                    if (e == player) continue;
                    double edx = (player.getX() - e.getX());
                    double edz = (player.getZ() - e.getZ());
                    
                    if (Math.abs(edx) < 60 && Math.abs(edz) < 60) {
                        int entColor = (e instanceof PlayerEntity) ? 0xFFFFFFFF : 0xFFFF0000;
                        int ex = (int)(edx);
                        int ez = (int)(edz);
                        HudRenderer.fillSafe(context, ex-1, ez-1, ex+1, ez+1, entColor);
                    }
                }
            }
            matrices.pop();
        }
        
        HudRenderer.fillSafe(context, x + halfSize - 2, y + halfSize - 2, x + halfSize + 2, y + halfSize + 2, 0xFF00FF00);
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
                
                // RAYCAST SCAN to find surface
                boolean found = false;
                for (int y = py + 20; y > -64; y--) {
                    pos.set(worldX, y, worldZ);
                    BlockState state = world.getBlockState(pos);
                    
                    if (!state.isAir()) {
                        MapColor mapColor = state.getMapColor(world, pos);
                        if (mapColor != null) {
                            int color = mapColor.color;
                            if (color == 0) color = 0x7F7F7F;
                            if (!state.getFluidState().isEmpty()) color = 0x4040FF;
                            
                            if (y < py) color = darken(color, 20);
                            else if (y > py) color = brighten(color, 20);
                            
                            int r = (color >> 16) & 0xFF;
                            int g = (color >> 8) & 0xFF;
                            int b = (color) & 0xFF;
                            int finalColor = 0xFF000000 | (b << 16) | (g << 8) | r;
                            
                            mapImage.setColor(x, z, finalColor);
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) mapImage.setColor(x, z, 0xFF000000);
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
