package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StartArg implements CommandArgument {
    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("start")) return "";
        if(!sender.hasPermission("yesboats.commmands.yesboats.admin")) return CommandResult.NO_PERMISSION;
        if(args.length > 2) return CommandResult.TOO_MANY_ARGS;

        Arena arena = null;
        if(args.length == 1) {
            Optional<Arena> arenaOptional = Arena.get((Player) sender);
            if(!arenaOptional.isPresent()) return CommandResult.NOT_IN_ARENA_SELF;
            arena = arenaOptional.get();
        }
        if(args.length == 2) {
            Optional<Arena> arenaOptional = Arena.get(args[1]);
            if(!arenaOptional.isPresent()) return CommandResult.ARENA_NOT_EXIST;
            arena = arenaOptional.get();
        }
        if(arena.getState() == 0) return CommandResult.TOO_FEW_PLAYERS;
        if(arena.getState() == 2) return CommandResult.ALREADY_RUNNING;
        arena.queueTimer.cancel();
        arena.startGame();
        return CommandResult.STARTED;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("start");
        if(args.length == 2 && args[0].equalsIgnoreCase("start"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
