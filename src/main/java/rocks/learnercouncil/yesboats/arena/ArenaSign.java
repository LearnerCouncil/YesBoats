package rocks.learnercouncil.yesboats.arena;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.*;
import java.util.stream.Collectors;

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
        Arena.State state = arena.getState();
        switch (state) {
            case WAITING:
                signColor = ChatColor.GREEN;
                sign.setLine(2, signColor + "Waiting...");
                break;
            case IN_QUEUE:
                signColor = ChatColor.YELLOW;
                sign.setLine(2, signColor + "Starting...");
            case RUNNING:
                signColor = ChatColor.RED;
                sign.setLine(2, signColor + "Running");
        }
        sign.setLine(3, signColor + "(" + players + "/" + maxPlayers + ")");
        sign.update();
    }

    public static List<String> serialize(Collection<ArenaSign> signs) {
        return signs.stream().map(s -> {
            Location location = s.sign.getLocation();
            return s.sign.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        }).collect(Collectors.toList());
    }

    public static Set<ArenaSign> deserialize(Arena arena, Collection<String> serializedSigns) {
        HashSet<ArenaSign> result = new HashSet<>();
        serializedSigns.forEach(s -> {
            Optional<ArenaSign> signOptional = ArenaSign.deserializeSingle(arena, s);
            if(!signOptional.isPresent()) {
                plugin.getLogger().warning("Arena Sign '" + s + "' failed to deserialize.");
                return;
            }
            result.add(signOptional.get());
        });
        return result;
    }

    private static Optional<ArenaSign> deserializeSingle(Arena arena, String serializedSign) {
        String[] segments = serializedSign.split(",");
        if(segments.length != 4) return Optional.empty();
        World world = plugin.getServer().getWorld(segments[1]);
        if(world == null) return Optional.empty();
        try {
            Block block = world.getBlockAt(Integer.parseInt(segments[1]), Integer.parseInt(segments[2]), Integer.parseInt(segments[3]));
            if(!(block instanceof Sign)) return Optional.empty();
            return Optional.of(new ArenaSign((Sign) block, arena));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static boolean contains(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().anyMatch(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation()));
    }

    public static Optional<ArenaSign> get(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().filter(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation())).findFirst();
    }

    public static boolean isValid(String[] text) {
        return text[0].equalsIgnoreCase("[YesBoats]") && Arena.get(text[1]).isPresent();
    }

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
        public void onSignEdit(SignChangeEvent e) {
            if (!ArenaSign.isValid(e.getLines())) return;

            Optional<Arena> arenaOptional = Arena.get(e.getLine(1));
            assert arenaOptional.isPresent();
            arenaOptional.get().signs.add(new ArenaSign((Sign) e.getBlock(), arenaOptional.get()));
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent e) {
            if(!(e.getBlock() instanceof Sign)) return;
            Sign sign = (Sign) e.getBlock();
            String[] text = Arrays.stream(sign.getLines()).map(ChatColor::stripColor).toArray(String[]::new);
            if(!text[0].equalsIgnoreCase("[YesBoats]")) return;
            if(!Arena.get(text[1]).isPresent()) return;

            Arena arena = Arena.get(text[1]).get();
            ArenaSign.get(arena.signs, sign).ifPresent(arena.signs::remove);
        }
    }
}
