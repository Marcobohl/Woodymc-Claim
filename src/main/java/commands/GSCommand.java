package commands;

import com.google.gson.Gson;
import handlers.ConfigHandler;
import handlers.VaultHandler;
import listener.GSListener;
import main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import utils.InventoryUtils;
import utils.WorldGuardUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GSCommand implements CommandExecutor {

    private final Map<Player, Location> playerStartPositions = new HashMap<>();
    private final HashMap<Player, Location[]> selectedPoints = new HashMap<>();
    private final HashMap<Player, Boolean> gsMode = new HashMap<>();
    private final Map<Player, Integer> playerFoodLevels = new HashMap<>();
    private final File dataFolder;

    private final VaultHandler vaultHandler;
    private final Main plugin;
    private final Logger logger;
    private ConfigHandler configHandler;

    public GSCommand(Main plugin, VaultHandler vaultHandler, File dataFolder, Logger logger) {
        this.vaultHandler = vaultHandler;
        this.plugin = plugin;
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Überprüfen, ob der Spieler in einer erlaubten Welt ist
            if (!plugin.getAllowedWorlds().contains(player.getWorld().getName())) {
                player.sendMessage("Der /gs-Befehl ist in dieser Welt nicht erlaubt.");
                return true;
            }

            // Hauptbefehl /gs
            if (command.getName().equalsIgnoreCase("gs")) {
                // Wenn keine zusätzlichen Argumente angegeben sind, wechsle in den GS-Modus

                // Verhindern, dass geschützte Spieler den /gs-Befehl ausführen
                if (GSListener.hasTemporaryProtection(player)) {
                    player.sendMessage("Du kannst den /gs-Befehl während deines Schutzes nicht verwenden.");
                    return true;
                }


                if (args.length == 0) {
                    activateGSMode(player);
                    return true;
                }

                // Sub-Befehl /gs add <playername>
                if (args[0].equalsIgnoreCase("add") && args.length == 2) {
                    String targetPlayerName = args[1];
                    addPlayerToRegion(player, targetPlayerName);
                    return true;
                }

                // Sub-Befehl /gs remove <playername>
                if (args[0].equalsIgnoreCase("remove") && args.length == 2) {
                    String targetPlayerName = args[1];
                    removePlayerFromRegion(player, targetPlayerName);
                    return true;
                }
            }
        }

        return false;
    }

    // Spieler zur Region hinzufügen
    private void addPlayerToRegion(Player owner, String playerNameToAdd) {
        OfflinePlayer playerToAdd = Bukkit.getOfflinePlayer(playerNameToAdd);

        // Prüfen, ob der hinzugefügte Spieler existiert
        if (!playerToAdd.hasPlayedBefore()) {
            owner.sendMessage("Spieler " + playerNameToAdd + " wurde nicht gefunden.");
            return;
        }

        // Überprüfen, ob der Spieler auf einem Grundstück steht
        String regionName = WorldGuardUtils.getRegionAtLocation(owner.getLocation());
        if (regionName == null) {
            owner.sendMessage("Du stehst nicht auf einem Grundstück.");
            return;
        }

        // Überprüfen, ob der Spieler der Besitzer der Region ist
        if (WorldGuardUtils.isOwnerOfRegion(owner, regionName)) {
            // Prüfen, ob der hinzugefügte Spieler der Besitzer ist
            if (owner.getUniqueId().equals(playerToAdd.getUniqueId())) {
                owner.sendMessage("Du kannst dich nicht selbst als Mitglied hinzufügen, da du der Besitzer dieses Grundstücks bist.");
                return;
            }

            // Prüfen, ob der Spieler bereits Mitglied ist
            if (WorldGuardUtils.isMemberOfRegion(playerToAdd.getUniqueId(), regionName, owner.getWorld())) {
                owner.sendMessage(playerNameToAdd + " ist bereits Mitglied dieses Grundstücks.");
            } else {
                // Spieler zur Region hinzufügen
                WorldGuardUtils.addPlayerToRegion(owner, playerToAdd.getUniqueId(), regionName);
                owner.sendMessage(playerNameToAdd + " wurde zu deinem Grundstück hinzugefügt.");
            }
        } else {
            owner.sendMessage("Du bist nicht der Besitzer dieses Grundstücks.");
        }
    }

    // Grundstücksmodus aktivieren
    private void activateGSMode(Player player) {
        if (gsMode.containsKey(player)) {
            player.sendMessage("Du bist bereits im GS-Modus.");
            return;
        }

        saveInventoryToJson(player);

        playerStartPositions.put(player, player.getLocation());
        gsMode.put(player, true);
        playerFoodLevels.put(player, player.getFoodLevel());
        player.setFoodLevel(20);

        player.getInventory().clear();

        // Setze spezielle Items in die Hotbar
        player.getInventory().setItem(0, new ItemStack(Material.GOLDEN_AXE)); // Goldene Axt für Blockauswahl
        player.getInventory().setItem(4, new ItemStack(Material.LIME_DYE));   // Grüner Farbstoff für Bestätigung
        player.getInventory().setItem(8, new ItemStack(Material.RED_DYE));    // Roter Farbstoff für Abbruch
        clearChat(player);
        player.sendMessage("Du bist jetzt im Grundstücksmodus. Wähle zwei Ecken mit der Goldaxt aus.");
    }

    // Spieler von der Region entfernen
    private void removePlayerFromRegion(Player owner, String playerNameToRemove) {
        OfflinePlayer playerToRemove = Bukkit.getOfflinePlayer(playerNameToRemove);
        if (playerToRemove.hasPlayedBefore()) {
            String regionName = "gs_" + owner.getName();
            if (WorldGuardUtils.regionExists(owner, regionName)) {
                WorldGuardUtils.removePlayerFromRegion(owner, playerToRemove.getUniqueId(), regionName);
                owner.sendMessage(playerNameToRemove + " wurde von deinem Grundstück entfernt.");
            } else {
                owner.sendMessage("Du besitzt kein Grundstück mit dem Namen " + regionName + ".");
            }
        } else {
            owner.sendMessage("Spieler " + playerNameToRemove + " wurde nicht gefunden.");
        }
    }

    // Methode, um Ecken für das Grundstück zu setzen
    public void selectCorner(Player player, Location location, boolean firstCorner) {

        int minY = location.getWorld().getMinHeight();  // Tiefster Punkt (-64 in Minecraft 1.18+)
        int maxY = location.getWorld().getMaxHeight();

        Location[] points = selectedPoints.getOrDefault(player, new Location[2]);

        // Passe die Y-Koordinate automatisch an: erste Ecke minY, zweite Ecke maxY
        if (firstCorner) {
            // Setze die erste Ecke auf die unterste Höhe (minY)
            location.setY(minY);
            points[0] = location;
            player.sendMessage("Erste Ecke ausgewählt auf " + location.toVector().toString() + " (von Y=" + minY + " bis Y=" + maxY + ")");
        } else {
            // Setze die zweite Ecke auf die höchste Höhe (maxY)
            location.setY(maxY);
            points[1] = location;
            player.sendMessage("Zweite Ecke ausgewählt auf " + location.toVector().toString() + " (von Y=" + minY + " bis Y=" + maxY + ")");
        }

        selectedPoints.put(player, points);
    }

    public Location[] getSelectedPoints(Player player) {
        return selectedPoints.get(player);
    }

    // Methode zur Bearbeitung des Grundstückskaufs
    public void handleLimeDyeClick(Player player) {
        Location[] points = getSelectedPoints(player);

        // Prüfe, ob Punkte vorhanden sind
        if (points == null || points[0] == null || points[1] == null) {
            player.sendMessage("Du musst beide Ecken markieren, bevor du das Grundstück bestätigen kannst!");
            return;
        }
        if (points[0] != null && points[1] != null) {

            // Mindestgröße (z.B. 5x5)
            int minSize = 5;

            // Maximalgröße basierend auf der Spielerpermission
            int maxSize = getMaxPlotSize(player);


            int sizeX = Math.abs(points[0].getBlockX() - points[1].getBlockX()) + 1;
            int sizeZ = Math.abs(points[0].getBlockZ() - points[1].getBlockZ()) + 1;
            int area = sizeX * sizeZ;

            // Preis pro Block aus der Konfiguration
            double pricePerBlock = configHandler.config("buypricePerBlock");// Beispielwert, später aus der config.yml laden
            double totalCost = area * pricePerBlock;

            // Überprüfe, ob das Grundstück die Mindestgröße und Maximalgröße erfüllt
            if (sizeX < minSize || sizeZ < minSize) {
                player.sendMessage("Dein Grundstück muss mindestens " + minSize + "x" + minSize + " groß sein.");
                return;
            }

            if (sizeX  > maxSize || sizeZ > maxSize) {
                player.sendMessage("Dein Grundstück darf maximal " + maxSize + "x" + maxSize + " groß sein.");
                return;
            }

            player.sendMessage("Grundstücksgröße: " + area + " Blöcke. Kosten: " + totalCost + " Coins.");

            // Überprüfen, ob der Spieler genug Geld hat
            if (vaultHandler.hasEnoughMoney(player, totalCost)) {
                // Geld abbuchen
                if (vaultHandler.withdrawMoney(player, totalCost)) {
                    player.sendMessage("Grundstück erfolgreich gekauft! Größe: " + area + " Blöcke.");
                    // Region erstellen
                    String regionName = "gs_" + player.getName();
                    if (WorldGuardUtils.regionExists(player, regionName)) {
                        player.sendMessage("Du hast bereits ein Grundstück mit diesem Namen.");
                    } else {
                        if (WorldGuardUtils.createRegion(player, points[0], points[1], regionName)) {
                            player.sendMessage("Deine Region wurde erstellt!");
                            exitGsMode(player);
                        }
                    }
                }
            } else {
                player.sendMessage("Du hast nicht genug Geld, um dieses Grundstück zu kaufen.");
            }
        } else {
            player.sendMessage("Du musst beide Ecken auswählen, bevor du das Grundstück sichern kannst.");
        }
    }

    // Überprüfen, ob ein Spieler im Grundstücksmodus ist
    public boolean isInGsMode(Player player) {
        return gsMode.getOrDefault(player, false);
    }

    public void clearChat(Player player) {
        for (int i = 0; i < 100; i++) {  // Sendet 100 leere Nachrichten, um den Chat zu leeren
            player.sendMessage("");
        }
        player.sendMessage("Der Chat wurde geleert, du bist jetzt im Grundstücksmodus.");
    }

    public int getMaxPlotSize(Player player) {
        // Standardgröße, falls keine Permission gefunden wird
        int defaultMaxSize = 10;

        // Iteriere durch alle Permissions des Spielers
        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();

            // Prüfe, ob die Permission mit "claim.max." beginnt
            if (permission.startsWith("claim.max.")) {
                try {
                    // Extrahiere die Zahl hinter "claim.max."
                    String sizeStr = permission.substring("claim.max.".length());
                    int maxSize = Integer.parseInt(sizeStr);
                    return maxSize;  // Gib die gefundene Maximalgröße zurück
                } catch (NumberFormatException e) {
                    // Falls das, was nach "claim.max." kommt, keine gültige Zahl ist
                    player.sendMessage("Ungültige claim.max.-Permission: " + permission);
                }
            }
        }

        // Falls keine claim.max.-Permission gefunden wurde, verwende die Standardgröße
        return defaultMaxSize;
    }

    public boolean hasFirstCorner(Player player) {
        Location[] points = selectedPoints.get(player);

        // Prüfe, ob die erste Ecke (Index 0) nicht null ist
        return points != null && points[0] != null;
    }

    // Spieler aus dem GS-Modus entfernen und zurück zur Startposition setzen
    public void exitGsMode(Player player) {
        Location startPosition = playerStartPositions.get(player);
        if (startPosition != null) {
            player.teleport(startPosition); // Spieler zurücksetzen
            playerStartPositions.remove(player);
            gsMode.remove(player);
            selectedPoints.remove(player);

            // Setzt das Hungerniveau auf den ursprünglichen Wert zurück
            Integer originalFoodLevel = playerFoodLevels.get(player);
            if (originalFoodLevel != null) {
                player.setFoodLevel(originalFoodLevel);
                playerFoodLevels.remove(player);
            }

            // Aktiviert temporären Schutz für 30 Sekunden
            GSListener.activateTemporaryProtection(player);
            player.sendMessage("Du wurdest an deine Startposition zurückgesetzt. Schutz ist für 10 Sekunden aktiv.");
            restoreInventoryFromJson(player);
        }
    }

    // Speichert das Inventar als JSON-Datei im Ordner "data"
    public void saveInventoryToJson(Player player) {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        ItemStack[] inventoryContents = player.getInventory().getContents();
        String inventoryData = InventoryUtils.serializeInventory(inventoryContents);

        // Prüfen, ob die Serialisierung erfolgreich war
        if (inventoryData == null) {
            logger.warning("Fehler beim Serialisieren des Inventars für " + player.getName());
            return;
        }

        File file = new File(dataFolder, player.getUniqueId() + "_inventory.json");

        try (FileWriter writer = new FileWriter(file)) {
            new Gson().toJson(inventoryData, writer);
            player.sendMessage("Dein Inventar wurde gespeichert.");
        } catch (IOException e) {
            player.sendMessage("Fehler beim Speichern des Inventars.");
            e.printStackTrace();
        }
    }

    // Lädt das Inventar aus einer JSON-Datei im Ordner "data" und stellt es wieder her
    public void restoreInventoryFromJson(Player player) {
        File file = new File(dataFolder, player.getUniqueId() + "_inventory.json");
        if (!file.exists()) {
            return;
        }

        playerStartPositions.remove(player);
        gsMode.remove(player);
        selectedPoints.remove(player);

        try (FileReader reader = new FileReader(file)) {
            String inventoryData = new Gson().fromJson(reader, String.class);
            ItemStack[] inventoryContents = InventoryUtils.deserializeInventory(inventoryData, logger);
            player.getInventory().setContents(inventoryContents);
            player.sendMessage("Dein Inventar wurde wiederhergestellt.");
            file.delete(); // Datei löschen nach Wiederherstellung
        } catch (IOException e) {
            player.sendMessage("Fehler beim Wiederherstellen des Inventars.");
            e.printStackTrace();
        }
    }
}
