package com.axalotl.async.commands;

import com.axalotl.async.config.AsyncConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static com.axalotl.async.commands.AsyncCommand.prefix;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.Commands.argument;

public class ConfigCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> registerConfig(LiteralArgumentBuilder<CommandSourceStack> root) {
        return root.then(literal("config")
                .then(literal("toggle").requires(cmdSrc -> cmdSrc.hasPermission(2)).executes(cmdCtx -> {
                    AsyncConfig.disabled = !AsyncConfig.disabled;
                    AsyncConfig.saveConfig();
                    MutableComponent message = prefix.copy().append(Component.literal("Async is now ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                            .append(Component.literal(AsyncConfig.disabled ? "disabled" : "enabled").withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                    cmdCtx.getSource().sendSuccess(() -> message, true);
                    return 1;
                }))
                .then(literal("setDisableTNT").requires(cmdSrc -> cmdSrc.hasPermission(2))
                        .executes(cmdCtx -> {
                            boolean currentValue = AsyncConfig.disableTNT;
                            MutableComponent message = prefix.copy().append(Component.literal("Current value of disable TNT: ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                                    .append(Component.literal(String.valueOf(currentValue)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                            cmdCtx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                        .then(argument("value", BoolArgumentType.bool()).executes(cmdCtx -> {
                            boolean value = BoolArgumentType.getBool(cmdCtx, "value");
                            AsyncConfig.disableTNT = value;
                            AsyncConfig.saveConfig();
                            MutableComponent message = prefix.copy().append(Component.literal("Disable TNT set to ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                                    .append(Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        })))
                .then(literal("disableItemEntity").requires(cmdSrc -> cmdSrc.hasPermission(2))
                        .executes(cmdCtx -> {
                            boolean currentValue = AsyncConfig.disableItemEntity;
                            MutableComponent message = prefix.copy().append(Component.literal("Current value of disable ItemEntity: ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                                    .append(Component.literal(String.valueOf(currentValue)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                            cmdCtx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                        .then(argument("value", BoolArgumentType.bool()).executes(cmdCtx -> {
                            boolean value = BoolArgumentType.getBool(cmdCtx, "value");
                            AsyncConfig.disableItemEntity = value;
                            AsyncConfig.saveConfig();
                            MutableComponent message = prefix.copy().append(Component.literal("Disable ItemEntity set to ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                                    .append(Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        })))
                .then(literal("setEntityMoveSync").requires(cmdSrc -> cmdSrc.hasPermission(2))
                        .executes(cmdCtx -> {
                            boolean currentValue = AsyncConfig.enableEntityMoveSync;
                            MutableComponent message = prefix.copy().append(Component.literal("Current value of entity move sync: ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                                    .append(Component.literal(String.valueOf(currentValue)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                            cmdCtx.getSource().sendSuccess(() -> message, false);
                            return 1;
                        })
                        .then(argument("value", BoolArgumentType.bool()).executes(cmdCtx -> {
                            boolean value = BoolArgumentType.getBool(cmdCtx, "value");
                            AsyncConfig.enableEntityMoveSync = value;
                            AsyncConfig.saveConfig();
                            MutableComponent message = prefix.copy().append(Component.literal("Entity move sync set to ").withStyle(style -> style.withColor(ChatFormatting.WHITE)))
                                    .append(Component.literal(String.valueOf(value)).withStyle(style -> style.withColor(ChatFormatting.GREEN)));
                            cmdCtx.getSource().sendSuccess(() -> message, true);
                            return 1;
                        }))));
    }
}
