package listener;

import commands.GSCommand;
import handlers.ConfigHandler;
import main.Main;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.plugin.java.JavaPlugin.getPlugin;

public class GSListener implements Listener {

    private final GSCommand gsCommand;
    private static final Map<UUID, Long> temporaryProtection = new HashMap<>();
    private ConfigHandler configHandler;

    public GSListener(GSCommand gsCommand, ConfigHandler configHandler) {
        this.gsCommand = gsCommand;
        this.configHandler = configHandler;
    }



    // Aktiviert temporären Schutz für 30 Sekunden
    public static void activateTemporaryProtection(Player player, ConfigHandler configHandler) {

        int protectionTimeSeconds = configHandler.config("savetime");
        long protectionTimeTicks = protectionTimeSeconds * 20L;

        temporaryProtection.put(player.getUniqueId(), System.currentTimeMillis() + (protectionTimeSeconds * 1_000));

        new BukkitRunnable() {
            @Override
            public void run() {
                temporaryProtection.remove(player.getUniqueId()); // Entfernt den Schutz nach 30 Sekunden
                player.sendMessage("Dein temporärer Schutz ist abgelaufen.");
            }
        }.runTaskLater(getPlugin(Main.class), protectionTimeTicks); // 600 Ticks = 30 Sekunden
    }

    // Prüft, ob ein Spieler temporären Schutz hat
    public static boolean hasTemporaryProtection(Player player) {
        return temporaryProtection.containsKey(player.getUniqueId()) &&
                temporaryProtection.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    // Verhindert Hungerverlust im GS-Modus
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {

        if (event.getEntity() instanceof Player player && gsCommand.isInGsMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (gsCommand.isInNameMode(player)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        gsCommand.restoreInventoryFromJson(player); // Prüfen und ggf. Inventar wiederherstellen
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Überprüfen, ob der Spieler im GS-Modus ist
        if (gsCommand.isInGsMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Überprüfen, ob der Spieler im GS-Modus ist
        if (gsCommand.isInGsMode(player)) {
            event.setCancelled(true);
        }
    }


    // Blockiert das Senden von Nachrichten im GS-Modus
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        // Prüfe, ob der Spieler im GS-Modus ist
        if (gsCommand.isInGsMode(sender)) {

            // Prüfen, ob der Spieler sich im Namenswahlmodus befindet
            if (gsCommand.isInNameMode(sender) && !gsCommand.isInConfirmeMode(sender)) {
                event.setCancelled(true); // Blockiere die Chatnachricht
                String message = event.getMessage();

                    // Speichere den Grundstücksnamen und weise an, auf Bestätigung zu warten
                    gsCommand.setPropertyName(sender, message); // Setze den Namen
                    sender.sendTitle("Grundstücksnamen festlegen", "Aktueller Name: " + message, 10, 999999, 10);
                    sender.sendMessage("Klicke erneut auf den grünen Farbstoff, um den Namen zu bestätigen.");
            }

            // Blockiere das Senden der Nachricht im GS-Modus
            event.setCancelled(true);
        }
        // Entferne Empfänger im GS-Modus
        event.getRecipients().removeIf(receiver -> gsCommand.isInGsMode(receiver));
    }

    // Blockiert das Aufheben von Items im GS-Modus
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (gsCommand.isInGsMode(player)) {
            event.setCancelled(true);
        }
    }

    // Blockiert das Setzen von Blöcken im GS-Modus
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (gsCommand.isInGsMode(player)) {
            event.setCancelled(true);
        }
    }

    // Blockiert das Abbauen von Blöcken im GS-Modus
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (gsCommand.isInGsMode(player)) {
            event.setCancelled(true);
        }
    }

    // Verhindert Schaden an Spielern im GS-Modus
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (gsCommand.isInGsMode(player) || hasTemporaryProtection(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Verhindert, dass Monster Spieler im GS-Modus angreifen
    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        Entity target = event.getTarget();
        if (target instanceof Player) {
            Player player = (Player) target;
            if (gsCommand.isInGsMode(player) || hasTemporaryProtection(player)) {
                event.setCancelled(true);
            }
        }
    }


    // Blockiert das Droppen von Items im GS-Modus
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (gsCommand.isInGsMode(player)) {
            // Spieler ist im GS-Modus
            event.setCancelled(true);
        }
    }

    // Verhindert, dass Spieler im GS-Modus Mobs oder Spieler angreifen
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (gsCommand.isInGsMode(player) || hasTemporaryProtection(player)) {
                event.setCancelled(true);
            }
        }
    }

    // Blockiert das Klicken im Inventar im GS-Modus
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (gsCommand.isInGsMode(player)) {
            // Spieler ist im GS-Modus
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Main plugin = (Main) getPlugin(Main.class);

        // Überprüfen, ob der Spieler in einer erlaubten Welt ist
        if (!plugin.getAllowedWorlds().contains(player.getWorld().getName())) {
            return; // Nichts tun, wenn die Welt nicht erlaubt ist
        }


        // Wenn der Spieler im GS-Modus ist (er hat mindestens eine Ecke ausgewählt)
        if (gsCommand.isInGsMode(player)) {
            if (item != null && item.getType() == Material.GOLDEN_AXE) {
                // Spieler verwendet die Goldaxt für die Grundstücksauswahl
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    // Erste Ecke setzen
                    gsCommand.selectCorner(player, event.getClickedBlock().getLocation(), true);
                } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    // Prüfen, ob die erste Ecke bereits gesetzt wurde
                    if (gsCommand.hasFirstCorner(player)) {
                        gsCommand.selectCorner(player, event.getClickedBlock().getLocation(), false);  // Zweite Ecke setzen
                    } else {
                        player.sendMessage("Setze zuerst die erste Ecke, bevor du die zweite Ecke setzen kannst!");
                    }
                }
                event.setCancelled(true); // Verhindere Standardaktionen
            } else if (item != null && item.getType() == Material.LIME_DYE) {

                    // Spieler verwendet den grünen Farbstoff zur Bestätigung
                    gsCommand.handleLimeDyeClick(player);
                    event.setCancelled(true); // Verhindere Standardaktionen

            } else if (item != null && item.getType() == Material.RED_DYE) {
                // Spieler verwendet den roten Farbstoff zum Abbrechen
                gsCommand.exitGsMode(player); // Inventar wiederherstellen
                player.sendMessage("Grundstücksauswahl abgebrochen.");
                event.setCancelled(true); // Verhindere Standardaktionen
                event.setUseItemInHand(Event.Result.DENY);
            } else {
                event.setCancelled(true);
                event.setUseItemInHand(Event.Result.DENY);
            }
        }
    }

}

