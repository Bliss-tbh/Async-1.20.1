package com.axalotl.async.forge.platform;

import com.axalotl.async.common.AsyncCommon;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.Map;

public class ForgePermissions {
    private static final Map<String, PermissionNode<Boolean>> PERMISSIONS = ForgePermissions.build(
            "command.config",
            "command.statistics"
    );

    public static PermissionNode<Boolean> getPermissionNode(String node) {
        return PERMISSIONS.get(node);
    }

    public static void addNodes(PermissionGatherEvent.Nodes event) {
        for (PermissionNode<Boolean> node : PERMISSIONS.values()) {
            event.addNodes(node);
        }
    }

    private static Map<String, PermissionNode<Boolean>> build(String... nodes) {
        Map<String, PermissionNode<Boolean>> permissions = new Object2ObjectOpenHashMap<>();
        for (String node : nodes) {
            permissions.put(node, new PermissionNode<>(AsyncCommon.MODID, node, PermissionTypes.BOOLEAN, (x, y, z) -> false));
        }
        return permissions;
    }
}