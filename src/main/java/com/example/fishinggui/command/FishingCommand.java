package com.example.fishinggui.command;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.LevelCurveLoader;
import com.example.fishinggui.level.FishingData;
import com.example.fishinggui.level.FishingProgress;
import com.example.fishinggui.level.FishingService;
import com.example.fishinggui.level.LevelCurve;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /fishinggui level|xp set|add and /fishinggui query (operators only). */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishingCommand {
    private FishingCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("fishinggui").requires(s -> s.hasPermission(2))
                .then(Commands.literal("level")
                        .then(change("set", false, IntegerArgumentType.integer(1, LevelCurve.MAX_LEVEL),
                                (p, v) -> p.withLevel(v)))
                        .then(change("add", false,
                                IntegerArgumentType.integer(-LevelCurve.MAX_LEVEL, LevelCurve.MAX_LEVEL),
                                (p, v) -> p.withLevel(p.level() + v))))
                .then(Commands.literal("xp")
                        .then(change("set", false, IntegerArgumentType.integer(0),
                                (p, v) -> p.withXp(v, LevelCurveLoader.curve())))
                        .then(change("add", true, IntegerArgumentType.integer(0),
                                (p, v) -> p.addXp(v, LevelCurveLoader.curve()))))
                .then(Commands.literal("query").then(Commands.argument("target", EntityArgument.player())
                        .executes(FishingCommand::query))));
    }

    /** {@code showXpNotice}: send the chat XP notice (only for "xp add"). */
    private static LiteralArgumentBuilder<CommandSourceStack> change(String name, boolean showXpNotice,
            ArgumentType<Integer> type, BiFunction<FishingProgress, Integer, FishingProgress> op) {
        return Commands.literal(name).then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("value", type).executes(ctx -> {
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                    int value = IntegerArgumentType.getInteger(ctx, "value");
                    for (ServerPlayer p : targets) {
                        FishingProgress after = op.apply(FishingData.get(p), value);
                        FishingService.change(p, after, showXpNotice ? value : 0);
                    }
                    ctx.getSource().sendSuccess(
                            () -> Component.translatable("commands.fishinggui.updated", targets.size()), true);
                    return targets.size();
                })));
    }

    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "target");
        FishingProgress progress = FishingData.get(p);
        int need = LevelCurveLoader.curve().xpToNext(progress.level());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.fishinggui.query",
                p.getDisplayName(), progress.level(), progress.xp(), need), false);
        return progress.level();
    }
}
