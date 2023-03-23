package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class StopArg implements CommandArgument {
    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("stop")) return "";
        if(!sender.hasPermission("yesboats.commands.yesboats.admin")) return NO_PERMISSION;
        if(args.length > 2) return TOO_MANY_ARGS;

        Arena arena = null;
        if(args.length == 1) {
            Optional<Arena> arenaOptional = Arena.get((Player) sender);
            if(!arenaOptional.isPresent()) return NOT_IN_ARENA_SELF;
            arena = arenaOptional.get();
        }
        if(args.length == 2) {
            Optional<Arena> arenaOptional = Arena.get(args[1]);
            if(!arenaOptional.isPresent()) return ARENA_NOT_EXIST;
            arena = arenaOptional.get();
        }
        if(arena.getState() == Arena.State.WAITING) return NOT_RUNNING;
        if(arena.getState() == Arena.State.IN_QUEUE
                || arena.getState() == Arena.State.RUNNING) arena.stopGame();
        return STOPPED;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("stop");
        if(args.length == 2 && args[0].equalsIgnoreCase("stop"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
