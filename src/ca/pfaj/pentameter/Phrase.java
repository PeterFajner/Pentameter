package ca.pfaj.pentameter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.plugin.PluginLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.cartesianProduct;

/**
 * A list of words
 */
public record Phrase(List<Word> words, Dictionary dictionary) {
    public Phrase(String words, Dictionary dictionary) {
        this(Phrase.parseWords(words, dictionary), dictionary);
    }

    static List<Word> parseWords(String phrase, Dictionary dictionary) {
        List<Word> parsedWords = new LinkedList<>();
        // deconstruct the string, treating spaces and dashes as joiners
        int guard = 1000; // prevent infinite loops
        while (phrase != null && guard-- > 0) {
            String word; // next word that has been parsed
            String joiner; // joiner following the next word
            // check if word ends with a space or a dash, or the end of the phrase
            int nextSpace = phrase.indexOf(' ');
            int nextDash = phrase.indexOf('-');
            if (nextSpace == -1 && nextDash == -1) {
                // the phrase has only one word left
                word = phrase;
                joiner = null;
                phrase = null;
            } else if (nextDash == -1 || nextSpace <= nextDash) {
                // the next joiner is a space
                word = phrase.substring(0, nextSpace);
                joiner = " ";
                phrase = phrase.substring(nextSpace + 1);
            } else {
                // the next joiner is a dash
                word = phrase.substring(0, nextDash);
                joiner = "-";
                phrase = phrase.substring(nextDash + 1);
            }
            // clean up the word for dictionary matching - remove non-letters, make uppercase, etc
            var cleanedWord = Dictionary.cleanWord(word);
            // get the pronounciation options for this word; if none found, assume it's one syllable
            var pronounciations = dictionary.store.getOrDefault(cleanedWord, Dictionary.SINGLE);
            parsedWords.add(new Word(word, pronounciations));
            if (joiner != null) {
                parsedWords.add(new Word(joiner, Dictionary.SILENT));
            }
        }
        return parsedWords;
    }

    public boolean isIambic() {
        return getIambicPronounciation().isPresent();
    }

    public boolean isIambicPentameter() {
        return getIambicPentameterPronounciation().isPresent();
    }

    /**
     * Get all unique pronounciations of this phrase
     * @return all unique pronounciations of this phrase
     */
    public Set<PronouncedPhrase> uniqueProunounciations() {
        /*
         * The set of all unique pronounciations of this phrase is equal to the
         * cartesian product of the unique pronounciations of all the words.
         * ex: A phrase with three words (A, B, C). A has two pronounciations (i and ii),
         *     B has two pronounciations (i and ii), and C has three
         *     pronounciations (i, ii, iii). The possible unique pronounciations are
         *     [Ai  ]       [Bi  ]       [Ci  ]
         *     [Aii ]   x   [Bii ]   x   [Cii ]
         *                               [Ciii]
         *     = [AiBiCi, AiBiCii, AiBiCiii, AiBiiCi, AiBiiCii, AiBiiCiii,
         *        AiiBiCi, AiiBiCii, AiiBiCiii, AiiBiiCi, AiiBiiCii, AiiBiiCiii]
         */
        // get unique pronounciation for each word
        List<Set<PronouncedWord>> uniqueWords = new LinkedList<>();
        for (var word : words) {
            uniqueWords.add(word.uniquePronounciations());
        }
        // get cartesian product of the unique pronounciations
        return cartesianProduct(uniqueWords).stream().map(PronouncedPhrase::new).collect(Collectors.toSet());
    }

    /**
     * Colour the phrase, choosing a pronounciation that is iambic if possible.
     */
    public BaseComponent[] colour() {
        // get all unique pronounciations for this phrase
        var pronounciationOptions = uniqueProunounciations();
        // try to find iambic penameter, just iambic, or not iambic, in that order
        PronouncedPhrase firstIambicPentameter = null;
        PronouncedPhrase firstIambic = null;
        PronouncedPhrase chosen = pronounciationOptions.iterator().next();
        for (var option : pronounciationOptions) {
            if (option.isIambicPentameter()) {
                firstIambicPentameter = option;
                break;
            } else if (option.isIambic()) {
                firstIambic = option;
            } else {
                if (option.getNumIncorrectSyllables() < chosen.getNumIncorrectSyllables()) {
                    // this pronounciation isn't iambic, but it has fewer incorrect syllables, so choose it if no iambic
                    // pronounciation is found
                    chosen = option;
                }
            }
        }
        if (firstIambicPentameter != null) {
            chosen = firstIambicPentameter;
        } else if (firstIambic != null) {
            chosen = firstIambic;
        }
        // colour the phrase
        return chosen.colour();
    }

