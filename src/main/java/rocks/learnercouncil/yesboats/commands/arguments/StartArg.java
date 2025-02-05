package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class StartArg implements CommandArgument {
    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("start")) return NONE;
        if (!sender.hasPermission("yesboats.commands.yesboats.admin")) return NO_PERMISSION;
        if (args.length > 2) return TOO_MANY_ARGS;

        Arena arena = null;
        if (args.length == 1) {
            Optional<Arena> arenaOptional = Arena.get((Player) sender);
            if (arenaOptional.isEmpty()) return NOT_IN_ARENA_SELF;
            arena = arenaOptional.get();
        }
        if (args.length == 2) {
            Optional<Arena> arenaOptional = Arena.get(args[1]);
            if (arenaOptional.isEmpty()) return ARENA_NOT_EXIST;
            arena = arenaOptional.get();
        }
        if (arena.getState() == Arena.State.WAITING) return TOO_FEW_PLAYERS;
        if (arena.getState() == Arena.State.RUNNING) return ALREADY_RUNNING;
        arena.queueTimer.cancel();
        arena.startGame();
        return STARTED;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1)
            return Collections.singletonList("start");
        if (args.length == 2 && args[0].equalsIgnoreCase("start"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
