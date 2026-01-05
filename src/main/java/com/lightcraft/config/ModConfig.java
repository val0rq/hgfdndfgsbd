package com.lightcraft.config;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public boolean hudEnabled = true;
    public float hudScale = 1.0f;
    
    public boolean fpsEnabled = true;
    public int fpsX = 5;
    public int fpsY = 5;
    public int fpsUpdateInterval = 500;
    
    public boolean coordsEnabled = true;
    public int coordsX = 5;
    public int coordsY = 20;
    public boolean showDirection = true;
    
    // Minimap Settings
    public boolean minimapEnabled = true;
    public int minimapX = -110;
    public int minimapY = 10;
    public int minimapSize = 100;
    public int minimapZoom = 1;
    public boolean minimapRotate = true;
    public boolean minimapShowEntities = true;
    public boolean minimapCircular = true;
    public int minimapBorderColor = 0xFF555555; // Added the missing field here
    
    public List<Waypoint> waypoints = new ArrayList<>();
    public boolean hudEditorGridSnap = true;
    public int hudEditorGridSize = 5;
    
    public static class Waypoint {
        public String name;
        public int x, y, z;
        public int color;
        public String dimension;
        public boolean enabled;
        
        public Waypoint() { enabled = true; color = 0xFFFF0000; }
        
        public Waypoint(String name, int x, int y, int z, int color, String dimension) {
            this.name = name; this.x = x; this.y = y; this.z = z;
            this.color = color; this.dimension = dimension; this.enabled = true;
        }
    }
}
