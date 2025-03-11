package gui;

import Builder.ItemBuilder;
import com.plotsquared.core.location.BlockLoc;
import com.plotsquared.core.plot.Plot;
import commands.GSCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import utils.PlotsquardUtils;
import utils.WorldGuardUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUIGrundstueckliste implements GUI{

    private GSCommand gsCommand;
    private final GUIManager guiManager = GUIManager.getInstance();

    public void openInventory(Player player) {
        // Erstelle das Inventar mit einer Größe von 54 Slots
        Inventory gui = Bukkit.createInventory(null, 54, "Grundstück Liste");

        // Grundstücke laden (Beispielcode, implementiere deine eigene Methode)
        List<Plot> plots = PlotsquardUtils.getPlots(player); // Diese Methode muss erstellt werden

        // Grundstücke (WorldGuard) laden
        List<String> regions = WorldGuardUtils.getRegionsWithAccess(player);
        int index = 0;

        // Definiere die Slots, in denen die Grundstücke angezeigt werden können
        int[] plotSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        // Grundstücke hinzufügen
        for (String region : regions) {
            if (index >= plotSlots.length) break;

            boolean isOwner = WorldGuardUtils.isOwnerOfRegion(player, region);
            Material blockType = Material.GRASS_BLOCK;

            // Benutze den ItemBuilder
            new ItemBuilder(Material.GRASS_BLOCK)
                    .setDisplayName("§aGrundstück: " + region)
                    .addLore(isOwner ? "§7Status: Owner" : "§7Status: Member")
                    .setPersistentData("regionId", region)
                    .setSlot(plotSlots[index])
                    .addToInventory(gui);

            index++;
        }

        // Plots hinzufügen
        for (Plot plot : plots) {
            if (index >= plotSlots.length) break;

            boolean isOwner = plot.isOwner(player.getUniqueId());

            new ItemBuilder(Material.GOLD_BLOCK)
                    .setDisplayName("§6Plot: " + plot.getId())
                    .addLore(isOwner ? "§7Status: Owner" : "§7Status: Trusted")
                    .setPersistentData("plotId", plot.getId().toString())
                    .setSlot(plotSlots[index])
                    .addToInventory(gui);

            index++;
        }

        // Definiere die Slots, in denen die Glasblöcke gesetzt werden sollen
        List<Integer> glaspane = new ArrayList<>(Arrays.asList(
                0, 1, 2, 3, 5, 6, 7, 8,    // Erste Reihe
                9, 17,                        // Linke und rechte Seite in der zweiten Reihe
                18, 26,                       // Linke und rechte Seite in der dritten Reihe
                27, 35,                       // Linke und rechte Seite in der vierten Reihe
                36, 44,                       // Linke und rechte Seite in der fünften Reihe
                45, 46, 47, 50, 51, 52 // Letzte Reihe außer Slot 53
        ));


        // Navigation: Falls mehr Grundstücke oder Plots als Slots vorhanden sind, füge einen "Nächste Seite"-Pfeil hinzu
        if (index < regions.size() + plots.size()) {
            new ItemBuilder(Material.ARROW)
                    .setDisplayName("§eNächste Seite")
                    .setPersistentData("action", "next_page")
                    .setSlot(53)
                    .addToInventory(gui);
        } else {
            glaspane.add(53);
        }

        new ItemBuilder(Material.PLAYER_HEAD)
                .setPlayerSkull(player.getName())
                .setDisplayName("Schnell Übersicht")
                .addLore("§7Plots in Besitz: " + PlotsquardUtils.getOwnedPlots(player).size(),
                        "§7Gesamtgrundstücke: " + PlotsquardUtils.getPlots(player).size())
                .setSlot(4)
                .addToInventory(gui);

        // Zurück- und Schließen-Buttons
        ItemStack closeItem = new ItemBuilder(Material.BARRIER)
                .setDisplayName("§cSchließen")
                .setPersistentData("action", "close")
                .build();
        gui.setItem(49, closeItem);

        ItemStack backItem = new ItemBuilder(Material.FEATHER)
                .setDisplayName("§aZurück")
                .setPersistentData("action", "back")
                .build();
        gui.setItem(48, backItem);

        // Fülle die restlichen Slots mit schwarzen Glas-Panels
        ItemStack glassPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName("").build();

        // Setze die Glas-Pane-Items in die Slots
        for (int slot : glaspane) {
            gui.setItem(slot, glassPane);
        }

        // Öffnet die GUI für den Spieler
        player.openInventory(gui);
    }

    public void handleInventoryClick(InventoryClickEvent event) {

        System.out.println("handleInventoryClick in GUIGrundstueckliste aufgerufen!");

        // Verhindert, dass Items verschoben werden
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        String action = ItemBuilder.getPersistentData(clickedItem, "action");
        if (action != null) {
            switch (action) {
                case "next_page":
                    // Logik zum Laden der nächsten Seite
                    GUIGrundstueckliste grundstueckListe = new GUIGrundstueckliste();
                    GUIManager.openGUI(player, grundstueckListe);// Hier müsstest du die aktuelle Seite tracken
                    break;
                case "back":
                    // Zurück zur vorherigen Ansicht
                    // Öffne das GUI basierend auf den Zuständen
                    break;
                case "close":
                    // Schließen der GUI
                    player.closeInventory();
                    break;
                default:
                    break;
            }
            return;
        }

        // Grundstück-Interaktion basierend auf PersistentData
        String plotId = ItemBuilder.getPersistentData(clickedItem, "plotId");
        if (plotId != null) {
            // Spieler zu diesem Grundstück teleportieren (Beispielcode)
            PlotsquardUtils.teleportToPlot(player, plotId);
        }
    }

}
