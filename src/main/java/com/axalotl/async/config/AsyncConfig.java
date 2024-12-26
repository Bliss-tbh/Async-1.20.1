package com.axalotl.async.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class AsyncConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("Async Config");

    private static final Supplier<CommentedFileConfig> configSupplier =
            () -> CommentedFileConfig.builder(FabricLoader.getInstance().getConfigDir().resolve("async.toml"))
                    .preserveInsertionOrder()
                    .sync()
                    .build();

    private static CommentedFileConfig CONFIG;

    public static boolean disabled = false;
    public static int paraMax = -1;
    public static boolean disableTNT = true;
    public static boolean enableEntityMoveSync = false;

    public static void init() {
        LOGGER.info("Initializing Async Config...");
        CONFIG = configSupplier.get();
        try {
            CONFIG.load();
            loadConfigValues();
            LOGGER.info("Configuration successfully loaded.");
        } catch (Throwable t) {
            LOGGER.error("Error loading configuration, setting default values");
            setDefaultValues();
            saveConfig();
        }
    }

    public static void saveConfig() {
        CONFIG.set("disabled", disabled);
        CONFIG.setComment("disabled", "Globally disable all toggleable functionality within the async system. Set to true to stop all asynchronous operations.");

        CONFIG.set("paraMax", paraMax);
        CONFIG.setComment("paraMax", "Maximum number of threads to use for parallel processing. Set to -1 to use default value."
                + "Note: If 'virtualThreads' is enabled, this setting will be ignored.");

        CONFIG.set("disableTNT", disableTNT);
        CONFIG.setComment("disableTNT", "Disables TNT entity parallelization. Use this to prevent asynchronous processing of TNT-related tasks");

        CONFIG.set("enableEntityMoveSync", enableEntityMoveSync);
        CONFIG.setComment("enableEntityMoveSync", "Modifies entity movement processing: true for synchronous movement (vanilla mechanics intact, less performance), false for asynchronous movement (better performance, may break mechanics).");

        CONFIG.save();
        LOGGER.info("Configuration saved successfully.");
    }

    private static void loadConfigValues() {
        disabled = CONFIG.get("disabled");
        paraMax = CONFIG.get("paraMax");
        disableTNT = CONFIG.get("disableTNT");
        enableEntityMoveSync = CONFIG.get("enableEntityMoveSync");
    }

    private static void setDefaultValues() {
        CONFIG.set("disabled", false);
        CONFIG.setComment("disabled", "Globally disable all toggleable functionality within the async system. Set to true to stop all asynchronous operations.");

        CONFIG.set("paraMax", paraMax);
        CONFIG.setComment("paraMax", "Maximum number of threads to use for parallel processing. Set to -1 to use default value."
                + "Note: If 'virtualThreads' is enabled, this setting will be ignored.");

        CONFIG.set("disableTNT", disableTNT);
        CONFIG.setComment("disableTNT", "Disables TNT entity parallelization. Use this to prevent asynchronous processing of TNT-related tasks");

        CONFIG.set("enableEntityMoveSync", enableEntityMoveSync);
        CONFIG.setComment("enableEntityMoveSync", "Modifies entity movement processing: true for synchronous movement (vanilla mechanics intact, less performance), false for asynchronous movement (better performance, may break mechanics).");
    }

    public static int getParallelism() {
        if (paraMax <= 1) return Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), paraMax));
    }
}