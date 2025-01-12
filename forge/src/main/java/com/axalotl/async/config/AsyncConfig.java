package com.axalotl.async.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("Async Config");

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC;

    public static boolean disabled;
    public static int paraMax;
    public static boolean disableTNT;
    public static boolean enableEntityMoveSync;
    public static boolean disableItemEntity;

    private static final ForgeConfigSpec.ConfigValue<Boolean> disabledv;
    private static final ForgeConfigSpec.ConfigValue<Integer> paraMaxv;
    private static final ForgeConfigSpec.ConfigValue<Boolean> disableTNTv;
    private static final ForgeConfigSpec.ConfigValue<Boolean> enableEntityMoveSyncv;
    private static final ForgeConfigSpec.ConfigValue<Boolean> disableItemEntityv;

    static {
        BUILDER.push("Async Configs");

        disabledv = BUILDER.comment("Globally disable all toggleable functionality within the async system. Set to true to stop all asynchronous operations.")
                        .define("disabled", false);

        paraMaxv = BUILDER.comment("Maximum number of threads to use for parallel processing. Set to -1 to use default value.")
                .define("paraMax", -1);

        disableTNTv = BUILDER.comment("Disables TNT entity parallelization. Use this to prevent asynchronous processing of TNT-related tasks.")
                .define("disableTNT", false);

        disableItemEntityv = BUILDER.comment("Disables Item entity parallelization.")
                .define("disableItemEntity", false);

        enableEntityMoveSyncv = BUILDER.comment("Modifies entity movement processing: true for synchronous movement (vanilla mechanics intact, less performance), false for asynchronous movement (better performance, may break mechanics).")
                .define("enableEntityMoveSync", false);

        BUILDER.pop();
        SPEC = BUILDER.build();
        LOGGER.info("Config Loaded");
    }

    public static void castConfig() {
        disabled = disabledv.get();
        paraMax = paraMaxv.get();
        disableTNT = disableTNTv.get();
        disableItemEntity = disableItemEntityv.get();
        enableEntityMoveSync = enableEntityMoveSyncv.get();
        LOGGER.info("Config Casted");
    }

    public static void saveConfig() {
        disabledv.set(disabled);
        paraMaxv.set(paraMax);
        disableTNTv.set(disableTNT);
        disableItemEntityv.set(disableItemEntity);
        enableEntityMoveSyncv.set(enableEntityMoveSync);
        LOGGER.info("Config Saved");
    }

    public static int getParallelism() {
        if (paraMax <= 0) return Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), paraMax));
    }
}
