package rocks.learnercouncil.yesboats.arena;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import rocks.learnercouncil.yesboats.YesBoats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import static org.bukkit.ChatColor.*;

public class ArenaEditor {

    private static final YesBoats plugin = YesBoats.getInstance();

    public static HashMap<Player, ArenaEditor> editors = new HashMap<>();

    private final ItemStack[] playerInv;
    private final Player player;
    public final Arena arena;

    private Vector boxCorner1 = null, boxCorner2 = null;
    private BoundingBox createdBox;
    private BoundingBox selectedBox;
    private BukkitTask displayTask;


    public ArenaEditor(Player player, Arena arena) {
        this.player = player;
        playerInv = player.getInventory().getContents();
        this.arena = arena;
        initializeInventory();
        startBoxDisplay();
    }

    /**
     * Initializes the player's inventory with all the items used for editing.
     */
    private void initializeInventory() {
        player.getInventory().clear();
        Inventory inv = player.getInventory();

        inv.setItem(0, getItem(Material.IRON_AXE,
                BOLD.toString() + YELLOW + "Area Selector",
                GOLD + "Left click a block to select the 1st corner.",
                GOLD + "Right click a block to select the 2nd corner."));

        inv.setItem(1, getItem(Material.TNT,
                BOLD.toString() + RED + "Remove Bounding Box",
                DARK_RED + "Click to remove the selected",
                DARK_RED + "bounding box."));

        inv.setItem(2, getItem(Material.BARRIER,
                BOLD.toString() + RED + "Death Barrier",
                DARK_RED + "Click to set the selected",
                DARK_RED + "bouding box to a death barrier"));

        inv.setItem(3, getItem(Material.LIGHT_BLUE_BANNER,
                BOLD.toString() + AQUA + "Checkpoint",
                DARK_AQUA + "Click to set the selected",
                DARK_AQUA + "bouding box to a checkpoint"));



        //TODO add items for setting the minPlayers, maxPlayers, lobbyLocation, startWorld, startLocations, and lightLocations
    }

