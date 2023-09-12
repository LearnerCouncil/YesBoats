package rocks.learnercouncil.yesboats;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import rocks.learnercouncil.yesboats.arena.Arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class InventoryManager {

    private static final List<ItemStack> items = new ArrayList<>();

    public static void initialize(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(18, getItem(Material.OAK_BOAT, "Oak Boat"));
        inventory.setItem(19, getItem(Material.SPRUCE_BOAT, "Spruce Boat"));
        inventory.setItem(20, getItem(Material.BIRCH_BOAT, "Birch Boat"));
        inventory.setItem(21, getItem(Material.JUNGLE_BOAT, "Jungle Boat"));
        inventory.setItem(22, getItem(Material.ACACIA_BOAT, "Acacia Boat"));
        inventory.setItem(23, getItem(Material.DARK_OAK_BOAT, "Dark Oak Boat"));
        inventory.setItem(24, getItem(Material.MANGROVE_BOAT, "Mangrove Boat"));
        inventory.setItem(25, getItem(Material.CHERRY_BOAT, "Cherry Boat"));
        inventory.setItem(26, getItem(Material.BAMBOO_RAFT, "Bamboo Raft"));

        inventory.setItem(9, getItem(Material.OAK_CHEST_BOAT, "Oak Chest Boat"));
        inventory.setItem(10, getItem(Material.SPRUCE_CHEST_BOAT, "Spruce Chest Boat"));
        inventory.setItem(11, getItem(Material.BIRCH_CHEST_BOAT, "Birch Chest Boat"));
        inventory.setItem(12, getItem(Material.JUNGLE_CHEST_BOAT, "Jungle Chest Boat"));
        inventory.setItem(13, getItem(Material.ACACIA_CHEST_BOAT, "Acacia Chest Boat"));
        inventory.setItem(14, getItem(Material.DARK_OAK_CHEST_BOAT, "Dark Oak Chest Boat"));
        inventory.setItem(15, getItem(Material.MANGROVE_CHEST_BOAT, "Mangrove Chest Boat"));
        inventory.setItem(16, getItem(Material.CHERRY_CHEST_BOAT, "Cherry Chest Boat"));
        inventory.setItem(17, getItem(Material.BAMBOO_CHEST_RAFT, "Bamboo Chest Raft"));
    }

    private static ItemStack getItem(Material material, String boatType) {
        ItemStack item = new ItemStack(material, 1);
        items.add(item);
        ItemMeta meta = item.getItemMeta();
        if(meta == null) return item;
        meta.setDisplayName(ChatColor.BOLD.toString() + ChatColor.GREEN + boatType);
        meta.setLore(Collections.singletonList(ChatColor.DARK_GREEN + "Click to set your boat to a " + boatType));
        item.setItemMeta(meta);
        return item;
    }

    public static class Events implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(!items.contains(e.getCurrentItem())) return;
            if(!(e.getWhoClicked() instanceof Player)) return;
            Player player = (Player) e.getWhoClicked();
            if(Arena.get(player).isEmpty()) return;
            Arena arena = Arena.get(player).get();
            if(arena.getState() != Arena.State.IN_QUEUE) return;
            if(!player.isInsideVehicle()) return;
            if(!(player.getVehicle() instanceof Boat)) return;
            Boat vehicle = (Boat) player.getVehicle();

            Material itemType = Objects.requireNonNull(e.getCurrentItem()).getType();
            boolean vehicleHasChest = player.getVehicle() instanceof ChestBoat;
            boolean itemHasChest = itemType.toString().contains("CHEST");
            Boat.Type boatType = switch (itemType) {
                case OAK_BOAT, OAK_CHEST_BOAT -> Boat.Type.OAK;
                case SPRUCE_BOAT, SPRUCE_CHEST_BOAT -> Boat.Type.SPRUCE;
                case BIRCH_BOAT, BIRCH_CHEST_BOAT -> Boat.Type.BIRCH;
                case JUNGLE_BOAT, JUNGLE_CHEST_BOAT -> Boat.Type.JUNGLE;
                case ACACIA_BOAT, ACACIA_CHEST_BOAT -> Boat.Type.ACACIA;
                case DARK_OAK_BOAT, DARK_OAK_CHEST_BOAT -> Boat.Type.DARK_OAK;
                case MANGROVE_BOAT, MANGROVE_CHEST_BOAT -> Boat.Type.MANGROVE;
                case CHERRY_BOAT, CHERRY_CHEST_BOAT -> Boat.Type.CHERRY;
                case BAMBOO_RAFT, BAMBOO_CHEST_RAFT -> Boat.Type.BAMBOO;
                default -> vehicle.getBoatType();
            };

            if(vehicleHasChest != itemHasChest) respawnBoat(itemHasChest, boatType, arena.getPlayers().get(player));
            else vehicle.setBoatType(boatType);

            e.setCancelled(true);
        }

        @SuppressWarnings("ConstantConditions")
        private void respawnBoat(boolean hasChest, Boat.Type type, YesBoatsPlayer yesBoatsPlayer) {
            yesBoatsPlayer.canEnterBoat = true;
            yesBoatsPlayer.canExitBoat = true;
            Player player = yesBoatsPlayer.getPlayer();
            Boat boat;
            if(player.isInsideVehicle()) {
                boat = (Boat) player.getWorld().spawnEntity(player.getVehicle().getLocation(), hasChest ? EntityType.CHEST_BOAT : EntityType.BOAT);
                if(player.getVehicle().isInsideVehicle())
                    player.getVehicle().getVehicle().addPassenger(boat);
                player.getVehicle().getPassengers().forEach(p -> {
                    if(!(p instanceof Player))
                        p.remove();
                });
                player.getVehicle().remove();
            } else
                boat = (Boat) player.getWorld().spawnEntity(player.getLocation(), hasChest ? EntityType.CHEST_BOAT : EntityType.BOAT);
            boat.setInvulnerable(true);
            boat.setBoatType(type);
            boat.addPassenger(player);
            if(boat.getType() == EntityType.BOAT) {
                ArmorStand armorStand = (ArmorStand) boat.getWorld().spawnEntity(boat.getLocation(), EntityType.ARMOR_STAND);
                armorStand.setInvulnerable(true);
                armorStand.setInvisible(true);
                armorStand.setSmall(true);
                armorStand.setMarker(true);
                boat.addPassenger(armorStand);
            }
            yesBoatsPlayer.canExitBoat = false;
            yesBoatsPlayer.canEnterBoat = false;
        }
    }
}
