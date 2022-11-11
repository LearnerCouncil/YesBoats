package rocks.learnercouncil.yesboats;

import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.learnercouncil.yesboats.arena.ArenaEditor;
import rocks.learnercouncil.yesboats.commands.YesBoatsCmd;
import rocks.learnercouncil.yesboats.arena.Arena;
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

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        ConfigurationSerialization.registerClass(Arena.class);

        //initialize arena config
        arenaCfg = new ConfigFile(this, "arenas");
        List<?> arenas = arenaCfg.getConfig().getList("arenas", new ArrayList<>());
        if(!arenas.isEmpty() && arenas.get(0) instanceof Arena) {
            //noinspection unchecked
            Arena.arenas.addAll((List<Arena>) arenas);
        }

        //initialize regular config
        config = new ConfigFile(this, "config");
        Arena.queueTime = config.getConfig().getInt("queue-time");

        //noinspection ConstantConditions
        getCommand("yesboats").setExecutor(new YesBoatsCmd(this));

        registerEvents(
                new VehicleExit(),
                new ArenaEditor.Events()
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        arenaCfg.getConfig().set("arenas", Arena.arenas);
    }

    private void registerEvents(Listener... listeners) {
        for(Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
