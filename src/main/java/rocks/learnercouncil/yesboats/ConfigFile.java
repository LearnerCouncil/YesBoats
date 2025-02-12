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
        saveDefault();
    }

    public void reload() {
        if (file == null)
            file = new File(plugin.getDataFolder(), name);
        config = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource(name);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            config.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration get() {
        if (config == null)
            reload();
        return config;
    }

    public void save() {
        if (config == null || file == null)
            return;
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save file " + name, e);
        }
    }

    private void saveDefault() {
        if (file == null)
            file = new File(plugin.getDataFolder(), name);
        if (!file.exists())
            plugin.saveResource(name, false);
    }
}
