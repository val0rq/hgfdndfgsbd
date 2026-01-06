package com.lightcraft.client.render;

import com.lightcraft.config.ModConfig;
import com.lightcraft.client.LightCraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import com.mojang.blaze3d.systems.RenderSystem;

public class WaypointWorldRenderer {
    
    public static void render(WorldRenderContext context) {
        ModConfig config = LightCraftClient.getInstance().getConfig();
        if (!config.renderWaypointsInWorld) return;
        
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        
        Camera camera = context.camera();
        Vec3d camPos = camera.getPos();
        
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        
        Tessellator tessellator = Tessellator.getInstance();
        // Use reflection-safe buffer builder if possible, but standard likely works for this
        // We will try standard first.
        try {
             BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
             MatrixStack matrices = context.matrixStack();
             matrices.push();
             matrices.translate(-camPos.x, -camPos.y, -camPos.z);
             Matrix4f mat = matrices.peek().getPositionMatrix();
             
             for (ModConfig.Waypoint wp : config.waypoints) {
                 if (!wp.enabled) continue;
                 // Simple Vertical Beam
                 float r = ((wp.color >> 16) & 0xFF) / 255f;
                 float g = ((wp.color >> 8) & 0xFF) / 255f;
                 float b = (wp.color & 0xFF) / 255f;
                 
                 // Draw line from y=0 to y=320
                 buffer.vertex(mat, wp.x + 0.5f, 0, wp.z + 0.5f).color(r, g, b, 0.5f);
                 buffer.vertex(mat, wp.x + 0.5f, 320, wp.z + 0.5f).color(r, g, b, 0.5f);
             }
             
             BufferRenderer.drawWithGlobalProgram(buffer.end());
             matrices.pop();
        } catch (Exception e) {
             // Fallback for 1.21 changes in BufferBuilder
        }
        
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }
}
