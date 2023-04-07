package rocks.learnercouncil.yesboats.arena;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.*;
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
public class Arena implements ConfigurationSerializable, Cloneable {

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

    @Override
    public Arena clone() {
        Arena copy = new Arena(this.name);
        copy.minPlayers = this.minPlayers;
        copy.laps = this.laps;
        copy.time = this.time;
        copy.lobbyLocation = this.lobbyLocation;
        copy.startLineActivator = this.startLineActivator;
        copy.world = this.world;
        copy.startLocations = this.startLocations;
        copy.lightLocations = this.lightLocations;
        copy.deathBarriers = this.deathBarriers;
        copy.checkpointBoxes = this.checkpointBoxes;
        copy.checkpointSpawns = this.checkpointSpawns;
        copy.signs = this.signs;
        return copy;
    }

    //serialized fields
    public final String name;
    protected int minPlayers = 1;
    protected int laps = 1;
    protected int time = 300;
    protected Location lobbyLocation;
    protected Location startLineActivator;
    protected World world;
    protected List<Location> startLocations = new ArrayList<>();
    protected List<Location> lightLocations = new ArrayList<>();
    protected List<BoundingBox> deathBarriers = new ArrayList<>();
    protected List<BoundingBox> checkpointBoxes = new ArrayList<>();
    protected List<Location> checkpointSpawns = new ArrayList<>();
    protected Set<ArenaSign> signs = new HashSet<>();

    //non-serialized fields
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


    public Arena(String name) {
        this.name = name;
    }