    /**
     * Gets an {@link ItemStack} from a {@link Material}, name, and lore.
     * @param material The material of the item.
     * @param name The name of the item.
     * @param lore The lore of the Item.
     * @return The ItemStack
     */
    private ItemStack getItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if(meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private void setBoxCorner1(Vector boxCorner1) {
        this.boxCorner1 = boxCorner1;
        if(boxCorner2 != null)
            createdBox = BoundingBox.of(this.boxCorner1, this.boxCorner2);
    }

    private void setBoxCorner2(Vector boxCorner2) {
        this.boxCorner2 = boxCorner2;
        if(boxCorner1 != null)
            createdBox = BoundingBox.of(this.boxCorner1, this.boxCorner2);
    }

    /**
     * Adds a bounding box the the specified type the the {@link Arena}.
     * @param type The {@link BoundingBoxType}.
     */
    private void addBoundingBox(BoundingBoxType type) {
        //TODO add ability to remove a bounding box
        if(selectedBox == null) {
            player.sendMessage(DARK_RED + "[YesBoats] " + RED + "There is no bounding box selected");
            return;
        }
        switch (type) {
            case CHECKPOINT:
                arena.checkpointBoxes.add(selectedBox.clone());
                break;
            case DEATH_BARRIER:
                arena.deathBarriers.add(selectedBox.clone());
                break;
            case REMOVE:
                //Do nothing. The code below will already remove the bounding box.
                break;
            default:
                boxCorner1 = boxCorner2 = null;
                createdBox = null;
                return;
        }
        arena.checkpointBoxes.remove(selectedBox);
        arena.deathBarriers.remove(selectedBox);
        boxCorner1 = boxCorner2 = null;
        createdBox = null;
    }


    private void startBoxDisplay() {
        displayTask = new BukkitRunnable() {
            @Override
            public void run() {
                offsetDisplayBox = !offsetDisplayBox;
                HashSet<BoundingBox> boxes = new HashSet<>();
                boxes.add(createdBox);
                boxes.addAll(arena.deathBarriers);
                boxes.addAll(arena.checkpointBoxes);

                boolean clearSelection = true;
                for(BoundingBox box : boxes) {
                    if(boxRaycast(box)) {
                        selectedBox = box;
                        displayBoundingBox(selectedBox, new Particle.DustOptions(Color.YELLOW,1));
                        clearSelection = false;
                        break;
                    }
                }
                if(clearSelection) selectedBox = null;

                if(createdBox != null)
                    displayBoundingBox(createdBox, new Particle.DustOptions(Color.WHITE, 1));
                arena.deathBarriers.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.RED, 1)));
                arena.checkpointBoxes.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.AQUA, 1)));
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private boolean boxRaycast(BoundingBox box) {
        if(box == null) return false;
        for(double i = 0; i < 10; i += 0.5) {
            Vector directionVector = player.getEyeLocation().getDirection();
            Vector locationVector = player.getEyeLocation().toVector().add(directionVector.multiply(i));
            if(box.contains(locationVector.toBlockVector())) return true;
        }
        return false;
    }


    private boolean offsetDisplayBox = false;
    /**
     * Displays the specified {@link BoundingBox} using particles.
     * @param box the bounding box to display.
     * @param color the color and size on the particles in the for of a {@link Particle.DustOptions} object.
     */
    private void displayBoundingBox(BoundingBox box, Particle.DustOptions color) {
        Vector c1 = box.getMin();
        Vector c2 = box.getMax().add(new Vector(1, 1, 1));
        TriDouble spawnParticle = (x, y, z) -> player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, y, z), 1, color);

        double offset = offsetDisplayBox ? 0.5 : 0;

        //x axis lines
        for(double x = c1.getX() + offset; x <= c2.getX(); x += 1) {
            spawnParticle.spawn(x, c1.getY(), c1.getZ());
            spawnParticle.spawn(x, c1.getY(), c2.getZ());
            spawnParticle.spawn(x, c2.getY(), c1.getZ());
            spawnParticle.spawn(x, c2.getY(), c2.getZ());
        }
        //y axis lines
        for(double y = c1.getY() + offset; y <= c2.getY(); y += 1) {
            spawnParticle.spawn(c1.getX(), y, c1.getZ());
            spawnParticle.spawn(c1.getX(), y, c2.getZ());
            spawnParticle.spawn(c2.getX(), y, c1.getZ());
            spawnParticle.spawn(c2.getX(), y, c2.getZ());
        }
        //z axis lines
        for(double z = c1.getZ() + offset; z <= c2.getZ(); z += 1) {
            spawnParticle.spawn(c1.getX(), c1.getY(), z);
            spawnParticle.spawn(c1.getX(), c2.getY(), z);
            spawnParticle.spawn(c2.getX(), c1.getY(), z);
            spawnParticle.spawn(c2.getX(), c2.getY(), z);
        }
    }
    @FunctionalInterface
    private interface TriDouble {
        void spawn(double x, double y, double z);
    }



    public void restore() {
        player.getInventory().setContents(playerInv);
        displayTask.cancel();
        boxCorner1 = null;
        boxCorner2 = null;
        selectedBox = null;
        createdBox = null;
        if(!Arena.arenas.contains(arena)) Arena.arenas.add(arena);
    }
    public static class Events implements Listener {
        
        private final HashMap<Player, Boolean> isSettingCheckpoint = new HashMap<>();

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(e.getInventory() != e.getWhoClicked().getInventory()) return;
            if(!editors.containsKey((Player) e.getWhoClicked())) return;

            e.setCancelled(true);
        }

        @EventHandler
        public void onClick(PlayerInteractEvent e) {
            Action action = e.getAction();
            Player player = e.getPlayer();
            if(!editors.containsKey(player)) return;
            ArenaEditor editor = editors.get(player);
            if(e.getItem() == null) return;
            switch (e.getItem().getType()) {
                case IRON_AXE:
                    if(action == Action.LEFT_CLICK_BLOCK) {
                        //noinspection ConstantConditions
                        Location location = e.getClickedBlock().getLocation();
                        editor.setBoxCorner1(location.toVector());
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                                AQUA + "Position 1 set. (" +
                                        location.getBlockX() + ", " +
                                        location.getBlockY() + ", " +
                                        location.getBlockZ() + ")"));
                        e.setCancelled(true);
                    }
                    if(action == Action.RIGHT_CLICK_BLOCK) {
                        //noinspection ConstantConditions
                        Location location = e.getClickedBlock().getLocation();
                        editor.setBoxCorner2(location.toVector());
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(
                                AQUA + "Position 1 set. (" +
                                        location.getBlockX() + ", " +
                                        location.getBlockY() + ", " +
                                        location.getBlockZ() + ")"));
                        e.setCancelled(true);
                    }
                    break;
                case TNT:
                    if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        editor.addBoundingBox(BoundingBoxType.REMOVE);
                        e.setCancelled(true);
                    }
                case BARRIER:
                    if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        editor.addBoundingBox(BoundingBoxType.DEATH_BARRIER);
                        player.sendMessage(DARK_AQUA + "[YesBoats] " + AQUA + "Death barrier added.");
                        e.setCancelled(true);
                    }
                    break;
                case LIGHT_BLUE_BANNER:
                    if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        if(!isSettingCheckpoint.get(player)) {
                            editor.addBoundingBox(BoundingBoxType.CHECKPOINT);
                            player.sendMessage(DARK_AQUA + "[YesBoats]" + AQUA +"Bounding box for chectpoint #" + editor.arena.checkpointBoxes.size() + " set. Click again to set the spawnpoint.");
                            isSettingCheckpoint.put(player, true);
                        } else {
                            Location playerLocation = player.getLocation();
                            float yaw = (float) (Math.floor(playerLocation.getYaw() / 22.5) * 22.5);
                            editor.arena.checkpointSpawns.add(new Location(player.getWorld(), playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ(), yaw, 0));
                        }
                        e.setCancelled(true);
                    }
                    break;
                    //TODO add logic for the minPlayers, maxPlayers, lobbyLocation, startWorld, startLocations, and lightLocations items when added
            }
        }
    }

    private enum BoundingBoxType {
        DEATH_BARRIER, CHECKPOINT, REMOVE
    }
}
