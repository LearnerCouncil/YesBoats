package rocks.learnercouncil.yesboats;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import rocks.learnercouncil.yesboats.arena.Arena;

import java.util.*;

public class InventoryManager {
    public static HashMap<Player, PlayerData> playerData = new HashMap<>();

    private static final List<ItemStack> items = new ArrayList<>();

    public static void set(Player player) {
        if(!playerData.containsKey(player)) playerData.put(player, new PlayerData());
        PlayerData playerData = InventoryManager.playerData.get(player);
        playerData.inventory = player.getInventory().getContents();
        player.getInventory().clear();
        initializeItems(player);
        playerData.canFly = player.getAllowFlight();
        player.setAllowFlight(false);
        playerData.gameMode = player.getGameMode();
        player.setGameMode(GameMode.ADVENTURE);
        playerData.xp = player.getExp();
        player.setExp(0f);
        playerData.level = player.getLevel();
        player.setLevel(0);
        playerData.invulnerable = player.isInvulnerable();
        player.setInvulnerable(true);
        playerData.health = player.getHealth();
        player.setHealth(20);
        playerData.hunger = player.getFoodLevel();
        player.setFoodLevel(20);
        playerData.saturation = player.getSaturation();
        player.setSaturation(1.0f);
    }

    private static void initializeItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(19, getItem(Material.OAK_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Oak",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to oak."));

        inventory.setItem(20, getItem(Material.SPRUCE_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Spruce",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to spruce."));

        inventory.setItem(21, getItem(Material.BIRCH_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Birch",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to birch."));

        inventory.setItem(23, getItem(Material.JUNGLE_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Jungle",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to jungle."));

        inventory.setItem(24, getItem(Material.ACACIA_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Acacia",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to acacia."));
        inventory.setItem(25, getItem(Material.DARK_OAK_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Dark Oak",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to dark oak."));

    }

    private static ItemStack getItem(Material material, String... names) {
        ItemStack item = new ItemStack(material, 1);
        items.add(item);
        if(names.length == 0) return item;
        ItemMeta meta = item.getItemMeta();
        if(meta == null) return item;
        meta.setDisplayName(names[0]);
        List<String> lore = new LinkedList<>(Arrays.asList(names));
        lore.remove(0);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static void restore(Player p) {
        PlayerData playerData = InventoryManager.playerData.get(p);
        p.getInventory().setContents(playerData.inventory);
        p.setAllowFlight(playerData.canFly);
        p.setGameMode(playerData.gameMode);
        p.setExp(playerData.xp);
        p.setLevel(playerData.level);
        p.setInvulnerable(playerData.invulnerable);
        p.setHealth(playerData.health);
        p.setFoodLevel(playerData.hunger);
        p.setSaturation(playerData.saturation);
    }
    
    private static class PlayerData {
        public ItemStack[] inventory;
        public GameMode gameMode;
        public float xp;
        public int level;
        public boolean invulnerable;
        public double health;
        public int hunger;
        public float saturation;
        public boolean canFly;
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

            if(vehicleHasChest != itemHasChest) respawnBoat(itemHasChest, boatType);
            else vehicle.setBoatType(boatType);

            e.setCancelled(true);
        }

        private void respawnBoat(boolean hasChest, Boat.Type type) {
            //TODO enable entering and exiting of vehicles for player and kill and respawn the boat to the correct type.
        }
    }
}
