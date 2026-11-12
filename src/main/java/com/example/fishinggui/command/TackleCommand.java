package com.example.fishinggui.command;

import com.example.fishinggui.item.TackleRodItem;
import com.example.fishinggui.tackle.Part;
import com.example.fishinggui.tackle.RodSetup;
import com.example.fishinggui.tackle.Style;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code /fishinggui rod <targets> <rod> <line> <hook> [reel] [float] [leader] [lure] [species]}: gives a tackle rod
 * built from the named parts. A part is named by its rank (wood, iron, diamond, netherite) or by its style (big_game,
 * eging, ...), or "none" for the optional ones.
 */
final class TackleCommand {
    private static final List<Part> ORDER = List.of(Part.ROD_BLANK, Part.LINE, Part.HOOK, Part.REEL, Part.FLOAT,
            Part.LEADER, Part.LURE);
    private static final int REQUIRED = 3;
    private static final String NONE = "none";
    private static final String SPECIES = "species";

    private TackleCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        ArgumentBuilder<CommandSourceStack, ?> chain = null;
        for (int i = ORDER.size() - 1; i >= 0; i--) {
            Part part = ORDER.get(i);
            RequiredArgumentBuilder<CommandSourceStack, String> arg = Commands.argument(part.id(),
                    StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(names(), b));
            if (i >= REQUIRED - 1) {
                arg.executes(TackleCommand::give);
            }
            if (i == ORDER.size() - 1) {
                // the lure may also be told which fish it calls
                arg.then(Commands.argument(SPECIES, StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(
                                ForgeRegistries.ITEMS.getKeys().stream().filter(k -> k.getNamespace().equals(
                                        com.example.fishinggui.FishingGuiMod.MOD_ID)), b))
                        .executes(TackleCommand::give));
            }
            if (chain != null) {
                arg.then(chain);
            }
            chain = arg;
        }
        return Commands.literal("rod").then(Commands.argument("targets", EntityArgument.players()).then(chain));
    }

    private static List<String> names() {
        List<String> names = new ArrayList<>(List.of(Part.RANKS));
        for (Style style : Style.values()) {
            names.add(style.id());
        }
        names.add(NONE);
        return names;
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RodSetup setup = new RodSetup();
        for (int i = 0; i < ORDER.size(); i++) {
            Part part = ORDER.get(i);
            String name = value(ctx, part.id());
            if (name == null || name.equals(NONE)) {
                if (i < REQUIRED) {
                    return fail(ctx, part.id());
                }
                continue;
            }
            int rank = indexOf(Part.RANKS, name) + 1;
            Style style = Style.byId(name);
            if (rank == 0 && style == null) {
                return fail(ctx, name);
            }
            setup = rank > 0 ? setup.with(part, rank) : setup.with(part, style.rank(), style);
        }
        String species = value(ctx, SPECIES);
        if (species != null) {
            ResourceLocation id = ResourceLocation.tryParse(species);
            if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) {
                return fail(ctx, species);
            }
            setup = setup.withLureSpecies(id.toString());
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
        for (ServerPlayer player : targets) {
            ItemStack rod = TackleRodItem.assemble(setup, 0);
            if (!player.getInventory().add(rod)) {
                player.drop(rod, false);
            }
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.fishinggui.rod.given", targets.size()), true);
        return targets.size();
    }

    private static String value(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String what) {
        ctx.getSource().sendFailure(Component.translatable("commands.fishinggui.rod.bad", what));
        return 0;
    }
}
