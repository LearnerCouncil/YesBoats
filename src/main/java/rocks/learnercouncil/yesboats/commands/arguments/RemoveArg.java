package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RemoveArg implements CommandArgument {
    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("remove")) return "";
        if(!sender.hasPermission("yesboats.commmands.yesboats.admin")) return CommandResult.NO_PERMISSION;
        if(args.length < 2) return CommandResult.TOO_FEW_ARGS;
        if(args.length > 2) return CommandResult.TOO_MANY_ARGS;
        Optional<Arena> arenaOptional = Arena.get(args[1]);
        if(!arenaOptional.isPresent()) return CommandResult.ARENA_NOT_EXIST;
        Arena.arenas.remove(arenaOptional.get());
        return CommandResult.REMOVED;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("remove");
        if(args.length == 2)
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
