package rocks.learnercouncil.yesboats;

import lombok.Getter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.learnercouncil.yesboats.arena.Arena;
import rocks.learnercouncil.yesboats.arena.ArenaEditor;
import rocks.learnercouncil.yesboats.arena.ArenaSign;
import rocks.learnercouncil.yesboats.commands.YesBoatsCmd;

import java.util.ArrayList;
import java.util.List;

public final class YesBoats extends JavaPlugin {

    private static @Getter YesBoats plugin;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        ConfigurationSerialization.registerClass(Arena.class);

        // initialize arena config
        Configs.arena = new ConfigFile(this, "arenas.yml");
        List<?> arenas = Configs.arena.get().getList("arenas", new ArrayList<>());
        if (!arenas.isEmpty() && arenas.get(0) instanceof Arena) {
            //noinspection unchecked
            Arena.arenas.addAll((List<Arena>) arenas);
        }

        Configs.messages = new ConfigFile(this, "messages.yml");
        Messages.initialize(Configs.messages);

        // initialize regular config
        Configs.main = new ConfigFile(this, "config.yml");
        Arena.queueTime = Configs.main.get().getInt("queue-time");

        TabExecutor yb = new YesBoatsCmd();
        getCommand("yesboats").setExecutor(yb);
        getCommand("yesboats").setTabCompleter(yb);


        registerEvents(new Arena.Events(),
                new ArenaEditor.Events(),
                new ArenaSign.Events(),
                new InventoryManager.Events()
        );
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        new ArrayList<>(ArenaEditor.editors.values()).forEach(a -> a.restore(false));
        Arena.arenas.forEach(a -> {
            if (a.getState() != Arena.State.WAITING) return;
            a.stopGame();
        });

        Configs.arena.get().set("arenas", Arena.arenas);
        Configs.arena.save();
    }

    private void registerEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public static class Configs {
        public static ConfigFile messages;
        public static ConfigFile arena;
        public static ConfigFile main;
    }

    public static class Permissions {
        public static String ADMIN_MESSAGES = "yesboats.admin";
        public static String CREATE_SIGN = "yesboats.joinsign";
        public static String USER_COMMANDS = "yesboats.commands.yesboats.user";
        public static String ADMIN_COMMANDS = "yesboats.commands.yesboats.admin";
    }
}
