package rocks.learnercouncil.yesboats;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Arena implements ConfigurationSerializable {

    public static final List<Arena> arenas = new ArrayList<>();
    private static final YesBoats plugin = YesBoats.getInstance();

    public static int queueTime;

    public static Optional<Arena> get(String name) {
        return arenas.stream().filter(a -> a.name.equals(name)).findFirst();
    }

    public static boolean inGame(Player player) {
            return arenas.stream().anyMatch(a -> a.players.containsKey(player));
    }





    Map<Player, Boat> players = new HashMap<>();


    //serialized feilds
    public final String name;
    public int minPlayers, maxPlayers;
    public Location lobbyLocation;
    private World startWorld;
    public List<Location> startLocations = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
    }

    public void setGameStatus(Player player, boolean join) {
        if(join) {
            if(players.size() == maxPlayers) return;
            player.teleport(startLocations.get(players.size()));
            Boat boat = (Boat) startWorld.spawnEntity(startLocations.get(players.size()), EntityType.BOAT);
            boat.setInvulnerable(true);
            players.put(player, boat);
            boat.addPassenger(player);
            PlayerManager.set(player);
        } else {
            players.get(player).remove();
            player.teleport(lobbyLocation);
            PlayerManager.restore(player);
        }
    }

    public void startQueueTimer() {
        new BukkitRunnable() {
            int timeleft = queueTime;
            @Override
            public void run() {
                if(timeleft == 0) {
                    startGame();
                    cancel();
                }

                timeleft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void startGame() {
        //TODO game logic
    }







    public Arena(Map<String, Object> m) {
        name = (String) m.get("name");

        minPlayers = (int) m.get("minPlayers");
        maxPlayers = (int) m.get("maxPlayers");

        lobbyLocation = stringToLoc((String) m.get("lobbyLocation"));

        startWorld = plugin.getServer().getWorld((String) m.get("startWorld"));
        //noinspection unchecked
        startLocations = vStringsToLocs((List<String>) m.get("startLocations"), startWorld);
    }

    //Methods to change locations to and from their string representations
    private static String locToString(Location loc) {
        String world;
        if(loc.getWorld() == null) {
            plugin.getLogger().severe("World is not loaded. Defaulting to the first loaded world found.");
            world = plugin.getServer().getWorlds().stream().findFirst().orElseThrow(() -> new NullPointerException("Could not find any loaded worlds.")).getName();
        } else {
            world = loc.getWorld().getName();
        }

        return world + ',' +
                loc.getX() + ',' +
                loc.getY() + ',' +
                loc.getZ() + ',' +
                loc.getYaw() + ',' +
                loc.getPitch();
    }
    private static Location stringToLoc(String str) {
        String[] segments = str.split(",");
        return new Location(
                plugin.getServer().getWorld(segments[0]),
                Double.parseDouble(segments[1]),
                Double.parseDouble(segments[2]),
                Double.parseDouble(segments[3]),
                Float.parseFloat(segments[4]),
                Float.parseFloat(segments[5])
        );
    }

    private static List<String> locsToVStrings(List<Location> locs) {
        List<String> result = new ArrayList<>();
        locs.forEach(l -> {
            String str = l.getX() + "," +
                    l.getY() + "," +
                    l.getZ() + ",";
            result.add(str);
        });
        return result;
    }
    private static List<Location> vStringsToLocs(List<String> strs, World world) {
        List<Location> result = new ArrayList<>();
        strs.forEach(s -> {
            String[] segments = s.split(",");
            Location loc = new Location(world,
                    Double.parseDouble(segments[0]),
                    Double.parseDouble(segments[1]),
                    Double.parseDouble(segments[2]));
            result.add(loc);
        });
        return result;
    }

    @Override
    public Map<String, Object> serialize() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();

        m.put("name", name);

        m.put("minPlayers", minPlayers);
        m.put("maxplayers", maxPlayers);

        m.put("lobbyLocaion", locToString(lobbyLocation));


        m.put("startWorld", startWorld.getName());
        m.put("startLocations", locsToVStrings(startLocations));

        return m;
    }
}
