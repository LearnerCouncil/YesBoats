package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JoinArg implements CommandArgument {

    @Override
    public void execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!(args.length <= 1)) return;
        if(args[0].equalsIgnoreCase("join")) {
            //TODO add join logic
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 0) return Collections.singletonList("join");
        if(args.length == 1 && args[0].equalsIgnoreCase("join")) return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
