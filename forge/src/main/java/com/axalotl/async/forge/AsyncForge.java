package com.axalotl.async.forge;

import com.axalotl.async.common.AsyncCommon;
import com.axalotl.async.common.ExplosionProcessor;
import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.commands.AsyncCommand;
import com.axalotl.async.common.commands.StatsCommand;
import com.axalotl.async.forge.platform.ForgePermissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import org.slf4j.Logger;

import static com.axalotl.async.common.config.AsyncConfig.getParallelism;
import static com.axalotl.async.forge.config.AsyncConfigForge.SPEC;
import static com.axalotl.async.forge.config.AsyncConfigForge.loadConfig;

@Mod(AsyncForge.MOD_ID)
public class AsyncForge extends AsyncCommon {

    public static final String MOD_ID = "async";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AsyncForge(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing Async...");
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("Initializing Async Config...");
        context.registerConfig(ModConfig.Type.COMMON, SPEC, "async.toml");
        LOGGER.info("Async Initialized successfully");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Async Setting up thread-pool...");
        this.initialize();
        loadConfig();
        StatsCommand.runStatsThread();
        ExplosionProcessor.start();
        ParallelProcessor.setServer(event.getServer());
        ParallelProcessor.setupThreadPool(getParallelism(), this.getClass());
    }

    @SubscribeEvent
    public void registerCommandsEvent(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        AsyncCommand.register(dispatcher);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        StatsCommand.shutdown();
        ExplosionProcessor.stop();
        ParallelProcessor.stop();
    }

    @SubscribeEvent
    public void handlePermissionNodesGather(PermissionGatherEvent.Nodes event) {
        ForgePermissions.addNodes(event);
    }
}
