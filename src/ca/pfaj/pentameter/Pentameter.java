package ca.pfaj.pentameter;

import net.minecraft.world.level.storage.LevelVersion;
import org.apache.commons.io.FileUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Pentameter extends JavaPlugin {
    String DICT_FILENAME = "cmudict-0.7b";
    String DICT_URL = "http://svn.code.sf.net/p/cmusphinx/code/trunk/cmudict/cmudict-0.7b";
    PluginLogger logger = new PluginLogger(this);

    @Override
    public void onEnable() {
        // check if we have the CMU Pronouncing Dictionary, and download it if not
        var dictPath = new File(getDataFolder(), DICT_FILENAME);
        if (!dictPath.exists()) {
            try {
                URL url = new URL(DICT_URL);
                FileUtils.copyURLToFile(url, dictPath, 10_000, 10_000);
            } catch (MalformedURLException e) {
                logger.log(new LogRecord(Level.WARNING,
                        "CMU Pronouncing Dictionary URL is invalid, disabling plugin"));
                getServer().getPluginManager().disablePlugin(this);
            } catch (IOException e) {
                logger.log(new LogRecord(Level.WARNING,
                        "Couldn't download CMU Pronouncing Dictionary, disabling plugin"));
                getServer().getPluginManager().disablePlugin(this);
            }
        }

        // download the CMU Pronouncing Dictionary if we don't have it


        // load the dictionary into memory
        // todo implement

        // combine the dictionary with our own dictionary of Minecraft words
        // todo implement

        this.getServer().getPluginManager().registerEvents(new ChatListener(), this);
    }

    @Override
    public void onDisable() {
    }
}

class ChatListener implements Listener {
    @EventHandler
    public void onPlayerJoin(AsyncPlayerChatEvent event) {
        // async means a player sent it, as opposed to a plugin speaking for them
        if (event.isAsynchronous()) {
            event.setMessage(event.getMessage() + " :)");
        }
    }
}