package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddArg implements CommandArgument {
    @Override
    public void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length <= 2) return;
        if(args[0].equalsIgnoreCase("add")) {

        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 0) return Collections.singletonList("add");
        return new ArrayList<>();
    }
}
