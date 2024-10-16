package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.Collections;
import java.util.List;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class HelpArg implements CommandArgument {
    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("help")) return NONE;
        if (args.length > 1) return TOO_MANY_ARGS;

        return getHelpMenu(label, sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("help");
        return Collections.emptyList();
    }
}
