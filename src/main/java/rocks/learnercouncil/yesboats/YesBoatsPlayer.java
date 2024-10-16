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

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class YesBoatsPlayer {
    private final YesBoats plugin = YesBoats.getPlugin();

    private final @Getter Player player;
    private final @Getter Arena arena;
    private final @Getter ArenaScoreboard scoreboard;
    public boolean canEnterBoat = true;
    public boolean canExitBoat = false;
    private @Getter DebugPath debugPath;
    private @Getter boolean spectator = false;
    private Vector previousLocation;
    private int checkpoint = 0;
    private @Getter int lap = 1;
    private @Setter long time = 0;
    private PlayerData data;

    public YesBoatsPlayer(Arena arena, Player player) {
        ScoreboardManager scoreboardManager = plugin.getServer().getScoreboardManager();
        if (scoreboardManager == null)
            throw new NullPointerException("ScoreboardManager was assigned before the first world was loaded, and thus, is null.");
        this.arena = arena;
        this.player = player;
        this.scoreboard = new ArenaScoreboard(player, scoreboardManager.getNewScoreboard());
        this.debugPath = new DebugPath(player, arena);
    }

    public void updatePreviousLocation() {
        previousLocation = player.getLocation().toVector();
    }

    public void setSpectator(boolean spectator) {
        if (spectator) {
            this.spectator = true;
            if (player.isInsideVehicle())
                Objects.requireNonNull(player.getVehicle()).remove();
            player.setAllowFlight(true);
            arena.getPlayers().forEach((player, ybPlayer) -> {
                if (!ybPlayer.isSpectator()) {
                    player.hidePlayer(plugin, this.player);
                }
            });
            arena.getSpectators().add(player);
        } else {
            this.spectator = false;
            player.setAllowFlight(false);
            arena.getPlayers().keySet().forEach(p -> p.showPlayer(plugin, player));
            arena.getSpectators().remove(player);
        }
    }

    public boolean isIntersecting(BoundingBox boundingBox) {
        if (previousLocation == null) updatePreviousLocation();
        Vector previousPosition = previousLocation;
        Vector currentPosition = player.getLocation().toVector();

        if (currentPosition.equals(previousPosition)) return false;
        if (boundingBox.contains(currentPosition)) return true;

        Vector direction = currentPosition.clone().subtract(previousPosition).normalize();
        if (Double.isNaN(direction.getX())) direction = new Vector();

        RayTraceResult rayTraceResult = boundingBox.rayTrace(previousPosition, direction, currentPosition.distance(previousPosition));
        return rayTraceResult != null;
    }

    public void updateCheckpoint(List<BoundingBox> checkpointBoxes, int laps, boolean debug) {
        if (debug) {
            debugPath.vectors.add(player.getLocation().toVector());
            debugPath.ping = Math.max(debugPath.ping, player.getPing());
        }
        int currentCheckpoint = checkpoint;
        int nextCheckpoint = (currentCheckpoint == checkpointBoxes.size() - 1) ? 0 : currentCheckpoint + 1;

        if (!this.isIntersecting(checkpointBoxes.get(nextCheckpoint))) {
            if (!debug) return;
            int nextNextCheckpoint = (nextCheckpoint == checkpointBoxes.size() - 1) ? 0 : nextCheckpoint + 1;
            if (isIntersecting(checkpointBoxes.get(nextNextCheckpoint))) {
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

        if (nextCheckpoint == 0) {
            if (lap >= laps) finish();
            lap++;
        }

        checkpoint = nextCheckpoint;
        if (debug) debugPath = new DebugPath(player, arena);

    }

    public void respawn(List<Location> checkpointSpawns) {
        if (player.getVehicle() == null) return;
        if (!(player.getVehicle() instanceof Boat)) return;
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
        this.setSpectator(true);
        final int currentPlace = arena.getCurrentPlace();
        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withTrail().withColor(Color.AQUA).with(FireworkEffect.Type.BALL).build());
        firework.setFireworkMeta(meta);
        firework.detonate();

        Duration duration = Duration.ofMillis(System.currentTimeMillis() - time);
        String formattedTime;
        if (duration.toHours() > 1)
            formattedTime = String.format("%d:%02d:%02d.%03d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());
        else
            formattedTime = String.format("%02d:%02d.%03d", duration.toMinutes(), duration.toSecondsPart(), duration.toMillisPart());

        player.sendMessage(Messages.FINISH_SELF.formatted(formattedTime, placeOrdinal(currentPlace)));

        arena.getPlayers().keySet().forEach(p -> p.sendMessage(Messages.FINISH_OTHERS.formatted(player.getName(), placeOrdinal(currentPlace))));

        arena.incrementCurrentPlace();
        if (arena.getCurrentPlace() > arena.getPlayers().size()) {
            arena.getPlayers().keySet().forEach(p -> p.sendMessage(Messages.ALL_FINISHED));
            new BukkitRunnable() {
                @Override
                public void run() {
                    arena.stopGame();
                }
            }.runTaskLater(plugin, 100);
        }
    }

    private String placeOrdinal(int place) {
        String suffix;
        int lastTwoDigits = place % 100;
        int lastDigit = place % 10;
        if (lastDigit == 1 && lastTwoDigits != 11) {
            suffix = "st";
        } else if (lastDigit == 2 && lastTwoDigits != 12) {
            suffix = "nd";
        } else if (lastDigit == 3 && lastTwoDigits != 13) {
            suffix = "rd";
        } else {
            suffix = "th";
        }
        return place + suffix;
    }

    public void restoreData() {
        if (data == null) return;
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
