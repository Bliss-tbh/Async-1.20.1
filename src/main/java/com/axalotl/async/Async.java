package com.axalotl.async;

import com.axalotl.async.commands.AsyncCommand;
import com.axalotl.async.commands.ConfigCommand;
import com.axalotl.async.commands.StatsCommand;
import com.axalotl.async.config.AsyncConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Async implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static Boolean c2me = FabricLoader.getInstance().isModLoaded("c2me");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Async...");
        c2me = FabricLoader.getInstance().isModLoaded("c2me");
        AsyncConfig.init();
        StatsCommand.runStatsThread();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Async Setting up thread-pool...");
            ParallelProcessor.setServer(server);
            ParallelProcessor.setupThreadPool(AsyncConfig.getParallelism());
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AsyncCommand.register(dispatcher);
            dispatcher.register(ConfigCommand.registerConfig(CommandManager.literal("async")));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Shutting down Async thread pool...");
            ParallelProcessor.stop();
            StatsCommand.shutdown();
        });

        LOGGER.info("Async Initialized successfully");
    }
}