package com.axalotl.async.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.axalotl.async.common.config.AsyncConfig.*;

public class AsyncConfigForge {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<Boolean> disabledLocal;
    private static final ForgeConfigSpec.ConfigValue<Integer> maxThreadsLocal;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> synchronizedEntitiesLocal;
    private static final ForgeConfigSpec.ConfigValue<Boolean> enableAsyncSpawnLocal;
    private static final ForgeConfigSpec.ConfigValue<Boolean> enableAsyncRandomTicksLocal;

    static {
        BUILDER.push("Async Config");

        disabledLocal = BUILDER.comment("Enables parallel processing of entity.")
                .define(disabled.getKey(), disabled.getValue());

        maxThreadsLocal = BUILDER.comment("Maximum worker threads. -1 = auto.")
                .defineInRange(maxThreads.getKey(), maxThreads.getValue(), -1, Integer.MAX_VALUE);

        synchronizedEntitiesLocal = BUILDER.comment("""
                        List of entity IDs or namespaces (*):
                          - 'minecraft:zombie' = specific entity
                          - 'minecraft:*'      = all entities in namespace""")
                .defineListAllowEmpty(
                        synchronizedEntities.getKey(),
                        () -> new ArrayList<>(synchronizedEntities.getValue()),
                        obj -> obj instanceof String
                );

        enableAsyncSpawnLocal = BUILDER.comment("Enables async entity spawning. WARNING: incompatible with Carpet's lagFreeSpawning.")
                .define(enableAsyncSpawn.getKey(), enableAsyncSpawn.getValue());

        enableAsyncRandomTicksLocal = BUILDER.comment("Experimental! Enables async random ticks.")
                .define(enableAsyncRandomTicks.getKey(), enableAsyncRandomTicks.getValue());

        BUILDER.pop();
        SPEC = BUILDER.build();
        LOGGER.info("Configuration initialized.");
    }

    public static void loadConfig() {
        disabled.setValue(disabledLocal.get());
        maxThreads.setValue(maxThreadsLocal.get());
        enableAsyncSpawn.setValue(enableAsyncSpawnLocal.get());
        enableAsyncRandomTicks.setValue(enableAsyncRandomTicksLocal.get());

        List<? extends String> entries = synchronizedEntitiesLocal.get();
        Set<String> entities = new HashSet<>();
        if (!entries.isEmpty()) {
            entities.addAll(entries);
        }

        synchronizedEntities.setValue(entities.isEmpty()
                ? getDefaultSynchronizedEntities()
                : entities);
    }

    public static void saveConfig() {
        disabledLocal.set(disabled.getValue());
        maxThreadsLocal.set(maxThreads.getValue());
        enableAsyncSpawnLocal.set(enableAsyncSpawn.getValue());
        enableAsyncRandomTicksLocal.set(enableAsyncRandomTicks.getValue());
        synchronizedEntitiesLocal.set(new ArrayList<>(synchronizedEntities.getValue()));
        SPEC.save();
        onConfigLoaded();
    }
}
