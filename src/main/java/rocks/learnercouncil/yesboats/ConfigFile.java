package rocks.learnercouncil.yesboats;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigFile {

    private final YesBoats plugin;
    private final String name;
    private FileConfiguration config;
    private File file;


    public ConfigFile(YesBoats plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if(file == null)
            file = new File(plugin.getDataFolder(), name);
        config = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStram = plugin.getResource(name);
        if(defaultStram != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStram));
            config.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getConfig() {
        if(config == null)
            reloadConfig();
        return config;
    }

    public void saveConfig() {
        if(config == null || file == null)
            return;
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save file " + name, e);
        }
    }

    private void saveDefaultConfig() {
        if(file == null)
            file = new File(plugin.getDataFolder(), name);
        if(!file.exists())
            plugin.saveResource(name, false);
    }
}
