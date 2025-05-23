package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class StopArg implements CommandArgument {
    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("stop")) return none();
        if (!sender.hasPermission(YesBoats.Permissions.ADMIN_COMMANDS)) return noPermission();
        if (args.length > 2) return tooManyArgs();

        Arena arena = null;
        if (args.length == 1) {
            Optional<Arena> arenaOptional = Arena.get((Player) sender);
            if (arenaOptional.isEmpty()) return notInArenaSelf();
            arena = arenaOptional.get();
        }
        if (args.length == 2) {
            Optional<Arena> arenaOptional = Arena.get(args[1]);
            if (arenaOptional.isEmpty()) return arenaNotExist(args[1]);
            arena = arenaOptional.get();
        }
        if (arena.getState() == Arena.State.WAITING) return notRunning();
        if (arena.getState() == Arena.State.IN_QUEUE || arena.getState() == Arena.State.RUNNING) arena.stopGame();
        return stopped();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("stop");
        if (args.length == 2 && args[0].equalsIgnoreCase("stop"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
