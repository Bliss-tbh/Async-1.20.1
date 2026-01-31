package com.axalotl.async.common.config;

import com.axalotl.async.common.commands.AsyncCommand;
import com.axalotl.async.common.parallelised.utils.ModCompatibility;
import com.axalotl.async.common.platform.PlatformUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class AsyncConfig {
    public static final Logger LOGGER = LoggerFactory.getLogger("Async Config");

    //TODO: should probably get the values before so we don't spam .getValue() but shouldn't impact perf that much riggggghhtt??!?!?!?!??!!?!?!?!
    public static Map.Entry<String, Boolean> disabled = new AbstractMap.SimpleEntry<>("disabled", false);
    public static Map.Entry<String, Integer> maxThreads = new AbstractMap.SimpleEntry<>("paraMax", -1);
    public static Map.Entry<String, Boolean> enableAsyncSpawn = new AbstractMap.SimpleEntry<>("enableAsyncSpawn", true);
    public static Map.Entry<String, Boolean> enableAsyncRandomTicks = new AbstractMap.SimpleEntry<>("enableAsyncRandomTicks", false);
    public static Map.Entry<String, Set<String>> synchronizedEntities = new AbstractMap.SimpleEntry<>("synchronizedEntities", getDefaultSynchronizedEntities());

    // Caches
    private static final Map<ResourceLocation, Boolean> syncCache = new ConcurrentHashMap<>();
    private static final Set<String> exactEntities = new HashSet<>();
    private static final Set<String> namespaceWildcards = new HashSet<>();


    public static Set<String> getDefaultSynchronizedEntities() {
        final Set<String> defaultSynchronizedEntities = new HashSet<>(ModCompatibility.addUnsupportedMods());
        defaultSynchronizedEntities.addAll(Set.of(
                "minecraft:tnt",
                "minecraft:item",
                "minecraft:experience_orb"
        ));
        return defaultSynchronizedEntities;
    }


    public static int getParallelism() {
        if (maxThreads.getValue() <= 0) return Runtime.getRuntime().availableProcessors();
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), maxThreads.getValue()));
    }

    public static boolean isNamespaceWildcard(String input) {
        if (input == null) return false;
        int colon = input.indexOf(':');
        if (colon <= 0) return false;
        return input.substring(colon + 1).equals("*");
    }

    public static boolean existsNamespace(String namespace, CommandSourceStack source) {
        for (ResourceLocation id : AsyncCommand.getEntityAccess(source).keySet()) {
            if (id.getNamespace().equals(namespace)) return true;
        }
        return false;
    }

    public static boolean matchesExistingNamespaceWildcard(String input, CommandSourceStack source) {
        if (!isNamespaceWildcard(input)) return false;
        String ns = input.substring(0, input.indexOf(':'));
        return existsNamespace(ns, source);
    }

    public static void syncEntity(String entity) {
        if (synchronizedEntities.getValue().add(entity)) {
            rebuildCaches();
            PlatformUtils.saveConfig();
            LOGGER.info("Added sync entity: {}", entity);
        } else {
            LOGGER.warn("Entity already synchronized: {}", entity);
        }
    }

    public static void removeEntity(String entity) {
        if (synchronizedEntities.getValue().remove(entity)) {
            rebuildCaches();
            PlatformUtils.saveConfig();
            LOGGER.info("Removed sync entity: {}", entity);
        } else {
            LOGGER.warn("Entity not found: {}", entity);
        }
    }

    private static void rebuildCaches() {
        syncCache.clear();
        exactEntities.clear();
        namespaceWildcards.clear();

        for (String entry : synchronizedEntities.getValue()) {
            if (isNamespaceWildcard(entry)) {
                String ns = entry.substring(0, entry.indexOf(':'));
                namespaceWildcards.add(ns);
            } else {
                exactEntities.add(entry);
            }
        }
    }

    public static boolean isEntitySynchronized(ResourceLocation entityId) {
        Boolean cached = syncCache.get(entityId);
        if (cached != null) return cached;

        String idString = entityId.toString();
        if (exactEntities.contains(idString)) {
            syncCache.put(entityId, true);
            return true;
        }

        if (namespaceWildcards.contains(entityId.getNamespace())) {
            syncCache.put(entityId, true);
            return true;
        }

        syncCache.put(entityId, false);
        return false;
    }

    public static void onConfigLoaded() {
        rebuildCaches();
        LOGGER.info("Configuration loaded.");
    }

    public static void clearCaches() {
        syncCache.clear();
    }
}
