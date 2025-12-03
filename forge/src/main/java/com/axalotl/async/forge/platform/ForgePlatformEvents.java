package com.axalotl.async.forge.platform;

import com.axalotl.async.common.platform.PlatformEvents;
import com.axalotl.async.forge.config.AsyncConfigForge;
import net.minecraftforge.fml.ModList;

public class ForgePlatformEvents implements PlatformEvents {

    @Override
    public void saveConfig() {
        AsyncConfigForge.saveConfig();
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }

    @Override
    public boolean platformUsesRefmap() {
        return false;
    }
}
