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

public class LeaveArg implements CommandArgument {

    private static final YesBoats plugin = YesBoats.getPlugin();

    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("leave")) return CommandResult.none();
        if (!sender.hasPermission(YesBoats.Permissions.USER_COMMANDS)) return CommandResult.noPermission();
        boolean isAdmin = sender.hasPermission(YesBoats.Permissions.ADMIN_COMMANDS);
        if (args.length > (isAdmin ? 2 : 1)) return tooManyArgs();

        Optional<Arena> arenaOptional;
        if (args.length == 2) {
            String playerName = args[1];
            Player player = plugin.getServer().getPlayer(playerName);
            if (player == null) return playerNotFound();
            arenaOptional = Arena.get(player);
            if (arenaOptional.isEmpty()) return CommandResult.notInArenaOther();
            arenaOptional.get().remove(player);
            return leftOther(playerName);
        }
        arenaOptional = Arena.get((Player) sender);
        if (arenaOptional.isEmpty()) return notInArenaSelf();
        arenaOptional.get().remove((Player) sender);
        return left();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("leave");
        if (args.length == 2 && args[0].equalsIgnoreCase("leave") && sender.hasPermission(YesBoats.Permissions.ADMIN_COMMANDS))
            return plugin.getServer()
                    .getOnlinePlayers()
                    .stream()
                    .map(HumanEntity::getName)
                    .collect(Collectors.toList());
        return Collections.emptyList();
    }
}
