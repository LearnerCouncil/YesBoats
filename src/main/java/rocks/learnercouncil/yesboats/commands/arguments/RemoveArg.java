package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class RemoveArg implements CommandArgument {
    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("remove")) return none();
        if (!sender.hasPermission(YesBoats.Permissions.ADMIN_COMMANDS)) return noPermission();
        if (args.length < 2) return tooFewArgs();
        if (args.length > 2) return tooManyArgs();
        String name = args[1];
        Optional<Arena> arenaOptional = Arena.get(name);
        if (arenaOptional.isEmpty()) return arenaNotExist(name);
        Arena.arenas.remove(arenaOptional.get());
        return removed(name);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("remove");
        if (args.length == 2 && args[0].equalsIgnoreCase("remove"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
