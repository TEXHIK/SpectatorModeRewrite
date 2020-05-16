/*
 * SpectatorModeRewrite
 *
 * Copyright (c) 2020. Oliver Howe
 *
 * MIT License
 */

package me.ohowe12.spectatormode.listener;

import me.ohowe12.spectatormode.SpectatorMode;
import me.ohowe12.spectatormode.State;
import me.ohowe12.spectatormode.commands.Spectator;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.Objects;

import static java.lang.Math.abs;


public class OnMoveListener implements Listener {
    private final SpectatorMode plugin = SpectatorMode.getInstance();
    private Map<String, State> state;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent e) {
        int yLevel = plugin.getConfig().getInt("y-level", 0);
        boolean enforceY = plugin.getConfig().getBoolean("enforce-y", false);
        boolean enforceDistance = plugin.getConfig().getBoolean("enforce-distance", false);
        boolean enforceNonTransparent = plugin.getConfig().getBoolean("disallow-non-transparent-blocks", false);
        boolean enforceAllBlocks = plugin.getConfig().getBoolean("disallow-all-blocks", false);

        Player player = e.getPlayer();
        Location location = e.getTo();
        state = Spectator.getInstance().state;

        if (!(state.containsKey(player.getUniqueId().toString()))) {
            return;
        }
        if (player.hasPermission("spectator-bypass")) {
            return;
        }
        if (!(player.getGameMode().equals(GameMode.SPECTATOR))) {
            return;
        }

        assert location != null;
        location = new Location(location.getWorld(), location.getX(), location.getY()+1, location.getZ());
        if (enforceY) {
            if (location.getY() <= yLevel) {
                e.setTo(e.getFrom());
                e.setCancelled(true);
                return;
            }
        }
        Block currentBlock = location.getBlock();
        if (cannotPass(currentBlock, enforceNonTransparent, enforceAllBlocks) || !circleCheck(location, enforceNonTransparent, enforceAllBlocks)) {
            e.setTo(e.getFrom());
            e.setCancelled(true);
            return;
        }

        if (enforceDistance) {
            if (checkDistance(player.getUniqueId().toString(), location)) {
                e.setTo(e.getFrom());
                e.setCancelled(true);
                return;
            }
        }
        if (!(Objects.requireNonNull(location.getWorld()).getWorldBorder().isInside(location))) {
            e.setTo(e.getFrom());
            e.setCancelled(true);
        }
    }

    private boolean cannotPass(Block currentBlock, boolean enforceNonTransparent, boolean enforceAllBlocks) {
        return enforceAllBlocks && !currentBlock.getType().isAir()
                || enforceNonTransparent && currentBlock.getType().isOccluding();
    }

    private boolean checkDistance(String player, Location location) {
        int distance = plugin.getConfig().getInt("distance", 64);
        Location originalLocation = state.get(player).getPlayerLocation();
        return (originalLocation.distance(location)) > distance;
    }


    private boolean circleCheck(Location location, boolean enforceNonTransparent, boolean enforceAllBlocks) {
        //vanilla space avaliable in 1-block shaft: (*.3,*.3)..(*.7,*.7)
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        int xSign = x >= 0 ? 1 : -1;
        int zSign = z >= 0 ? 1 : -1;
        x = abs(x);
        z = abs(z);
        double xFrac = x - (int) x;
        double yFrac = y - (int) y;
        double zFrac = z - (int) z;
        World world = location.getWorld();

        if (yFrac > 0.2) {//still passing. Whithout this if - completely broken
            if (cannotPass(new Location(world, x * xSign, y + 1, z * zSign).getBlock(), enforceNonTransparent, enforceAllBlocks)) {
                return false;
            }
        }
        if (xFrac < 0.3) {
            if (cannotPass(new Location(world, (x - 1) * xSign, y, z * zSign).getBlock(), enforceNonTransparent, enforceAllBlocks)) {
                return false;
            }
        } else if (xFrac > 0.7) {
            if (cannotPass(new Location(world, (x + 1) * xSign, y, z * zSign).getBlock(), enforceNonTransparent, enforceAllBlocks)) {
                return false;
            }
        }

        if (zFrac < 0.3) {
            if (cannotPass(new Location(world, x * xSign, y, (z - 1) * zSign).getBlock(), enforceNonTransparent, enforceAllBlocks)) {
                return false;
            }
        } else if (zFrac > 0.7) {
            if (cannotPass(new Location(world, x * xSign, y, (z + 1) * zSign).getBlock(), enforceNonTransparent, enforceAllBlocks)) {
                return false;
            }
        }

        return true;
    }
}
