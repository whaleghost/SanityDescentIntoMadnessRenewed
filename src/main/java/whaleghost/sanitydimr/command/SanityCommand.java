package whaleghost.sanitydimr.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import whaleghost.sanitydimr.capability.Sanity;
import whaleghost.sanitydimr.config.DimensionConfig;

public class SanityCommand {

    private static final float MIN_SANITY = 0f;
    private static final float MAX_SANITY = 100f;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("sanity")
                .requires(stack -> stack.hasPermission(2))
                .then(buildSet())
                .then(buildAdd())
                .then(buildConfig())
        );
    }

    // /sanity set [<player>] <value>
    private static LiteralArgumentBuilder<CommandSourceStack> buildSet() {
        return Commands.literal("set")
            .then(Commands.argument("value", FloatArgumentType.floatArg(MIN_SANITY, MAX_SANITY))
                .executes(ctx -> {
                    var source = ctx.getSource();
                    var player = (ServerPlayer) source.getEntityOrException();
                    var value = FloatArgumentType.getFloat(ctx, "value");
                    applySet(player, value);
                    source.sendSuccess(() ->
                        Component.translatable("commands.sanity.set.success", player.getDisplayName(), value),
                        true
                    );
                    return (int) value;
                })
            )
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("value", FloatArgumentType.floatArg(MIN_SANITY, MAX_SANITY))
                    .executes(ctx -> {
                        var source = ctx.getSource();
                        var player = EntityArgument.getPlayer(ctx, "target");
                        var value = FloatArgumentType.getFloat(ctx, "value");
                        applySet(player, value);
                        source.sendSuccess(() ->
                            Component.translatable("commands.sanity.set.success", player.getDisplayName(), value),
                            true
                        );
                        return (int) value;
                    })
                )
            );
    }

    // /sanity add [<player>] <value>
    private static LiteralArgumentBuilder<CommandSourceStack> buildAdd() {
        return Commands.literal("add")
            .then(Commands.argument("value", FloatArgumentType.floatArg(-MAX_SANITY, MAX_SANITY))
                .executes(ctx -> {
                    var source = ctx.getSource();
                    var player = (ServerPlayer) source.getEntityOrException();
                    var delta = FloatArgumentType.getFloat(ctx, "value");
                    applyAdd(player, delta);
                    source.sendSuccess(() ->
                        Component.translatable("commands.sanity.add.success", delta, player.getDisplayName()),
                        true
                    );
                    return (int) delta;
                })
            )
            .then(Commands.argument("target", EntityArgument.player())
                .then(Commands.argument("value", FloatArgumentType.floatArg(-MAX_SANITY, MAX_SANITY))
                    .executes(ctx -> {
                        var source = ctx.getSource();
                        var player = EntityArgument.getPlayer(ctx, "target");
                        var delta = FloatArgumentType.getFloat(ctx, "value");
                        applyAdd(player, delta);
                        source.sendSuccess(() ->
                            Component.translatable("commands.sanity.add.success", delta, player.getDisplayName()),
                            true
                        );
                        return (int) delta;
                    })
                )
            );
    }

    // /sanity config reload
    private static LiteralArgumentBuilder<CommandSourceStack> buildConfig()
    {
        return Commands.literal("config")
            .then(Commands.literal("reload").executes(ctx -> {
                DimensionConfig.init();
                ctx.getSource().sendSuccess(
                    () -> Component.translatable("commands.sanity.config.reload"), true);
                return Command.SINGLE_SUCCESS;
            }));
    }

    // -- Single-player mutations --

    private static void applySet(ServerPlayer player, float value)
    {
        player.getData(Sanity.ATTACHMENT).setSanity(toStored(value));
    }

    private static void applyAdd(ServerPlayer player, float delta)
    {
        var sanity = player.getData(Sanity.ATTACHMENT);
        sanity.setSanity(sanity.getSanity() - delta / MAX_SANITY);
    }

    private static float toStored(float displayValue)
    {
        return (MAX_SANITY - displayValue) / MAX_SANITY;
    }
}