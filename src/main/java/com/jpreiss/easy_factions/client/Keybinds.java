package com.jpreiss.easy_factions.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static final KeyMapping OPEN_FACTION_GUI = new KeyMapping(
            "key.easy_factions.open_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.easy_factions"
    );
}
