package gui;

import Builder.ItemBuilder;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import utils.PlotsquardUtils;
import utils.WorldGuardUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GUIGrundstueckliste implements GUI {


    private final int currentPage;
    private final GUIManager guiManager = GUIManager.getInstance();

    public GUIGrundstueckliste() {
        this.currentPage = 0;
    }

    public GUIGrundstueckliste(int page) {
        this.currentPage = page;
    }

    public void openInventory(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "Grundstück Liste");

        List<String> regions = WorldGuardUtils.getRegionsWithAccess(player);
        List<Plot> plots = PlotsquardUtils.getPlots(player);

        List<Object> entries = new ArrayList<>();
        entries.addAll(regions);
        entries.addAll(plots);

        int[] plotSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int itemsPerPage = plotSlots.length;
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        for (int i = start, slotIndex = 0; i < end && slotIndex < plotSlots.length; i++, slotIndex++) {
            Object entry = entries.get(i);

            if (entry instanceof String regionName) {
                String worldName = WorldGuardUtils.getWorldOfRegion(player, regionName);
                boolean isOwner = WorldGuardUtils.isOwnerOfRegion(player, regionName);

                new ItemBuilder(Material.GRASS_BLOCK)
                        .setDisplayName("§aGrundstück: " + regionName)
                        .addLore(
                                isOwner ? "§7Status: Owner" : "§7Status: Member",
                                "§7Welt: " + worldName
                        )
                        .setPersistentData("regionId", regionName)
                        .setPersistentData("plotWorld", worldName)
                        .setSlot(plotSlots[slotIndex])
                        .addToInventory(gui);

            } else if (entry instanceof Plot plot) {
                boolean isOwner = plot.isOwner(player.getUniqueId());
                String worldName = plot.getWorldName(); // ← Das geht mit PlotSquared

                new ItemBuilder(Material.GOLD_BLOCK)
                        .setDisplayName("§6Plot: " + plot.getId())
                        .addLore(
                                isOwner ? "§7Status: Owner" : "§7Status: Trusted",
                                "§7Welt: " + worldName
                        )
                        .setPersistentData("plotId", plot.getId().toString())
                        .setPersistentData("plotWorld", worldName)
                        .setSlot(plotSlots[slotIndex])
                        .addToInventory(gui);
            }
        }

        // Navigation
        if (end < entries.size()) {
            new ItemBuilder(Material.ARROW)
                    .setDisplayName("\u00a7eNächste Seite")
                    .setPersistentData("action", "next_page")
                    .setSlot(53)
                    .addToInventory(gui);
        } else {
            new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setSlot(53)
                    .addToInventory(gui);
        }
        if (currentPage > 0) {
            new ItemBuilder(Material.ARROW)
                    .setDisplayName("\u00a7eVorherige Seite")
                    .setPersistentData("action", "previous_page")
                    .setSlot(45)
                    .addToInventory(gui);
        } else {
            new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setSlot(45)
                    .addToInventory(gui);
        }

            // Glasränder
        List<Integer> glaspane = new ArrayList<>(Arrays.asList(
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52
        ));
        ItemStack glassPane = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName("").build();
        for (int slot : glaspane) gui.setItem(slot, glassPane);

        // Kopf
        new ItemBuilder(Material.PLAYER_HEAD)
                .setPlayerSkull(player.getName())
                .setDisplayName("Schnell Übersicht")
                .addLore("\u00a77Plots: " + PlotsquardUtils.getOwnedPlots(player).size(),
                        "\u00a77Grundstücke: " + regions.size())
                .setSlot(4)
                .addToInventory(gui);

        // Buttons
        new ItemBuilder(Material.BARRIER)
                .setDisplayName("\u00a7cSchließen")
                .setPersistentData("action", "close")
                .setSlot(49)
                .addToInventory(gui);

        new ItemBuilder(Material.FEATHER)
                .setDisplayName("\u00a7aZurück")
                .setPersistentData("action", "back")
                .setSlot(48)
                .addToInventory(gui);

        player.openInventory(gui);
    }

    public void handleInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        // Öffne das GUI basierend auf den Zuständen
        GUIGrundstueckverwaltung gui = new GUIGrundstueckverwaltung(this);


        String action = ItemBuilder.getPersistentData(clickedItem, "action");
        if (action != null) {
            switch (action) {
                case "next_page" -> guiManager.changeGUI(player, new GUIGrundstueckliste(currentPage + 1));
                case "previous_page" -> guiManager.changeGUI(player, new GUIGrundstueckliste(currentPage - 1));
                case "close" -> player.closeInventory();
                case "back" -> guiManager.changeGUI(player,  gui);
            }
            return;
        }

        String plotId = ItemBuilder.getPersistentData(clickedItem, "plotId");
        String worldName = ItemBuilder.getPersistentData(clickedItem, "plotWorld");
        if (plotId != null) {
            PlotsquardUtils.teleportToPlot(player, plotId, worldName);
        }
    }
}
