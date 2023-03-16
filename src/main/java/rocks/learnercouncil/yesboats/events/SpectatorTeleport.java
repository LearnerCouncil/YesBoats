package rocks.learnercouncil.yesboats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import rocks.learnercouncil.yesboats.arena.Arena;

import java.util.Optional;

public class SpectatorTeleport implements Listener {

    @EventHandler
    public void onSpectatorTeleport(PlayerTeleportEvent event) {
        if(event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if(event.getPlayer().hasPermission("yesboats.spectatortp")) return;
        Optional<Arena> arenaOptional = Arena.get(event.getPlayer());
        if(!arenaOptional.isPresent()) return;
        Optional<Player> targetOptional = arenaOptional.get().getPlayers().stream().filter(p -> p.getLocation().equals(event.getTo())).findAny();
        if(!targetOptional.isPresent()) {
            event.setCancelled(true);
            return;
        }
        Player target = targetOptional.get();
        Optional<Arena> targetArenaOptional = Arena.get(target);
        if (!targetArenaOptional.isPresent() || !targetArenaOptional.get().equals(arenaOptional.get()))
            event.setCancelled(true);


    }

}
