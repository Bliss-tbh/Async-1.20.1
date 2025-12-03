package com.axalotl.async.forge.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashSet;
import java.util.List;

import static com.axalotl.async.common.config.AsyncConfig.*;

public class AsyncConfigForge {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.ConfigValue<Boolean> disabledForge;
    private static final ForgeConfigSpec.ConfigValue<Integer> paraMaxForge;
    private static final ForgeConfigSpec.ConfigValue<List<String>> synchronizedEntitiesForge;
    private static final ForgeConfigSpec.ConfigValue<Boolean> enableAsyncSpawnForge;
    private static final ForgeConfigSpec.ConfigValue<Boolean> enableAsyncRandomTicksForge;

    static {
        BUILDER.push("Async Config");

        disabledForge = BUILDER.comment("Enables parallel processing of entity.")
                .define(disabled.getKey(), disabled.getValue());

        paraMaxForge = BUILDER.comment("Maximum number of threads to use for parallel processing. Set to -1 to use default value.")
                .define(paraMax.getKey(), paraMax.getValue());

        synchronizedEntitiesForge = BUILDER.comment("List of entity class for sync processing.")
                .define(synchronizedEntities.getKey(), synchronizedEntities.getValue().stream().map(ResourceLocation::toString).toList());

        enableAsyncSpawnForge = BUILDER.comment("Enables parallel processing of entity spawns.")
                .define(enableAsyncSpawn.getKey(), enableAsyncSpawn.getValue());

        enableAsyncRandomTicksForge = BUILDER.comment("Experimental! Enables async processing of random ticks.")
                .define(enableAsyncRandomTicks.getKey(), enableAsyncRandomTicks.getValue());

        BUILDER.pop();
        SPEC = BUILDER.build();
        LOGGER.info("Configuration successfully loaded.");
    }

    public static void loadConfig() {
        disabled.setValue(disabledForge.get());
        paraMax.setValue(paraMaxForge.get());
        enableAsyncSpawn.setValue(enableAsyncSpawnForge.get());
        enableAsyncRandomTicks.setValue(enableAsyncRandomTicksForge.get());

        synchronizedEntities.setValue(new HashSet<>());
        List<String> ids = synchronizedEntitiesForge.get();
        HashSet<ResourceLocation> set = new HashSet<>();
        for (String id : ids) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            if (rl != null) {
                set.add(rl);
            }
        }

        synchronizedEntities.setValue(set.isEmpty()
                ? getDefaultSynchronizedEntities()
                : set);
    }

    public static void saveConfig() {
        disabledForge.set(disabled.getValue());
        paraMaxForge.set(paraMax.getValue());
        enableAsyncSpawnForge.set(enableAsyncSpawn.getValue());
        enableAsyncRandomTicksForge.set(enableAsyncRandomTicks.getValue());
        synchronizedEntitiesForge.set(synchronizedEntities.getValue().stream().map(ResourceLocation::toString).toList());
        SPEC.save();
        LOGGER.info("Configuration successfully saved.");
    }
}
