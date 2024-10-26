package main;

import commands.GSCommand;
import handlers.ConfigHandler;
import handlers.VaultHandler;
import listener.GSListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {

    private VaultHandler vaultHandler;
    private boolean vaultEnabled;
    private List<String> allowedWorlds;
    private ConfigHandler configHandler;

    @Override
    public void onEnable() {

        // Erstellt den Ordner für Daten, falls er nicht existiert
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Initialisiere Konfigurationshandler und lade Konfigurationsdatei
        configHandler = new ConfigHandler(this);
        configHandler.reloadConfig(); // Konfigurationswerte laden

        // Vault-Initialisierung
        setupVault();

        // Erlaubte Welten aus der Konfiguration laden
        allowedWorlds = configHandler.config("allowed-worlds");

        // Befehle und Listener registrieren
        registerCommands(dataFolder);
        registerListeners();

        getLogger().info("GSPlugin wurde erfolgreich geladen!");
    }

    private void setupVault() {
        vaultHandler = new VaultHandler();
        Plugin vaultPlugin = Bukkit.getServer().getPluginManager().getPlugin("Vault");

        if (vaultPlugin == null) {
            getLogger().warning("Vault was not found... You need it if you want to work with Permissions, Permission Groups or Money! Get it here: https://www.spigotmc.org/resources/vault.34315/");
            vaultEnabled = false;
        } else {
            getLogger().info("Vault plugin found.");
            vaultEnabled = true;
            vaultHandler.setupEconomy();
        }
    }

    private void registerCommands(File dataFolder) {
        GSCommand gsCommand = new GSCommand(this, vaultHandler, dataFolder, getLogger());
        getCommand("gs").setExecutor(gsCommand);
        getCommand("gs add").setExecutor(gsCommand);
        getCommand("gs remove").setExecutor(gsCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GSListener((GSCommand) getCommand("gs").getExecutor()), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // Getter für die erlaubten Welten
    public List<String> getAllowedWorlds() {
        return allowedWorlds;
    }
}
