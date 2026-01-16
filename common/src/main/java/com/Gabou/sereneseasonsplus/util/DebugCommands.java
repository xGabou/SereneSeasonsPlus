package com.Gabou.sereneseasonsplus.util;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DebugCommands {
    private DebugCommands() {}

    public static void registerTo(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("sspdebug")
                        .requires((Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                        .then(Commands.literal("on").executes(ctx -> set(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> set(ctx.getSource(), false)))
                        .then(Commands.literal("toggle").executes(ctx -> toggle(ctx.getSource())))
                        .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
        );
    }

    private static int set(CommandSourceStack source, boolean value) {
        DebugMode.setEnabled(value);
        source.sendSuccess(() -> Component.literal("SereneSeasonsPlus debug " + (value ? "enabled" : "disabled")), true);
        return 1;
    }

    private static int toggle(CommandSourceStack source) {
        boolean now = DebugMode.toggle();
        source.sendSuccess(() -> Component.literal("SereneSeasonsPlus debug " + (now ? "enabled" : "disabled")), true);
        return now ? 1 : 0;
    }

    private static int status(CommandSourceStack source) {
        boolean now = DebugMode.isEnabled();
        source.sendSuccess(() -> Component.literal("SereneSeasonsPlus debug is " + (now ? "ENABLED" : "DISABLED")), false);
        return now ? 1 : 0;
    }
}

