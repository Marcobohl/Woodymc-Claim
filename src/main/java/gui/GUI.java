package gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface GUI {
    void openInventory(Player player);

    void handleInventoryClick(InventoryClickEvent event);
}
