package utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static javax.swing.plaf.synth.SynthLookAndFeel.getRegion;

public class WorldGuardUtils {

    // Zugriff auf WorldGuard-Instanz
    public static WorldGuardPlugin getWorldGuard() {
        return (WorldGuardPlugin) Bukkit.getPluginManager().getPlugin("WorldGuard");
    }

    // Gibt eine Liste von Regionen zurück, auf die der Spieler Zugriff hat
    public static List<String> getRegionsWithAccess(Player player) {
        List<String> regions = new ArrayList<>();

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (String worldName : Main.getInstance().getAllowedWorlds()) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) continue;

            for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
                ProtectedRegion region = entry.getValue();
                String regionName = entry.getKey();

                if (region.getOwners().contains(player.getUniqueId())) {
                    regions.add(regionName); // Optionale Weltkennung
                } else if (region.getMembers().contains(player.getUniqueId())) {
                    regions.add(regionName); // Optionale Weltkennung
                }
            }
        }

        return regions;
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

            region.setFlag(Flags.GREET_MESSAGE, "Du betrittst das Grundstück von: " + player.getName());
            region.setFlag(Flags.FAREWELL_MESSAGE, "Du verlässt das Grundstück von: " + player.getName());

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

    public static String getWorldOfRegion(Player player, String regionName) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager != null && manager.hasRegion(regionName)) {
                return world.getName();
            }
        }

        return "Unbekannt";
    }

    public static boolean isRegionOverlapping(Player player, Location point1, Location point2) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regions == null) {
            return false;
        }

        BlockVector3 min = BlockVector3.at(
                Math.min(point1.getBlockX(), point2.getBlockX()),
                player.getWorld().getMinHeight(),
                Math.min(point1.getBlockZ(), point2.getBlockZ())
        );
        BlockVector3 max = BlockVector3.at(
                Math.max(point1.getBlockX(), point2.getBlockX()),
                player.getWorld().getMaxHeight(),
                Math.max(point1.getBlockZ(), point2.getBlockZ())
        );

        // Erstelle eine temporäre Region
        ProtectedCuboidRegion tempRegion = new ProtectedCuboidRegion("tempRegion", min, max);

        // Prüfe auf Überschneidung mit bestehenden Regionen
        ApplicableRegionSet overlappingRegions = regions.getApplicableRegions(tempRegion);
        return overlappingRegions.size() > 0;
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

        UUID uuid = player.getUniqueId();
        String name = player.getName();

        return region.getOwners().contains(uuid) || region.getOwners().contains(name);
    }

    // Prüft, ob der Spieler in irgendeiner Region als Owner oder Member eingetragen ist
    public static boolean hasAnyRegionOwnershipOrMembership(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) {
            return false; // Keine Region in dieser Welt vorhanden
        }

        // Iteriere über alle Regionen in der Welt
        for (Map.Entry<String, ProtectedRegion> entry : regionManager.getRegions().entrySet()) {
            ProtectedRegion region = entry.getValue();

            // Überprüfe, ob der Spieler als Owner oder Member in der Region eingetragen ist
            if (region.getOwners().contains(player.getUniqueId()) || region.getMembers().contains(player.getUniqueId())) {
                return true; // Spieler ist entweder Owner oder Member einer Region
            }
        }
        return false; // Spieler ist weder Owner noch Member einer Region
    }
}