    /**
     * Get the phrase's iambic pronounciation, if it exists. If multiple exist, choose one.
     * @return the phrase's iambic pronounciation, if it exists
     */
    public Optional<PronouncedPhrase> getIambicPronounciation() {
        var iambicPentameterPronounciation = getIambicPentameterPronounciation();
        // try to find iambic pentameter
        if (iambicPentameterPronounciation.isPresent()) {
            return iambicPentameterPronounciation;
        }
        // try to find other iambic
        for (var phrase : uniqueProunounciations()) {
            if (phrase.isIambic()) {
                return Optional.of(phrase);
            }
        }
        // return nothing
        return Optional.empty();
    }

    /**
     * Get the phrase's iambic pentameter pronounciation, if it exists. If multiple exists, choose one.
     * @return the phrase's iambic pentameter pronounciation, if it exists
     */
    public Optional<PronouncedPhrase> getIambicPentameterPronounciation() {
        for (var phrase : uniqueProunounciations()) {
            if (phrase.isIambicPentameter()) {
                return Optional.of(phrase);
            }
        }
        return Optional.empty();
    }
}

/**
 * A phrase with a single, chosen pronounciation for all the words.
 */
class PronouncedPhrase {
    List<PronouncedWord> words;
    int numIncorrectSyllables = 0;
    boolean iambic = true;

    public PronouncedPhrase(List<PronouncedWord> words) {
        this.words = words;
        // check if phrase is iambic
        var expected = Stress.LOW;
        for (var syllable : getStress()) {
            if (syllable == Stress.HIGH) {
                if (expected == Stress.HIGH) {
                    // expected high, found high
                    expected = Stress.LOW;
                } else {
                    // expected low, found high
                    numIncorrectSyllables++;
                    iambic = false;
                }
            } else {
                if (expected == Stress.HIGH) {
                    // expected high, found low
                    numIncorrectSyllables++;
                    iambic = false;
                } else {
                    // expected low, found low
                    expected = Stress.HIGH;
                }
            }
        }
    }

    public int getNumIncorrectSyllables() {
        return numIncorrectSyllables;
    }

    public boolean isIambic() {
        return iambic;
    }

    public boolean isIambicPentameter() {
        return this.isIambic() && this.getStress().size() == 10;
    }

    /**
     * Get all of the syllables in this phrase in a single list, ignoring silent syllables
     * @return all of the syllables in this phrase, ignoring silent syllables
     */
    public List<Stress> getStress() {
        List<Stress> stress = new LinkedList<>();
        for (var word : words) {
            for (var syllable : word.pronounciation().stress()) {
                if (syllable != Stress.SILENT) {
                    stress.add(syllable);
                }
            }
        }
        return stress;
    }

    public BaseComponent[] colour() {
        var coloured = new ComponentBuilder();
        var lookingFor = Stress.LOW; // expected syllable stress
        for (var word : words) {
            var name = word.name();
            var length = name.length();
            var numSyllables = word.pronounciation().stress().size();
            var syllableSize = (int) Math.floor((double) length / (double) numSyllables);
            List<String> fragments = new LinkedList<>();
            // break the word up into its constituent syllables in a naive way
            for (int i = 0; i < numSyllables; i++) {
                if (i < numSyllables - 1) {
                    fragments.add(name.substring(i * syllableSize, (i+1) * syllableSize));
                } else {
                    // the last fragment may be larger
                    fragments.add(name.substring(i * syllableSize));
                }
            }
            // colour each syllable
            for (int i = 0; i < numSyllables; i++) {
                var syllable = word.pronounciation().stress().get(i);
                var fragment = fragments.get(i);
                coloured.append(fragment);
                coloured.reset();
                if (syllable.equals(Stress.LOW)) {
                    if (lookingFor.equals(Stress.LOW)) {
                        // found low, want low
                        coloured.color(ChatColor.GREEN);
                        coloured.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(new ComponentBuilder(fragment + ": expected unstressed syllable, found unstressed syllable").color(ChatColor.GREEN).create())));
                    } else {
                        // found low, want high
                        coloured.color(ChatColor.LIGHT_PURPLE);
                        coloured.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(new ComponentBuilder(fragment + ": expected stressed syllable, found unstressed syllable").color(ChatColor.LIGHT_PURPLE).create())));
                    }
                    lookingFor = Stress.HIGH;
                } else if (syllable.equals(Stress.HIGH)) {
                    if (lookingFor.equals(Stress.LOW)) {
                        // found high, want low
                        coloured.color(ChatColor.DARK_PURPLE);
                        coloured.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(new ComponentBuilder(fragment + ": expected unstressed syllable, found stressed syllable").color(ChatColor.DARK_PURPLE).create())));
                    } else {
                        // found high, want high
                        coloured.color(ChatColor.DARK_GREEN);
                        coloured.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text(new ComponentBuilder(fragment + ": expected stressed syllable, found stressed syllable").color(ChatColor.DARK_GREEN).create())));
                    }
                    lookingFor = Stress.LOW;
                } else if (syllable.equals(Stress.SILENT)) {
                    coloured.reset();
                }
                // if the phrase is in iambic pentameter, bold it
                if (isIambicPentameter()) {
                    coloured.bold(true);
                }
            }
        }
        return coloured.create();
    }
}