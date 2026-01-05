package com.lightcraft.client.gui;
import com.lightcraft.client.minimap.WaypointManager;
import com.lightcraft.config.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class WaypointScreen extends Screen {
    private final WaypointManager manager;
    private final BlockPos pos;
    private TextFieldWidget nameField;
    private int selectedColor = 0xFFFF0000;
    
    public WaypointScreen(ModConfig c, ConfigManager cm, WaypointManager wm, BlockPos p, boolean isNew) {
        super(Text.of("Add Waypoint"));
        this.manager = wm; this.pos = p;
    }
    
    @Override
    protected void init() {
        int x = width / 2 - 100;
        nameField = new TextFieldWidget(textRenderer, x, 60, 200, 20, Text.of("Name"));
        nameField.setText("Waypoint");
        addDrawableChild(nameField);
        
        addDrawableChild(ButtonWidget.builder(Text.of("Save"), b -> {
            String d = MinecraftClient.getInstance().world.getRegistryKey().getValue().toString();
            manager.addWaypoint(nameField.getText(), pos, selectedColor, d);
            close();
        }).dimensions(x, 100, 200, 20).build());
        
        addDrawableChild(ButtonWidget.builder(Text.of("Cancel"), b -> close()).dimensions(x, 130, 200, 20).build());
    }
    
    @Override
    public void render(DrawContext c, int mx, int my, float d) {
        // FIXED: Use simple dark background instead of renderBackground to avoid blur crash
        HudRenderer.fillSafe(c, 0, 0, width, height, 0xC0000000);
        
        String titleStr = title.getString();
        int titleW = textRenderer.getWidth(titleStr);
        HudRenderer.drawTextSafe(c, textRenderer, titleStr, width/2 - titleW/2, 20, 0xFFFFFFFF, true);
        
        String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        int coordsW = textRenderer.getWidth(coords);
        HudRenderer.drawTextSafe(c, textRenderer, coords, width/2 - coordsW/2, 45, 0xFFAAAAAA, true);
        
        super.render(c, mx, my, d);
    }
    
    @Override
    public void close() { MinecraftClient.getInstance().setScreen(null); }
}
