package rocks.learnercouncil.yesboats;

import org.bukkit.plugin.java.JavaPlugin;
import rocks.learnercouncil.yesboats.commands.YesBoatsCmd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class YesBoats extends JavaPlugin {

    public static ConfigFile arenaCfg;
    public static ConfigFile config;

    public static YesBoats instance;
    public static YesBoats getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        arenaCfg.getConfig().set("arenas", Arena.arenas);
    }
}
