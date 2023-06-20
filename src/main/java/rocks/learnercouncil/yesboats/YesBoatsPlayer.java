package rocks.learnercouncil.yesboats;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaScoreboard;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class YesBoatsPlayer {
    private final YesBoats plugin = YesBoats.getInstance();
    private final @Getter Player player;
    private final @Getter Arena arena;

    private @Getter boolean spectator = false;
    private final @Getter ArenaScoreboard scoreboard;
    private final @Getter Set<Player> hiddenPlayers = new HashSet<>();
    private @Getter Vector previousLocation;
    public void updatePreviousLocation() {
        previousLocation = player.getLocation().toVector();
    }
    private @Getter @Setter int checkpoint = 0;
    private @Getter int lap = 1;
    public void incrementLap() {
        lap++;
    }
    private @Getter @Setter long time = 0;

    public boolean canEnterBoat = true;
    public boolean canExitBoat = false;

    private PlayerData data;

    public YesBoatsPlayer(Arena arena, Player player) {
        ScoreboardManager scoreboardManager = plugin.getServer().getScoreboardManager();
        if(scoreboardManager == null) throw new NullPointerException("ScoreboardManager was assigned before the first world was loaded, and thus, is null.");
        this.arena = arena;
        this.player = player;
        this.scoreboard = new ArenaScoreboard(player, scoreboardManager.getNewScoreboard());
    }

    public void setSpectator() {
        spectator = true;
        if(player.isInsideVehicle())
            Objects.requireNonNull(player.getVehicle()).remove();
        player.setAllowFlight(true);
        arena.getPlayers().forEach(((p, ybp) -> {
            p.hidePlayer(plugin, player);
            ybp.getHiddenPlayers().add(player);
        }));
        arena.getSpectators().add(player);
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
