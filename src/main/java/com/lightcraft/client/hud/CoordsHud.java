package com.lightcraft.client.hud;
import com.lightcraft.config.ModConfig;
import com.lightcraft.client.gui.HudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

public class CoordsHud {
    private final ModConfig config;
    public CoordsHud(ModConfig config) { this.config = config; }
    
    public void render(DrawContext context, int w, int h) {
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        
        int x = config.coordsX;
        int y = config.coordsY;
        HudRenderer.fillSafe(context, x - 2, y - 2, x + 150, y + (config.showDirection ? 24 : 12), 0x80000000);
        
        String coords = String.format("XYZ: %.1f / %.1f / %.1f", player.getX(), player.getY(), player.getZ());
        HudRenderer.drawTextSafe(context, MinecraftClient.getInstance().textRenderer, coords, x, y, 0xFFFFFFFF, true);
        
        if (config.showDirection) {
            String dir = getDirection(player.getYaw());
            HudRenderer.drawTextSafe(context, MinecraftClient.getInstance().textRenderer, "Facing: " + dir, x, y + 11, 0xFFAAAAAA, true);
        }
    }
    
    private String getDirection(float yaw) {
        yaw = MathHelper.wrapDegrees(yaw);
        if (yaw >= -45 && yaw < 45) return "South";
        if (yaw >= 45 && yaw < 135) return "West";
        if (yaw >= -135 && yaw < -45) return "East";
        return "North";
    }
}
