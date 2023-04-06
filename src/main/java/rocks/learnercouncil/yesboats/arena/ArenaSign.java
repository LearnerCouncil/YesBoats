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

    public ArenaSign(Sign sign) {
        this.sign = sign;
    }

    public void initializeText(String arenaName) {
        sign.setLine(0, ChatColor.DARK_AQUA + "[YesBoats]");
        sign.setLine(1, ChatColor.AQUA + arenaName);
        sign.update();
    }

    public void update(Arena.State state, int players, int maxPlayers) {
        ChatColor signColor = ChatColor.BLACK;
        switch (state) {
            case WAITING:
                signColor = ChatColor.GREEN;
                sign.setLine(2, signColor + "Waiting...");
                break;
            case IN_QUEUE:
                signColor = ChatColor.YELLOW;
                sign.setLine(2, signColor + "Starting...");
                break;
            case RUNNING:
                signColor = ChatColor.RED;
                sign.setLine(2, signColor + "Running");
                break;
        }
        sign.setLine(3, signColor + "[" + players + "/" + maxPlayers + "]");
        sign.update();
    }

    public static List<String> serialize(Collection<ArenaSign> signs) {
        return signs.stream().map(s -> {
            Location location = s.sign.getLocation();
            return s.sign.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        }).collect(Collectors.toList());
    }

    public static Set<ArenaSign> deserialize(Collection<String> serializedSigns) {
        HashSet<ArenaSign> result = new HashSet<>();
        serializedSigns.forEach(sign -> {
            Optional<ArenaSign> signOptional = ArenaSign.deserializeSingle(sign);
            if(!signOptional.isPresent()) {
                plugin.getLogger().warning("Arena Sign '" + sign + "' failed to deserialize.");
                return;
            }
            result.add(signOptional.get());
        });
        return result;
    }

    private static Optional<ArenaSign> deserializeSingle(String serializedSign) {
        String[] segments = serializedSign.split(",");
        if(segments.length != 4) return Optional.empty();
        World world = plugin.getServer().getWorld(segments[0]);
        if(world == null) return Optional.empty();
        try {
            Block block = world.getBlockAt(Integer.parseInt(segments[1]), Integer.parseInt(segments[2]), Integer.parseInt(segments[3]));
            if(!(block.getState() instanceof Sign)) return Optional.empty();
            return Optional.of(new ArenaSign((Sign) block.getState()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static boolean contains(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().anyMatch(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation()));
    }

    public static Optional<ArenaSign> get(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().filter(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation())).findFirst();
    }

    public static boolean isInvalid(String[] text) {
        return !text[0].equalsIgnoreCase("[YesBoats]")
                || !Arena.get(text[1]).isPresent();
    }

    public static class Events implements Listener {

        @EventHandler
        public void onClick(PlayerInteractEvent event) {
            if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if(!(Objects.requireNonNull(event.getClickedBlock()).getState() instanceof Sign)) return;
            Sign sign = (Sign) event.getClickedBlock().getState();
            String[] text = Arrays.stream(sign.getLines()).map(ChatColor::stripColor).toArray(String[]::new);
            if(ArenaSign.isInvalid(text)) return;

            assert Arena.get(text[1]).isPresent();
            if(!ArenaSign.contains(Arena.get(text[1]).get().signs, sign)) return;

            plugin.getServer().dispatchCommand(event.getPlayer(), "yesboats join " + text[1]);
        }

        @EventHandler
        public void onSignEdit(SignChangeEvent event) {
            if (ArenaSign.isInvalid(event.getLines())) return;

            Optional<Arena> arenaOptional = Arena.get(event.getLine(1));
            assert arenaOptional.isPresent();
            ArenaSign sign = new ArenaSign((Sign) event.getBlock().getState());
            Arena arena = arenaOptional.get();
            arena.signs.add(sign);
            sign.initializeText(arena.name);
            arena.updateSigns();
            event.setCancelled(true);
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if(!(event.getBlock().getState() instanceof Sign)) return;
            Sign sign = (Sign) event.getBlock().getState();
            String[] text = Arrays.stream(sign.getLines()).map(ChatColor::stripColor).toArray(String[]::new);
            if(ArenaSign.isInvalid(text)) return;

            assert Arena.get(text[1]).isPresent();
            Arena arena = Arena.get(text[1]).get();
            ArenaSign.get(arena.signs, sign).ifPresent(arena.signs::remove);
        }
    }
}
