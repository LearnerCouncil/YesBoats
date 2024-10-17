package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaEditor;
import rocks.learnercouncil.yesboats.commands.CommandArgument;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class EditArg implements CommandArgument {

    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("edit")) return none();
        if (!sender.hasPermission("yesboats.commands.yesboats.admin")) return noPermission();
        if (args.length < 2) return tooFewArgs();
        if (args.length > 2) return tooManyArgs();

        String name = args[1];
        if (Arena.get(name).isEmpty()) return arenaNotExist(name);

        ArenaEditor.editors.put((Player) sender, new ArenaEditor((Player) sender, Arena.get(name).get().clone()));
        return editing(name);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("edit");
        if (args.length == 2 && args[0].equalsIgnoreCase("edit"))
            return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
