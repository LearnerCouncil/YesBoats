package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.*;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class JoinArg implements CommandArgument {

    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 2) return TOO_FEW_ARGS;
        if(args[0].equalsIgnoreCase("join")) {
            Optional<Arena> arenaO = Arena.get(args[1]);
            if(!arenaO.isPresent()) return ARENA_NOT_EXIST;
            arenaO.get().setGameStatus((Player) sender, true);
            return JOINED;
        }
        return "";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 0) return Collections.singletonList("join");
        if(args.length == 1 && args[0].equalsIgnoreCase("join")) return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
