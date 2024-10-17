package rocks.learnercouncil.yesboats.commands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.Messages;
import rocks.learnercouncil.yesboats.arena.DebugPath;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.Messages.Commands.*;

public class CommandResult {

    private static BaseComponent[] components(String legacy) {
        return TextComponent.fromLegacyText(legacy);
    }

    // Results
    public static BaseComponent[] none() {
        return new BaseComponent[0];
    }

    public static BaseComponent[] created(String name) {
        return components(CREATED.formatted(name));
    }

    public static BaseComponent[] removed(String name) {
        return components(REMOVED.formatted(name));
    }

    public static BaseComponent[] joined() {
        return components(JOINED);
    }

    public static BaseComponent[] left() {
        return components(LEFT);
    }

    public static BaseComponent[] editing(String name) {
        return components(EDITING.formatted(name));
    }

    public static BaseComponent[] started() {
        return components(STARTED);
    }

    public static BaseComponent[] stopped() {
        return components(STOPPED);
    }

    public static BaseComponent[] displayingPath() {
        return components(DISPLAYING_PATH);
    }

    public static BaseComponent[] clearingPath() {
        return components(CLEARING_PATH);
    }

    public static BaseComponent[] joinedOther(String name) {
        return components(JOINED_OTHER.formatted(name));
    }

    public static BaseComponent[] leftOther(String name) {
        return components(LEFT_OTHER.formatted(name));
    }

    // Errors
    public static BaseComponent[] noPermission() {
        return components(NO_PERMISSION);
    }

    public static BaseComponent[] tooFewArgs() {
        return components(TOO_FEW_ARGS);
    }

    public static BaseComponent[] tooManyArgs() {
        return components(TOO_MANY_ARGS);
    }

    public static BaseComponent[] arenaExists(String name) {
        return components(ARENA_EXISTS.formatted(name));
    }

    public static BaseComponent[] arenaNotExist(String name) {
        return components(ARENA_NOT_EXIST.formatted(name));
    }

    public static BaseComponent[] playerNotFound() {
        return components(PLAYER_NOT_FOUND);
    }

    public static BaseComponent[] alreadyRunning() {
        return components(ALREADY_RUNNING);
    }

    public static BaseComponent[] notRunning() {
        return components(NOT_RUNNING);
    }

    public static BaseComponent[] tooFewPlayers() {
        return components(TOO_FEW_PLAYERS);
    }

    public static BaseComponent[] notInArenaSelf() {
        return components(NOT_IN_ARENA_SELF);
    }

    public static BaseComponent[] notInArenaOther() {
        return components(NOT_IN_ARENA_OTHER);
    }

    public static BaseComponent[] alreadyInArenaSelf() {
        return components(ALREADY_IN_ARENA_SELF);
    }

    public static BaseComponent[] alreadyInArenaOther() {
        return components(ALREADY_IN_ARENA_OTHER);
    }

    public static BaseComponent[] needsPlayer(String label) {
        return components(NEEDS_PLAYER.formatted(label));
    }

    public static BaseComponent[] invalidArgs(String label) {
        return new ComponentBuilder().appendLegacy(INVALID_ARGS.formatted(label))
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + label + " help"))
                .create();
    }

    public static BaseComponent[] invalidNumber(String number) {
        return components(INVALID_NUMBER.formatted(number));
    }


    // Misc.
    public static BaseComponent[] getReports() {
        ComponentBuilder message = new ComponentBuilder().appendLegacy(Messages.PREFIX)
                .append(" ")
                .appendLegacy(BAR)
                .color(ChatColor.AQUA)
                .append("\n");
        for (DebugPath path : DebugPath.debugPaths) {
            message.append(path.toReport()).color(ChatColor.YELLOW).append("\n");
        }
        message.appendLegacy(BAR).color(ChatColor.AQUA);
        return message.create();
    }

    public static BaseComponent[] getHelpMenu(String label, CommandSender sender) {
        // @formatter:off
        return new HelpMenuBuilder(label, sender)
                .withoutPermission("yesboats.commands.yesboats.admin", b -> b
                        .add("join <arena>", "Joins the specified arena.", "join Racetrack")
                        .add("leave <arena>", "Leaves the specified arena.", "leave Racetrack")
                ).withPermission("yesboats.commands.yesboats.admin", b -> b
                        .add("join <arena> <player>", "Forces the specified player to join the specified arena.", "join Racetrack Herobrine")
                        .add("leave <arena> <player>", "Forces the specified player to leave the specified arena.", "leave Racetrack Herobrine")
                        .add("add <name>", "Creates an arena with the specified name, and enters that arena's editor.", "add Racetrack")
                        .add("remove <arena>", "Removes the specified arena.", "remove Racetrack")
                        .add("edit <arena>", "Enters the editor of the specified arena.", "edit Racetrack")
                        .add("start [arena]", "Starts the specified arena, or the current arena if unspecified.", "start Racetrack", "start")
                        .add("stop [arena]", "Stops the specified arena, or the current arena if unspecified.", "stop Racetrack", "stop")
                )
                .create();
        // @formatter:on
    }

    private static class HelpMenuBuilder {
        private final ComponentBuilder helpMenu;
        private final String label;
        private final CommandSender sender;

        private HelpMenuBuilder(String label, CommandSender sender) {
            this.label = label;
            this.sender = sender;
            helpMenu = new ComponentBuilder().appendLegacy(Messages.PREFIX)
                    .append(" ")
                    .appendLegacy(BAR)
                    .color(ChatColor.AQUA)
                    .append("\n");
        }

        public HelpMenuBuilder withoutPermission(String permission, Consumer<HelpMenuBuilder> builder) {
            if (!sender.hasPermission(permission)) builder.accept(this);
            return this;
        }

        public HelpMenuBuilder withPermission(String permission, Consumer<HelpMenuBuilder> builder) {
            if (sender.hasPermission(permission)) builder.accept(this);
            return this;
        }

        public HelpMenuBuilder add(String argument, String description, String... examples) {
            if (examples.length == 0) examples = new String[]{ argument };
            BaseComponent[] argumentExample = new ComponentBuilder(Arrays.stream(examples)
                    .map(e -> "/" + label + " " + e)
                    .collect(Collectors.joining("\n"))).color(ChatColor.YELLOW).create();

            helpMenu.append("/" + label + " " + argument)
                    .color(ChatColor.DARK_AQUA)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(argumentExample)))
                    .append(" - " + description + "\n")
                    .color(ChatColor.AQUA);

            return this;
        }

        public BaseComponent[] create() {
            add("help", "Shows this menu.");
            return helpMenu.appendLegacy(BAR).reset().color(ChatColor.AQUA).create();
        }
    }

}
