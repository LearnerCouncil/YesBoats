package rocks.learnercouncil.yesboats.commands.arguments;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaEditor;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.Collections;
import java.util.List;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class AddArg implements CommandArgument {
    @Override
    public BaseComponent[] execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!args[0].equalsIgnoreCase("add")) return CommandResult.none();
        if (!sender.hasPermission(YesBoats.Permissions.USER_COMMANDS)) return noPermission();
        if (args.length < 2) return tooFewArgs();
        if (args.length > 2) return tooManyArgs();

        String name = args[1];
        if (Arena.get(name).isPresent()) return CommandResult.arenaExists(name);
        ArenaEditor.editors.put((Player) sender, new ArenaEditor((Player) sender, new Arena(name)));
        return CommandResult.created(name);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Collections.singletonList("add");
        return Collections.emptyList();
    }
}
