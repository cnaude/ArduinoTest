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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Chris Naudé
 */
public class ArduinoTest extends JavaPlugin {

    public String LOG_HEADER;
    public String LOG_HEADER_F;
    static final Logger LOG = Logger.getLogger("Minecraft");
    private File pluginFolder;
    private File configFile;
    private boolean debugEnabled;
    private String outputFilename;
    private String defaultMat;
    private long refreshRate;
    private long interval;
    private Lock lock;

    protected Map<Integer, Map<String, Map<String, Object>>> frameMap;

    public ArduinoTest() {

    }

    /**
     * Very first method that gets called when starting the plugin.
     */
    @Override
    public void onEnable() {
        LOG_HEADER = "[" + this.getName() + "]";
        LOG_HEADER_F = ChatColor.LIGHT_PURPLE + "[" + this.getName() + "]" + ChatColor.RESET;
        pluginFolder = getDataFolder();
        configFile = new File(pluginFolder, "config.yml");

        lock = new ReentrantLock();
        
        createConfigDirs();
        createConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadConfig();        

        getServer().getPluginManager().registerEvents(new EventListeners(this), this);
        getCommand("ard").setExecutor(new CommandHandler(this));
        
        //frameRefreshAsync();        

    }

    /**
     * Called when plugin is told to stop.
     */
    @Override
    public void onDisable() {
        saveFrames();
    }

    /**
     * Return the current debug mode status
     *
     * @return
     */
    public boolean debugMode() {
        return debugEnabled;
    }

    protected void loadConfig() {
        frameMap = new HashMap<>();
        try {
            getConfig().load(configFile);
        } catch (IOException | InvalidConfigurationException ex) {
            logError(ex.getMessage());
        }
        debugEnabled = getConfig().getBoolean("debug", false);
        outputFilename = getConfig().getString("output-file", "ard.out");
        defaultMat = getConfig().getString("block-type", "WOOL");
        refreshRate = getConfig().getLong("refresh-rate", 50);
        interval = getConfig().getLong("interval", 50);
        try {
            for (String frameNumStr : getConfig().getConfigurationSection("frames").getKeys(false)) {
                logDebug("Loading frame: " + frameNumStr);
                if (!frameNumStr.startsWith("MemorySection")) {
                    int frameNum;
                    try {
                        frameNum = Integer.parseInt(frameNumStr);
                    } catch (Exception ex) {
                        logError(ex.getMessage());
                        break;
                    }
                    Map<String, Map<String, Object>> ledMap = new HashMap<>();
                    for (String led : getConfig().getConfigurationSection("frames." + frameNum).getKeys(false)) {
                        Map<String, Object> ledMap2 = new HashMap<>();
                        logDebug("Loading led: " + led);
                        if (!led.startsWith("MemorySection")) {
                            String color = getConfig().getString("frames." + frameNumStr + "." + led + ".color");
                            int lednum = getConfig().getInt("frames." + frameNumStr + "." + led + ".lednum");
                            logDebug(" color ->: " + color);
                            logDebug(" lednum ->: " + lednum);
                            ledMap2.put("COLOR", color);
                            ledMap2.put("LEDNUM", lednum);
                            ledMap.put(led, ledMap2);
                        }
                    }
                    frameMap.put(frameNum, ledMap);
                }
            }
        } catch (Exception ex) {
            logDebug(ex.getMessage());
        }        
    }

    /**
     *
     * @param sender
     */
    public void reloadMainConfig(CommandSender sender) {
        sender.sendMessage(LOG_HEADER_F + " Reloading config.yml ...");
        reloadConfig();
        getConfig().options().copyDefaults(false);
        loadConfig();
        sender.sendMessage(LOG_HEADER_F + " Done.");
    }

    private void createConfigDirs() {
        if (!pluginFolder.exists()) {
            try {
                logInfo("Creating " + pluginFolder.getAbsolutePath());
                pluginFolder.mkdir();
            } catch (Exception e) {
                logError(e.getMessage());
            }
        }

    }

