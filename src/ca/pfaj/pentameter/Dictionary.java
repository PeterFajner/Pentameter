package ca.pfaj.pentameter;

import java.util.*;

class Dictionary {
    Map<String, Set<Pronounciation>> store = new HashMap<>();

    // A single-syllable word can be either stressed or unstressed
    static Set<Pronounciation> SINGLE = Set.of(
            new Pronounciation[]{
                    new Pronounciation(List.of(Stress.HIGH)),
                    new Pronounciation(List.of(Stress.LOW)),
            });

    // A silent word
    static Set<Pronounciation> SILENT = Set.of(new Pronounciation(List.of(Stress.SILENT)));

    /**
     * Add a word with a pronounciation
     * @param word the text of the word
     * @param pronounciation the word's pronounciation
     */
    public void add(String word, Pronounciation pronounciation) {
        var entry = store.computeIfAbsent(word, k -> new Word(word, new HashSet<>()));
        entry.add(pronounciation);
    }

    /**
     * Add a word with a pronounciation (as a list of stresses)
     * @param word the text of the word
     * @param pronounciation the word's pronounciation
     */
    public void add(String word, List<Stress> pronounciation) {
        Pronounciation p = new Pronounciation(pronounciation);
        add(word, p);
    }

    /**
     * Add a word with a single syllable and a single pronounciation;
     * @param word the text of the word
     * @param pronounciation the stress of the word's single syllable
     */
    public void add(String word, Stress pronounciation) {
        var list = new LinkedList<Stress>();
        list.add(pronounciation);
        add(word, list);
    }

    @Deprecated
    public String colourPhrase(String phrase) {
        // all pronounciation options for each word
        List<PronounciationOptions> parsedWords = new LinkedList<>();
        // silent "words", like joiners
        var silent = Set.of(List.of(Stress.SILENT));
        // unknown words are assumed to be one syllable and can be stressed or unstressed
        var unknown = new HashSet<List<Stress>>();
        unknown.add(List.of(new Stress[]{Stress.HIGH}));
        unknown.add(List.of(new Stress[]{Stress.LOW}));
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
            var options = store.getOrDefault(cleanWord(word), unknown);
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
            } else {
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
        }

        // check if there are any iambic options, and pick a particular pronounciation to use
        MaybeIambicPhrase chosen = colourPhrase(allPronounciations.get(0));
        for (var option : allPronounciations) {
            var evaluatedPhrase = colourPhrase(option);
            var iambic = evaluatedPhrase.isIambic();
            if (iambic) {
                chosen = evaluatedPhrase;
            }
        }

        // colour the chosen pronounciation
        return chosen.colouredString();
    }

    public static String cleanWord(String word) {
        word = word.toUpperCase();
        word = word.replaceAll("[^a-zA-Z]", "");
        return word;
    }

    @Deprecated
    public static MaybeIambicPhrase colourPhrase(List<Pronounciation> phrase) {
        var isIambic = true;
        var lookingFor = Stress.LOW;
        StringBuilder coloured = new StringBuilder();
        for (var word : phrase) {
            // break the word into fragments based on number of syllables
            // this isn't a good way to do this, but it's easy
            var w = word.word();
            var length = w.length();
            var numSyllables = word.pronounciation().size();
            var fragmentSize = (int) Math.floor((double) length / (double) numSyllables);
            List<String> fragments = new LinkedList<>();
            for (int i = 0; i < numSyllables; i++) {
                if (i < numSyllables - 1) {
                    fragments.add(w.substring(i * fragmentSize, (i + 1) * fragmentSize));
                } else {
                    // the last fragment may be larger
                    fragments.add(w.substring(i * fragmentSize));
                }
            }

            for (int i = 0; i < numSyllables; i++) {
                var syllable = word.pronounciation().get(i);
                var fragment = fragments.get(i);
                if (syllable.equals(Stress.LOW) && lookingFor.equals(Stress.LOW)) {
                    coloured.append("§a");
                    coloured.append(fragment);
                    coloured.append("§r");
                    lookingFor = Stress.HIGH;
                } else if (syllable.equals(Stress.LOW) && lookingFor.equals(Stress.HIGH)) {
                    coloured.append("§d");
                    coloured.append(fragment);
                    coloured.append("§r");
                    lookingFor = Stress.LOW;
                    isIambic = false;
                } else if (syllable.equals(Stress.HIGH) && lookingFor.equals(Stress.LOW)) {
                    coloured.append("§5");
                    coloured.append(fragment);
                    coloured.append("§r");
                    lookingFor = Stress.HIGH;
                    isIambic = false;
                } else if (syllable.equals(Stress.HIGH) && lookingFor.equals(Stress.HIGH)) {
                    coloured.append("§2");
                    coloured.append(fragment);
                    coloured.append("§r");
                    lookingFor = Stress.LOW;
                } else if (syllable.equals(Stress.SILENT)) {
                    coloured.append("§r");
                    coloured.append(word.word());
                }
            }
        }
        return new MaybeIambicPhrase(coloured.toString(), isIambic);
    }
}
