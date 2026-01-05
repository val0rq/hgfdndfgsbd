package com.lightcraft.client.minimap;
import com.lightcraft.config.ConfigManager;
import com.lightcraft.config.ModConfig;
import com.lightcraft.client.LightCraftClient;
import net.minecraft.util.math.BlockPos;
import java.util.List;

public class WaypointManager {
    private final ConfigManager configManager;
    private List<ModConfig.Waypoint> waypoints;
    
    public WaypointManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.waypoints = LightCraftClient.getInstance() != null ? 
            LightCraftClient.getInstance().getConfig().waypoints : new java.util.ArrayList<>();
    }
    
    public void addWaypoint(String name, BlockPos pos, int color, String dim) {
        waypoints.add(new ModConfig.Waypoint(name, pos.getX(), pos.getY(), pos.getZ(), color, dim));
        save();
    }
    
    public void removeWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.remove(index);
            save();
        }
    }
    
    public void toggleWaypoint(int index) {
        if (index >= 0 && index < waypoints.size()) {
            waypoints.get(index).enabled = !waypoints.get(index).enabled;
            save();
        }
    }
    
    private void save() {
        ModConfig config = LightCraftClient.getInstance().getConfig();
        config.waypoints = waypoints;
        configManager.saveConfig(config);
    }
    
    public List<ModConfig.Waypoint> getWaypoints() { return waypoints; }
}
