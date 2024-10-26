package utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WorldGuardUtils {

    // Zugriff auf WorldGuard-Instanz
    public static WorldGuardPlugin getWorldGuard() {
        return (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    // Erstellen einer neuen Region mit zwei Eckpunkten
    public static boolean createRegion(Player player, Location loc1, Location loc2, String regionName) {
        World world = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));// Verwendet BukkitAdapter zur Anpassung der Welt

        if (regions == null) {
            player.sendMessage("WorldGuard-Regionen konnten nicht geladen werden.");
            return false;
        }

        // Blockkoordinaten der zwei Ecken als BlockVector3 setzen
        BlockVector3 min = BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ());
        BlockVector3 max = BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());

        // Neue Region erstellen
        ProtectedRegion region = new ProtectedCuboidRegion(regionName, min, max);

        // Spieler als Besitzer der Region hinzufügen
        region.getOwners().addPlayer(player.getUniqueId());

        try {
            regions.addRegion(region); // Region zur Liste der Regionen hinzufügen
            player.sendMessage("Grundstück erfolgreich gesichert!");
            return true;
        } catch (Exception e) {
            player.sendMessage("Fehler beim Erstellen der Region.");
            e.printStackTrace();
            return false;
        }
    }

    // Überprüfen, ob eine Region bereits existiert
    public static boolean regionExists(Player player, String regionName) {
        World world = player.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world)); // Verwendet BukkitAdapter zur Anpassung der Welt

        if (regions == null) {
            return false;
        }

        // Überprüfen, ob die Region mit dem angegebenen Namen bereits existiert
        return regions.hasRegion(regionName);
    }

    // Fügt einen Spieler zur Region hinzu, falls er nicht bereits ein Mitglied ist
    public static void addPlayerToRegion(Player owner, UUID playerToAddUUID, String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(owner.getWorld()));

        if (regions != null) {
            ProtectedRegion region = regions.getRegion(regionName);
            if (region != null) {
                // Spieler zur Region hinzufügen
                region.getMembers().addPlayer(playerToAddUUID);
            }
        }
    }


    public static boolean isMemberOfRegion(UUID playerUUID, String regionName, World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));

        if (regions == null) return false;

        ProtectedRegion region = regions.getRegion(regionName);
        if (region == null) return false;

        return region.getMembers().contains(playerUUID);
    }



    // Spieler von der Region entfernen
    public static void removePlayerFromRegion(Player owner, UUID playerUUID, String regionName) {
        World world = owner.getWorld();
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));

        if (regions != null) {
            ProtectedRegion region = regions.getRegion(regionName);
            if (region != null) {
                region.getMembers().removePlayer(playerUUID); // Entferne den Spieler als Mitglied
            }
        }
    }

    // Gibt den Namen der Region an der aktuellen Position zurück (oder null, falls keine Region gefunden wurde)
    public static String getRegionAtLocation(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(location.getWorld()));

        if (regions == null) return null;

        ApplicableRegionSet regionSet = regions.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
        for (ProtectedRegion region : regionSet) {
            return region.getId();  // Rückgabe des ersten gefundenen Regionsnamens
        }

        return null;  // Keine Region gefunden
    }

    // Prüft, ob der Spieler der Besitzer der Region ist
    public static boolean isOwnerOfRegion(Player player, String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions == null) return false;

        ProtectedRegion region = regions.getRegion(regionName);
        if (region == null) return false;

        // Prüfen, ob der Spieler als Owner in der Region eingetragen ist
        return region.getOwners().contains(player.getUniqueId());
    }
}

