package rocks.learnercouncil.yesboats;

import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaEditor;
import rocks.learnercouncil.yesboats.arena.ArenaSign;
import rocks.learnercouncil.yesboats.commands.YesBoatsCmd;
import rocks.learnercouncil.yesboats.events.SpectatorTeleport;
import rocks.learnercouncil.yesboats.events.VehicleExit;

import java.util.ArrayList;
import java.util.List;

public final class YesBoats extends JavaPlugin {

    public static ConfigFile arenaCfg;
    public static ConfigFile config;

    private static YesBoats instance;

    /**
     * @return An instance of the main class
     */
    public static YesBoats getInstance() {
        return instance;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        ConfigurationSerialization.registerClass(Arena.class);

        //initialize arena config
        arenaCfg = new ConfigFile(this, "arenas.yml");
        List<?> arenas = arenaCfg.getConfig().getList("arenas", new ArrayList<>());
        if(!arenas.isEmpty() && arenas.get(0) instanceof Arena) {
            //noinspection unchecked
            Arena.arenas.addAll((List<Arena>) arenas);
        }

        //initialize regular config
        config = new ConfigFile(this, "config.yml");
        Arena.queueTime = config.getConfig().getInt("queue-time");

        TabExecutor yb = new YesBoatsCmd();
        getCommand("yesboats").setExecutor(yb);
        getCommand("yesboats").setTabCompleter(yb);


        registerEvents(
                new VehicleExit(),
                new SpectatorTeleport(),
                new ArenaEditor.Events(),
                new ArenaSign.Events(),
                new PlayerManager.Events()
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        ArenaEditor.editors.values().forEach(a -> a.restore(false));
        
        arenaCfg.getConfig().set("arenas", Arena.arenas);
        arenaCfg.saveConfig();
    }

    private void registerEvents(Listener... listeners) {
        for(Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

}
