package com.jpreiss.easy_factions.client.integration;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.client.ClientConfig;
import com.jpreiss.easy_factions.client.data_store.ClientClaimCache;
import com.mojang.logging.LogUtils;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
public class JourneyMapCompat implements IClientPlugin {

    private static IClientAPI jmAPI = null;
    private static final Logger LOGGER = LogUtils.getLogger();

    // Tracking for active overlays per dimension, reduces computation cost when removing them
    private static final Map<ResourceLocation, List<PolygonOverlay>> activeOverlays = new HashMap<>();

    @Override
    public void initialize(IClientAPI journeyMapAPI) {
        jmAPI = journeyMapAPI;
    }

    @Override
    public String getModId() {
        return EasyFactions.MODID;
    }

    @Override
    public void onEvent(ClientEvent clientEvent) {
    }

    /**
     * Recalculates and merges all claimed chunks in a dimension.
     *
     * @param dim The dimension to rebuild
     */
    public static void rebuildDimension(ResourceKey<Level> dim) {
        if (jmAPI == null) return;
        ResourceLocation dimLoc = dim.location();

        // Remove old overlays for this dimension
        List<PolygonOverlay> existing = activeOverlays.remove(dimLoc);
        if (existing != null) {
            for (PolygonOverlay overlay : existing) {
                jmAPI.remove(overlay);
            }
        }

        Map<Long, Integer> dimClaims = ClientClaimCache.getClaims().get(dimLoc);
        if (dimClaims == null || dimClaims.isEmpty()) return;

        // Group chunks by color and grid size
        Map<String, List<ChunkPos>> chunksByGroup = new HashMap<>();
        int gridSize = ClientConfig.claimMergeGridSize;

        for (Map.Entry<Long, Integer> entry : dimClaims.entrySet()) {
            ChunkPos pos = new ChunkPos(entry.getKey());
            int color = entry.getValue();

            // Divide the world into chunk grids based on the config.
            int gridX = Math.floorDiv(pos.x, gridSize);
            int gridZ = Math.floorDiv(pos.z, gridSize);

            // Create a unique key for the group to prevent them from overriding each other: "color_gridX_gridZ"
            String groupKey = color + "_" + gridX + "_" + gridZ;
            chunksByGroup.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(pos);
        }

        List<PolygonOverlay> newOverlays = new ArrayList<>();

        // Merge chunks into grid areas and extract polygons for each
        for (Map.Entry<String, List<ChunkPos>> entry : chunksByGroup.entrySet()) {
            String groupKey = entry.getKey(); // Grab the unique grid key
            int color = Integer.parseInt(groupKey.split("_")[0]);
            Area mergedArea = new Area();

            // Feed all chunk boundaries into the AWT Area
            for (ChunkPos pos : entry.getValue()) {
                mergedArea.add(new Area(new Rectangle(pos.getMinBlockX(), pos.getMinBlockZ(), 16, 16)));
            }

            extractAndSubmitPolygons(dim, color, groupKey, mergedArea, newOverlays);
        }

        // Cache the new optimized overlays
        activeOverlays.put(dimLoc, newOverlays);
    }

    /**
     * Extracts vertices from the merged AWT Area and sends them to JourneyMap.
     */
    private static void extractAndSubmitPolygons(ResourceKey<Level> dim, int color, String groupKey, Area area, List<PolygonOverlay> overlayTracker) {
        PathIterator pi = area.getPathIterator(null);

        List<BlockPos> currentPoints = new ArrayList<>();
        List<MapPolygon> currentHoles = new ArrayList<>();
        MapPolygon currentOuter = null;

        int shapeIndex = 0;

        while (!pi.isDone()) {
            double[] coords = new double[6];
            int type = pi.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    currentPoints.add(new BlockPos((int) coords[0], 100, (int) coords[1]));
                    break;
                case PathIterator.SEG_CLOSE:
                    if (!currentPoints.isEmpty()) {
                        boolean hole = isHole(currentPoints);

                        if (!hole) {
                            // If we already have a loaded outer polygon, submit it before starting the next distinct one
                            if (currentOuter != null) {
                                submitOverlay(dim, color, groupKey, currentOuter, currentHoles, shapeIndex++, overlayTracker);
                                currentHoles.clear();
                            }
                            currentOuter = new MapPolygon(currentPoints);
                        } else {
                            currentHoles.add(new MapPolygon(currentPoints));
                        }
                        currentPoints = new ArrayList<>();
                    }
                    break;
            }
            pi.next();
        }

        // Submit the shape
        if (currentOuter != null) {
            submitOverlay(dim, color, groupKey, currentOuter, currentHoles, shapeIndex, overlayTracker);
        }
    }

    /**
     * Submits a MapPolygon to JourneyMap.
     */
    private static void submitOverlay(ResourceKey<Level> dim, int color, String groupKey, MapPolygon outer, List<MapPolygon> holes, int index, List<PolygonOverlay> overlayTracker) {
        String displayId = "faction_merge_" + groupKey + "_" + index;

        ShapeProperties properties = new ShapeProperties().setStrokeColor(color).setStrokeOpacity(ClientConfig.chunkBorderOpacity).setStrokeWidth(ClientConfig.chunkBorderWidth).setFillColor(color).setFillOpacity(ClientConfig.chunkOverlayOpacity);

        PolygonOverlay overlay;
        if (holes.isEmpty()) {
            overlay = new PolygonOverlay(EasyFactions.MODID, displayId, dim, properties, outer);
        } else {
            overlay = new PolygonOverlay(EasyFactions.MODID, displayId, dim, properties, outer, holes);
        }

        try {
            jmAPI.show(overlay);
            overlayTracker.add(overlay);
        } catch (Exception e) {
            LOGGER.atError().log(e.getMessage());
        }
    }

    /**
     * Mathematical helper to determine winding order (Outer Boundary vs Internal Hole).
     * Note: Due to Minecraft's X/Z coordinate grid, the standard AWT winding math is inverted.
     */
    private static boolean isHole(List<BlockPos> vertices) {
        double sum = 0;
        for (int i = 0; i < vertices.size(); i++) {
            BlockPos p1 = vertices.get(i);
            BlockPos p2 = vertices.get((i + 1) % vertices.size());
            sum += (p2.getX() - p1.getX()) * (p2.getZ() + p1.getZ());
        }
        // Flipped operator: AWT Area outer boundaries evaluate to sum > 0, holes evaluate to sum < 0 (thanks for this, AI)
        return sum < 0;
    }
}