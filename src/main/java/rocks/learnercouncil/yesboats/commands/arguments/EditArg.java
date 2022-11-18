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
import java.util.stream.Collectors;

public class EditArg implements CommandArgument {

    @Override
    public String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 2) return CommandResult.TOO_FEW_ARGS;
        if(args[0].equalsIgnoreCase("edit")) {
            String name = args[1];
            if(!Arena.get(name).isPresent()) return CommandResult.ARENA_NOT_EXIST;
            ArenaEditor.editors.put((Player) sender, new ArenaEditor((Player) sender, new Arena(name)));
            return CommandResult.EDITING;
        }
        return "";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if(args.length == 1) return Collections.singletonList("edit");
        if(args.length == 2 && args[0].equalsIgnoreCase("edit")) return Arena.arenas.stream().map(a -> a.name).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
