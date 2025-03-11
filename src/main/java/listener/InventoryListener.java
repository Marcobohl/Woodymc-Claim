package listener;

import gui.GUI;
import gui.GUIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        System.out.println("InventoryClickEvent ausgelöst!");

        GUI gui = GUIManager.getOpenGUI(player);
        if (gui != null) {
            System.out.println("GUI-Handler gefunden: " + gui.getClass().getSimpleName());
            gui.handleInventoryClick(event);
        } else {
            System.out.println("Kein GUI für den Spieler gefunden.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (GUIManager.changingGUI.contains(player)) {
            System.out.println("[Claim] Spieler wechselt gerade das GUI: " + player.getName());
            return;
        } else {
            GUIManager.closeGUI(player);
        }
    }
}
