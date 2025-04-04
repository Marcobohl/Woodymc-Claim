package gui;

import Builder.ItemBuilder;
import commands.GSCommand;
import main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import utils.PlotsquardUtils;
import utils.WorldGuardUtils;

import java.util.function.Supplier;

import static org.bukkit.Bukkit.getLogger;


public class GUIGrundstueckverwaltung implements GUI{

    private GSCommand gsCommand;
    private final GUIManager guiManager = GUIManager.getInstance();

    public GUIGrundstueckverwaltung(GSCommand gsCommand) {
        this.gsCommand = gsCommand;
    }

    public void openInventory(Player player) {

        boolean isOnGSAsOwner = false;
        // Überprüfen, ob der Spieler auf einem Grundstück steht
        String regionName = WorldGuardUtils.getRegionAtLocation(player.getLocation());
        if (regionName != null) {
            isOnGSAsOwner = WorldGuardUtils.isOwnerOfRegion(player, regionName);
        }

        boolean hasGs =  WorldGuardUtils.hasAnyRegionOwnershipOrMembership(player);
        boolean hasPlot = PlotsquardUtils.hasAnyPlotOwnershipOrMembership(player);

        // Erstelle das Inventar
        Inventory gui = Bukkit.createInventory(null, 27, "Grundstücksverwaltung");

        if ((hasPlot || hasGs) && !isOnGSAsOwner) {

            // Erstellen und Platzieren der Gegenstände mit Slot-Angabe
            new ItemBuilder(Material.GOLDEN_AXE)
                    .setDisplayName("Grundstück erstellen")
                    .setPersistentData("id","grundstuecke_create")
                    .setSlot(12)
                    .addToInventory(gui);

            new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName("Grundstück Liste")
                    .setPersistentData("id","grundstuecke_liste")
                    .setSlot(14)
                    .addToInventory(gui);

            // Situation 3: Spieler hat ein Grundstück und steht darauf als Besitzer
        } else if ((hasPlot || hasGs) && isOnGSAsOwner) {

            // Grdunstück löschen
            new ItemBuilder(Material.TNT)
                    .setDisplayName("Grundstück löschen")
                    .setPersistentData("id","grundstuecke_loeschen")
                    .setSlot(10)
                    .addToInventory(gui);

            // Grdunstück erstellen
            new ItemBuilder(Material.GOLDEN_AXE)
                    .setDisplayName("Grundstück erstellen")
                    .setPersistentData("id","grundstuecke_create")
                    .setSlot(12)
                    .addToInventory(gui);

            // Grundstücksliste
            new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName("Grundstück Liste")
                    .setPersistentData("id","grundstuecke_liste")
                    .setSlot(14)
                    .addToInventory(gui);

            // Einstellungen
            new ItemBuilder(Material.REDSTONE_TORCH)
                    .setDisplayName("Grundstück Einstellungen")
                    .setPersistentData("id","grundstuecke_settings")
                    .setSlot(16)
                    .addToInventory(gui);
        } else {

            // Überprüfen, ob der Spieler in einer erlaubten Welt ist
            if (!Main.getInstance().getAllowedWorlds().contains(player.getWorld().getName())) {
                // Erstellen und Platzieren der Gegenstände mit Slot-Angabe
                new ItemBuilder(Material.BARRIER)
                        .setDisplayName("Grundstück erstellen")
                        .setPersistentData("id","grundstuecke_create_error")
                        .setSlot(13)
                        .addToInventory(gui);
            } else {

                // Erstellen und Platzieren der Gegenstände mit Slot-Angabe
                new ItemBuilder(Material.GOLDEN_AXE)
                        .setDisplayName("Grundstück erstellen")
                        .setPersistentData("id","grundstuecke_create")
                        .setSlot(13)
                        .addToInventory(gui);

            }
        }

        // Fülle leere Slots mit einem schwarzen Glas-Panel
        ItemStack glassPane = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, "");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, glassPane);
            }
        }

        // Öffnet die GUI für den Spieler
        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        // Verhindert, dass Items verschoben werden
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String action = ItemBuilder.getPersistentData(clickedItem, "id");
        if (action == null) {
            // Muss nach der Entwicklung noch enfernt werden nur noch return....
            event.getWhoClicked().sendMessage("Keine Aktion für dieses Item definiert.");
            return;
        }

        switch (action) {
            case "grundstuecke_create":
                // Aktion für (Grundstück erstellen)
                player.closeInventory();
                gsCommand.activateGSMode(player);
                break;
            case "grundstuecke_settings":
                // Aktion für (Grundstückseinstellungen)
                break;
            case "grundstuecke_liste":

                // Logik zum Laden der nächsten Seite
                GUIGrundstueckliste gui = new GUIGrundstueckliste();
                guiManager.changeGUI(player, gui);

                // Aktion für (Grundstücksliste)
                break;
            case "grundstuecke_loeschen":
                // Aktion für (Grundstück löschen)
                break;
            case "grundstuecke_create_error":
                player.closeInventory();
                player.sendMessage("Diese Akton ist in dieser Welt nicht erlaubt.");
            default:
                // Anderes Verhalten
                break;
        }
    }
}
