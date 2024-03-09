package rocks.learnercouncil.yesboats.commands;


import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.arena.DebugPath;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommandResult {

    private static final ChatColor PREFIX = ChatColor.DARK_AQUA;
    private static final ChatColor RESULT = ChatColor.AQUA;
    private static final ChatColor ERROR = ChatColor.RED;
    private static final ChatColor SPECIAL = ChatColor.YELLOW;
    
    private static ComponentBuilder prefix() {
        return new ComponentBuilder().append("[YesBoats] ").color(PREFIX);
    }
    
    public static final BaseComponent[] NONE = {};
    
    //Errors
    public static final BaseComponent[]
            NO_PERMISSION = prefix().append("You don't have permission to execute this command.").color(ERROR).create(),
            TOO_FEW_ARGS = prefix().append("Too few arguments.").color(ERROR).create(),
            TOO_MANY_ARGS = prefix().append("Too many arguments").color(ERROR).create(),
            ARENA_EXISTS = prefix().append("That arena already exists.").color(ERROR).create(),
            ARENA_NOT_EXIST = prefix().append("That arena doesn't exist.").color(ERROR).create(),
            PLAYER_NOT_FOUND = prefix().append("Player not found.").color(ERROR).create(),
            ALREADY_RUNNING = prefix().append("Arena already running.").color(ERROR).create(),
            NOT_RUNNING = prefix().append("Arena not running.").color(ERROR).create(),
            TOO_FEW_PLAYERS = prefix().append("Arena doesn't have enough players.").color(ERROR).create(),
            NOT_IN_ARENA_SELF = prefix().append("You are not in an arena.").color(ERROR).create(),
            NOT_IN_ARENA_OTHER = prefix().append("That player is not in an arena.").color(ERROR).create(),
            ALREADY_IN_ARENA_SELF = prefix().append("You are already in an arena.").color(ERROR).create(),
            ALREADY_IN_ARENA_OTHER = prefix().append("That player is already in an arena.").color(ERROR).create(),
            INVALID_NUMBER = prefix().append("Invalid number.").color(ERROR).create();


    //Results
    public static BaseComponent[] joinedOther(String name) {
        return prefix().append("Added ").color(RESULT).append(name).color(SPECIAL).append(" to the game").color(RESULT).create();
    }
    public static BaseComponent[] leftOther(String name) {
        return prefix().append("Removed ").color(RESULT).append(name).color(SPECIAL).append(" to the game").color(RESULT).create();
    }
    public static BaseComponent[] getReports() {
        ComponentBuilder message = prefix()
                .append("====================\n").color(RESULT);
        for(DebugPath path : DebugPath.debugPaths) {
            message.append(path.toReport()).color(SPECIAL).append("\n");
        }
        message.append("====================").color(RESULT);
        return message.create();
    }

    public static final BaseComponent[]
            CREATED = prefix().append("Arena successfully created. Now editing.").color(RESULT).create(),
            REMOVED = prefix().append("Arena successfully removed.").color(RESULT).create(),
            JOINED = prefix().append("Joined the game.").color(RESULT).create(),
            LEFT = prefix().append("Left the game.").color(RESULT).create(),
            EDITING = prefix().append("Now editing.").color(RESULT).create(),
            STARTED = prefix().append("Started the game.").color(RESULT).create(),
            STOPPED = prefix().append("Stopped the game.").color(RESULT).create(),
            DISPLAYING_PATH = prefix().append("Now displaying debug path.").color(RESULT).create(),
            CLEARING_PATH = prefix().append("Clearing desplayed debug paths.").color(RESULT).create();
    
    public static BaseComponent[] getHelpMenu(String label, CommandSender sender) {
        return new HelpMenuBuilder(label, sender)
                .add("join <arena>", "Joins the specified arena.", "join Racetrack")
                .add("leave <arena>", "Leaves the specified arena.", "leave Racetrack")
                .withPermission("yesboats.commands.yesboats.admin", b -> b
                        .add("add <name>", "Creates an arena with the specified name, and enters that arena's editor.", "add Racetrack")
                        .add("remove <arena>", "Removes the specified arena.", "remove Racetrack")
                        .add("edit <arena>", "Enters the editor of the specified arena.", "edit Racetrack")
                        .add("start [arena]", "Starts the specified arena, or the current arena if unspecified.", "start Racetrack", "start")
                        .add("stop [arena]", "Stops the specified arena, or the current arena if unspecified.", "stop Racetrack", "stop")
                        .add("join <arena> <player>", "Forces the specified player to join the specified arena.", "join Racetrack Herobrine")
                        .add("leave <arena> <player>", "Forces the specified player to leave the specified arena.", "leave Racetrack Herobrine")
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
        
        public HelpMenuBuilder withPermission(String permission, Consumer<HelpMenuBuilder> builder) {
            if(sender.hasPermission(permission)) builder.accept(this);
            return this;
        }
        
        public HelpMenuBuilder add(String argument, String description, String... examples) {
            if(examples.length == 0) examples = new String[] {argument};
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
