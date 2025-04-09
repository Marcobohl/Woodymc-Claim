package utils;

import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import main.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlotsquardUtils {

    // Methode zum Abrufen des PlotAPI-Objekts nur bei Bedarf
    private static Optional<PlotAPI> getPlotAPI() {
        return Main.isPlotSquaredAvailable() ? Optional.of(new PlotAPI()) : Optional.empty();
    }

    // Prüft, ob der Spieler in PlotSquared als Owner oder Member eines Plots eingetragen ist
    public static boolean hasAnyPlotOwnershipOrMembership(Player player) {
        // Falls PlotSquared nicht verfügbar ist, immer "false" zurückgeben
        if (!Main.isPlotSquaredAvailable()) {
            return false;
        }

        // Hole die PlotAPI, falls verfügbar
        Optional<PlotAPI> plotAPI = getPlotAPI();
        if (plotAPI.isEmpty()) {
            return false;
        }

        PlotPlayer<?> plotPlayer = plotAPI.get().wrapPlayer(player.getUniqueId());

        if (plotPlayer == null) {
            return false; // Spieler ist nicht als PlotSquared-Player registriert
        }

        for (Plot plot : plotPlayer.getPlots()) {
            // Prüft, ob der Spieler als Owner oder Member in einem Plot eingetragen ist
            if (plot.isOwner(plotPlayer.getUUID()) || plot.getMembers().contains(plotPlayer.getUUID())) {
                return true; // Spieler ist entweder Owner oder Member eines Plots
            }
        }
        return false; // Spieler ist weder Owner noch Member eines Plots
    }



    /**
     * Gibt eine Liste aller Plots zurück, die einem Spieler gehören oder zu denen er Zugriff hat.
     *
     * @param player Der Spieler.
     * @return Eine Liste aller Plots, die dem Spieler gehören oder auf die er Zugriff hat.
     */
    public static List<Plot> getPlots(Player player) {

        if (!Main.isPlotSquaredAvailable()) {
            return new ArrayList<>(); // Leere Liste zurückgeben, wenn PlotSquared nicht verfügbar ist
        } else {

            List<Plot> plots = new ArrayList<>();
            UUID playerUUID = player.getUniqueId();

            // Hole die PlotAPI, falls verfügbar
            Optional<PlotAPI> plotAPI = getPlotAPI();
            PlotPlayer<?> plotPlayer = plotAPI.get().wrapPlayer(player.getUniqueId());

            if (plotPlayer != null) {
                plots.addAll(plotPlayer.getPlots());
            }

            return plots;

        }
    }

    /**
     * Gibt eine Liste aller Grundstücke zurück, die einem Spieler gehören.
     *
     * @param player Der Spieler, für den die Grundstücke überprüft werden.
     * @return Eine Liste von Grundstücken, die dem Spieler gehören.
     */
    public static List<Plot> getOwnedPlots(Player player) {

        if (!Main.isPlotSquaredAvailable()) {
            return new ArrayList<>(); // Leere Liste zurückgeben, wenn PlotSquared nicht verfügbar ist
        } else {

            List<Plot> ownedPlots = new ArrayList<>();
            UUID playerUUID = player.getUniqueId();

            // Hole die PlotAPI, falls verfügbar
            Optional<PlotAPI> plotAPI = getPlotAPI();
            PlotPlayer<?> plotPlayer = plotAPI.get().wrapPlayer(player.getUniqueId());

            if (plotPlayer != null) {
                for (Plot plot : plotPlayer.getPlots()) {
                    if (plot.isOwner(playerUUID)) {
                        ownedPlots.add(plot);
                    }
                }
            }

            return ownedPlots;

        }
    }



    public static void teleportToPlot(Player player, String plotId, String worldName) {
        if (!Main.isPlotSquaredAvailable()) {
            player.sendMessage("§cPlotSquared ist nicht verfügbar.");

            return;
        }


        String command = "p home " +  worldName + ";"+ plotId;
        player.performCommand(command);
    }

    public static Location convertToBukkitLocation(com.plotsquared.core.location.Location plotLocation) {
        // Welt abrufen
        World world = Bukkit.getWorld(plotLocation.getWorldName());
        if (world == null) {
            return null; // Rückgabe null, wenn die Welt nicht existiert
        }

        // Bukkit-Location erstellen und zurückgeben
        return new Location(
                world,
                plotLocation.getX(),
                plotLocation.getY(),
                plotLocation.getZ(),
                plotLocation.getYaw(),  // Optional, falls PlotSquared diese Werte bereitstellt
                plotLocation.getPitch() // Optional, falls PlotSquared diese Werte bereitstellt
        );
    }


    /**
     * Sucht ein Grundstück anhand seiner ID.
     *
     * @param plotId Die ID des Grundstücks.
     * @return Das entsprechende Grundstück oder Optional.empty, wenn es nicht gefunden wurde.
     */
    public static Optional<Plot> getPlotById(String plotId) {
        return getPlotAPI().flatMap(plotAPI -> {
            for (PlotArea area : plotAPI.getPlotAreas(null)) {
                for (Plot plot : area.getPlots()) {
                    if (plot.getId().toString().equalsIgnoreCase(plotId)) {
                        return Optional.of(plot);
                    }
                }
            }
            return Optional.empty();
        });
    }
}
