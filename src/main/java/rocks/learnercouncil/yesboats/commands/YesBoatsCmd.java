package rocks.learnercouncil.yesboats.commands;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import rocks.learnercouncil.yesboats.commands.arguments.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        if(!(sender instanceof Player)) {
            sender.sendMessage("[YesBoats] This command must be executed by a player");
            return true;
        }
        if(cmd.getName().equalsIgnoreCase("yesboats")) {
            for (CommandArgument a : arguments) {
                final BaseComponent[] result = a.execute(sender, cmd, label, args);
                if(result.length != 0) {
                    sender.spigot().sendMessage(result);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> arguments;
        List<String> completions = new ArrayList<>();
        arguments = this.arguments.stream().flatMap(arg -> arg.tabComplete(sender, cmd, alias, args).stream()).collect(Collectors.toList());
        StringUtil.copyPartialMatches(args[args.length - 1], arguments, completions);
        return completions;

    }
}
