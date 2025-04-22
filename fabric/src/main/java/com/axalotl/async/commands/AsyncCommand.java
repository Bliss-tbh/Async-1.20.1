package com.axalotl.async.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManagerourceStack;
import net.minecraft.text.Component;

import static net.minecraft.server.command.CommandManager.literal;

public class AsyncCommand {
    public final static Component prefix = Component.literal("§8[§f\uD83C\uDF00§8]§7 ");

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> main = literal("async").requires(source -> source.hasPermission(4));
        main = ConfigCommand.registerConfig(main).requires(source -> source.hasPermission(4));
        main = StatsCommand.registerStatus(main).requires(source -> source.hasPermission(4));
        dispatcher.register(main);
    }
}
