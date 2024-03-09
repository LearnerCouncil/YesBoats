package rocks.learnercouncil.yesboats.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface CommandArgument {
    BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args);
    List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args);
}
