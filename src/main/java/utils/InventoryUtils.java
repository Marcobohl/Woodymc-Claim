package utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.StringReader;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class InventoryUtils {

    // Konvertiert ein ItemStack[] in einen Base64-String
    public static String serializeInventory(ItemStack[] items) {
        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.set("inventory", items);
        String yamlString = yamlConfig.saveToString(); // YAML-Daten in einen String speichern
        return Base64.getEncoder().encodeToString(yamlString.getBytes());  // In Base64 konvertieren
    }

    // Konvertiert einen Base64-String zurück in ein ItemStack[]
    public static ItemStack[] deserializeInventory(String data, Logger logger) {
        if (data == null || data.isEmpty()) return new ItemStack[0];

        try {
            String yamlString = new String(Base64.getDecoder().decode(data));  // Base64-Daten dekodieren
            YamlConfiguration yamlConfig = new YamlConfiguration();
            yamlConfig.load(new StringReader(yamlString));  // YAML-Daten laden
            List<ItemStack> itemList = (List<ItemStack>) yamlConfig.get("inventory");
            return itemList.toArray(new ItemStack[0]);
        } catch (Exception e) {
            logger.severe("Fehler beim Deserialisieren des Inventars: " + e.getMessage());
            e.printStackTrace();
            return new ItemStack[0];
        }
    }
}
