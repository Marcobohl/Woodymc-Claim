package main;

import commands.GSCommand;
import handlers.ConfigHandler;
import handlers.VaultHandler;
import listener.GSListener;
import listener.InventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class Main extends JavaPlugin {

    private VaultHandler vaultHandler;
    private boolean vaultEnabled;
    private List<String> allowedWorlds;
    private ConfigHandler configHandler;
    private static boolean isPlotSquaredAvailable;
    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;

        // Erstellt den Ordner für Daten, falls er nicht existiert
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        // Prüfen, ob PlotSquared auf dem Server installiert ist
        Plugin plotSquared = Bukkit.getPluginManager().getPlugin("PlotSquared");
        isPlotSquaredAvailable = plotSquared != null && plotSquared.isEnabled();

        if (!isPlotSquaredAvailable) {
            getLogger().info("PlotSquared nicht gefunden. Einige Funktionen sind deaktiviert.");
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

    public static Main getInstance() {
        return instance;
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
        GSCommand gsCommand = new GSCommand(this, vaultHandler, dataFolder, getLogger(), configHandler);
        getCommand("gs").setExecutor(gsCommand);
        getCommand("gs add").setExecutor(gsCommand);
        getCommand("gs remove").setExecutor(gsCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new GSListener((GSCommand) getCommand("gs").getExecutor(), configHandler), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(), this);
    }

    public static boolean isPlotSquaredAvailable() {
        return isPlotSquaredAvailable;
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
