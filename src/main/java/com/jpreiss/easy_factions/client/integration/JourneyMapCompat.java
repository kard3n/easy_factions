package com.jpreiss.easy_factions.client.integration;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.client.ClientConfig;
import com.mojang.logging.LogUtils;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.util.PolygonHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@journeymap.client.api.ClientPlugin
public class JourneyMapCompat implements IClientPlugin {

    private static IClientAPI jmAPI = null;
    private static final Logger LOGGER = LogUtils.getLogger();

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
     * Highlights a specific chunk on the JourneyMap with a specific color.
     *
     * @param level    The dimension of the chunk
     * @param chunkPos The chunk coordinates to highlight
     * @param color    The hex color integer (e.g., 0xFF0000 for red)
     */
    public static void highlightChunk(ResourceKey<Level> level, ChunkPos chunkPos, int color) {
        if (jmAPI == null) return;

        try {
            String displayId = getChunkDisplayId(chunkPos);

            // Create the Polygon using JourneyMap's Helper
            MapPolygon poly = PolygonHelper.createChunkPolygon(chunkPos.x, 100, chunkPos.z);


            // Define appearance
            ShapeProperties properties = new ShapeProperties().setStrokeColor(color).setStrokeOpacity(ClientConfig.chunkBorderOpacity).setStrokeWidth(ClientConfig.chunkBorderWidth).setFillColor(color).setFillOpacity(ClientConfig.chunkOverlayOpacity);

            // Create overlay
            PolygonOverlay overlay = new PolygonOverlay(EasyFactions.MODID, // Mod ID
                    displayId,     // Unique ID for this specific overlay
                    level,   // Dimension ID
                    properties,    // Visual properties
                    poly           // The shape
            );

            // 5. Send to JourneyMap
            jmAPI.show(overlay);

        } catch (Exception e) {
            LOGGER.atDebug().log(e.getMessage());
        }
    }

    /**
     * Removes a chunk highlight.
     */
    public static void removeChunkHighlight(ResourceKey<Level> level, ChunkPos chunkPos) {
        if (jmAPI == null) return;

        String displayId = getChunkDisplayId(chunkPos);

        // Create a dummy overlay with the same displayID and tell JourneyMap to remove it.
        MapPolygon dummyPoly = PolygonHelper.createChunkPolygon(chunkPos.x, 100, chunkPos.z);
        ShapeProperties dummyProps = new ShapeProperties();

        PolygonOverlay overlayToRemove = new PolygonOverlay(EasyFactions.MODID, displayId, level, dummyProps, dummyPoly);

        jmAPI.remove(overlayToRemove);
    }

    /**
     * Clear all overlays created by this mod
     */
    public static void clearAll() {
        if (jmAPI == null) return;
        jmAPI.removeAll(EasyFactions.MODID);
    }

    private static String getChunkDisplayId(ChunkPos pos) {
        return "chunk_" + pos.x + "_" + pos.z;
    }
}