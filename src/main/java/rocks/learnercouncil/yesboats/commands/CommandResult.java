package rocks.learnercouncil.yesboats.commands;


import static org.bukkit.ChatColor.*;

public class CommandResult {

    private static final String prefix = DARK_AQUA + "[YesBoats] ";
    //Errors
    public static final String
            TOO_FEW_ARGS = prefix + RED + "Too few arguments.",
            ARENA_EXISTS = prefix + RED + "That arena already exists.",
            ARENA_NOT_EXIST = prefix + RED + "That arena doesn't exsist.";
    //Results
    public static final String
            ARENA_CREATED = prefix + AQUA + "Arena successfully created.",
            JOINED = prefix + AQUA + "Joined the game.";
}
