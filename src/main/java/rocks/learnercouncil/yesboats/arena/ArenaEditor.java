package rocks.learnercouncil.yesboats.arena;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ArenaEditor {

    public static HashMap<Player, ArenaEditor> editors = new HashMap<>();

    private final ItemStack[] playerInv;
    private final Player player;
    public final ArenaBuilder arenaBuilder;

    public ArenaEditor(Player player, ArenaBuilder arenaBuilder) {
        this.player = player;
        playerInv = player.getInventory().getContents();
        this.arenaBuilder = arenaBuilder;
        initializeInventory();

    }

    private void initializeInventory() {
        player.getInventory().clear();
        Inventory inv = player.getInventory();
    }

    public void restore() {
        player.getInventory().setContents(playerInv);
    }
}
