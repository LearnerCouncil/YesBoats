package rocks.learnercouncil.yesboats;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaScoreboard;
import rocks.learnercouncil.yesboats.arena.DebugPath;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class YesBoatsPlayer {
    private final YesBoats plugin = YesBoats.getInstance();

    private final @Getter Player player;
    private final @Getter Arena arena;
    private final @Getter ArenaScoreboard scoreboard;

    private @Getter DebugPath debugPath;
    private final @Getter Set<Player> hiddenPlayers = new HashSet<>();
    private @Getter boolean spectator = false;
    private Vector previousLocation;
    public void updatePreviousLocation() {
        previousLocation = player.getLocation().toVector();
    }
    private int checkpoint = 0;
    private @Getter int lap = 1;
    private @Setter long time = 0;

    public boolean canEnterBoat = true;
    public boolean canExitBoat = false;

    private PlayerData data;

    public YesBoatsPlayer(Arena arena, Player player) {
        ScoreboardManager scoreboardManager = plugin.getServer().getScoreboardManager();
        if(scoreboardManager == null) throw new NullPointerException("ScoreboardManager was assigned before the first world was loaded, and thus, is null.");
        this.arena = arena;
        this.player = player;
        this.scoreboard = new ArenaScoreboard(player, scoreboardManager.getNewScoreboard());
        this.debugPath = new DebugPath(player, arena);
    }

    public void setSpectator() {
        spectator = true;
        if(player.isInsideVehicle())
            Objects.requireNonNull(player.getVehicle()).remove();
        player.setAllowFlight(true);
        arena.getPlayers().forEach(((p, ybp) -> {
            if(!ybp.isSpectator()) {
                p.hidePlayer(plugin, player);
                ybp.getHiddenPlayers().add(player);
            }
        }));
        hiddenPlayers.clear();
        arena.getSpectators().add(player);
    }

    public boolean isIntersecting(BoundingBox boundingBox) {
        if(previousLocation == null) updatePreviousLocation();
        Vector previousPosition = previousLocation;
        Vector currentPosition = player.getLocation().toVector();

        if(currentPosition.equals(previousPosition)) return false;
        if(boundingBox.contains(currentPosition)) return true;

        Vector direction = currentPosition.clone().subtract(previousPosition).normalize();
        if(Double.isNaN(direction.getX())) direction = new Vector();

        RayTraceResult rayTraceResult = boundingBox.rayTrace(previousPosition, direction, currentPosition.distance(previousPosition));
        return rayTraceResult != null;
    }

    public void updateCheckpoint(List<BoundingBox> checkpointBoxes, int laps, boolean debug) {
        if(debug) {
            debugPath.vectors.add(player.getLocation().toVector());
            debugPath.ping = Math.max(debugPath.ping, player.getPing());
        }
        int currentCheckpoint = checkpoint;
        int nextCheckpoint = (currentCheckpoint == checkpointBoxes.size() - 1) ? 0 : currentCheckpoint + 1;

        if(!this.isIntersecting(checkpointBoxes.get(nextCheckpoint))) {
            if(!debug) return;
            int nextNextCheckpoint = (nextCheckpoint == checkpointBoxes.size() - 1) ? 0 : nextCheckpoint + 1;
            if(isIntersecting(checkpointBoxes.get(nextNextCheckpoint))) {
                debugPath.timestamp = System.currentTimeMillis();
                DebugPath.debugPaths.add(debugPath);
                plugin.getServer().getOnlinePlayers().stream().filter(p -> p.hasPermission("yesboats.admin")).forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                        + ChatColor.AQUA + "Player "
                        + ChatColor.YELLOW + player.getName()
                        + ChatColor.AQUA + " has skipped checkpoint "
                        + ChatColor.YELLOW + "#" + nextCheckpoint
                        + ChatColor.AQUA + "."));
                checkpoint = nextNextCheckpoint;
                debugPath = new DebugPath(player, arena);
            }
            return;
        }

        if(nextCheckpoint == 0) {
            if(lap >= laps) finish();
            lap++;
        }

        checkpoint = nextCheckpoint;
        if(debug) debugPath = new DebugPath(player, arena);

    }

    public void respawn(List<Location> checkpointSpawns) {
        if(player.getVehicle() == null) return;
        if(!(player.getVehicle() instanceof Boat)) return;
        Boat oldBoat = (Boat) player.getVehicle();

        List<Entity> passengers = oldBoat.getPassengers().stream().filter(e -> e.getType() != EntityType.PLAYER).collect(Collectors.toList());
        Boat newBoat = (Boat) player.getWorld().spawnEntity(checkpointSpawns.get(checkpoint), oldBoat.getType());

        canExitBoat = true;
        oldBoat.removePassenger(player);

        player.teleport(checkpointSpawns.get(checkpoint));
        player.setFireTicks(-1);
        newBoat.setBoatType(oldBoat.getBoatType());

        canExitBoat = false;
        canEnterBoat = true;
        newBoat.addPassenger(player);
        canEnterBoat = false;

        newBoat.setInvulnerable(true);
        passengers.forEach(e -> {
            oldBoat.removePassenger(e);
            newBoat.addPassenger(e);
        });
        oldBoat.remove();
    }

    public void finish() {
        setSpectator();
        final int currentPlace = arena.getCurrentPlace();
        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withTrail().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        firework.detonate();

        double totalSeconds = ((double) (System.currentTimeMillis() - time)) / 1000;
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
        player.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                + ChatColor.AQUA + "You have completed the race with a time of "
                + ChatColor.YELLOW + minutes + ":" + (seconds < 10 ? "0"+seconds : seconds)
                + ChatColor.AQUA + ". That puts you in "
                + ChatColor.YELLOW + currentPlace + suffix
                + ChatColor.AQUA + " place."
                + "\nYou can now spectate the other players or type "
                + ChatColor.YELLOW + "/yb leave"
                + ChatColor.AQUA + " to return to the lobby.");
        arena.getPlayers().keySet().forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] "
                + ChatColor.AQUA + ChatColor.BOLD + player.getName()
                + ChatColor.RESET + ChatColor.AQUA + " has completed the race in "
                + ChatColor.YELLOW + currentPlace + suffix
                + ChatColor.AQUA + " place."));
        arena.incrementCurrentPlace();
        if(arena.getCurrentPlace() > arena.getPlayers().size()) {
            arena.getPlayers().keySet().forEach(p -> p.sendMessage(ChatColor.DARK_AQUA + "[YesBoats] " + ChatColor.AQUA + "All Players have finished. Returning to lobby in 5 seconds."));
            new BukkitRunnable() {
                @Override
                public void run() {
                    arena.stopGame();
                }
            }.runTaskLater(plugin, 100);
        }
    }

    public void restoreData() {
        if(data == null) return;
        player.getInventory().setContents(data.inventory);
        player.setAllowFlight(data.allowFlight);
        player.setInvulnerable(data.invulnerable);
        player.setGameMode(data.gameMode);
        player.setExp(data.xp);
        player.setLevel(data.level);
        player.setHealth(data.health);
        player.setFoodLevel(data.hunger);
        player.setSaturation(data.saturation);
    }
    public void setData() {
        data = new PlayerData();

        data.inventory = player.getInventory().getContents();
        player.getInventory().clear();

        data.allowFlight = player.getAllowFlight();
        player.setAllowFlight(false);

        data.invulnerable = player.isInvulnerable();
        player.setInvulnerable(true);

        data.gameMode = player.getGameMode();
        player.setGameMode(GameMode.ADVENTURE);

        data.xp = player.getExp();
        player.setExp(0.0f);

        data.level = player.getLevel();
        player.setLevel(0);

        data.health = player.getHealth();
        player.setHealth(20);

        data.hunger = player.getFoodLevel();
        player.setFoodLevel(20);

        data.saturation = player.getSaturation();
        player.setSaturation(1.0f);
    }
    private static class PlayerData {
        public ItemStack[] inventory;
        public boolean allowFlight;
        public boolean invulnerable;
        public GameMode gameMode;
        public float xp;
        public int level;
        public double health;
        public int hunger;
        public float saturation;
    }
}
