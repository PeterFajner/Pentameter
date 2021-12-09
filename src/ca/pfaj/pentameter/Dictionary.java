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
        var entry = store.computeIfAbsent(word, k -> new HashSet<>());
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

    public static String cleanWord(String word) {
        word = word.toUpperCase();
        word = word.replaceAll("[^a-zA-Z]", "");
        return word;
    }
}
