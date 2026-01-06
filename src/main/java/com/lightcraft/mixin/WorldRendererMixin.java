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

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        // This runs at the end of the world rendering frame
        renderWaypoints(tickCounter, camera);
    }

    private void renderWaypoints(RenderTickCounter tickCounter, Camera camera) {
        if (LightCraftClient.getInstance() == null) return;
        ModConfig config = LightCraftClient.getInstance().getConfig();
        if (!config.renderWaypointsInWorld) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        String dim = client.world.getRegistryKey().getValue().toString();

        Vec3d camPos = camera.getPos();
        Tessellator tessellator = Tessellator.getInstance();
        
        // Setup Render State
        RenderSystem.disableDepthTest(); // See through blocks
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.0f);

        try {
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            
            // We don't have a matrix stack passed in usually at RETURN, so we rely on the buffer's internal translation
            // But actually we need to offset vertices by -camPos
            
            for (ModConfig.Waypoint wp : config.waypoints) {
                if (!wp.enabled || !wp.dimension.equals(dim)) continue;

                float r = ((wp.color >> 16) & 0xFF) / 255f;
                float g = ((wp.color >> 8) & 0xFF) / 255f;
                float b = (wp.color & 0xFF) / 255f;
                float a = 1.0f;

                double x = wp.x - camPos.x + 0.5;
                double z = wp.z - camPos.z + 0.5;
                
                // Draw a vertical beam from -64 to 320 relative to camera
                buffer.vertex((float)x, (float)(-64 - camPos.y), (float)z).color(r, g, b, a);
                buffer.vertex((float)x, (float)(320 - camPos.y), (float)z).color(r, g, b, a);
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            
        } catch (Exception e) {
            // Rendering errors in mixins can crash the game, better to swallow them if version differs
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.lineWidth(1.0f);
    }
}
