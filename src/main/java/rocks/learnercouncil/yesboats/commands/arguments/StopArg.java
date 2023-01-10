package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.List;
import java.util.Optional;

public class StopArg implements CommandArgument {
    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("stop")) return "";
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
        if(arena.state == 0) return CommandResult.NOT_RUNNING;
        if(arena.state == 1) arena.queueTimer.cancel();
        if(arena.state == 2) arena.stopGame();
        return CommandResult.STOPPED;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return null;
    }
}
