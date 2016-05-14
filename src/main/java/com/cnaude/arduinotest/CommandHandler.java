/*
 * Copyright (C) 2015 cnaude
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.cnaude.arduinotest;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author cnaude
 */
public class CommandHandler implements CommandExecutor {

    private final ArduinoTest plugin;

    /**
     *
     * @param plugin the plugin object
     */
    public CommandHandler(ArduinoTest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.logDebug("Command: " + label);
        if (args.length >= 1) {
            String subCommnd = args[0];
            plugin.logDebug("SubCommand: " + subCommnd);
            if (subCommnd.toLowerCase().startsWith("i")) {                
                if (sender.hasPermission("arduino.interval")) {
                    int interval;
                    try {
                        interval = Integer.parseInt(args[1]);                        
                        plugin.intervalChange(sender, interval);                        
                    } catch (Exception ex) {
                        sender.sendMessage(ChatColor.WHITE + "Usage: " + ChatColor.GOLD + "/ard interval [milliseconds]");
                        plugin.logDebug(ex.getMessage());
                        return true;
                    }
                }
            } else if (subCommnd.toLowerCase().startsWith("ref")) {
                if (sender.hasPermission("arduino.refresh")) {
                    plugin.frameRefreshAsync(sender);
                }
            } else if (subCommnd.toLowerCase().startsWith("rel")) {
                if (sender.hasPermission("arduino.reload")) {
                    sender.sendMessage(ChatColor.WHITE + "Reloading configuration...");
                    plugin.loadConfig();
                    sender.sendMessage(ChatColor.WHITE + "Done.");
                }
            } else if (subCommnd.toLowerCase().startsWith("c")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "You're not a player!");
                    return true;
                }

                Player player = (Player) sender;

                int frameNum;
                Material mat;
                mat = Material.getMaterial(plugin.getDefaultMat());

                try {
                    frameNum = Integer.parseInt(args[1]);
                } catch (Exception ex) {
                    sender.sendMessage(ChatColor.WHITE + "Usage: " + ChatColor.GOLD + "/ard [frame number] ([wool|glass])");
                    plugin.logDebug(ex.getMessage());
                    return true;
                }

                if (args.length >= 3) {
                    if (args[2].startsWith("w") || args[1].startsWith("W")) {
                        mat = Material.WOOL;
                    } else if (args[2].startsWith("g") || args[1].startsWith("G")) {
                        mat = Material.STAINED_GLASS;
                    } else {
                        sender.sendMessage(ChatColor.WHITE + "Block type should be wool or glass.");
                    }
                }

                buildFrame(player, frameNum, mat, true);

            }
        }
        return true;
    }

    public void buildFrame(final Player player, final int frameNum, final Material mat, final boolean createSign) {
        Location loc = player.getLocation();
        int x = loc.getBlockX() + 1;
        int y = loc.getBlockY();
        int z = loc.getBlockZ() - 1;
        Map<String, Map<String, Object>> tmpMap = new HashMap<>();
        int ledNum = 0;

        player.sendMessage(ChatColor.GOLD + "Creating 10x10 grid for frame "
                + frameNum + " at " + x + "," + y + "," + z);
        for (int xx = 1; xx <= 10; xx++) {
            for (int zz = 1; zz <= 10; zz++) {
                int newX = x + xx;
                int newZ = z - zz;
                if (newZ == z - 10 || newX == x + 10 || newZ == z - 1 || newX == x + 1) {
                    final Block block = player.getWorld().getBlockAt(newX, y, newZ);
                    block.setType(Material.GLOWSTONE);
                } else {
                    ledNum++;
                    Block block = player.getWorld().getBlockAt(newX, y, newZ);
                    block.setType(mat);
                    block.setData((byte) 15); // black for initial led off                    
                    Map<String, Object> led = new HashMap<>();
                    //led.put("COLOR", plugin.getRgbCode(DyeColor.BLACK));
                    led.put("COLOR", "0");
                    led.put("LEDNUM", ledNum);
                    tmpMap.put(plugin.getBlockCoord(block), led);
                }
            }
        }
        /*
        for (int xx = 1; xx <= 10; xx++) {
            for (int zz = 1; zz <= 10; zz++) {
                int newX = x + xx;
                int newZ = z + zz;
                if (newZ == z + 10 || newX == x + 10 || newZ == z + 1 || newX == x + 1) {
                    final Block block = player.getWorld().getBlockAt(newX, y, newZ);
                    block.setType(Material.GLOWSTONE);
                } else {
                    ledNum++;
                    Block block = player.getWorld().getBlockAt(newX, y, newZ);
                    block.setType(mat);
                    block.setData((byte) 15); // black for initial led off
                    Map<String, Object> led = new HashMap<>();
                    led.put("COLOR", plugin.getRgbCode(DyeColor.BLACK));
                    led.put("LEDNUM", ledNum);
                    tmpMap.put(plugin.getBlockCoord(block), led);
                }
            }
        }
         */
        plugin.frameMap.put(frameNum, tmpMap);

        if (createSign) {
            // Let's make a nice sign indicating the frame number
            final Block block = player.getWorld().getBlockAt(x, y, z - 10);
            block.setType(Material.SIGN_POST);
            Sign sign = (Sign) block.getState();
            sign.setRawData((byte) 6);
            sign.setLine(0, "+=============+");
            sign.setLine(1, "  Frame #");
            sign.setLine(2, "  " + frameNum);
            sign.setLine(3, "+=============+");
            sign.update();

        }
    }

}
