package gui;

import main.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GUIManager {
    private static final GUIManager instance = new GUIManager();
    private static final Map<Player, GUI> openGUIs = new HashMap<>();
    public static final Set<Player> changingGUI = new HashSet<>();

    // Private Konstruktor, damit nur eine Instanz existiert
    private GUIManager() {}

    public static GUIManager getInstance() {
        return instance;
    }

    public static void openGUI(Player player, GUI gui) {
        openGUIs.put(player, gui);
        System.out.println("Geöffnete GUI: " + gui.getClass().getSimpleName());
        gui.openInventory(player); // Jede GUI-Klasse hat eine eigene openInventory-Methode
    }

    public static void changeGUI(Player player, GUI newGui) {
        changingGUI.add(player); // Markiere, dass ein Wechsel stattfindet

        openGUIs.put(player, newGui);
        System.out.println("Wechsle GUI zu: " + newGui.getClass().getSimpleName());

        newGui.openInventory(player);

        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> changingGUI.remove(player), 1L); // Entferne nach 1 Tick
    }

    public static void closeGUI(Player player) {
        openGUIs.remove(player);
    }

    public static GUI getOpenGUI(Player player) {
        return openGUIs.get(player);
    }
}
