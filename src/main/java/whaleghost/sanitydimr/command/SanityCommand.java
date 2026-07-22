package whaleghost.sanitydimr.command;

import java.util.Collection;
import java.util.Collections;

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
                    return applySet(
                        source,
                        Collections.singleton((ServerPlayer) source.getEntityOrException()),
                        FloatArgumentType.getFloat(ctx, "value")
                    );
                })
            )
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("value", FloatArgumentType.floatArg(MIN_SANITY, MAX_SANITY))
                    .executes(ctx ->
                        applySet(
                            ctx.getSource(),
                            EntityArgument.getPlayers(ctx, "targets"),
                            FloatArgumentType.getFloat(ctx, "value")
                        )
                    )
                )
            );
    }

    // /sanity add [<player>] <value>
    private static LiteralArgumentBuilder<CommandSourceStack> buildAdd() {
        return Commands.literal("add")
            .then(Commands.argument("value", FloatArgumentType.floatArg(-MAX_SANITY, MAX_SANITY))
                .executes(ctx -> {
                    var source = ctx.getSource();
                    return applyAdd(
                        source,
                        Collections.singleton((ServerPlayer) source.getEntityOrException()),
                        FloatArgumentType.getFloat(ctx, "value")
                    );
                })
            )
            .then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("value", FloatArgumentType.floatArg(-MAX_SANITY, MAX_SANITY))
                    .executes(ctx ->
                        applyAdd(
                            ctx.getSource(),
                            EntityArgument.getPlayers(ctx, "targets"),
                            FloatArgumentType.getFloat(ctx, "value")
                        )
                    )
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
                return 1;
            }));
    }

    // -- Sanity mutation helpers --
    private static int applySet(CommandSourceStack source,
                                Collection<? extends ServerPlayer> targets,
                                float value)
    {
        for (var player : targets)
            player.getData(Sanity.ATTACHMENT).setSanity(toStored(value));

        sendSetFeedback(source, value, targets);
        return (int) value;
    }

    private static int applyAdd(CommandSourceStack source,
                                Collection<? extends ServerPlayer> targets,
                                float delta)
    {
        for (var player : targets) {
            var sanity = player.getData(Sanity.ATTACHMENT);
            sanity.setSanity(sanity.getSanity() - delta / MAX_SANITY);
        }

        sendAddFeedback(source, delta, targets);
        return (int) delta;
    }

    private static float toStored(float displayValue)
    {
        return (MAX_SANITY - displayValue) / MAX_SANITY;
    }

    // -- Feedback --
    private static void sendSetFeedback(CommandSourceStack source, float value,
                                        Collection<? extends ServerPlayer> targets)
    {
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable(
                "commands.sanity.set.success.single",
                targets.iterator().next().getDisplayName(), value), true);
        } else {
            source.sendSuccess(() -> Component.translatable(
                "commands.sanity.set.success.multiple", value, targets.size()), true);
        }
    }

    private static void sendAddFeedback(CommandSourceStack source, float delta,
                                        Collection<? extends ServerPlayer> targets)
    {
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.translatable(
                "commands.sanity.add.success.single",
                delta, targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(() -> Component.translatable(
                "commands.sanity.add.success.multiple", targets.size(), delta), true);
        }
    }
}