package rocks.learnercouncil.yesboats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rocks.learnercouncil.yesboats.commands.arguments.*;

import java.util.Collections;
import java.util.List;

public class YesBoatsCmd implements TabExecutor {

    private final CommandArgument[] ARGUMENTS = { new JoinArg(), new LeaveArg(), new AddArg(), new RemoveArg(), new EditArg(), new StartArg(), new StopArg(), new ReportsArg(), new HelpArg(), };

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("yesboats")) return false;
        if (!(sender instanceof Player)) {
            sender.spigot().sendMessage(CommandResult.needsPlayer(label));
            return true;
        }
        if (!sender.hasPermission("yesboats.commands.yesboats.user")) {
            sender.spigot().sendMessage(CommandResult.noPermission());
            return true;
        }

        sender.spigot().sendMessage(CommandArgument.parseCommand(sender, cmd, label, args, ARGUMENTS));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("yesboats.commands.yesboats.user")) return Collections.emptyList();
        return CommandArgument.parseTabCompletion(sender, cmd, alias, args, ARGUMENTS);
    }
}
