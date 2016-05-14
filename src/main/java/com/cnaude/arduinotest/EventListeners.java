/*
 * Copyright (C) 2014 cnaude
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

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Chris Naude
 */
public class EventListeners implements Listener {

    private final ArduinoTest plugin;

    /**
     *
     * @param plugin the PurpleIRC plugin
     */
    public EventListeners(ArduinoTest plugin) {
        this.plugin = plugin;
    }

    /**
     *
     * @param event
     */
    @EventHandler
    public void onPlayerJoinEvent(final PlayerJoinEvent event) {
        plugin.logDebug("onPlayerJoinEvent: " + event.getPlayer().getName());

    }

    @EventHandler
    public void onBlockBreakEvent(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        String coord = plugin.getBlockCoord(block);
        for (int frameNum : plugin.frameMap.keySet()) {
            if (plugin.frameMap.get(frameNum).containsKey(coord)) {
                if (player != null) {
                    player.sendMessage(ChatColor.RED + "This is a protected block!");
                }
                event.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.isCancelled() && event.getItem() != null) {
            Player player = event.getPlayer();
            Block block = event.getClickedBlock();
            ItemStack itemStack = event.getItem();
            if (block == null) {
                return;
            }
            if (block.getState() instanceof Sign) {
                Sign s = (Sign) block.getState();
                player.sendMessage(Byte.toString(s.getRawData()));
            }
            Material blockType = block.getType();
            Action action = event.getAction();

            if (player.hasPermission("arduino.dyewool")) {
                if (action.equals(Action.RIGHT_CLICK_BLOCK)
                        && (blockType.equals(Material.WOOL)
                        || blockType.equals(Material.STAINED_GLASS)
                        || blockType.equals(Material.STAINED_GLASS_PANE))
                        && itemStack.getType().equals(Material.INK_SACK)) {

                    //DyeColor dyeColor = DyeColor.getByData((byte) (15 - itemStack.getDurability()));
                    DyeColor blockColor = DyeColor.getByData((byte) (itemStack.getDurability()));
                    block.setData(blockColor.getDyeData());

                    String coord = plugin.getBlockCoord(block);
                    for (int frameNum : plugin.frameMap.keySet()) {
                        if (plugin.frameMap.get(frameNum).containsKey(coord)) {
                            //plugin.frameMap.get(frameNum).get(coord).put("COLOR", plugin.getRgbCode(dyeColor));                            
                            //byte b = (byte) plugin.frameMap.get(frameNum).get(coord).get("COLOR");
                            
                            String color = String.format("%2X ", blockColor.getData());
                            plugin.frameMap.get(frameNum).get(coord).put("COLOR", color);
                            
                            int ledNum = (Integer) plugin.frameMap.get(frameNum).get(coord).get("LEDNUM");
                            
                            String led = String.format("%s:%d:%s", frameNum, ledNum, color);
                            if (plugin.debugMode()) {
                                player.sendMessage(led);
                            }
                            plugin.writeToFileAsync(led + "\n");
                            //plugin.frameRefresh();
                        }
                    }

                    if (player.hasPermission("arduino.dyewool.infinite")) {
                        return;
                    }

                    if (itemStack.getAmount() == 1) {
                        event.getPlayer().getInventory().remove(event.getItem());
                    } else {
                        itemStack.setAmount(itemStack.getAmount() - 1);
                    }

                }

            }
        }
    }

}
