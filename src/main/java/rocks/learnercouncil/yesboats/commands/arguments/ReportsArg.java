package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.DebugPath;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class ReportsArg implements CommandArgument {


    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("reports")) return NONE;
        if (!sender.hasPermission("yesboats.commands.yesboats.admin")) return NO_PERMISSION;
        if (args.length < 2) return TOO_FEW_ARGS;

        String name = args[1];
        if (Arena.get(name).isEmpty()) return ARENA_NOT_EXIST;

        if (args.length == 2) {
            return CommandResult.getReports();
        }

        if (args.length == 3) {
            if (args[2].equalsIgnoreCase("clear")) {
                DebugPath.clearDisplay();
                return CommandResult.CLEARING_PATH;
            }
            int index;
            try {
                index = Integer.parseInt(args[2]);
                DebugPath.display(index, (Player) sender);
                return CommandResult.DISPLAYING_PATH;
            } catch (NumberFormatException e) {
                return CommandResult.invalidNumber(args[2]);
            }
        }
        return TOO_MANY_ARGS;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return Collections.singletonList("reports");
        if (args.length == 2)
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        if (args.length == 3)
            return Collections.singletonList("clear");
        return Collections.emptyList();
    }
}
