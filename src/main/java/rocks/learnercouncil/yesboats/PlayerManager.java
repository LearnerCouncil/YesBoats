package rocks.learnercouncil.yesboats;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class PlayerManager {
    public static HashMap<Player, PlayerData> playerData = new HashMap<>();

    public static void set(Player p) {
        if(!playerData.containsKey(p)) playerData.put(p, new PlayerData());
        PlayerData playerData = PlayerManager.playerData.get(p);
        playerData.inventory = p.getInventory().getContents();
        p.getInventory().clear();
        playerData.gameMode = p.getGameMode();
        p.setGameMode(GameMode.ADVENTURE);
        playerData.xp = p.getExp();
        p.setExp(0f);
        playerData.level = p.getLevel();
        p.setLevel(0);
        p.setInvulnerable(true);
    }
    public static void restore(Player p) {
        PlayerData playerData = PlayerManager.playerData.get(p);
        p.getInventory().setContents(playerData.inventory);
        p.setGameMode(playerData.gameMode);
        p.setExp(playerData.xp);
        p.setLevel(playerData.level);
        p.setInvulnerable(false);
    }
    
    private static class PlayerData {
        public ItemStack[] inventory;
        public GameMode gameMode;
        public float xp;
        public int level;
    }
}