    /**
     * Updates arena signs
     */
    public void updateSigns() {
        signs.forEach(s -> s.update(state, players.size(), startLocations.size()));
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
            if(!getStartLocation().isPresent()) return;

            Location startLocation = getStartLocation().get();
            player.teleport(startLocation);
            Boat boat = (Boat) world.spawnEntity(startLocation, EntityType.BOAT);
            boat.setInvulnerable(true);

            players.add(player);
            playerArenaMap.put(player, this);
            gameData.put(player, new GameData(player, scoreboardManager.getNewScoreboard()));
            PlayerManager.set(player);

            ArmorStand queueStand = spawnArmorStand(startLocation);
            queueStands.add(queueStand);
            queueStand.addPassenger(boat);
            boat.addPassenger(player);
            boat.addPassenger(spawnArmorStand(startLocation));

            gameData.get(player).canEnterBoat = false;
            if(players.size() >= minPlayers && state == State.WAITING) startQueueTimer();
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

            if(state == State.IN_QUEUE) {
                if(players.size() < minPlayers) {
                    queueTimer.cancel();
                    players.forEach(p -> gameData.get(p).scoreboard.updateScores(-1));
                    state = State.WAITING;
                }
            }
            if(state == State.RUNNING)
                if(players.size() <= 0) stopGame();
        }
        updateSigns();
    }

    private Optional<Location> getStartLocation() {
        return startLocations.stream().filter(l -> !queueStands.stream().map(Entity::getLocation).collect(Collectors.toList()).contains(l)).findFirst();
    }

    @SuppressWarnings("ConstantConditions")
    private void removeVehicle(Player player) {
        if (!player.isInsideVehicle()) return;
        Entity boat = player.getVehicle();
        if (boat.isInsideVehicle()) {
            Entity queueStand = boat.getVehicle();
            if (queueStand instanceof ArmorStand)
                queueStands.remove(queueStand);
            queueStand.remove();
        }
        boat.getPassengers().forEach(passenger -> {
            if(passenger.getType() != EntityType.PLAYER) passenger.remove();
        });
        boat.remove();
    }

    private ArmorStand spawnArmorStand(Location location) {
        ArmorStand stand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setInvulnerable(true);
        stand.setInvisible(true);
        stand.setSmall(true);
        stand.setMarker(true);
        return stand;
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
        currentPlace = 1;
        queueStands.forEach(Entity::remove);
        queueStands.clear();
        players.forEach(p -> {
            p.playSound(p.getLocation(), Sound.BLOCK_TRIPWIRE_ATTACH, 1, 1);
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
                        players.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
                    } else {
                        lightLocations.forEach(location -> {
                            Lightable locationBlockData = (Lightable) location.getBlock().getBlockData();
                            locationBlockData.setLit(false);
                            location.getBlock().setBlockData(locationBlockData);
                            players.forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
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
                        if(deathBarrier.contains(player.getLocation().add(0, -0.55, 0).toVector()))
                            respawn(player);
                    });
                });

                if(secondCounter == 20 && prestartTimer == -1) {
                    if(timeLeft <= 0) {
                        stopGame();
                        players.forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] " + ChatColor.AQUA + "The timer has run out. Returning to the lobby."));
                    }
                    timeLeft--;
                }
            }
        }.runTaskTimer(plugin, 0, 1);

    }

    /**
     * Stops the game.
     */
    public void stopGame() {
        if(state == State.RUNNING) mainLoop.cancel();
        if(state == State.IN_QUEUE) queueTimer.cancel();
        state = State.WAITING;
        List<Player> playersCopy = new ArrayList<>(players);
        for (Player p : playersCopy) setGameStatus(p, false);
        startLineActivator.getBlock().setType(Material.REDSTONE_BLOCK);
        lightLocations.forEach(l -> {
            Lightable blockData = (Lightable) l.getBlock().getBlockData();
            blockData.setLit(false);
            l.getBlock().setBlockData(blockData);
        });

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

        Firework firework = (Firework) world.spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withTrail().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        firework.detonate();

        double totalSeconds = ((double) (System.currentTimeMillis() - playerData.time)) / 1000;
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;
        String suffix;
        int lastDigit = currentPlace % 10;
            if(lastDigit == 1 && currentPlace != 11)
                suffix = "st";
            else if(lastDigit == 2 && currentPlace != 12)
                suffix = "nd";
            else if(lastDigit == 3 && currentPlace != 13)
                suffix = "rd";
            else
                suffix = "th";

        player.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                + ChatColor.AQUA + "You have completed the race with in "
                + ChatColor.YELLOW + minutes + ":" + seconds
                + ChatColor.AQUA + ". That puts you in "
                + ChatColor.YELLOW + currentPlace + suffix
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
        Boat oldBoat = (Boat) player.getVehicle();
        GameData playerData = this.gameData.get(player);

        List<Entity> passengers = oldBoat.getPassengers().stream().filter(e -> e.getType() != EntityType.PLAYER).collect(Collectors.toList());
        Boat newBoat = (Boat) world.spawnEntity(checkpointSpawns.get(playerData.checkpoint), EntityType.BOAT);

        playerData.canExitBoat = true;
        oldBoat.removePassenger(player);

        player.teleport(checkpointSpawns.get(playerData.checkpoint));
        player.setFireTicks(-1);
        newBoat.setWoodType(oldBoat.getWoodType());

        playerData.canExitBoat = false;
        playerData.canEnterBoat = true;
        newBoat.addPassenger(player);
        playerData.canEnterBoat = false;

        newBoat.setInvulnerable(true);
        passengers.forEach(p -> {
            oldBoat.removePassenger(p);
            newBoat.addPassenger(p);
        });
        oldBoat.remove();
    }


    @SuppressWarnings({"unused", "unchecked"})
    public Arena(Map<String, Object> m) {
        name = (String) m.get("name");

        world = plugin.getServer().getWorld((String) m.get("world"));
        //for backwards compatability
        if(world == null) world = plugin.getServer().getWorld((String) m.get("startWorld"));

        minPlayers = (int) m.get("minPlayers");
        laps = (int) m.get("laps");
        time = (int) m.get("time");

        lobbyLocation = stringToLoc((String) m.get("lobbyLocation"), world);
        startLineActivator = vectorStringToLoc((String) m.get("startLineActivator"), world);

        startLocations = stringToLocList((List<String>) m.get("startLocations"), world);
        lightLocations = vectorStringToLocList((List<String>) m.get("lightLocations"), world);

        deathBarriers = stringToBoxList((List<String>) m.get("deathBarriers"));

        checkpointBoxes = stringToBoxList((List<String>) m.get("checkpointBoxes"));
        checkpointSpawns = stringToLocList((List<String>) m.get("checkpointSpawns"), world);

        signs = ArenaSign.deserialize((List<String>) m.get("signs"));
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
     * @param locations The list of Locations
     * @return The list of strings
     */
    private static List<String> locToStringList(List<Location> locations) {
        return locations.stream().map(Arena::locToString).collect(Collectors.toList());
    }
    /**
     * Functionally identical to {@link Arena#stringToLoc(String, World)} except it works on a List of Strings.
     * @see Arena#locToStringList(List)
     * @param strings The list of strings
     * @return The list of Locations
     */
    private static List<Location> stringToLocList(List<String> strings, World world) {
        return strings.stream().map(s -> stringToLoc(s, world)).collect(Collectors.toList());
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
     * @param locations The list of Locations
     * @return The list of strings
     */
    private static List<String> locToVectorStringList(List<Location> locations) {
        return locations.stream().map(Arena::locToVectorString).collect(Collectors.toList());
    }
    /**
     * Functionally identical to {@link Arena#vectorStringToLoc(String, World)} except it works on a List of Strings.
     * @see Arena#locToVectorStringList(List)
     * @param strings The list of strings
     * @param world The world
     * @return The list of Locations
     */
    private static List<Location> vectorStringToLocList(List<String> strings, World world) {
        return strings.stream().map(s -> vectorStringToLoc(s, world)).collect(Collectors.toList());
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
     * @param strings The list of strings
     * @return The list of bounding boxes
     */
    private static List<BoundingBox> stringToBoxList(List<String> strings) {
        return strings.stream()
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

        m.put("world", world.getName());
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
        boolean canEnterBoat = true;
        boolean canExitBoat = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return name.equals(arena.name);
    }

    public static class Events implements Listener {

        @EventHandler
        public void onSpectatorTeleport(PlayerTeleportEvent event) {
            if(event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
            if(event.getPlayer().hasPermission("yesboats.spectatortp")) return;
            Optional<Arena> arenaOptional = Arena.get(event.getPlayer());
            if(!arenaOptional.isPresent()) return;
            Optional<Player> targetOptional = arenaOptional.get().getPlayers().stream().filter(p -> p.getLocation().equals(event.getTo())).findAny();
            if(!targetOptional.isPresent()) {
                event.setCancelled(true);
                return;
            }
            Player target = targetOptional.get();
            Optional<Arena> targetArenaOptional = Arena.get(target);
            if (!targetArenaOptional.isPresent() || !targetArenaOptional.get().equals(arenaOptional.get()))
                event.setCancelled(true);
        }

        @EventHandler
        public void onVehicleEnter(VehicleEnterEvent event) {
            if(!(event.getEntered() instanceof Player)) return;
            Player player = (Player) event.getEntered();
            if(!Arena.get(player).isPresent()) return;
            Arena arena = Arena.get(player).get();
            if(arena.gameData.get(player).canEnterBoat) return;

            System.out.println(player.getName() + " was blocked from entering a boat.");
            event.setCancelled(true);
        }

        @EventHandler
        public void onVehicleExit(VehicleExitEvent event) {
            if(!(event.getExited() instanceof Player)) return;
            Player player = (Player) event.getExited();
            if(!Arena.get(player).isPresent()) return;
            Arena arena = Arena.get(player).get();
            if(arena.gameData.get(player).canExitBoat) return;

            System.out.println(player.getName() + " was blocked from exiting a boat.");
            event.setCancelled(true);
        }

        @EventHandler
        public void onVehicleBreak(VehicleDestroyEvent event) {
            Vehicle boat = event.getVehicle();
            if(boat.getPassengers().isEmpty()) return;
            if(!boat.getPassengers().stream().anyMatch(p -> p instanceof Player)) return;
            Player player = (Player) boat.getPassengers().stream().filter(p -> p instanceof Player).findAny().orElseThrow(() -> new NullPointerException("Boat is empty."));
            if(!Arena.get(player).isPresent()) return;
            Arena arena = Arena.get(player).get();
            if(arena.gameData.get(player).canExitBoat) return;

            event.setCancelled(true);
        }
    }
}
