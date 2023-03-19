package rocks.learnercouncil.yesboats.arena;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.BoundingBox;
import rocks.learnercouncil.yesboats.PlayerManager;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.*;
import java.util.stream.Collectors;

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

    public static Arena copy(Arena other) {
        Arena copy = new Arena(other.name);
        copy.minPlayers = other.minPlayers;
        copy.laps = other.laps;
        copy.time = other.time;
        copy.lobbyLocation = other.lobbyLocation;
        copy.startLineActivator = other.startLineActivator;
        copy.startWorld = other.startWorld;
        copy.startLocations = other.startLocations;
        copy.lightLocations = other.lightLocations;
        copy.deathBarriers = other.deathBarriers;
        copy.checkpointBoxes = other.checkpointBoxes;
        copy.checkpointSpawns = other.checkpointSpawns;
        copy.signs = other.signs;
        return copy;
    }

    //unserialized feilds
    private final List<Player> players = new ArrayList<>();
    public List<Player> getPlayers() {
        return players;
    }
    private final Set<ArmorStand> queueStands = new HashSet<>();
    public BukkitTask queueTimer;
    private BukkitTask mainLoop;
    private final Map<Player, GameData> gameData = new HashMap<>();
    private int currentPlace = 1;

    private State state = State.WAITING;
    public State getState() {
        return state;
    }
    public enum State {
        WAITING, IN_QUEUE, RUNNING
    }

    //serialized feilds
    public final String name;
    protected int minPlayers = 1;
    protected int laps = 1;
    protected int time = 300;
    protected Location lobbyLocation;
    protected Location startLineActivator;
    protected World startWorld;
    protected List<Location> startLocations = new ArrayList<>();
    protected List<Location> lightLocations = new ArrayList<>();
    protected List<BoundingBox> deathBarriers = new ArrayList<>();
    protected List<BoundingBox> checkpointBoxes = new ArrayList<>();
    protected List<Location> checkpointSpawns = new ArrayList<>();
    protected Set<ArenaSign> signs = new HashSet<>();

    public Arena(String name) {
        this.name = name;
    }

    /**
     * Updates arena signs
     */
    public void updateSigns() {
        signs.forEach(s -> s.update(players.size(), startLocations.size()));
    }

    /**
     * Adds/removes a player from the arena.
     * @param player The player.
     * @param join 'true' to add the player to the arena, 'false' to remove them.
     */
    public void setGameStatus(Player player, boolean join) {
        ScoreboardManager scoreboardManager = plugin.getServer().getScoreboardManager();
        assert scoreboardManager != null;
        if(join) {
            //failsafe
            if(players.size() == startLocations.size()) return;

            Location location = startLocations.get(players.size());
            player.teleport(location);
            Boat boat = (Boat) startWorld.spawnEntity(location, EntityType.BOAT);
            boat.setInvulnerable(true);

            players.add(player);
            playerArenaMap.put(player, this);
            gameData.put(player, new GameData(player, scoreboardManager.getNewScoreboard()));
            PlayerManager.set(player);
            updateSigns();

            spawnQueueStand(location).addPassenger(boat);
            boat.addPassenger(player);

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
            player.setScoreboard(scoreboardManager.getMainScoreboard());
            updateSigns();

            if(state == State.IN_QUEUE) {
                if(players.size() < minPlayers) {
                    queueTimer.cancel();
                    players.forEach(p -> gameData.get(p).scoreboard.updateScores(-1));
                }
            }
            if(state == State.RUNNING)
                if(players.size() == 0) stopGame();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void removeVehicle(Player player) {
        if (!player.isInsideVehicle()) return;
        Entity vehicle = player.getVehicle();
        if (vehicle.isInsideVehicle())
            vehicle.getVehicle().remove();
        vehicle.getPassengers().forEach(passenger -> {
            if(passenger.getType() != EntityType.PLAYER) passenger.remove();
        });
        vehicle.remove();
    }

    private ArmorStand spawnQueueStand(Location location) {
        if(location.getWorld() == null) throw new NullPointerException("queueStand world is null.");
        ArmorStand qs = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        qs.setInvulnerable(true);
        qs.setInvisible(true);
        qs.setSmall(true);
        qs.setMarker(true);
        queueStands.add(qs);
        return qs;
    }

    public void startQueueTimer() {
        state = State.IN_QUEUE;
        players.forEach(player -> gameData.get(player).scoreboard.updateScores(-1));
        queueTimer = new BukkitRunnable() {
            int timeLeft = queueTime;
            @Override
            public void run() {
                if(timeLeft == 0) {
                    startGame();
                    cancel();
                }
                timeLeft--;
                players.forEach(player -> gameData.get(player).scoreboard.updateScores(timeLeft));
            }
        }.runTaskTimer(plugin, 0, 20);
    }


    /**
     * Starts the game
     */
    public void startGame() {
        state = State.RUNNING;
        queueStands.forEach(Entity::remove);
        queueStands.clear();
        players.forEach(p -> {
            GameData playerData = gameData.get(p);
            playerData.time = System.currentTimeMillis();
            playerData.scoreboard.updateScores(time, playerData.lap, laps);
            p.getInventory().clear();
        });
        final Iterator<Location> lightsIterator = lightLocations.iterator();
        mainLoop = new BukkitRunnable() {
            int prestartTimer = 0;
            int secondCounter = 0;
            int timeLeft = time;
            @Override
            public void run() {
                secondCounter = secondCounter < 20 ? ++secondCounter : 0;
                if(prestartTimer != -1) {
                    prestartTimer++;
                    if(prestartTimer % 20 != 0) return;
                    if(lightsIterator.hasNext()) {
                        Block block = lightsIterator.next().getBlock();
                        block.setType(Material.REDSTONE_LAMP);
                        Lightable blockData = (Lightable) block.getBlockData();
                        blockData.setLit(true);
                        block.setBlockData(blockData);
                    } else {
                        lightLocations.forEach(location -> {
                            Lightable locationBlockData = (Lightable) location.getBlock().getBlockData();
                            locationBlockData.setLit(false);
                            location.getBlock().setBlockData(locationBlockData);
                        });
                        startLineActivator.getBlock().setType(Material.RED_CONCRETE);
                        prestartTimer = -1;
                    }
                }
                players.forEach(player -> {
                    GameData playerData = gameData.get(player);
                    if(playerData.spectator) return;
                    playerData.scoreboard.updateScores(timeLeft, playerData.lap, laps);
                    updateCheckpoint(player);
                    deathBarriers.forEach(deathBarrier -> {
                        if(deathBarrier.contains(player.getLocation().toVector()))
                            respawn(player);
                    });
                });

                if(secondCounter == 20) {
                    if(timeLeft <= 0) stopGame();
                    timeLeft--;
                }
            }
        }.runTaskTimer(plugin, 0, 1);

    }

    /**
     * Stops the game.
     */
    public void stopGame() {
        state = State.WAITING;
        mainLoop.cancel();
        List<Player> playersCopy = new ArrayList<>(players);
        for (Player p : playersCopy) setGameStatus(p, false);
        startLineActivator.getBlock().setType(Material.REDSTONE_BLOCK);
    }


    private void updateCheckpoint(Player player) {
        GameData playerData = this.gameData.get(player);
        int previousCheckpoint = playerData.checkpoint;
        for (BoundingBox b : checkpointBoxes) {
            int currentCheckpoint = checkpointBoxes.indexOf(b);
            if(!b.contains(player.getLocation().toVector())) continue;
            if(currentCheckpoint == previousCheckpoint) continue;

            if(currentCheckpoint == previousCheckpoint + 1)
                playerData.checkpoint = currentCheckpoint;
            if(currentCheckpoint == 0 && previousCheckpoint == checkpointBoxes.size() - 1) {
                if(playerData.lap >= laps) setSpectator(player);
                playerData.lap += 1;
                playerData.checkpoint = currentCheckpoint;
            }
        }
    }

    private void setSpectator(Player player) {
        removeVehicle(player);
        player.setGameMode(GameMode.SPECTATOR);
        GameData playerData = this.gameData.get(player);
        playerData.spectator = true;

        Firework firework = (Firework) startWorld.spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withTrail().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        firework.detonate();

        double totalSeconds = ((double) (System.currentTimeMillis() - playerData.time)) / 1000;
        int minutes = (int) totalSeconds / 60;
        double seconds = totalSeconds % 60;
        String place;
        switch (currentPlace) {
            case 21:
            case 1:
                place = "1st";
                break;
            case 22:
            case 2:
                place = "2nd";
                break;
            case 23:
            case 3:
                place = "3rd";
                break;
            default:
                place = currentPlace + "th";
        }

        player.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                + ChatColor.AQUA + "You have completed the race with a time of "
                + ChatColor.YELLOW + minutes + ":" + seconds
                + ChatColor.AQUA + ". That puts you in "
                + ChatColor.YELLOW + place
                + ChatColor.AQUA + " place."
                + "\nYou can now spectate the other players or type "
                + ChatColor.YELLOW + "/yb leave"
                + ChatColor.AQUA + " to return to the lobby.");
        currentPlace++;
        if(currentPlace > players.size()) {
            players.forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] " + ChatColor.AQUA + "All Players have finished. Returning to lobby in 5 seconds."));
            new BukkitRunnable() {
                @Override
                public void run() {
                    stopGame();
                }
            }.runTaskLater(plugin, 100);
        }
    }

    /**
     * Respawns the specified player at the last checkpoint they passed.
     * @param player The player to teleport
     */
    private void respawn(Player player) {
        if(player.getVehicle() == null) return;
        if(player.getVehicle().getType() != EntityType.BOAT) return;
        Boat vehicle = (Boat) player.getVehicle();

        List<Entity> passengers = vehicle.getPassengers().stream().filter(e -> e.getType() != EntityType.PLAYER).collect(Collectors.toList());
        Boat newBoat = (Boat) startWorld.spawnEntity(checkpointSpawns.get(gameData.get(player).checkpoint), EntityType.BOAT);
        vehicle.removePassenger(player);
        player.teleport(checkpointSpawns.get(gameData.get(player).checkpoint));
        newBoat.setWoodType(vehicle.getWoodType());
        newBoat.addPassenger(player);
        passengers.forEach(p -> {
            vehicle.removePassenger(p);
            newBoat.addPassenger(p);
        });
        vehicle.remove();
    }


    @SuppressWarnings({"unused", "unchecked"})
    public Arena(Map<String, Object> m) {
        name = (String) m.get("name");

        startWorld = plugin.getServer().getWorld((String) m.get("startWorld"));

        minPlayers = (int) m.get("minPlayers");
        laps = (int) m.get("laps");
        time = (int) m.get("time");

        lobbyLocation = stringToLoc((String) m.get("lobbyLocation"), startWorld);
        startLineActivator = vectorStringToLoc((String) m.get("startLineActivator"), startWorld);

        startLocations = stringToLocList((List<String>) m.get("startLocations"), startWorld);
        lightLocations = vectorStringToLocList((List<String>) m.get("lightLocations"), startWorld);

        deathBarriers = stringToBoxList((List<String>) m.get("deathBarriers"));

        checkpointBoxes = stringToBoxList((List<String>) m.get("checkpointBoxes"));
        checkpointSpawns = stringToLocList((List<String>) m.get("checkpointSpawns"), startWorld);

        signs = ArenaSign.deserialize(this, (List<String>) m.get("signs"));
    }

    //Methods to change locations to and from their string representations

    /**
     * Turns a {@link Location} into a String representing it.
     * @param loc The location
     * @return The string representation of that location
     * @see Arena#stringToLoc(String, World)
     */
    private static String locToString(Location loc) {
        return loc.getX() + ","
                + loc.getY() + ","
                + loc.getZ() + ","
                + loc.getYaw() + ","
                + loc.getPitch();
    }
    /**
     * Turns a string representation of a {@link Location} into a Location object.
     * @param str The String representing the location.
     * @param world The world the location is supposed to be in.
     * @return The location object.
     * @see Arena#locToString(Location)
     */
    private static Location stringToLoc(String str, World world) {
        String[] segments = str.split(",");
        return new Location(
                world,
                Double.parseDouble(segments[0]),
                Double.parseDouble(segments[1]),
                Double.parseDouble(segments[2]),
                Float.parseFloat(segments[3]),
                Float.parseFloat(segments[4])
        );
    }

    /**
     * Functionally identical to {@link Arena#locToString(Location)} except it works on a List of Locations.
     * @see Arena#stringToLocList(List, World)
     * @param locs The list of Locations
     * @return The list of strings
     */
    private static List<String> locToStringList(List<Location> locs) {
        return locs.stream().map(Arena::locToString).collect(Collectors.toList());
    }
    /**
     * Functionally identical to {@link Arena#stringToLoc(String, World)} except it works on a List of Strings.
     * @see Arena#locToStringList(List)
     * @param strs The list of strings
     * @return The list of Locations
     */
    private static List<Location> stringToLocList(List<String> strs, World world) {
        return strs.stream().map(s -> stringToLoc(s, world)).collect(Collectors.toList());
    }

    /**
     * Turns a {@link Location} into a string representing only its x, y and z coordinates.
     * @param loc The location
     * @return The string representing it
     * @see Arena#vectorStringToLoc(String, World)
     */
    private static String locToVectorString(Location loc) {
        return loc.getBlockX() + ","
                + loc.getBlockY() + ","
                + loc.getBlockZ();
    }
    /**
     * Turns a string representing x, y, and z coordinates into a {@link Location} object, given you provide a world.
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
     * Functionally identical to {@link Arena#locToVectorString(Location)} except it works on a List of Locations.
     * @see Arena#vectorStringToLocList(List, World)
     * @param locs The list of Locations
     * @return The list of strings
     */
    private static List<String> locToVectorStringList(List<Location> locs) {
        return locs.stream().map(Arena::locToVectorString).collect(Collectors.toList());
    }
    /**
     * Functionally identical to {@link Arena#vectorStringToLoc(String, World)} except it works on a List of Strings.
     * @see Arena#locToVectorStringList(List)
     * @param strs The list of strings
     * @param world The world
     * @return The list of Locations
     */
    private static List<Location> vectorStringToLocList(List<String> strs, World world) {
        return strs.stream().map(s -> vectorStringToLoc(s, world)).collect(Collectors.toList());
    }

    /**
     * Turns a list of {@link BoundingBox}es into their string representations.
     * @see Arena#stringToBoxList(List)
     * @param boxes The list of bounding boxes
     * @return The string representation
     */
    private static List<String> boxToStringList(List<BoundingBox> boxes) {
        return boxes.stream()
                .map(b -> ((int) b.getMinX()) + ","
                + ((int) b.getMinY()) + ","
                + ((int) b.getMinZ()) + ","
                + ((int) b.getMaxX()) + ","
                + ((int) b.getMaxY()) + ","
                + ((int) b.getMaxZ()))
                .collect(Collectors.toList());
    }
    /**
     * Turns a list of strings representing bounding boxes into the {@link BoundingBox} objects.
     * @see Arena#boxToStringList(List)
     * @param strs The list of strings
     * @return The list of bounding boxes
     */
    private static List<BoundingBox> stringToBoxList(List<String> strs) {
        return strs.stream()
                .map(s -> {
                    String[] segments = s.split(",");
                    return new BoundingBox(
                            Double.parseDouble(segments[0]),
                            Double.parseDouble(segments[1]),
                            Double.parseDouble(segments[2]),
                            Double.parseDouble(segments[3]),
                            Double.parseDouble(segments[4]),
                            Double.parseDouble(segments[5])
                    );
                }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> serialize() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();

        m.put("name", name);

        m.put("minPlayers", minPlayers);
        m.put("laps", laps);
        m.put("time", time);

        m.put("lobbyLocation", locToString(lobbyLocation));
        m.put("startLineActivator", locToVectorString(startLineActivator));

        m.put("startWorld", startWorld.getName());
        m.put("startLocations", locToStringList(startLocations));

        m.put("lightLocations", locToVectorStringList(lightLocations));

        m.put("deathBarriers", boxToStringList(deathBarriers));

        m.put("checkpointBoxes", boxToStringList(checkpointBoxes));
        m.put("checkpointSpawns", locToStringList(checkpointSpawns));

        m.put("signs", ArenaSign.serialize(signs));
        return m;
    }

    private static class GameData {
        public GameData(Player player, Scoreboard scoreboard) {
            this.scoreboard = new ArenaScoreboard(player, scoreboard);
        }

        final ArenaScoreboard scoreboard;
        int checkpoint = 0;
        int lap = 1;
        long time = 0;
        boolean spectator = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return name.equals(arena.name);
    }
}
