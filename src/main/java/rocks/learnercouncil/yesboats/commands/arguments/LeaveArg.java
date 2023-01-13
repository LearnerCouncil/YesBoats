package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class LeaveArg implements CommandArgument {

    private static final YesBoats plugin = YesBoats.getInstance();

    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("leave")) return "";
        if(!sender.hasPermission("yesboats.commmands.yesboats.user")) return CommandResult.NO_PERMISSION;
        if(args.length < 2) return TOO_FEW_ARGS;
        boolean isAdmin = sender.hasPermission("yesboats.commands.yesboats.admin");
        if(args.length > (isAdmin ? 3 : 2)) return TOO_MANY_ARGS;

        Optional<Arena> arenaOptional = Arena.get(args[1]);
        if(!arenaOptional.isPresent()) return ARENA_NOT_EXIST;
        if(args.length == 3) {
            String playername = args[2];
            Player player = plugin.getServer().getPlayer(playername);
            if(player == null) return PLAYER_NOT_FOUND;
            if(!Arena.get(player).isPresent()) return CommandResult.NOT_IN_ARENA_OTHER;
            arenaOptional.get().setGameStatus(player, false);
            return leftOther(playername);
        }
        if(!Arena.get((Player) sender).isPresent()) return CommandResult.NOT_IN_ARENA_SELF;
        arenaOptional.get().setGameStatus((Player) sender, false);
        return LEFT;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("leave");
        if(args.length == 2 && args[0].equalsIgnoreCase("leave"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        if(args.length == 3 && args[0].equalsIgnoreCase("leave") && sender.hasPermission("yesboats.commands.yesboats.admin"))
            return plugin.getServer().getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
