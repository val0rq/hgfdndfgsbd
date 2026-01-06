package com.lightcraft.mixin;

import com.lightcraft.client.LightCraftClient;
import com.lightcraft.config.ModConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    // Target: standard 1.21.1 signature (TickCounter, boolean, Camera, GameRenderer, LightmapTextureManager, Matrix4f, Matrix4f)
    @Inject(method = "render(Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", 
            at = @At("RETURN"), 
            require = 0) 
    private void onRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        renderWaypoints(camera, positionMatrix);
    }

    private void renderWaypoints(Camera camera, Matrix4f positionMatrix) {
        try {
            if (LightCraftClient.getInstance() == null) return;
            ModConfig config = LightCraftClient.getInstance().getConfig();
            if (!config.renderWaypointsInWorld) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;
            String dim = client.world.getRegistryKey().getValue().toString();

            // Setup Render State
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            
            Vec3d camPos = camera.getPos();
            double cx = camPos.x;
            double cy = camPos.y;
            double cz = camPos.z;

            for (ModConfig.Waypoint wp : config.waypoints) {
                if (!wp.enabled || !wp.dimension.equals(dim)) continue;

                float r = ((wp.color >> 16) & 0xFF) / 255f;
                float g = ((wp.color >> 8) & 0xFF) / 255f;
                float b = (wp.color & 0xFF) / 255f;
                float a = 1.0f;

                // Draw Vertical Beam
                buffer.vertex(positionMatrix, (float)(wp.x - cx + 0.5), (float)(-64 - cy), (float)(wp.z - cz + 0.5))
                      .color(r, g, b, a);
                      
                buffer.vertex(positionMatrix, (float)(wp.x - cx + 0.5), (float)(320 - cy), (float)(wp.z - cz + 0.5))
                      .color(r, g, b, a);
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
        } catch (Exception e) {
            // Prevent crash
        }
    }
}
