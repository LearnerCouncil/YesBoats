package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
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
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("reports")) return "";
        if(!sender.hasPermission("yesboats.commands.yesboats.admin")) return NO_PERMISSION;
        if(args.length < 2) return TOO_FEW_ARGS;

        String name = args[1];
        if(!Arena.get(name).isPresent()) return ARENA_NOT_EXIST;
        if(args.length == 2) {
            sender.spigot().sendMessage(getReports());
            return " ";
        } else if(args.length == 3) {
            if(args[2].equalsIgnoreCase("clear")) {
                DebugPath.clearDisplay();
            } else {
                int index;
                try {
                    index = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    return CommandResult.INVALID_NUMBER;
                }
                DebugPath.display(index, (Player) sender);
                return CommandResult.DISPLAYING_PATH;
            }
        }
        return TOO_MANY_ARGS;
    }

    private BaseComponent[] getReports() {
        ComponentBuilder message = new ComponentBuilder("[YesBoats] ").color(ChatColor.DARK_AQUA)
                .append("====================\n").color(ChatColor.AQUA);
        for(DebugPath path : DebugPath.debugPaths) {
            message.append(path.toReport()).append("\n");
        }
        message.append("====================").color(ChatColor.AQUA);
        return message.create();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("reports");
        if(args.length == 2)
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        if(args.length == 3)
            return Collections.singletonList("clear");
        return Collections.emptyList();
    }
}
