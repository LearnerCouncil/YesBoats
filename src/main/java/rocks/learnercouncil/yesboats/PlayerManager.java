package rocks.learnercouncil.yesboats;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerManager {
    public static HashMap<Player, PlayerData> playerData = new HashMap<>();

    private static final List<ItemStack> items = new ArrayList<>();

    public static void set(Player player) {
        if(!playerData.containsKey(player)) playerData.put(player, new PlayerData());
        PlayerData playerData = PlayerManager.playerData.get(player);
        playerData.inventory = player.getInventory().getContents();
        player.getInventory().clear();
        initializeItems(player);
        playerData.gameMode = player.getGameMode();
        player.setGameMode(GameMode.ADVENTURE);
        playerData.xp = player.getExp();
        player.setExp(0f);
        playerData.level = player.getLevel();
        player.setLevel(0);
        playerData.invulnerable = player.isInvulnerable();
        player.setInvulnerable(true);
    }

    private static void initializeItems(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setItem(19, getItem(Material.OAK_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Oak",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to oak."));

        inventory.setItem(20, getItem(Material.SPRUCE_BOAT,
                ChatColor.BOLD.toString() + ChatColor.GREEN + "Spruce",
                ChatColor.DARK_GREEN + "Click to set your boat's wood type to Spruce."));

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
        PlayerData playerData = PlayerManager.playerData.get(p);
        p.getInventory().setContents(playerData.inventory);
        p.setGameMode(playerData.gameMode);
        p.setExp(playerData.xp);
        p.setLevel(playerData.level);
        p.setInvulnerable(playerData.invulnerable);
    }
    
    private static class PlayerData {
        public ItemStack[] inventory;
        public GameMode gameMode;
        public float xp;
        public int level;
        public boolean invulnerable;
    }

    public static class Events implements Listener {

        @EventHandler
        public void onInventoryClick(InventoryClickEvent e) {
            if(!items.contains(e.getCurrentItem())) return;
            if(!(e.getWhoClicked() instanceof Player)) return;
            Player player = (Player) e.getWhoClicked();
            if(!player.isInsideVehicle()) return;
            if(!(player.getVehicle() instanceof Boat)) return;
            Boat vehicle = (Boat) player.getVehicle();
            switch (Objects.requireNonNull(e.getCurrentItem()).getType()) {
                case OAK_BOAT:
                    vehicle.setWoodType(TreeSpecies.GENERIC);
                    break;
                case SPRUCE_BOAT:
                    vehicle.setWoodType(TreeSpecies.REDWOOD);
                    break;
                case BIRCH_BOAT:
                    vehicle.setWoodType(TreeSpecies.BIRCH);
                    break;
                case JUNGLE_BOAT:
                    vehicle.setWoodType(TreeSpecies.JUNGLE);
                    break;
                case ACACIA_BOAT:
                    vehicle.setWoodType(TreeSpecies.ACACIA);
                    break;
                case DARK_OAK_BOAT:
                    vehicle.setWoodType(TreeSpecies.DARK_OAK);
                    break;
                default:
                    return;
            }
            e.setCancelled(true);
        }
    }
}
