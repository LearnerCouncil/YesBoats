package rocks.learnercouncil.yesboats.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface CommandArgument {
    
    static BaseComponent[] parseCommand(CommandSender sender, Command cmd, String label, String[] args, CommandArgument[] arguments) {
        if(args.length < 1) {
            YesBoats.getInstance().getServer().dispatchCommand(sender, label + " help");
            return CommandResult.NONE;
        }
        for (CommandArgument a : arguments) {
            final BaseComponent[] result = a.execute(sender, cmd, label, args);
            if(result.length != 0) {
                return result;
            }
        }
        //return CommandResult.invalidArgs(label);
        return CommandResult.NONE;
    }
    
    static List<String> parseTabCompletion(CommandSender sender, Command cmd, String alias, String[] args, CommandArgument[] arguments) {
        List<String> completions = new ArrayList<>();
        List<String> argumentCompletions = Arrays.stream(arguments)
                .flatMap(arg -> arg.tabComplete(sender, cmd, alias, args).stream())
                .collect(Collectors.toList());
        if(args.length > 0) StringUtil.copyPartialMatches(args[args.length - 1], argumentCompletions, completions);
        return completions;
    }
        
    BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args);
    List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args);
}
