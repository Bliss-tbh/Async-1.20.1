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
        if (source.hasPermission(level)) {
            return true;
        }

        ServerPlayer player = source.getPlayer();
        PermissionNode<Boolean> permission = null;

        for (PermissionNode<?> thisnode : PermissionAPI.getRegisteredNodes()) {
            if (thisnode.getNodeName().equals(node)) {
                if (thisnode.getType() == PermissionTypes.BOOLEAN) {
                    permission = (PermissionNode<Boolean>) thisnode;
                }
            }
        }

        if (player == null || permission == null) {
            return false;
        }

        return PermissionAPI.getPermission(player, permission);
    }

}