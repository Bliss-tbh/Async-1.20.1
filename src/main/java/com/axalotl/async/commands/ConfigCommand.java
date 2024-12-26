package com.axalotl.async.commands;

import com.axalotl.async.config.AsyncConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static com.axalotl.async.commands.AsyncCommand.prefix;
import static net.minecraft.server.command.CommandManager.literal;

public class ConfigCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> registerConfig(LiteralArgumentBuilder<ServerCommandSource> root) {
        return root.then(literal("config")
                .then(CommandManager.literal("toggle").requires(cmdSrc -> cmdSrc.hasPermissionLevel(2)).executes(cmdCtx -> {
                    AsyncConfig.disabled = !AsyncConfig.disabled;
                    AsyncConfig.saveConfig();
                    MutableText message = prefix.copy().append(Text.literal("Async is now ").styled(style -> style.withColor(Formatting.WHITE)))
                            .append(Text.literal(AsyncConfig.disabled ? "disabled" : "enabled").styled(style -> style.withColor(Formatting.GREEN)));
                    cmdCtx.getSource().sendFeedback(() -> message, true);
                    return 1;
                }))
                .then(CommandManager.literal("setDisableTNT").requires(cmdSrc -> cmdSrc.hasPermissionLevel(2))
                        .then(CommandManager.argument("value", BoolArgumentType.bool()).executes(cmdCtx -> {
                            boolean value = BoolArgumentType.getBool(cmdCtx, "value");
                            AsyncConfig.disableTNT = value;
                            AsyncConfig.saveConfig();
                            MutableText message = prefix.copy().append(Text.literal("Disable TNT set to ").styled(style -> style.withColor(Formatting.WHITE)))
                                    .append(Text.literal(String.valueOf(value)).styled(style -> style.withColor(Formatting.GREEN)));
                            cmdCtx.getSource().sendFeedback(() -> message, true);
                            return 1;
                        })))
                .then(CommandManager.literal("setEntityMoveSync").requires(cmdSrc -> cmdSrc.hasPermissionLevel(2))
                        .then(CommandManager.argument("value", BoolArgumentType.bool()).executes(cmdCtx -> {
                            boolean value = BoolArgumentType.getBool(cmdCtx, "value");
                            AsyncConfig.enableEntityMoveSync = value;
                            AsyncConfig.saveConfig();
                            MutableText message = prefix.copy().append(Text.literal("Entity move sync set to ").styled(style -> style.withColor(Formatting.WHITE)))
                                    .append(Text.literal(String.valueOf(value)).styled(style -> style.withColor(Formatting.GREEN)));
                            cmdCtx.getSource().sendFeedback(() -> message, true);
                            return 1;
                        }))));
    }
}