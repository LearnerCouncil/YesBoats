package rocks.learnercouncil.yesboats.arena;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.*;
import java.util.stream.Collectors;

public class ArenaSign {

    private static final YesBoats plugin = YesBoats.getPlugin();

    private final Sign sign;
    private final Side side;
    private final SignSide signSide;

    public ArenaSign(Sign sign, Side side) {
        this.sign = sign;
        this.side = side;
        signSide = sign.getSide(side);
    }

    public static List<String> serialize(Collection<ArenaSign> signs) {
        return signs.stream().map(s -> {
            Location location = s.sign.getLocation();
            return s.sign.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "," + s.side;
        }).collect(Collectors.toList());
    }

    public static Set<ArenaSign> deserialize(Collection<String> serializedSigns) {
        HashSet<ArenaSign> result = new HashSet<>();
        serializedSigns.forEach(sign -> {
            Optional<ArenaSign> signOptional = ArenaSign.deserializeSingle(sign);
            if (signOptional.isEmpty()) {
                plugin.getLogger().warning("Arena Sign '" + sign + "' failed to deserialize.");
                return;
            }
            result.add(signOptional.get());
        });
        return result;
    }

    private static Optional<ArenaSign> deserializeSingle(String serializedSign) {
        String[] segments = serializedSign.split(",");
        boolean legacy = segments.length == 4;
        if (segments.length < 4 || segments.length > 5) return Optional.empty();
        World world = plugin.getServer().getWorld(segments[0]);
        if (world == null) return Optional.empty();
        try {
            Block block = world.getBlockAt(Integer.parseInt(segments[1]), Integer.parseInt(segments[2]), Integer.parseInt(segments[3]));
            if (!(block.getState() instanceof Sign)) return Optional.empty();
            final Sign state = (Sign) block.getState();
            if (legacy) state.setWaxed(true);
            return Optional.of(new ArenaSign(state, legacy ? Side.FRONT : Side.valueOf(segments[4])));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean contains(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().anyMatch(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation()));
    }

    public static Optional<ArenaSign> get(Collection<ArenaSign> signs, Sign sign) {
        return signs.stream().filter(s -> s.sign.equals(sign) || s.sign.getLocation().equals(sign.getLocation())).findFirst();
    }

    public void initializeText(String arenaName) {
        signSide.setLine(0, ChatColor.DARK_AQUA + "[YesBoats]");
        signSide.setLine(1, ChatColor.AQUA + arenaName);
        sign.setWaxed(true);
    }

    public void update(Arena.State state, int players, int maxPlayers) {
        ChatColor signColor = ChatColor.BLACK;
        switch (state) {
            case WAITING -> {
                signColor = ChatColor.GREEN;
                signSide.setLine(2, signColor + "Waiting...");
            }
            case IN_QUEUE -> {
                signColor = ChatColor.YELLOW;
                signSide.setLine(2, signColor + "Starting...");
            }
            case RUNNING -> {
                signColor = ChatColor.RED;
                signSide.setLine(2, signColor + "Running.");
            }
        }
        signSide.setLine(3, signColor + "[" + players + "/" + maxPlayers + "]");
        sign.update();
    }

    public static class Events implements Listener {

        private String[] getValidText(Sign sign) {
            String[] frontText = getValidText(sign.getSide(Side.FRONT).getLines());
            String[] backText = getValidText(sign.getSide(Side.BACK).getLines());
            if (frontText.length != 0) return frontText;
            if (backText.length != 0) return backText;
            return new String[0];
        }

        private String[] getValidText(String[] lines) {
            String[] text = Arrays.stream(lines).map(ChatColor::stripColor).toArray(String[]::new);
            return text[0].equalsIgnoreCase("[YesBoats]") && Arena.get(text[1]).isPresent() ? text : new String[0];
        }

        @EventHandler
        public void onClick(PlayerInteractEvent event) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (!(Objects.requireNonNull(event.getClickedBlock()).getState() instanceof Sign)) return;
            Sign sign = (Sign) event.getClickedBlock().getState();

            String[] text = getValidText(sign);
            if (text.length == 0) return;

            assert Arena.get(text[1]).isPresent();
            if (!ArenaSign.contains(Arena.get(text[1]).get().signs, sign)) return;

            plugin.getServer().dispatchCommand(event.getPlayer(), "yesboats join " + text[1]);
        }


        @EventHandler
        public void onSignEdit(SignChangeEvent event) {
            if (!event.getPlayer().hasPermission("yesboats.joinsign")) return;
            if (getValidText(event.getLines()).length == 0) return;

            assert Arena.get(event.getLine(1)).isPresent();
            Arena arena = Arena.get(event.getLine(1)).get();
            ArenaSign sign = new ArenaSign((Sign) event.getBlock().getState(), event.getSide());
            arena.signs.add(sign);
            new BukkitRunnable() {
                @Override
                public void run() {
                    sign.initializeText(arena.name);
                    arena.updateSigns();
                }
            }.runTaskLater(plugin, 1);
            event.setCancelled(true);
        }

        @EventHandler
        public void onBlockBreak(BlockBreakEvent event) {
            if (!(event.getBlock().getState() instanceof Sign)) return;
            Sign sign = (Sign) event.getBlock().getState();
            String[] text = getValidText(sign);
            if (text.length == 0) return;

            assert Arena.get(text[1]).isPresent();
            Arena arena = Arena.get(text[1]).get();
            ArenaSign.get(arena.signs, sign).ifPresent(arena.signs::remove);
        }
    }
}
