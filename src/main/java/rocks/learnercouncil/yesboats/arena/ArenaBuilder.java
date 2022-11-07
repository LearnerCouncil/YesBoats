package rocks.learnercouncil.yesboats.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public class ArenaBuilder {

    public ArenaBuilder(String name) {
        this.name = name;
    }

    private final String name;
    private int minPlayers, maxPlayers;
    private Location lobbyLocation;
    private World startWorld = null;
    private final List<Location> startLocations = new ArrayList<>();
    private List<Location> lightLocations = new ArrayList<>();
    private final List<BoundingBox> deathBarriers = new ArrayList<>();
    private final List<BoundingBox> checkpointBoxes = new ArrayList<>();
    private final List<Location> checkpointSpawns = new ArrayList<>();

    public ArenaBuilder setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
        return this;
    }

    public ArenaBuilder setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        return this;
    }

    public ArenaBuilder setLobbyLocation(Location lobbyLocation) {
        this.lobbyLocation = lobbyLocation;
        return this;
    }

    public ArenaBuilder addStartLocation(Location location) {
        if(!startLocations.contains(location)) {
            startLocations.add(location);
            if (startWorld == null)
                startWorld = location.getWorld();
        } else {
            startLocations.remove(location);
            if(startLocations.isEmpty()) startWorld = null;
        }
        return this;
    }

    public ArenaBuilder addLightLocation(Location location) {
        if(!lightLocations.contains(location))
            lightLocations.add(location);
        else
            lightLocations.remove(location);
        return this;
    }

    public Arena build() {
        return new Arena(
                name,
                minPlayers,
                maxPlayers,
                lobbyLocation,
                startWorld,
                startLocations,
                lightLocations,
                deathBarriers,
                checkpointBoxes,
                checkpointSpawns
        );
    }

}
