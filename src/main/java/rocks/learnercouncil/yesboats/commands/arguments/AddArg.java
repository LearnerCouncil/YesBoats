package rocks.learnercouncil.yesboats.commands.arguments;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaEditor;
import rocks.learnercouncil.yesboats.commands.CommandArgument;
import rocks.learnercouncil.yesboats.commands.CommandResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static rocks.learnercouncil.yesboats.commands.CommandResult.*;

public class AddArg implements CommandArgument {
    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(!args[0].equalsIgnoreCase("add")) return "";
        if(!sender.hasPermission("yesboats.commmands.yesboats.admin")) return NO_PERMISSION;
        if(args.length < 2) return TOO_FEW_ARGS;
        if(args.length > 2) return TOO_MANY_ARGS;

        String name = args[1];
        if(Arena.get(name).isPresent()) return CommandResult.ARENA_EXISTS;
        ArenaEditor.editors.put((Player) sender, new ArenaEditor((Player) sender, new Arena(name)));
        return CommandResult.CREATED;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1)
            return Collections.singletonList("add");
        return new ArrayList<>();
    }
}