    private void createConfig() {
        if (!configFile.exists()) {
            try {
                logInfo("Creating config.yml");
                configFile.createNewFile();
            } catch (IOException e) {
                logError(e.getMessage());
            }
        }

    }

    /**
     * Informational logger
     *
     * @param message
     */
    public void logInfo(String message) {
        LOG.log(Level.INFO, String.format("%s %s", LOG_HEADER, message));
    }

    /**
     * Error logger
     *
     * @param message
     */
    public void logError(String message) {
        LOG.log(Level.SEVERE, String.format("%s %s", LOG_HEADER, message));
    }

    /**
     * Debug logger
     *
     * @param message
     */
    public void logDebug(String message) {
        if (debugEnabled) {
            LOG.log(Level.INFO, String.format("%s [DEBUG] %s", LOG_HEADER, message));
        }
    }

    protected void frameRefreshAsync() {
        getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                frameRefresh();
            }
        }, 0);
    }

    protected void frameRefreshAsync(final CommandSender sender) {
        getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                sender.sendMessage(ChatColor.WHITE + "Refreshing led grid...");
                frameRefresh();
                sender.sendMessage(ChatColor.WHITE + "Done.");
            }
        }, 0);
    }

    protected void intervalChange(CommandSender sender, final int interval) {
        sender.sendMessage(ChatColor.WHITE + "Changing animation frame interval to " + interval + "ms...");
        getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                writeToFile("i:" + interval + "\n");
            }
        }, 0);
        sender.sendMessage(ChatColor.WHITE + "Done.");
    }

    private void frameRefresh() {
        try {
            lock.lock();
            for (int frameNum : frameMap.keySet()) {
                for (String coord : frameMap.get(frameNum).keySet()) {
                    String color = (String) frameMap.get(frameNum).get(coord).get("COLOR");
                    int ledNum = (Integer) frameMap.get(frameNum).get(coord).get("LEDNUM");
                    String led = String.format("%s:%d:%s", frameNum, ledNum, color);
                    writeToFile("i:" + interval + "\n");
                    writeToFile(led + "\n");
                    try {
                        Thread.sleep(refreshRate);
                    } catch (InterruptedException ex) {
                        logError(ex.getMessage());
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void writeToFile(final String message) {
        try {
            lock.lock();
            FileWriter writer;
            try {
                writer = new FileWriter(outputFilename, true);
                writer.write(message);
                writer.close();
            } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                logError(ex.getMessage());
            } catch (IOException ex) {
                logError(ex.getMessage());
            }
        } finally {
            lock.unlock();
        }

    }

    public void writeToFileAsync(final String message) {
        getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                try {
                    lock.lock();
                    FileWriter writer;
                    try {
                        writer = new FileWriter(outputFilename, true);
                        writer.write(message);
                        writer.close();
                    } catch (FileNotFoundException | UnsupportedEncodingException ex) {
                        logError(ex.getMessage());
                    } catch (IOException ex) {
                        logError(ex.getMessage());
                    }
                } finally {
                    lock.unlock();
                }
            }
        }, 0);
    }

    public void saveFrames() {
        getConfig().set("frames", null);
        for (int frameNum : frameMap.keySet()) {
            for (String coord : frameMap.get(frameNum).keySet()) {
                String color = (String) frameMap.get(frameNum).get(coord).get("COLOR");
                int ledNum = (Integer) frameMap.get(frameNum).get(coord).get("LEDNUM");
                getConfig().set("frames." + frameNum + "." + coord + ".color", color);
                getConfig().set("frames." + frameNum + "." + coord + ".lednum", ledNum);
            }
        }
        saveConfig();
    }

    //protected String getRgbCode(DyeColor dyeColor) {
    //    String dyeColorName = dyeColor.name().toUpperCase();
    //    String rgbCode = getConfig().getString("color-codes." + dyeColorName, "000000");
    //    return rgbCode;
    //}
    public String getBlockCoord(Block block) {
        return String.format("%s,%s,%s", block.getX(), block.getY(), block.getZ());
    }

    public String getDefaultMat() {
        return defaultMat;
    }

}
