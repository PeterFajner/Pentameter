package ca.pfaj.pentameter;

import org.apache.commons.io.FileUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Pentameter extends JavaPlugin {
    String DICT_FILENAME = "cmudict-0.7b";
    String DICT_URL = "http://svn.code.sf.net/p/cmusphinx/code/trunk/cmudict/cmudict-0.7b";
    PluginLogger logger = new PluginLogger(this);
    Dictionary dictionary = new Dictionary();

    @Override
    public void onEnable() {
        // check if we have the CMU Pronouncing Dictionary, and download it if not
        var dictPath = new File(getDataFolder(), DICT_FILENAME);
        if (!dictPath.exists()) {
            info("CMU Pronouncing Dictionary not found, downloading...");
            try {
                URL url = new URL(DICT_URL);
                FileUtils.copyURLToFile(url, dictPath, 10_000, 10_000);
            } catch (MalformedURLException e) {
                warn("CMU Pronouncing Dictionary URL is invalid, disabling plugin");
                getServer().getPluginManager().disablePlugin(this);
            } catch (IOException e) {
                warn("Couldn't download CMU Pronouncing Dictionary, disabling plugin");
                getServer().getPluginManager().disablePlugin(this);
            }
            info ("CMU Pronouncing Dictionary downloaded successfully.");
        } else {
            info("CMU Pronouncing Dictionary found.");
        }

        // load the dictionary into memory
        try {
            var reader = new BufferedReader(new FileReader(dictPath));
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
                // 0 is no stress, 1 is primary stress, 2 is secondary stress; collapse the latter two
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

        // combine the dictionary with our own dictionary of Minecraft words
        // todo implement

        // register chat listener
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
    }

    private void log(Level level, String msg) {
        logger.log(new LogRecord(level, msg));
    }

    private void warn(String msg) {
        log(Level.WARNING, msg);
    }

    private void info(String msg) {
        log(Level.INFO, msg);
    }
}

enum Stress {
    HIGH,
    LOW,
    SILENT,
}

class Dictionary {
    Map<String, Set<List<Stress>>> store = new HashMap<>();
    
    public void add(String word, List<Stress> pronounciation) {
        var entry = store.computeIfAbsent(word, k -> new HashSet<>());
        entry.add(pronounciation);
    }
    
    public void add(String word, Stress[] pronounciation) {
        var asList = List.of(pronounciation);
        add(word, asList);
    }

    public void add(String word, Stress pronounciation) {
        var asArray = new Stress[] {pronounciation};
        add(word, asArray);
    }

    public String colourPhrase(String phrase) {
        // all pronounciation options for each word
        List<PronounciationOptions> parsedWords = new LinkedList<>();
        // silent "words", like joiners
        var silent = Set.of(List.of(Stress.SILENT));
        // unknown words are assumed to be one syllable and can be stressed or unstressed
        var unknown = Set.of(List.of(new Stress[]{Stress.HIGH, Stress.LOW}));
        // deconstruct the phrase, using spaces and dashes as joiners, and make a list of possible pronounciation options
        while (phrase != null) {
            String word;
            String joiner;
            // check if word ends with a space or a dash, or the end of the phrase
            int nextSpace = phrase.indexOf(' ');
            int nextDash = phrase.indexOf('-');
            if (nextSpace == -1 && nextDash == -1) {
                word = phrase;
                joiner = null;
                phrase = null;
            } else if (nextDash == -1 || nextSpace <= nextDash) {
                word = phrase.substring(0, nextSpace);
                joiner = " ";
                phrase = phrase.substring(nextSpace + 1);
            } else {
                word = phrase.substring(0, nextDash);
                joiner = "-";
                phrase = phrase.substring(nextDash + 1);
            }
            // pronounciation options for this word; if not found, assume single syllable
            var options = store.getOrDefault(word.toUpperCase(), unknown);
            parsedWords.add(new PronounciationOptions(word, options));
            if (joiner != null) {
                parsedWords.add(new PronounciationOptions(joiner, silent));
            }
        }
        /*
         * Check all pronounciation options. If an iambic option is found, colour based on that,
         * otherwise just colour the first option for each.
         */
        // each entry is a possible pronounciation for the whole phrase
        List<List<Pronounciation>> allPronounciations = new LinkedList<>();
        for (var word : parsedWords) {
            // if list is empty, create a list of pronounciation options for this word
            if (allPronounciations.isEmpty()) {
                for (var option : word.pronounciationOptions()) {
                    var newList = new LinkedList<Pronounciation>();
                    newList.add(new Pronounciation(word.word(), option));
                    allPronounciations.add(newList);
                }
            }
            // create a new list of [entries in old list * pronounciation options for this word]
            List<List<Pronounciation>> newList = new LinkedList<>();
            for (var entry : allPronounciations) {
                for (var option : word.pronounciationOptions()) {
                    var copiedEntry = new LinkedList<>(List.copyOf(entry));
                    copiedEntry.add(new Pronounciation(word.word(), option));
                    newList.add(copiedEntry);
                }
            }
            allPronounciations = newList;
        }

        // check if there are any iambic options, and pick a particular pronounciation to use
        List<Pronounciation> chosen = allPronounciations.get(0);
        for (var option : allPronounciations) {
            if (Dictionary.isIambic(option)) {
                chosen = option;
                break;
            }
        }

        // colour the chosen pronounciation
        var coloured = colourPhrase(chosen);
        return coloured;
    }

    public static String colourPhrase(List<Pronounciation> phrase) {
        var lookingFor = Stress.LOW;
        StringBuilder coloured = new StringBuilder();
        for (var word : phrase) {
            System.out.println(word.word());
            // break the word into fragments based on number of syllables
            // this isn't a good way to do this, but it's easy
            var w = word.word();
            var length = w.length();
            var numSyllables = word.pronounciation().size();
            var fragmentSize = (int) Math.floor((double) length / (double) numSyllables);
            List<String> fragments = new LinkedList<>();
            for (int i = 0; i < numSyllables; i++) {
                if (i < numSyllables - 1) {
                    fragments.add(w.substring(i*fragmentSize, (i+1)*fragmentSize));
                } else {
                    // the last fragment may be larger
                    fragments.add(w.substring(i*fragmentSize));
                }
            }

            for (int i = 0; i < numSyllables; i++) {
                var syllable = word.pronounciation().get(i);
                var fragment = fragments.get(i);
                if (syllable.equals(Stress.LOW) && lookingFor.equals(Stress.LOW)) {
                    coloured.append("§a");
                    coloured.append(fragment);
                    coloured.append("§r");
                    coloured.append("gl");
                    lookingFor = Stress.HIGH;
                } else if (syllable.equals(Stress.LOW) && lookingFor.equals(Stress.HIGH)) {
                    coloured.append("§d");
                    coloured.append(fragment);
                    coloured.append("§r");
                    coloured.append("bl");
                    lookingFor = Stress.LOW;
                } else if (syllable.equals(Stress.HIGH) && lookingFor.equals(Stress.LOW)) {
                    coloured.append("§5");
                    coloured.append(fragment);
                    coloured.append("§r");
                    coloured.append("bh");
                    lookingFor = Stress.HIGH;
                } else if (syllable.equals(Stress.HIGH) && lookingFor.equals(Stress.HIGH)) {
                    coloured.append("§2");
                    coloured.append(fragment);
                    coloured.append("§r");
                    coloured.append("gh");
                    lookingFor = Stress.LOW;
                } else if (syllable.equals(Stress.SILENT)) {
                    coloured.append("§r");
                    coloured.append(word.word());
                }
            }
        }
        return coloured.toString();
    }

    public static boolean isIambic(List<Pronounciation> phrase) {
        var lookingFor = Stress.LOW;
        for (var word : phrase) {
            for (var syllable : word.pronounciation()) {
                if (syllable.equals(lookingFor)) {
                    // if the syllable is good, switch which one we're looking for next loop
                    if (lookingFor.equals(Stress.LOW)) {
                        lookingFor = Stress.HIGH;
                    } else {
                        lookingFor = Stress.LOW;
                    }
                } else if (syllable.equals(Stress.SILENT)) {
                    // ignore silent syllables
                    continue;
                } else {
                    // if the syllable is bad, the phrase is not iambic
                    return false;
                }
            }
        }
        return true;
    }
}

// a definite pronounciation for one word
record Pronounciation(String word, List<Stress> pronounciation) {

};

// several possible pronounciations for one word
record PronounciationOptions(String word, Set<List<Stress>> pronounciationOptions) {

};

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
            var msg = event.getMessage();
            var coloured = plugin.dictionary.colourPhrase(msg);
            event.setMessage(coloured);
        }
    }
}