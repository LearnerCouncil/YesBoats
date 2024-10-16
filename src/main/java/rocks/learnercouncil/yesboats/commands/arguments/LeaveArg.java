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
        if(!args[0].equalsIgnoreCase("leave")) return CommandResult.NONE;
        if(!sender.hasPermission("yesboats.commands.yesboats.user")) return CommandResult.NO_PERMISSION;
        boolean isAdmin = sender.hasPermission("yesboats.commands.yesboats.admin");
        if(args.length > (isAdmin ? 2 : 1)) return TOO_MANY_ARGS;

        Optional<Arena> arenaOptional;
        if(args.length == 2) {
            String playername = args[1];
            Player player = plugin.getServer().getPlayer(playername);
            if(player == null) return PLAYER_NOT_FOUND;
            arenaOptional = Arena.get(player);
            if(arenaOptional.isEmpty()) return CommandResult.NOT_IN_ARENA_OTHER;
            arenaOptional.get().remove(player);
            return leftOther(playername);
        }
        arenaOptional = Arena.get((Player) sender);
        if(arenaOptional.isEmpty()) return NOT_IN_ARENA_SELF;
        arenaOptional.get().remove((Player) sender);
        return LEFT;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("leave");
        if(args.length == 2 && args[0].equalsIgnoreCase("leave") && sender.hasPermission("yesboats.commands.yesboats.admin"))
            return plugin.getServer().getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
