package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class JoinArg implements CommandArgument {

    private static final YesBoats plugin = YesBoats.getPlugin();

    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("join")) return CommandResult.none();
        if (!sender.hasPermission(YesBoats.Permissions.USER_COMMANDS)) return CommandResult.noPermission();
        if (args.length < 2) return tooFewArgs();
        boolean isAdmin = sender.hasPermission(YesBoats.Permissions.ADMIN_COMMANDS);
        if (args.length > (isAdmin ? 3 : 2)) return tooManyArgs();

        Optional<Arena> arenaOptional = Arena.get(args[1]);
        if (arenaOptional.isEmpty()) return arenaNotExist(args[1]);
        Arena arena = arenaOptional.get();
        if (arena.getState() == Arena.State.RUNNING) return alreadyRunning();

        if (args.length == 3) {
            String playerName = args[2];
            Player player = plugin.getServer().getPlayer(playerName);
            if (player == null) return playerNotFound();
            if (Arena.get(player).isPresent()) return CommandResult.alreadyInArenaOther();
            arena.add(player);
            return joinedOther(playerName);
        }
        if (Arena.get((Player) sender).isPresent()) return CommandResult.alreadyInArenaSelf();
        arena.add((Player) sender);
        return joined();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("join");
        if (args.length == 2 && args[0].equalsIgnoreCase("join"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("join") && sender.hasPermission(YesBoats.Permissions.ADMIN_COMMANDS))
            return plugin.getServer()
                    .getOnlinePlayers()
                    .stream()
                    .map(HumanEntity::getName)
                    .collect(Collectors.toList());
        return Collections.emptyList();
    }
}
