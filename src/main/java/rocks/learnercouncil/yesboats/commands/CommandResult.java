package rocks.learnercouncil.yesboats.commands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.arena.DebugPath;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandResult {

    public static final BaseComponent[] NONE = {};
    private static final ChatColor
            PREFIX = ChatColor.DARK_AQUA,
            RESULT = ChatColor.AQUA,
            ERROR = ChatColor.RED,
            SPECIAL = ChatColor.YELLOW;
    public static final BaseComponent[]
            CREATED = result("Arena successfully created. Now editing."),
            REMOVED = result("Arena successfully removed."),
            JOINED = result("Joined the game."),
            LEFT = result("Left the game."),
            EDITING = result("Now editing."),
            STARTED = result("Started the game."),
            STOPPED = result("Stopped the game."),
            DISPLAYING_PATH = result("Now displaying debug path."),
            CLEARING_PATH = result("Clearing displayed debug paths.");
    public static final BaseComponent[]
            NO_PERMISSION = error("You don't have permission to execute this command."),
            TOO_FEW_ARGS = error("Too few arguments."),
            TOO_MANY_ARGS = error("Too many arguments"),
            ARENA_EXISTS = error("That arena already exists."),
            ARENA_NOT_EXIST = error("That arena doesn't exist."),
            PLAYER_NOT_FOUND = error("Player not found."),
            ALREADY_RUNNING = error("Arena already running."),
            NOT_RUNNING = error("Arena not running."),
            TOO_FEW_PLAYERS = error("Arena doesn't have enough players."),
            NOT_IN_ARENA_SELF = error("You are not in an arena."),
            NOT_IN_ARENA_OTHER = error("That player is not in an arena."),
            ALREADY_IN_ARENA_SELF = error("You are already in an arena."),
            ALREADY_IN_ARENA_OTHER = error("That player is already in an arena.");

    private static ComponentBuilder prefix() {
        return new ComponentBuilder().append("[YesBoats] ").color(PREFIX);
    }

    private static BaseComponent[] result(String message) {
        return prefix().append(message).color(RESULT).create();
    }

    private static BaseComponent[] error(String message) {
        return prefix().append(message).color(ERROR).create();
    }

    public static BaseComponent[] joinedOther(String name) {
        return prefix().append("Added ").color(RESULT).append(name).color(SPECIAL).append(" to the game").color(RESULT).create();
    }

    public static BaseComponent[] leftOther(String name) {
        return prefix().append("Removed ").color(RESULT).append(name).color(SPECIAL).append(" to the game").color(RESULT).create();
    }

    public static BaseComponent[] getReports() {
        ComponentBuilder message = prefix()
                .append("====================\n").color(RESULT);
        for (DebugPath path : DebugPath.debugPaths) {
            message.append(path.toReport()).color(SPECIAL).append("\n");
        }
        message.append("====================").color(RESULT);
        return message.create();
    }

    public static BaseComponent[] needsPlayer(String label) {
        return prefix().append("Command '").color(ERROR)
                .append('/' + label).color(SPECIAL)
                .append("' must be executed by a player.").color(ERROR)
                .create();
    }

    public static BaseComponent[] invalidArgs(String label) {
        return prefix().append("Invalid arguments. Try '").color(ERROR)
                .append('/' + label + " help").color(SPECIAL).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, '/' + label + " help"))
                .append("' for help.").color(ERROR)
                .create();
    }

    public static BaseComponent[] invalidNumber(String number) {
        return prefix().append("'").color(ERROR).append(number).color(SPECIAL).append("' is not a valid number.").color(ERROR).create();
    }


    public static BaseComponent[] getHelpMenu(String label, CommandSender sender) {
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
    }

    private static class HelpMenuBuilder {
        private final ComponentBuilder helpMenu;
        private final String label;
        private final CommandSender sender;

        private HelpMenuBuilder(String label, CommandSender sender) {
            this.label = label;
            this.sender = sender;
            helpMenu = prefix().append("====================\n").color(RESULT);
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
            if (examples.length == 0) examples = new String[]{argument};
            BaseComponent[] argumentExample = new ComponentBuilder(Arrays.stream(examples).map(e -> "/" + label + " " + e).collect(Collectors.joining("\n"))).color(SPECIAL).create();

            helpMenu.append("/" + label + " " + argument).color(PREFIX)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(argumentExample)))
                    .append(" - " + description + "\n").color(RESULT);

            return this;
        }

        public BaseComponent[] create() {
            add("help", "Shows this menu.");
            return helpMenu.append("====================").reset().color(RESULT).create();
        }
    }

}
