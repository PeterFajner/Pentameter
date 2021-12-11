package ca.pfaj.pentameter;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Pentameter extends JavaPlugin {
    String DICT_FILENAME = "cmudict-0.7b";
    String DICT_URL = "https://svn.code.sf.net/p/cmusphinx/code/trunk/cmudict/cmudict-0.7b";
    PluginLogger logger = new PluginLogger(this);
    Dictionary dictionary;

    /**
     * Download the CMU Pronounciation Dictionary, if it's not already downloaded
     * @param filename filename to save the dictionary as
     * @param url URL to get the dictionary from
     * @return whether the dictionary was downloaded
     */
    boolean downloadDictionary(String filename, String url) {
        var dictPath = new File(getDataFolder(), filename);
        if (dictPath.exists()) {
            return false;
        } else {
            info("CMU Pronouncing Dictionary not found, downloading...");
            try {
                URL _url = new URL(url);
                FileUtils.copyURLToFile(_url, dictPath, 10_000, 10_000);
            } catch (MalformedURLException e) {
                warn("CMU Pronouncing Dictionary URL is invalid, disabling plugin");
                getServer().getPluginManager().disablePlugin(this);
            } catch (IOException e) {
                warn("Couldn't download CMU Pronouncing Dictionary, disabling plugin");
                getServer().getPluginManager().disablePlugin(this);
            }
            return true;
        }
    }

    Dictionary loadDictionary(String filename) {
        // load the dictionary into memory
        var dictionary = new Dictionary();
        try {
            var path = new File(getDataFolder(), filename);
            var reader = new BufferedReader(new FileReader(path));
            reader.lines().forEach(line -> {
                // ignore non-words
                if (!Character.isLetter(line.charAt(0))) {
                    return;
                }
                var split = line.split(" ", 2);
                var word = split[0];
                // find words like WOJCIECH(1) which indicate an alternative pronounciation, and trim the ()
                if (word.indexOf('(') > 0) {
                    word = word.substring(0, word.indexOf('('));
                }
                // parse the pronounciation string (like V OY1 CH EH0 K)
                // 0 is no stress, 1 is primary stress, 2 is secondary stress; combine the latter two
                var stress = new LinkedList<Stress>();
                split[1].chars().forEach(c -> {
                    switch (c) {
                        case '0' -> stress.add(Stress.LOW);
                        case '1', '2' -> stress.add(Stress.HIGH);
                    }
                });
                // if word has one syllable, can be stressed or unstressed; otherwise save the one stress pattern
                if (stress.size() == 1) {
                    dictionary.add(word, Stress.HIGH);
                    dictionary.add(word, Stress.LOW);
                } else {
                    dictionary.add(word, stress);
                }
            });
        } catch (IOException e) {
            warn("IOException when reading CMU Pronouncing Dictionary");
            e.printStackTrace();
        }
        return dictionary;
    }

    @Override
    public void onEnable() {
        // download the CMU Pronouncing dictionary
        if (downloadDictionary(DICT_FILENAME, DICT_URL)) {
            info ("CMU Pronouncing Dictionary downloaded successfully.");
        } else {
            info("CMU Pronouncing Dictionary found.");
        }
        this.dictionary = loadDictionary(DICT_FILENAME);

        // combine the dictionary with our own dictionary of Minecraft words
        // todo implement

        // register chat listener
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    void log(Level level, String msg) {
        logger.log(new LogRecord(level, msg));
    }

    void warn(String msg) {
        log(Level.WARNING, msg);
    }

    void info(String msg) {
        log(Level.INFO, msg);
    }

    void debug(String msg) {
        log(Level.FINE, msg);
    }
}

class ChatListener implements Listener {
    Pentameter plugin;

    public ChatListener(Pentameter plugin) {
        super();
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerChatEvent event) {
        // async means a player sent it, as opposed to a plugin speaking for them
        if (event.isAsynchronous()) {
            // colour message
            var msg = event.getMessage();
            var phrase = new Phrase(msg, plugin.dictionary);
            var coloured = phrase.colour();
            // add the speaker's name, since it doesn't get send automatically when sending per-player messages
            var playerName_ = "<" + event.getPlayer().getDisplayName() + "> ";
            var playerName = new ComponentBuilder(playerName_).create();
            // combine the speaker's name with the coloured message (not the cleanest way to combine arrays, maybe)
            List<BaseComponent> message_ = new ArrayList<>(List.of(playerName));
            message_.addAll(List.of(coloured));
            BaseComponent[] message = message_.toArray(new BaseComponent[]{});
            // cancel the chat event
            event.setCancelled(true);
            // send coloured message to each player, and log in the server console
            Bukkit.getScheduler().runTask(plugin, new Thread(() -> {
                for (var player : Bukkit.getServer().getOnlinePlayers()) {
                    player.spigot().sendMessage(message);
                }
                Bukkit.getLogger().info(playerName_ + msg);
            }));
            // apply status effects - has to be done on main thread
            Bukkit.getScheduler().runTask(plugin, new Thread(() -> {
                var player = event.getPlayer();
                if (phrase.isIambicPentameter()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 3*20, 1));
                } else if (phrase.isIambic()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 1, 0));
                } else {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 10*20, 0));
                }
            }));
        }
    }
}