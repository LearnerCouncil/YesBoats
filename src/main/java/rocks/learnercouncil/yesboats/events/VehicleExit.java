package rocks.learnercouncil.yesboats.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;
import rocks.learnercouncil.yesboats.arena.Arena;

public class VehicleExit implements Listener {

    @EventHandler
    public void onVehicleExit(VehicleExitEvent e) {
        if(!(e.getExited() instanceof Player)) return;
        Player player = (Player) e.getExited();
        if(!Arena.get(player).isPresent()) return;
        e.setCancelled(true);
    }
}
