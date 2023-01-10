package rocks.learnercouncil.yesboats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
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

        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage("This command must be executed by a player");
            return true;
        }
        if(cmd.getName().equalsIgnoreCase("yesboats")) {
            for (CommandArgument a : arguments) {
                String result = a.execute(sender, cmd, label, args);
                if(!result.isEmpty()) {
                    sender.sendMessage(result);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> arguments;
        List<String> completions = new ArrayList<>();
        arguments = this.arguments.stream().flatMap(arg -> arg.tabComplete(sender, cmd, alias, args).stream()).collect(Collectors.toList());
        StringUtil.copyPartialMatches(args[args.length - 1], arguments, completions);
        return completions;

    }
}
