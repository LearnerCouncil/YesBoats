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
import org.bukkit.event.vehicle.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.BoundingBox;
import rocks.learnercouncil.yesboats.InventoryManager;
import rocks.learnercouncil.yesboats.YesBoats;
import rocks.learnercouncil.yesboats.YesBoatsPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static rocks.learnercouncil.yesboats.Serializers.*;

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
        copy.debug = this.debug;
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
    protected boolean debug = false;

    //non-serialized fields
    private final @Getter Map<Player, YesBoatsPlayer> players = new HashMap<>();
    private final @Getter Set<Player> spectators = new HashSet<>();
    private final Set<ArmorStand> queueStands = new HashSet<>();
    public BukkitTask queueTimer;
    private BukkitTask mainLoop;
    private @Getter int currentPlace = 1;
    public void incrementCurrentPlace() {
        currentPlace++;
    }
    private int timeLeft;

    private @Getter State state = State.WAITING;
    public enum State {
        WAITING, IN_QUEUE, RUNNING
    }


    public Arena(String name) {
        this.name = name;
    }

    public void updateSigns() {
        signs.forEach(s -> s.update(state, players.size(), startLocations.size()));
    }

    public void add(Player player) {
        //failsafe
        if(players.size() == startLocations.size()) return;
        if(!getStartLocation().isPresent()) return;

        Location startLocation = getStartLocation().get();
        player.teleport(startLocation);
        Boat boat = (Boat) world.spawnEntity(startLocation, EntityType.BOAT);
        boat.setInvulnerable(true);

        players.put(player, new YesBoatsPlayer(this, player));
        playerArenaMap.put(player, this);
        players.get(player).setData();

        InventoryManager.initialize(player);

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
        updateSigns();
    }
    public void remove(Player leavingPlayer) {
        //failsafe
        if(!Arena.get(leavingPlayer).isPresent()) return;

        removeVehicle(leavingPlayer);
        players.keySet().forEach(p -> p.showPlayer(plugin, leavingPlayer));

        leavingPlayer.teleport(lobbyLocation);
        players.get(leavingPlayer).restoreData();

        players.remove(leavingPlayer);
        playerArenaMap.remove(leavingPlayer);
        spectators.remove(leavingPlayer);

        ScoreboardManager scoreboardManager = plugin.getServer().getScoreboardManager();
        assert scoreboardManager != null;
        leavingPlayer.setScoreboard(scoreboardManager.getMainScoreboard());

        if(state == State.IN_QUEUE && players.size() < minPlayers) {
            queueTimer.cancel();
            players.values().forEach(p -> p.getScoreboard().updateScores(-1));
            state = State.WAITING;
        }
        if(state == State.RUNNING && players.size() <= 0) stopGame();
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
        players.values().forEach((player) -> {
            player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.BLOCK_TRIPWIRE_ATTACH, 1, 1);
            player.setTime(System.currentTimeMillis());
            player.getScoreboard().updateScores(time, player.getLap(), laps);
            player.getPlayer().getInventory().clear();
        });
        final Iterator<Location> lightsIterator = lightLocations.iterator();
        timeLeft = time;
        mainLoop = new BukkitRunnable() {
            boolean countdownFinished = false;
            int secondCounter = 0;
            @Override
            public void run() {
                secondCounter = (secondCounter < 20) ? ++secondCounter : 0;
                if(secondCounter == 20) {
                    if(!countdownFinished) countdownFinished = incrementCountdown(lightsIterator);
                    else decrementTimer();
                }
                players.values().forEach(p -> updatePlayer(p));
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    private void decrementTimer() {
        if(timeLeft <= 0) {
            stopGame();
            players.keySet().forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] " + ChatColor.AQUA + "The timer has run out. Returning to the lobby."));
        }
        timeLeft--;
    }
    private boolean incrementCountdown(Iterator<Location> lightsIterator) {
        if(lightsIterator.hasNext()) {
            setLamp(lightsIterator.next().getBlock(), true);
            players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1));
            return false;
        } else {
            lightLocations.forEach(location -> setLamp(location.getBlock(), false));
            players.keySet().forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2));
            startLineActivator.getBlock().setType(Material.RED_CONCRETE);
            return true;
        }
    }
    private void setLamp(Block lamp, boolean lit) {
        if(lamp.getType() != Material.REDSTONE_LAMP) lamp.setType(Material.REDSTONE_LAMP);
        Lightable blockData = (Lightable) lamp.getBlockData();
        blockData.setLit(lit);
        lamp.setBlockData(blockData);
    }
    private void updatePlayer(YesBoatsPlayer player) {
        if(player.isSpectator()) return;
        player.getScoreboard().updateScores(timeLeft, player.getLap(), laps);
        player.updateCheckpoint(checkpointBoxes, laps, debug);
        boolean inVoid = player.getPlayer().getLocation().getY() < -16.0;
        deathBarriers.forEach(deathBarrier -> {
            if(inVoid || player.isIntersecting(deathBarrier))
                player.respawn(checkpointSpawns);
        });
        player.updatePreviousLocation();
    }

    public void stopGame() {
        if(state == State.RUNNING) mainLoop.cancel();
        if(state == State.IN_QUEUE) queueTimer.cancel();
        state = State.WAITING;
        Set<Player> playersCopy = new HashSet<>(players.keySet());
        for (Player p : playersCopy) remove(p);
        startLineActivator.getBlock().setType(Material.REDSTONE_BLOCK);
        lightLocations.forEach(l -> {
            if(!(l.getBlock().getBlockData() instanceof Lightable)) return;
            Lightable blockData = (Lightable) l.getBlock().getBlockData();
            blockData.setLit(false);
            l.getBlock().setBlockData(blockData);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return name.equals(arena.name);
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

        lobbyLocation = toLocation((String) m.get("lobbyLocation"), world);
        startLineActivator = toVectorLocation((String) m.get("startLineActivator"), world);

        startLocations = toLocationList((List<String>) m.get("startLocations"), world);
        lightLocations = toVectorLocationList((List<String>) m.get("lightLocations"), world);

        deathBarriers = toBoxList((List<String>) m.get("deathBarriers"));

        checkpointBoxes = toBoxList((List<String>) m.get("checkpointBoxes"));
        checkpointSpawns = toLocationList((List<String>) m.get("checkpointSpawns"), world);

        signs = ArenaSign.deserialize((List<String>) m.get("signs"));

        debug = m.containsKey("debug") ? (boolean) m.get("debug") : false;
    }
    @Override
    public Map<String, Object> serialize() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();

        m.put("name", name);

        m.put("minPlayers", minPlayers);
        m.put("laps", laps);
        m.put("time", time);

        m.put("lobbyLocation", fromLocation(lobbyLocation));
        m.put("startLineActivator", fromVectorLocation(startLineActivator));

        m.put("world", world.getName());
        m.put("startLocations", fromLocationList(startLocations));

        m.put("lightLocations", fromVectorLocationList(lightLocations));

        m.put("deathBarriers", fromBoxList(deathBarriers));

        m.put("checkpointBoxes", fromBoxList(checkpointBoxes));
        m.put("checkpointSpawns", fromLocationList(checkpointSpawns));

        m.put("signs", ArenaSign.serialize(signs));

        m.put("debug", debug);
        return m;
    }

    public static class Events implements Listener {

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if(!Arena.get(player).isPresent()) return;
            Arena.get(player).get().remove(player);
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
            if (boat.getPassengers().isEmpty() || boat.getPassengers().stream().noneMatch(p -> p instanceof Player)) return;
            Player player = (Player) boat.getPassengers().stream().filter(p -> p instanceof Player).findAny().orElseThrow(() -> new NullPointerException("Boat is empty."));
            if(!Arena.get(player).isPresent()) return;
            Arena arena = Arena.get(player).get();
            if(arena.players.get(player).canExitBoat) return;

            event.setCancelled(true);
        }
    }
}
