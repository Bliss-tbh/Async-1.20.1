package com.axalotl.async.mixin.utils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SynchronisePlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Multimap<String, String> methodsToSynchronize = ArrayListMultimap.create();
    private final Multimap<String, String> methodsToExclude = ArrayListMultimap.create();
    private final Set<String> syncAllClasses = new TreeSet<>();

    @Override
    public void onLoad(String mixinPackage) {
        MappingResolver mappingResolver = FabricLoader.getInstance().getMappingResolver();

        methodsToExclude.put(
                "com.axalotl.async.mixin.utils.SyncAllMixin",
                mappingResolver.mapMethodName("intermediary", "net.minecraft.class_2806", "method_12165", "()V")
        );

        syncAllClasses.addAll(List.of(
                "com.axalotl.async.mixin.utils.FastUtilsMixin",
                "com.axalotl.async.mixin.utils.SyncAllMixin"
        ));
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Collection<String> targetMethods = methodsToSynchronize.get(mixinClassName);
        Collection<String> excludedMethods = methodsToExclude.get(mixinClassName);

        if (!targetMethods.isEmpty()) {
            synchronizeSpecificMethods(targetClassName, targetClass, targetMethods);
        } else if (syncAllClasses.contains(mixinClassName)) {
            synchronizeAllApplicableMethods(targetClassName, targetClass, excludedMethods);
        }
    }

    private void synchronizeSpecificMethods(String className, ClassNode classNode, Collection<String> methods) {
        for (MethodNode method : classNode.methods) {
            if (methods.contains(method.name)) {
                method.access |= Opcodes.ACC_SYNCHRONIZED;
                logSynchronization(method.name, className);
            }
        }
    }

    private void synchronizeAllApplicableMethods(String className, ClassNode classNode, Collection<String> excludedMethods) {
        final int filter = Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_BRIDGE;

        for (MethodNode method : classNode.methods) {
            if ((method.access & filter) == 0
                    && !"<init>".equals(method.name)
                    && !excludedMethods.contains(method.name)) {
                method.access |= Opcodes.ACC_SYNCHRONIZED;
                logSynchronization(method.name, className);
            }
        }
    }

    private void logSynchronization(String methodName, String className) {
        LOGGER.debug("Synchronized method: {} in {}", methodName, className);
    }
}