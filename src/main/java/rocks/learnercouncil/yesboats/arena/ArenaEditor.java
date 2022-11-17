package rocks.learnercouncil.yesboats.arena;

import org.bukkit.*;
import org.bukkit.block.data.type.Bed;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

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

    private void initializeInventory() {
        player.getInventory().clear();
        Inventory inv = player.getInventory();

        ItemStack selectionAxe = getItem(Material.IRON_AXE,
                ChatColor.BOLD.toString() + ChatColor.YELLOW + "Area Selector",
                ChatColor.GOLD + "Left click a block to select the 1st corner.",
                ChatColor.GOLD + "Right click a block to select the 2nd corner.");
        inv.setItem(0, selectionAxe);

        ItemStack deathBarrier = getItem(Material.BARRIER,
                ChatColor.BOLD.toString() + ChatColor.RED + "Death Barrier",
                ChatColor.DARK_RED + "Click to set the selected bouding box to a death barrier");
        inv.setItem(1, deathBarrier);

        ItemStack checkpoint = getItem(Material.RED_BANNER,
                ChatColor.BOLD.toString() + ChatColor.AQUA + "Checkpoint",
                ChatColor.DARK_AQUA + "Click to set the selected bouding box to a checkpoint");
        inv.setItem(2, checkpoint);
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

    private void addBoundingBox(BoundingBoxType type) {
        if(selectedBox == null) return;
        switch (type) {
            case CHECKPOINT:
                arena.checkpointBoxes.add(selectedBox.clone());
                break;
            case DEATH_BARRIER:
                arena.deathBarriers.add(selectedBox.clone());
                break;
        }
        boxCorner1 = null;
        boxCorner2 = null;
    }


    private void startBoxDisplay() {
        displayTask = new BukkitRunnable() {
            @Override
            public void run() {
                displayBoxOffset = !displayBoxOffset;
                HashSet<BoundingBox> boxes = new HashSet<>();
                boxes.add(createdBox);
                boxes.addAll(arena.deathBarriers);
                boxes.addAll(arena.checkpointBoxes);
                for(BoundingBox box : boxes) {
                    if(boxRaycast(box)) {
                        selectedBox = box;
                        plugin.getLogger().info("Bounding box at " + box.getCenter() + " selected.");
                        displayBoundingBox(selectedBox, new Particle.DustOptions(Color.YELLOW,1));
                        break;
                    }
                }

                if(createdBox != null)
                    displayBoundingBox(createdBox, new Particle.DustOptions(Color.WHITE, 1));
                arena.deathBarriers.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.RED, 1)));
                arena.checkpointBoxes.forEach(b -> displayBoundingBox(b, new Particle.DustOptions(Color.AQUA, 1)));
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private boolean boxRaycast(BoundingBox box) {
        if(box == null) return false;
        for(int i = 0; i < 10; i++) {
            Vector directionVector = player.getEyeLocation().getDirection().multiply(i);
            Vector locationVector = player.getEyeLocation().toVector().multiply(directionVector);
            plugin.getLogger().info("directionVector" + box.getCenter() + ", locationVector: " + locationVector);
            if(box.contains(locationVector)) return true;
        }
        return false;
    }

    private void stopBoxDisplay() {
        displayTask.cancel();
    }


    private boolean displayBoxOffset = false;
    /**
     * Displays the specified {@link BoundingBox} using particles.
     * @param box the bounding box to display.
     * @param color the color and size on the particles in the for of a {@link Particle.DustOptions} object.
     */
    private void displayBoundingBox(BoundingBox box, Particle.DustOptions color) {
        Vector c1 = box.getMin();
        Vector c2 = box.getMax().add(new Vector(1, 1, 1));
        TriDouble spawnParticle = (x, y, z) -> player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, y, z), 1, color);

        double offset = displayBoxOffset ? 0.5 : 0;

        //x axis lines
        for(double x = c1.getX() + offset; x <= c2.getX(); x += 1) {
            spawnParticle.accept(x, c1.getY(), c1.getZ());
            spawnParticle.accept(x, c1.getY(), c2.getZ());
            spawnParticle.accept(x, c2.getY(), c1.getZ());
            spawnParticle.accept(x, c2.getY(), c2.getZ());
        }
        //y axis lines
        for(double y = c1.getY() + offset; y <= c2.getY(); y += 1) {
            spawnParticle.accept(c1.getX(), y, c1.getZ());
            spawnParticle.accept(c1.getX(), y, c2.getZ());
            spawnParticle.accept(c2.getX(), y, c1.getZ());
            spawnParticle.accept(c2.getX(), y, c2.getZ());
        }
        //z axis lines
        for(double z = c1.getZ() + offset; z <= c2.getZ(); z += 1) {
            spawnParticle.accept(c1.getX(), c1.getY(), z);
            spawnParticle.accept(c1.getX(), c2.getY(), z);
            spawnParticle.accept(c2.getX(), c1.getY(), z);
            spawnParticle.accept(c2.getX(), c2.getY(), z);
        }
    }
    @FunctionalInterface
    private interface TriDouble {
        void accept(double x, double y, double z);
    }



    public void restore() {
        player.getInventory().setContents(playerInv);
        stopBoxDisplay();
        boxCorner1 = null;
        boxCorner2 = null;
        selectedBox = null;
        createdBox = null;
    }
    public static class Events implements Listener {

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
                        editor.setBoxCorner1(e.getClickedBlock().getLocation().toVector());
                        player.sendMessage("#1: " + e.getClickedBlock().getLocation().toVector());
                        e.setCancelled(true);
                    }
                    if(action == Action.RIGHT_CLICK_BLOCK) {
                        //noinspection ConstantConditions
                        editor.setBoxCorner2(e.getClickedBlock().getLocation().toVector());
                        player.sendMessage("#2: " + e.getClickedBlock().getLocation().toVector());
                        e.setCancelled(true);
                    }
                    break;
                case BARRIER:
                    if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        editor.addBoundingBox(BoundingBoxType.DEATH_BARRIER);
                        e.setCancelled(true);
                    }
                    break;
                case RED_BANNER:
                    if(action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        editor.addBoundingBox(BoundingBoxType.CHECKPOINT);
                        e.setCancelled(true);
                    }
                    break;
            }
        }
    }

    enum BoundingBoxType {
        DEATH_BARRIER, CHECKPOINT
    }
}
