package rocks.learnercouncil.yesboats;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Represents an arena
 */
public class Arena implements ConfigurationSerializable {

    /**
     * A list of all arenas, gets instantiated by 'arenas.yml' if it exists
     */
    public static final List<Arena> arenas = new ArrayList<>();
    private static final YesBoats plugin = YesBoats.getInstance();

    public static int queueTime;

    /**
     * Gets an {@link Arena} object based on its name
     * @param name the name of the arena
     * @return An optional that contains the arena with the given name, if it exists
     */
    public static Optional<Arena> get(String name) {
        return arenas.stream().filter(a -> a.name.equals(name)).findFirst();
    }

    /**
     * @param player a player
     * @return true if the given player in in a game, false otherwise
     */
    public static boolean inGame(Player player) {
            return arenas.stream().anyMatch(a -> a.players.containsKey(player));
    }





    private final Map<Player, Boat> players = new HashMap<>();
    private final Set<ArmorStand> queueStands = new HashSet<>();
    private int state = 0;


    //serialized feilds
    public final String name;
    public int minPlayers, maxPlayers;
    public Location lobbyLocation;
    private World startWorld;
    public List<Location> startLocations = new ArrayList<>();
    public Location lightLocation;
    public BlockFace lightDirection;

    public Arena(String name) {
        this.name = name;
    }

    public void setGameStatus(Player player, boolean join) {
        if(join) {
            if(players.size() == maxPlayers) return;
            Location loc = startLocations.get(players.size());
            player.teleport(loc);
            Boat boat = (Boat) startWorld.spawnEntity(loc, EntityType.BOAT);
            boat.setInvulnerable(true);
            players.put(player, boat);
            boat.addPassenger(player);
            spawnQueueStand(loc).addPassenger(boat);
            PlayerManager.set(player);
        } else {
            if(players.get(player).getVehicle() != null)
                //noinspection ConstantConditions
                players.get(player).getVehicle().remove();
            players.get(player).remove();
            player.teleport(lobbyLocation);
            PlayerManager.restore(player);
        }
    }

    private ArmorStand spawnQueueStand(Location loc) {
        if(loc.getWorld() == null) throw new NullPointerException("queueStand world is null.");
        ArmorStand qs = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        qs.setInvulnerable(true);
        qs.setInvisible(true);
        qs.setSmall(true);
        qs.setMarker(true);
        queueStands.add(qs);
        return qs;
    }

    public void startQueueTimer() {
        state = 1;
        queueStands.forEach(Entity::remove);
        queueStands.clear();
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
        state = 2;
        //TODO game logic
    }







    @SuppressWarnings("unused")
    public Arena(Map<String, Object> m) {
        name = (String) m.get("name");

        minPlayers = (int) m.get("minPlayers");
        maxPlayers = (int) m.get("maxPlayers");

        lobbyLocation = stringToLoc((String) m.get("lobbyLocation"));

        startWorld = plugin.getServer().getWorld((String) m.get("startWorld"));
        //noinspection unchecked
        startLocations = vectorStringToLocList((List<String>) m.get("startLocations"), startWorld);

        lightLocation = vectorStringToLoc((String) m.get("lightLocation"), startWorld);
        lightDirection = BlockFace.valueOf((String) m.get("lightDirection"));
    }

    //Methods to change locations to and from their string representations

    /**
     * Turns a {@link Location} into a String representing it.
     * @param loc The location
     * @return The string representaion of that location
     * @see Arena#stringToLoc(String)
     */
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

    /**
     * Turns a string representation of a {@link Location} into a Location object.
     * @param str The String representing the location.
     * @return  the location object
     * @see Arena#locToString(Location)
     */
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


    /**
     * Turns a {@link Location} into a string representing only it's x, y and z coordinates.
     * @param loc The location
     * @return The string representing it
     * @see Arena#vectorStringToLoc(String, World)
     */
    private static String locToVectorString(Location loc) {
        return loc.getX() + "," + loc.getY() + "," + loc.getZ();
    }

    /**
     * Turns a string reperesenting x, y, and z coordinates into a {@link Location} object, given you provide a world.
     * @param str The string representing the x, y, and z coordinates
     * @param world The world the coordinates are supposed to be in
     * @return The Location object
     * @see Arena#locToVectorString(Location)
     */
    private static Location vectorStringToLoc(String str, World world) {
        String[] segments = str.split(",");
        return new Location(world,
                Double.parseDouble(segments[0]),
                Double.parseDouble(segments[1]),
                Double.parseDouble(segments[2]));
    }


    /**
     * Fuctionally identical to {@link Arena#locToVectorString(Location)} except it works on a List of Locations.
     * @see Arena#vectorStringToLocList(List, World)
     * @param locs The list of Locations
     * @return The list of strings
     */
    private static List<String> locToVectorStringList(List<Location> locs) {
        List<String> result = new ArrayList<>();
        locs.forEach(l -> {
            String str = l.getX() + "," +
                    l.getY() + "," +
                    l.getZ() + ",";
            result.add(str);
        });
        return result;
    }

    /**
     * Fuctionally identical to {@link Arena#vectorStringToLoc(String, World)} except it works on a List of Strings.
     * @see Arena#locToVectorStringList(List)
     * @param strs The list of strings
     * @param world The world
     * @return The list of Locations
     */
    private static List<Location> vectorStringToLocList(List<String> strs, World world) {
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
        m.put("startLocations", locToVectorStringList(startLocations));

        m.put("lightLocation", locToVectorString(lightLocation));

        return m;
    }
}
