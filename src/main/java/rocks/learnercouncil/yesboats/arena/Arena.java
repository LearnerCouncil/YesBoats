package rocks.learnercouncil.yesboats.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import rocks.learnercouncil.yesboats.PlayerManager;
import rocks.learnercouncil.yesboats.YesBoats;

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

    private static final HashMap<Player, Arena> playerArenaMap = new HashMap<>();
    /**
     * Gets an {@link Arena} object based on a {@link Player}
     * @param player the player
     * @return An optional that contains the arena containing the given player, if it exists
     */
    public static Optional<Arena> get(Player player) {
        return Optional.ofNullable(playerArenaMap.get(player));
    }

    //unserialized feilds
    private final List<Player> players = new ArrayList<>();
    private final Set<ArmorStand> queueStands = new HashSet<>();
    /**
     * The state the game is in: 0 = idle, 1 = in queue, 2 = running.
     */
    public int state = 0;
    public BukkitTask queueTimer;
    private BukkitTask mainLoop;
    private final Map<Player, GameData> gameData = new HashMap<>();


    //serialized feilds
    public final String name;
    protected int minPlayers;
    protected Location lobbyLocation;
    protected Location startLineActivator;
    protected World startWorld;
    protected List<Location> startLocations = new ArrayList<>();
    protected List<Location> lightLocations = new ArrayList<>();
    protected List<BoundingBox> deathBarriers = new ArrayList<>();
    protected List<BoundingBox> checkpointBoxes = new ArrayList<>();
    protected List<Location> checkpointSpawns = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
        this.minPlayers = 1;
    }


    /**
     * Adds/removes a player from the arena.
     * @param player The player.
     * @param join 'true' to add the player to the arena, 'false' to remove them.
     */
    public void setGameStatus(Player player, boolean join) {
        if(join) {
            //failsafe
            if(players.size() == startLocations.size()) return;

            Location loc = startLocations.get(players.size());
            player.teleport(loc);
            Boat boat = (Boat) startWorld.spawnEntity(loc, EntityType.BOAT);
            boat.setInvulnerable(true);

            players.add(player);
            playerArenaMap.put(player, this);
            gameData.put(player, new GameData());

            boat.addPassenger(player);
            spawnQueueStand(loc).addPassenger(boat);
            PlayerManager.set(player);

            if(players.size() <= minPlayers) startQueueTimer();
        } else {
            //failsafe
            if(!Arena.get(player).isPresent()) return;

            removeVehicle(player);

            player.teleport(lobbyLocation);
            PlayerManager.restore(player);

            players.remove(player);
            playerArenaMap.remove(player);
            gameData.remove(player);

            if(state == 1)
                if(players.size() < minPlayers) queueTimer.cancel();
            if(state == 2)
                if(players.size() == 0) stopGame();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void removeVehicle(Player player) {
        if (!player.isInsideVehicle()) return;
        Entity vehicle = player.getVehicle();
        if (vehicle.isInsideVehicle())
            vehicle.getVehicle().remove();
        vehicle.getPassengers().forEach(p -> {
            if(p.getType() != EntityType.PLAYER) p.remove();
        });
        vehicle.remove();
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
        queueTimer = new BukkitRunnable() {
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


    /**
     * Starts the game
     */
    public void startGame() {
        state = 2;
        queueStands.forEach(Entity::remove);
        queueStands.clear();
        players.forEach(p -> p.getInventory().clear());
        final Iterator<Location> lightsIterator = lightLocations.iterator();
        //TODO game logic
        mainLoop = new BukkitRunnable() {
            int prestartTimer = 0;
            @Override
            public void run() {
                if(prestartTimer != -1) {
                    prestartTimer++;
                    if(prestartTimer % 20 != 0) return;

                    Block block = lightsIterator.next().getBlock();
                    block.setType(Material.REDSTONE_LAMP);
                    ((Lightable) block.getBlockData()).setLit(true);
                    if(!lightsIterator.hasNext()) {
                        lightLocations.forEach(l -> ((Lightable) l.getBlock().getBlockData()).setLit(false));
                        startLineActivator.getBlock().setType(Material.RED_CONCRETE);
                    }
                }
                players.forEach(p -> deathBarriers.forEach(b -> {
                    if(b.contains(p.getLocation().toVector())) respawn(p);
                }));
                players.forEach(p -> updateCheckpoint(p));
            }
        }.runTaskTimer(plugin, 0, 1);

    }

    /**
     * Stops the game.
     */
    public void stopGame() {
        state = 0;
        mainLoop.cancel();
        players.forEach(p -> {
            p.teleport(lobbyLocation);
            PlayerManager.restore(p);
        });
        players.clear();
        playerArenaMap.clear();
        gameData.clear();
        startLineActivator.getBlock().setType(Material.REDSTONE_BLOCK);

        //todo game stop logic
    }

    private void updateCheckpoint(Player player) {
        int previousCheckpoint = gameData.get(player).checkpoint;
        for (BoundingBox b : checkpointBoxes) {
            int currentCheckpoint = checkpointBoxes.indexOf(b);
            if(!b.contains(player.getLocation().toVector())) continue;
            if(currentCheckpoint == previousCheckpoint) continue;

            if(currentCheckpoint == previousCheckpoint + 1)
                gameData.get(player).checkpoint = currentCheckpoint;
            if(currentCheckpoint == 0 && previousCheckpoint == checkpointBoxes.size() - 1) {
                gameData.get(player).lap += 1;
                gameData.get(player).checkpoint = currentCheckpoint;
            }
        }
    }

    /**
     * Respawns the specified player at the last checkpoint they passed.
     * @param player The player to teleport
     */
    private void respawn(Player player) {
        if(player.getVehicle() == null) return;
        player.getVehicle().teleport(checkpointSpawns.get(gameData.get(player).checkpoint));
    }


    @SuppressWarnings({"unused", "unchecked"})
    public Arena(Map<String, Object> m) {
        name = (String) m.get("name");

        startWorld = plugin.getServer().getWorld((String) m.get("startWorld"));

        minPlayers = (int) m.get("minPlayers");

        lobbyLocation = stringToLoc((String) m.get("lobbyLocation"));
        startLineActivator = stringToLoc((String) m.get("startLineActivator"));

        startLocations = vectorStringToLocList((List<String>) m.get("startLocations"), startWorld);
        lightLocations = vectorStringToLocList((List<String>) m.get("lightLocations"), startWorld);

        deathBarriers = stringToBoxList((List<String>) m.get("deathBarriers"));

        checkpointBoxes = stringToBoxList((List<String>) m.get("checkpointBoxes"));
        checkpointSpawns = stringToLocList((List<String>) m.get("checkpointSpawns"));
    }

    //Methods to change locations to and from their string representations

    /**
     * Turns a {@link Location} into a String representing it.
     * @param loc The location
     * @return The string representaion of that location
     * @see Arena#stringToLoc(String)
     */
    private static String locToString(Location loc) {
        if(loc == null) return "";
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
        if(str.isEmpty()) return null;
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
     * Fuctionally identical to {@link Arena#locToString(Location)} except it works on a List of Locations.
     * @see Arena#stringToLocList(List)
     * @param locs The list of Locations
     * @return The list of strings
     */
    private static List<String> locToStringList(List<Location> locs) {
        List<String> result = new ArrayList<>();
        locs.forEach(l -> {
            String world;
            if(l.getWorld() == null) {
                plugin.getLogger().severe("World is not loaded. Defaulting to the first loaded world found.");
                world = plugin.getServer().getWorlds().stream().findFirst().orElseThrow(() -> new NullPointerException("Could not find any loaded worlds.")).getName();
            } else {
                world = l.getWorld().getName();
            }
            String str = world + "," +
                    l.getX() + "," +
                    l.getY() + "," +
                    l.getZ() + "," +
                    l.getYaw() + "," +
                    l.getPitch();
            result.add(str);
        });
        return result;
    }
    /**
     * Fuctionally identical to {@link Arena#stringToLoc(String)} except it works on a List of Strings.
     * @see Arena#locToStringList(List)
     * @param strs The list of strings
     * @return The list of Locations
     */
    private static List<Location> stringToLocList(List<String> strs) {
        List<Location> result = new ArrayList<>();
        strs.forEach(s -> {
            String[] segments = s.split(",");
            Location loc = new Location(plugin.getServer().getWorld(segments[0]),
                    Double.parseDouble(segments[1]),
                    Double.parseDouble(segments[2]),
                    Double.parseDouble(segments[3]),
                    Float.parseFloat(segments[4]),
                    Float.parseFloat(segments[5]));
            result.add(loc);
        });
        return result;
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

    /**
     * Turns a list of {@link BoundingBox}es into their string representations.
     * @see Arena#stringToBoxList(List)
     * @param boxes The list of bounding boxes
     * @return The string representation
     */
    private static List<String> boxToStringList(List<BoundingBox> boxes) {
        List<String> result = new ArrayList<>();
        boxes.forEach(b -> {
            String str = b.getMinX() + "," + b.getMinY() + "," + b.getMinZ() + "," + b.getMaxX() + "," + b.getMaxY() + "," + b.getMaxZ();
            result.add(str);
        });
        return result;
    }
    /**
     * Turns a list of strings representing bounding boxes into the {@link BoundingBox} objects.
     * @see Arena#boxToStringList(List)
     * @param strs The list of strings
     * @return The list of bounding boxes
     */
    private static List<BoundingBox> stringToBoxList(List<String> strs) {
        List<BoundingBox> result = new ArrayList<>();
        strs.forEach(s -> {
            String[] segments = s.split(",");
            BoundingBox box = new BoundingBox(
                    Double.parseDouble(segments[0]),
                    Double.parseDouble(segments[1]),
                    Double.parseDouble(segments[2]),
                    Double.parseDouble(segments[3]),
                    Double.parseDouble(segments[4]),
                    Double.parseDouble(segments[5])
            );
            result.add(box);
        });
        return result;
    }

    @Override
    public Map<String, Object> serialize() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();

        m.put("name", name);

        m.put("minPlayers", minPlayers);

        m.put("lobbyLocaion", locToString(lobbyLocation));
        m.put("startLineActivator", locToString(startLineActivator));

        m.put("startWorld", startWorld.getName());
        m.put("startLocations", locToVectorStringList(startLocations));

        m.put("lightLocations", locToVectorStringList(lightLocations));

        m.put("deathBarriers", boxToStringList(deathBarriers));

        m.put("checkpointBoxes", boxToStringList(checkpointBoxes));
        m.put("checkpointSpawns", locToStringList(checkpointSpawns));

        return m;
    }

    private static class GameData {
        int checkpoint = 0;
        int lap = 0;
    }
}
