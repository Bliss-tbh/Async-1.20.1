package com.axalotl.async.forge.platform;

import com.axalotl.async.common.platform.MinecraftPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;


public class ForgeMinecraftPlatform implements MinecraftPlatform {
    @Override
    public boolean hasPermission(CommandSourceStack source, String node, int level) {
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            return source.hasPermission(level);
        }

        PermissionNode<Boolean> permission = PermissionAPI.getRegisteredNodes().stream()
                .filter(n -> n.getNodeName().equals(node))
                .filter(n -> n.getType() == PermissionTypes.BOOLEAN)
                .map(n -> (PermissionNode<Boolean>) n)
                .findFirst()
                .orElse(null);

        if (permission != null) {
            return PermissionAPI.getPermission(player, permission);
        }

        return source.hasPermission(level);
    }
}