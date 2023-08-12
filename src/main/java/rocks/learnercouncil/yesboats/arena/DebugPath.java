package rocks.learnercouncil.yesboats.arena;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.YesBoats;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class DebugPath {
    public static final List<DebugPath> debugPaths = new LinkedList<>();
    private static BukkitTask displayLoop;

    public static void display(int index, Player player) {
        DebugPath path = debugPaths.get(index);
        BaseComponent[] actionbarMessage = new ComponentBuilder()
                .append("ID: ").color(ChatColor.DARK_AQUA).append(String.valueOf(index)).color(ChatColor.AQUA)
                .append(" | ").color(ChatColor.YELLOW).append("Player: ").color(ChatColor.DARK_AQUA).append(path.player.getName()).color(ChatColor.AQUA)
                .append(" | ").color(ChatColor.YELLOW).append("Ping: ").color(ChatColor.DARK_AQUA).append(String.valueOf(path.ping)).color(ChatColor.AQUA)
                .create();
        if(displayLoop != null) displayLoop.cancel();
        displayLoop = new BukkitRunnable() {
            @Override
            public void run() {
                for(Vector vector : path.vectors) {
                    Location l = vector.toLocation(player.getWorld());
                    player.spawnParticle(Particle.END_ROD, l, 1, 0, 0, 0, 0);
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, actionbarMessage);
            }
        }.runTaskTimer(YesBoats.getInstance(), 0, 10);
    }
    public static void clearDisplay() {
        displayLoop.cancel();
    }

    public final HashSet<Vector> vectors = new HashSet<>();
    public final Player player;
    public final Arena arena;
    public long timestamp;
    public int ping;

    public DebugPath(Player player, Arena arena) {
        this.player = player;
        this.arena = arena;
    }

    public BaseComponent[] toReport() {
        Date date = new Date(timestamp);
        DateFormat format = new SimpleDateFormat("HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        String dateString = format.format(date);
        int index = debugPaths.indexOf(this);
        return new ComponentBuilder()
                .append(index + ". ").color(ChatColor.YELLOW)
                .append(dateString + " | " + player.getName()).color(ChatColor.AQUA)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/yesboats reports " + arena.name + " " + index))
                .create();
    }

    @Override
    public String toString() {
        return "DebugPath{" +
                "vectors=" + vectors +
                ", player=" + player +
                ", arena=" + arena +
                ", timestamp=" + timestamp +
                ", ping=" + ping +
                '}';
    }
}
