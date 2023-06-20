package rocks.learnercouncil.yesboats.arena;

import lombok.Getter;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.PlayerManager;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.YesBoatsPlayer;

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

    public static Optional<Arena> get(String name) {
        return arenas.stream().filter(a -> a.name.equals(name)).findFirst();
    }

    private static final HashMap<Player, Arena> playerArenaMap = new HashMap<>();
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
    private final @Getter Map<Player, YesBoatsPlayer> players = new HashMap<>();
    private final @Getter Set<Player> spectators = new HashSet<>();
    private final Set<ArmorStand> queueStands = new HashSet<>();
    public BukkitTask queueTimer;
    private BukkitTask mainLoop;
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

    public void updateSigns() {
        signs.forEach(s -> s.update(state, players.size(), startLocations.size()));
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
            if(!getStartLocation().isPresent()) return;

            Location startLocation = getStartLocation().get();
            player.teleport(startLocation);
            Boat boat = (Boat) world.spawnEntity(startLocation, EntityType.BOAT);
            boat.setInvulnerable(true);

            players.put(player, new YesBoatsPlayer(this, player));
            playerArenaMap.put(player, this);
            PlayerManager.set(player);

            spectators.forEach(s -> {
                player.hidePlayer(s);
                players.get(player).getHiddenPlayers().add(s);
            });

            ArmorStand queueStand = spawnArmorStand(startLocation);
            queueStands.add(queueStand);
            queueStand.addPassenger(boat);
            boat.addPassenger(player);
            boat.addPassenger(spawnArmorStand(startLocation));

            players.get(player).canEnterBoat = false;
            if(players.size() >= minPlayers && state == State.WAITING) startQueueTimer();
        } else {
            //failsafe
            if(!Arena.get(player).isPresent()) return;

            removeVehicle(player);
            players.get(player).getHiddenPlayers().forEach(p -> player.showPlayer(plugin, p));

            player.teleport(lobbyLocation);
            PlayerManager.restore(player);

            players.remove(player);
            playerArenaMap.remove(player);
            spectators.remove(player);
            player.setScoreboard(scoreboardManager.getMainScoreboard());

            if(state == State.IN_QUEUE && players.size() < minPlayers) {
                queueTimer.cancel();
                players.values().forEach(p -> p.getScoreboard().updateScores(-1));
                state = State.WAITING;
            }
            if(state == State.RUNNING && players.size() <= 0) stopGame();
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
        players.values().forEach(player -> player.getScoreboard().updateScores(-1));
        queueTimer = new BukkitRunnable() {
            int timeLeft = queueTime;
            @Override
            public void run() {
                if(timeLeft == 0) {
                    startGame();
                    cancel();
                }
                timeLeft--;
                players.values().forEach(player -> player.getScoreboard().updateScores(timeLeft));
            }
        }.runTaskTimer(plugin, 0, 20);
    }


    public void startGame() {
        state = State.RUNNING;
        currentPlace = 1;
        queueStands.forEach(Entity::remove);
        queueStands.clear();
        players.forEach((player, yesBoatsPlayer) -> {
            player.playSound(player.getLocation(), Sound.BLOCK_TRIPWIRE_ATTACH, 1, 1);
            yesBoatsPlayer.setTime(System.currentTimeMillis());
            yesBoatsPlayer.getScoreboard().updateScores(time, yesBoatsPlayer.getLap(), laps);
            player.getInventory().clear();
        });
        final Iterator<Location> lightsIterator = lightLocations.iterator();
        mainLoop = new BukkitRunnable() {
            boolean inCountdown = true;
            int secondCounter = 0;
            int timeLeft = time;
            @Override
            public void run() {
                secondCounter = secondCounter < 20 ? ++secondCounter : 0;
                if(inCountdown && secondCounter == 20) {
                    if(lightsIterator.hasNext()) {
                        setLamp(lightsIterator.next().getBlock(), true);
                        players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
                    } else {
                        lightLocations.forEach(location -> setLamp(location.getBlock(), false));
                        players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
                        startLineActivator.getBlock().setType(Material.RED_CONCRETE);
                        inCountdown = false;
                    }
                }
                players.values().forEach(player -> {
                    if(spectators.contains(player)) return;
                    player.getScoreboard().updateScores(timeLeft, player.getLap(), laps);
                    updateCheckpoint(player);
                    boolean inVoid = player.getPlayer().getLocation().getY() < -16.0;
                    deathBarriers.forEach(deathBarrier -> {
                        if(inVoid || isIntersecting(player, deathBarrier))
                            respawn(player);
                    });
                    player.updatePreviousLocation();
                });

                if(secondCounter == 20 && !inCountdown) {
                    if(timeLeft <= 0) {
                        stopGame();
                        players.keySet().forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] " + ChatColor.AQUA + "The timer has run out. Returning to the lobby."));
                    }
                    timeLeft--;
                }
            }
        }.runTaskTimer(plugin, 0, 1);

    }

    private void setLamp(Block lamp, boolean lit) {
        if(lamp.getType() != Material.REDSTONE_LAMP) lamp.setType(Material.REDSTONE_LAMP);
        Lightable blockData = (Lightable) lamp.getBlockData();
        blockData.setLit(lit);
        lamp.setBlockData(blockData);
    }

    public void stopGame() {
        if(state == State.RUNNING) mainLoop.cancel();
        if(state == State.IN_QUEUE) queueTimer.cancel();
        state = State.WAITING;
        Set<Player> playersCopy = new HashSet<>(players.keySet());
        for (Player p : playersCopy) setGameStatus(p, false);
        startLineActivator.getBlock().setType(Material.REDSTONE_BLOCK);
        lightLocations.forEach(l -> {
            Lightable blockData = (Lightable) l.getBlock().getBlockData();
            blockData.setLit(false);
            l.getBlock().setBlockData(blockData);
        });
    }


    private boolean isIntersecting(YesBoatsPlayer player, BoundingBox boundingBox) {
        Vector previousPosition = player.getPreviousLocation();
        Vector currentPosition = player.getPlayer().getLocation().toVector();

        if(currentPosition.equals(previousPosition)) return false;
        if(boundingBox.contains(currentPosition)) return true;

        Vector direction = currentPosition.clone().subtract(previousPosition).normalize();
        if(Double.isNaN(direction.getX())) direction = new Vector();

        RayTraceResult rayTraceResult = boundingBox.rayTrace(previousPosition, direction, currentPosition.distance(previousPosition));
        return rayTraceResult != null;
    }

    private void updateCheckpoint(YesBoatsPlayer player) {
        int previousCheckpoint = player.getCheckpoint();
        int currentCheckpoint = previousCheckpoint == checkpointBoxes.size() - 1 ? 0 : previousCheckpoint + 1;

        if(!isIntersecting(player, checkpointBoxes.get(currentCheckpoint))) return;

        if(currentCheckpoint == 0) {
            if(player.getLap() >= laps) finish(player);
            player.incrementLap();
        }

        player.setCheckpoint(currentCheckpoint);
    }

    private void finish(YesBoatsPlayer player) {
        player.setSpectator();

        Firework firework = (Firework) world.spawnEntity(player.getPlayer().getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withTrail().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        firework.detonate();

        double totalSeconds = ((double) (System.currentTimeMillis() - player.getTime())) / 1000;
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;
        String s = "th";
        int lastDigit = currentPlace % 10;
        if(lastDigit == 1 && currentPlace != 11)
            s = "st";
        else if(lastDigit == 2 && currentPlace != 12)
            s = "nd";
        else if(lastDigit == 3 && currentPlace != 13)
            s = "rd";
        final String suffix = s;
        player.getPlayer().sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                + ChatColor.AQUA + "You have completed the race with a time of "
                + ChatColor.YELLOW + minutes + ":" + seconds
                + ChatColor.AQUA + ". That puts you in "
                + ChatColor.YELLOW + currentPlace + suffix
                + ChatColor.AQUA + " place."
                + "\nYou can now spectate the other players or type "
                + ChatColor.YELLOW + "/yb leave"
                + ChatColor.AQUA + " to return to the lobby.");
        players.keySet().forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                + ChatColor.AQUA + ChatColor.BOLD + player.getPlayer().getName()
                + ChatColor.RESET + ChatColor.AQUA + " has completed the race in "
                + ChatColor.YELLOW + currentPlace + suffix
                + ChatColor.AQUA + " place."));
        currentPlace++;
        if(currentPlace > players.size()) {
            players.keySet().forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] " + ChatColor.AQUA + "All Players have finished. Returning to lobby in 5 seconds."));
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
    private void respawn(YesBoatsPlayer player) {
        if(player.getPlayer().getVehicle() == null) return;
        if(player.getPlayer().getVehicle().getType() != EntityType.BOAT) return;
        Boat oldBoat = (Boat) player.getPlayer().getVehicle();

        List<Entity> passengers = oldBoat.getPassengers().stream().filter(e -> e.getType() != EntityType.PLAYER).collect(Collectors.toList());
        Boat newBoat = (Boat) world.spawnEntity(checkpointSpawns.get(player.getCheckpoint()), EntityType.BOAT);

        player.canExitBoat = true;
        oldBoat.removePassenger(player.getPlayer());

        player.getPlayer().teleport(checkpointSpawns.get(player.getCheckpoint()));
        player.getPlayer().setFireTicks(-1);
        newBoat.setWoodType(oldBoat.getWoodType());

        player.canExitBoat = false;
        player.canEnterBoat = true;
        newBoat.addPassenger(player.getPlayer());
        player.canEnterBoat = false;

        newBoat.setInvulnerable(true);
        passengers.forEach(e -> {
            oldBoat.removePassenger(e);
            newBoat.addPassenger(e);
        });
        oldBoat.remove();
    }

    //Methods to change locations to and from their string representations
    
    private static String locToString(Location loc) {
        return loc.getX() + ","
                + loc.getY() + ","
                + loc.getZ() + ","
                + loc.getYaw() + ","
                + loc.getPitch();
    }
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

    private static List<String> locToStringList(List<Location> locations) {
        return locations.stream().map(Arena::locToString).collect(Collectors.toList());
    }
    private static List<Location> stringToLocList(List<String> strings, World world) {
        return strings.stream().map(s -> stringToLoc(s, world)).collect(Collectors.toList());
    }

    private static String locToVectorString(Location loc) {
        return loc.getBlockX() + ","
                + loc.getBlockY() + ","
                + loc.getBlockZ();
    }
    private static Location vectorStringToLoc(String str, World world) {
        String[] segments = str.split(",");
        return new Location(world,
                Double.parseDouble(segments[0]),
                Double.parseDouble(segments[1]),
                Double.parseDouble(segments[2]));
    }

    private static List<String> locToVectorStringList(List<Location> locations) {
        return locations.stream().map(Arena::locToVectorString).collect(Collectors.toList());
    }
    private static List<Location> vectorStringToLocList(List<String> strings, World world) {
        return strings.stream().map(s -> vectorStringToLoc(s, world)).collect(Collectors.toList());
    }

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

    @SuppressWarnings({"unused", "unchecked"})
    public Arena(Map<String, Object> m) {
        name = (String) m.get("name");

        String worldName = (String) m.get("world");
        //for backwards compatability
        if(worldName == null) worldName = (String) m.get("startWorld");

        world = plugin.getServer().getWorld(worldName);

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return name.equals(arena.name);
    }

    public static class Events implements Listener {

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if(!Arena.get(player).isPresent()) return;
            Arena.get(player).get().setGameStatus(player, false);
        }

        @EventHandler
        public void onVehicleEnter(VehicleEnterEvent event) {
            if(!(event.getEntered() instanceof Player)) return;
            Player player = (Player) event.getEntered();
            if(!Arena.get(player).isPresent()) return;
            Arena arena = Arena.get(player).get();
            if(arena.players.get(player).canEnterBoat) return;

            event.setCancelled(true);
        }

        @EventHandler
        public void onVehicleExit(VehicleExitEvent event) {
            if(!(event.getExited() instanceof Player)) return;
            Player player = (Player) event.getExited();
            if(!Arena.get(player).isPresent()) return;
            Arena arena = Arena.get(player).get();
            if(arena.players.get(player).canExitBoat) return;

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
            if(arena.players.get(player).canExitBoat) return;

            event.setCancelled(true);
        }
    }
}
