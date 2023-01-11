package rocks.learnercouncil.yesboats.arena;

import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class ArenaSign {

    private static final YesBoats plugin = YesBoats.getInstance();

    private final Sign sign;
    private final Arena arena;

    protected int players;
    protected int maxPlayers;

    public ArenaSign(Sign sign, Arena arena) {
        this.sign = sign;
        this.arena = arena;

        this.sign.setLine(0, ChatColor.DARK_AQUA + "[YesBoats]");
        this.sign.setLine(1, ChatColor.AQUA + arena.name);
        arena.updateSigns();
    }

    public void update(int players, int maxPlayers) {
        this.players = players;
        this.maxPlayers = maxPlayers;
        ChatColor signColor = ChatColor.BLACK;
        int state = arena.getState();
        switch (state) {
            case 0:
                signColor = ChatColor.GREEN;
                sign.setLine(2, signColor + "Waiting...");
                break;
            case 1:
                signColor = ChatColor.YELLOW;
                sign.setLine(2, signColor + "Starting...");
            case 2:
                signColor = ChatColor.RED;
                sign.setLine(2, signColor + "Running");
        }
        sign.setLine(3, signColor + "(" + players + "/" + maxPlayers + ")");
        sign.update();
    }

    public static boolean contains(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().anyMatch(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation()));
    }

    public static boolean isValid(String[] text) {
        return text[0].equalsIgnoreCase("[YesBoats]") && Arena.get(text[1]).isPresent();
    }

    //TODO sign serialzation and deserialization

    public static class Events implements Listener {

        @EventHandler
        public void onClick(PlayerInteractEvent e) {
            if(e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if(!(e.getClickedBlock() instanceof Sign)) return;
            Sign sign = (Sign) e.getClickedBlock();
            String[] text = Arrays.stream(sign.getLines()).map(ChatColor::stripColor).toArray(String[]::new);
            if(!text[0].equalsIgnoreCase("[YesBoats]")) return;
            if(!Arena.get(text[1]).isPresent()) return;
            if(!ArenaSign.contains(Arena.get(text[1]).get().signs, sign)) return;

            plugin.getServer().dispatchCommand(e.getPlayer(), "yesboats join " + text[1]);
        }

        @EventHandler
        public void OnSignEdit(SignChangeEvent e) {
            if (!ArenaSign.isValid(e.getLines())) return;

            Optional<Arena> arenaOptional = Arena.get(e.getLine(1));
            assert arenaOptional.isPresent();
            arenaOptional.get().signs.add(new ArenaSign((Sign) e.getBlock(), arenaOptional.get()));
        }

        //TODO sign breaking
    }
}
