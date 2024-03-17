package rocks.learnercouncil.yesboats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import rocks.learnercouncil.yesboats.commands.arguments.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YesBoatsCmd implements TabExecutor {

    private final List<CommandArgument> arguments = initializeArgs();

    private List<CommandArgument> initializeArgs() {
        List<CommandArgument> result = new ArrayList<>();

        result.add(new JoinArg());
        result.add(new LeaveArg());
        result.add(new AddArg());
        result.add(new RemoveArg());
        result.add(new EditArg());
        result.add(new StartArg());
        result.add(new StopArg());
        result.add(new ReportsArg());
        result.add(new HelpArg());

        return result;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if(!cmd.getName().equalsIgnoreCase("yesboats")) return false;
        if(!(sender instanceof Player)) {
            sender.spigot().sendMessage(CommandResult.needsPlayer(label));
            return true;
        }
        if(!sender.hasPermission("yesboats.commands.yesboats.user")) {
            sender.spigot().sendMessage(CommandResult.NO_PERMISSION);
            return true;
        }
        
        sender.spigot().sendMessage(CommandArgument.parseCommand(sender, cmd, label, args, arguments.toArray(CommandArgument[]::new)));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        if(!sender.hasPermission("yesboats.commands.yesboats.user")) return Collections.emptyList();
        return CommandArgument.parseTabCompletion(sender, cmd, alias, args, arguments.toArray(CommandArgument[]::new));
    }
}
