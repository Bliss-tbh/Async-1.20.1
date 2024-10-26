package com.axalotl.async.serdes;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.axalotl.async.config.BlockEntityLists;
import com.axalotl.async.config.SerDesConfig;
import com.axalotl.async.serdes.filter.*;
import com.axalotl.async.serdes.pools.ChunkLockPool;
import com.axalotl.async.serdes.pools.ISerDesPool;
import com.axalotl.async.serdes.pools.PostExecutePool;
import com.axalotl.async.serdes.pools.SingleExecutionPool;

import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SerDesRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Class<?>, ISerDesFilter> EMPTYMAP = new ConcurrentHashMap<>();
    private static final Set<Class<?>> EMPTYSET = ConcurrentHashMap.newKeySet();

    static Map<ISerDesHookType, Map<Class<?>, ISerDesFilter>> optimisedLookup = new ConcurrentHashMap<>();
    static Map<ISerDesHookType, Set<Class<?>>> whitelist = new ConcurrentHashMap<>();
    static Set<Class<?>> unknown = ConcurrentHashMap.newKeySet();
    static ArrayList<ISerDesFilter> filters = new ArrayList<>();
    static Set<ISerDesHookType> hookTypes = new HashSet<>();

    private static final ISerDesFilter DEFAULT_FILTER = new DefaultFilter();

    static {
        Collections.addAll(hookTypes, SerDesHookTypes.values());
    }

    public static void init() {
        initPools();
        initFilters();
        initLookup();
    }

    public static void initFilters() {
        filters.clear();
        filters.add(new VanillaFilter());
        filters.add(new LegacyFilter());
        for (SerDesConfig.FilterConfig fpc : SerDesConfig.getFilters()) {
            filters.add(new GenericConfigFilter(fpc));
        }
        filters.add(AutoFilter.singleton());
        filters.add(DEFAULT_FILTER);
        filters.forEach(ISerDesFilter::init);
    }

    public static void initLookup() {
        optimisedLookup.clear();
        for (ISerDesFilter f : filters) {
            Set<Class<?>> rawTgt = Optional.ofNullable(f.getTargets()).orElseGet(ConcurrentHashMap::newKeySet);
            Set<Class<?>> rawWl = Optional.ofNullable(f.getWhitelist()).orElseGet(ConcurrentHashMap::newKeySet);
            Map<ISerDesHookType, Set<Class<?>>> whitelisted = group(rawWl);
            for (ISerDesHookType sh : hookTypes) {
                for (Class<?> i : rawTgt) {
                    if (sh.isTargetable(i)) {
                        optimisedLookup.computeIfAbsent(sh, k -> new ConcurrentHashMap<>()).put(i, f);
                        whitelisted.computeIfAbsent(sh, k -> ConcurrentHashMap.newKeySet()).remove(i);
                    }
                }
                whitelisted.computeIfAbsent(sh, k -> ConcurrentHashMap.newKeySet()).addAll(rawWl);
            }
        }
    }

    public static Map<ISerDesHookType, Set<Class<?>>> group(Set<Class<?>> set) {
        Map<ISerDesHookType, Set<Class<?>>> out = new ConcurrentHashMap<>();
        for (Class<?> i : set) {
            for (ISerDesHookType sh : hookTypes) {
                if (sh.isTargetable(i)) {
                    out.computeIfAbsent(sh, k -> ConcurrentHashMap.newKeySet()).add(i);
                }
            }
        }
        return out;
    }

    public static ISerDesFilter getFilter(ISerDesHookType isdh, Class<?> clazz) {
        if (whitelist.getOrDefault(isdh, EMPTYSET).contains(clazz)) {
            return null;
        }
        return optimisedLookup.getOrDefault(isdh, EMPTYMAP).getOrDefault(clazz, DEFAULT_FILTER);
    }

    static Map<String, ISerDesPool> registry = new ConcurrentHashMap<>();

    public static ISerDesPool getPool(String name) {
        return registry.get(name);
    }

    public static ISerDesPool getOrCreatePool(String name, Function<String, ISerDesPool> source) {
        return registry.computeIfAbsent(name, source);
    }

    public static ISerDesPool getOrCreatePool(String name, Supplier<ISerDesPool> source) {
        return getOrCreatePool(name, i -> {
            ISerDesPool out = source.get();
            out.init(i, new HashMap<>());
            return out;
        });
    }

    public static void initPools() {
        registry.clear();
        getOrCreatePool("LEGACY", ChunkLockPool::new);
        getOrCreatePool("SINGLE", SingleExecutionPool::new);
        getOrCreatePool("POST", () -> PostExecutePool.POOL);
        List<SerDesConfig.PoolConfig> pcl = SerDesConfig.getPools();
        if (pcl != null) {
            for (SerDesConfig.PoolConfig pc : pcl) {
                if (!registry.containsKey(pc.getName())) {
                    try {
                        Class<?> c = Class.forName(pc.getClazz());
                        Constructor<?> init = c.getConstructor();
                        Object o = init.newInstance();
                        if (o instanceof ISerDesPool) {
                            registry.put(pc.getName(), (ISerDesPool) o);
                            ((ISerDesPool) o).init(pc.getName(), pc.getInitParams());
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                             IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Error initializing pool: {}", pc.getName(), e);
                    }
                }
            }
        }
    }

    public static class DefaultFilter implements ISerDesFilter {
        ISerDesPool clp;
        ISerDesPool.ISerDesOptions config;

        @Override
        public void init() {
            clp = SerDesRegistry.getOrCreatePool("LEGACY", ChunkLockPool::new);
            Map<String, Object> cfg = new HashMap<>();
            cfg.put("range", "1");
            config = clp.compileOptions(cfg);
        }

        @Override
        public void serialise(Runnable task, Object obj, BlockPos bp, World w, ISerDesHookType hookType) {
            if (!unknown.contains(obj.getClass())) {
                ClassMode mode = ClassMode.UNKNOWN;
                for (ISerDesFilter isdf : filters) {
                    ClassMode cm = isdf.getModeOnline(obj.getClass());
                    if (cm.compareTo(mode) < 0) {
                        mode = cm;
                    }
                    if (mode == ClassMode.BLACKLIST) {
                        optimisedLookup.computeIfAbsent(hookType, i -> new ConcurrentHashMap<>()).put(obj.getClass(), isdf);
                        isdf.serialise(task, obj, bp, w, hookType);
                        return;
                    }
                }
                if (mode == ClassMode.WHITELIST) {
                    whitelist.computeIfAbsent(hookType, k -> ConcurrentHashMap.newKeySet()).add(obj.getClass());
                    task.run();
                    return;
                }
                unknown.add(obj.getClass());
            }
            if (hookType.equals(SerDesHookTypes.TETick) && filterTE(obj)) {
                if (clp == null) {
                    clp = SerDesRegistry.getOrCreatePool("LEGACY", ChunkLockPool::new);
                }
                clp.serialise(task, obj, bp, w, config);
            } else {
                try {
                    task.run();
                } catch (Exception e) {
                    LOGGER.error("Exception running {} asynchronously", obj.getClass().getName(), e);
                    AutoFilter.singleton().addClassToBlacklist(obj.getClass());
                    if (e instanceof RuntimeException) throw e;
                }
            }
        }

        public static boolean filterTE(Object tte) {
            boolean isLocking = BlockEntityLists.teBlackList.contains(tte.getClass());
            if (!isLocking && !tte.getClass().getName().startsWith("net.minecraft.block.entity.")) {
                isLocking = true;
            }
            if (isLocking && BlockEntityLists.teWhiteList.contains(tte.getClass())) {
                isLocking = false;
            }
            return isLocking || tte instanceof PistonBlockEntity;
        }
    }
}