package com.axalotl.async.forge.platform;

import com.axalotl.async.common.platform.ModPlatform;
import com.axalotl.async.forge.config.AsyncConfigForge;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgeModPlatform implements ModPlatform {

    @Override
    public void saveConfig() {
        AsyncConfigForge.saveConfig();
    }

    @Override
    public void reloadConfig() {
        AsyncConfigForge.loadConfig();
        com.axalotl.async.common.config.AsyncConfig.onConfigLoaded();
    }

    @Override
    public boolean isModLoaded(String id) {
        return FMLLoader.getLoadingModList().getModFileById(id) != null;
    }

    @Override
    public boolean platformUsesRefmap() {
        return false;
    }
}