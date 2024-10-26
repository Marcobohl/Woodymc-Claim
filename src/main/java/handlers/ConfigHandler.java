package handlers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigHandler {

    private final JavaPlugin plugin;
    private final FileConfiguration config;

    public ConfigHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); // Erstellt die Standardkonfigurationsdatei, falls sie nicht existiert
        this.config = plugin.getConfig(); // Lädt die Konfigurationsdaten
    }

    // Methode, die auf den Konfigurationswert zugreift und ihn basierend auf dem Typ zurückgibt
    public <T> T config(String key) {
        return (T) config.get(key);
    }

    // Nachladen der Konfiguration (bei z.B. "/reload")
    public void reloadConfig() {
        plugin.reloadConfig();
    }
}
