package rocks.learnercouncil.yesboats.arena;

import org.bukkit.ChatColor;
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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;

public class ArenaEditor {

    public static HashMap<Player, ArenaEditor> editors = new HashMap<>();

    private final ItemStack[] playerInv;
    private final Player player;
    public final Arena arena;

    private Vector bbCorner1 = null, bbCorner2 = null;
    private BoundingBox selectedBox;
    private BukkitTask displayTask;
    private boolean boxDisplay;


    public ArenaEditor(Player player, Arena arena) {
        this.player = player;
        playerInv = player.getInventory().getContents();
        this.arena = arena;
        initializeInventory();
    }

    private void initializeInventory() {
        player.getInventory().clear();
        Inventory inv = player.getInventory();
        ItemMeta meta;

        ItemStack selectionAxe = getItem(Material.IRON_AXE,
                ChatColor.BOLD.toString() + ChatColor.YELLOW + "Area Selector",
                ChatColor.GOLD + "Left click a block to select the 1st corner.",
                ChatColor.GOLD + "Right click a block to select the 2nd corner.");
        inv.setItem(0, selectionAxe);

        ItemStack deathBarrier = getItem(Material.BARRIER,
                ChatColor.BOLD.toString() + ChatColor.RED + "Death Barrier",
                ChatColor.DARK_RED + "Click to set the selected bouding box to a death barrier");
        inv.setItem(1, deathBarrier);
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

    private void setBBCorner1(Vector bbCorner1) {
        this.bbCorner1 = bbCorner1;
        if(bbCorner2 != null)
            selectedBox = new BoundingBox(bbCorner1.getX(), bbCorner1.getY(), bbCorner1.getZ(), bbCorner2.getX(), bbCorner2.getY(), bbCorner2.getZ());
    }

    private void setBbCorner2(Vector bbCorner2) {
        this.bbCorner2 = bbCorner2;
        if(bbCorner1 != null)
            selectedBox = new BoundingBox(bbCorner1.getX(), bbCorner1.getY(), bbCorner1.getZ(), bbCorner2.getX(), bbCorner2.getY(), bbCorner2.getZ());
    }

    private void addBoundingBox(BoundingBoxType type) {
        if(bbCorner1 == null || bbCorner2 == null) return;
        switch (type) {
            case CHECKPOINT:
                arena.checkpointBoxes.add(selectedBox.clone());
                break;
            case DEATH_BARRIER:
                arena.deathBarriers.add(selectedBox.clone());
                break;
        }
        bbCorner1 = null;
        bbCorner2 = null;
    }


    private void startBoxDisplay() {

    }
    private void stopBoxDisplay() {
        boxDisplay = false;
    }

    private void displayBoundingBox(BoundingBox bb, Particle.DustOptions color) {
        Vector c1 = bb.getMin();
        Vector c2 = bb.getMax();
        //x axis lines
        for(double x = c1.getX(); x >= c2.getX(); x += 0.2) {
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, c1.getY(), c1.getZ()), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, c1.getY(), c2.getZ()), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, c2.getY(), c1.getZ()), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), x, c2.getY(), c2.getZ()), 1, color);
        }
        //y axis lines
        for(double y = c1.getY(); y >= c2.getY(); y += 0.2) {
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c1.getX(), y, c1.getZ()), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c1.getX(), y, c2.getZ()), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c2.getX(), y, c1.getZ()), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c2.getX(), y, c2.getZ()), 1, color);
        }
        //z axis lines
        for(double z = c1.getZ(); z >= c2.getZ(); z += 0.2) {
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c1.getX(), c1.getY(), z), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c1.getX(), c2.getY(), z), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c2.getX(), c1.getY(), z), 1, color);
            player.spawnParticle(Particle.REDSTONE, new Location(player.getWorld(), c2.getX(), c2.getY(), z), 1, color);
        }
    }


    public void restore() {
        player.getInventory().setContents(playerInv);
    }
    public static class Events implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(e.getInventory() != e.getWhoClicked().getInventory()) return;
            if(!editors.containsKey((Player) e.getWhoClicked())) return;

            e.setCancelled(true);
        }

        @EventHandler
        public void onClickBlock(PlayerInteractEvent e) {
            if(e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
            if(!editors.containsKey(e.getPlayer())) return;

            e.setCancelled(true);
        }
    }

    enum BoundingBoxType {
        DEATH_BARRIER, CHECKPOINT
    }
}
