package handlers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import static org.bukkit.Bukkit.getLogger;

public class VaultHandler {

    public static Economy economy = null;

    // Setup Vault Economy
    public boolean setupEconomy() {

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault plugin not found! Please install Vault.");
            return false;
        }

        RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (economyProvider == null) {
            getLogger().severe("No Economy Plugin was found... You need one if you want to work with Money! Get it there: http://plugins.bukkit.org/");
            return false;
        }

        economy = economyProvider.getProvider();
        return economy != null;

    }

    // Überprüfen, ob der Spieler genug Geld hat
    public boolean hasEnoughMoney(Player player, double amount) {
        return economy.has(player, amount);
    }

    // Geld vom Konto des Spielers abbuchen
    public boolean withdrawMoney(Player player, double amount) {
        if (economy.has(player, amount)) {
            economy.withdrawPlayer(player, amount);
            player.sendMessage("Dir wurden " + amount + " Coins abgezogen.");
            return true;
        } else {
            player.sendMessage("Du hast nicht genug Geld für den Kauf.");
            return false;
        }
    }

    // Kontostand des Spielers abrufen
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public Economy getEconomy() {
        return economy;
    }
}
