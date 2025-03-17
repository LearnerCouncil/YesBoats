package rocks.learnercouncil.yesboats;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rocks.learnercouncil.yesboats.arena.Arena;

import java.util.Optional;

import static rocks.learnercouncil.yesboats.Messages.Placeholders.*;

public class PlaceholderManager extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "yesboats";
    }

    @Override
    public @NotNull String getAuthor() {
        return "h2ofiremaster";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    /*
    %yesboats_<arena>_name%
    %yesboats_<arena>_status%
    %yesboats_<arena>_maxplayers%
    %yesboats_<arena>_minplayers%
    %yesboats_<arena>_players%
     */
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String[] parts = params.split("_");
        if (parts.length != 2) return "";
        Optional<Arena> arenaOptional = Arena.get(parts[0]);
        if (arenaOptional.isPresent()) {
            Arena arena = arenaOptional.get();
            switch (parts[1]) {
                case "name" -> {
                    return arena.name;
                }
                case "status" -> {
                    return switch (arena.getState()) {
                        case WAITING -> STATUS_WAITING;
                        case IN_QUEUE -> STATUS_STARTING;
                        case RUNNING -> STATUS_RUNNING;
                    };
                }
                case "maxplayers" -> {
                    return String.valueOf(arena.getMaxPlayers());
                }
                case "minplayers" -> {
                    return String.valueOf(arena.getMinPlayers());
                }
                case "players" -> {
                    return String.valueOf(arena.getPlayers().size());
                }
                default -> {
                    return "";
                }
            }
        } else {
            return ARENA_NOT_FOUND;
        }
    }
}
