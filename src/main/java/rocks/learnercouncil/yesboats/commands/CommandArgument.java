package rocks.learnercouncil.yesboats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface CommandArgument {
    String execute(CommandSender sender, Command cmd, String label, String[] args);
    List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args);
}
