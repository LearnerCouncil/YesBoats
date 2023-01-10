package rocks.learnercouncil.yesboats.commands;


import static org.bukkit.ChatColor.*;

public class CommandResult {

    private static final String prefix = DARK_AQUA + "[YesBoats] ";
    //Errors
    public static final String
            NO_PERMISSION = prefix + RED + "You don't have permission to execute this command.",
            TOO_FEW_ARGS = prefix + RED + "Too few arguments.",
            TOO_MANY_ARGS = prefix + RED + "Too many arguments",
            ARENA_EXISTS = prefix + RED + "That arena already exists.",
            ARENA_NOT_EXIST = prefix + RED + "That arena doesn't exsist.",
            PLAYER_NOT_FOUND = prefix + RED + "Player not found.",
            ALREADY_RUNNING = prefix + RED + "Arena already running.",
            NOT_RUNNING = prefix + RED + "Arena not running.",
            TOO_FEW_PLAYERS = prefix + RED + "Arena doesn't have enough players.",
            NOT_IN_ARENA_SELF = prefix + RED + "You are not in an arena.",
            NOT_IN_ARENA_OTHER = prefix + RED + "That player is not in an arena.",
            ALREADY_IN_ARENA_SELF = prefix + RED + "You are already in an arena.",
            ALREADY_IN_ARENA_OTHER = prefix + RED + "That player is already in an arena.";


    public static String joinedOther(String name) {
        return prefix + AQUA + "Added " + name + " to the game";
    }
    public static String leftOther(String name) {
        return prefix + AQUA + "Added " + name + " to the game";
    }

    //Results
    public static final String
            CREATED = prefix + AQUA + "Arena successfully created. Now editing.",
            REMOVED = prefix + AQUA + "Arena successfully removed.",
            JOINED = prefix + AQUA + "Joined the game.",
            LEFT = prefix + AQUA + "Joined the game.",
            EDITING = prefix + AQUA + "Now editing.",
            STARTED = prefix + AQUA + "Started the game.",
            STOPPED = prefix + AQUA + "Stopped the game.";


}
