package ca.pfaj.pentameter;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
        while (phrase != null) {
            String word; // next word that has been parsed
            String joiner; // joiner following the next word
            // check if word ends with a space or a dash, or the end of the phrase
            int nextSpace = phrase.indexOf(' ');
            int nextDash = phrase.indexOf('-');
            if (nextSpace == -1 && nextDash == -1) {
                // the phrase has only one word left
                word = phrase;
                joiner = null;
                word = null;
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
        throw new UnsupportedOperationException();
    }

    public boolean isIambicPentameter() {
        throw new UnsupportedOperationException();
    }

    /**
     * Colour the phrase, choosing a pronounciation that is iambic if possible.
     */
    public String colour() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the phrase's iambic pronounciation, if it exists.
     * @return a list of words with a single chosen pronounciation each
     */
    public Optional<PronouncedPhrase> getIambicPronounciation() {
        throw new UnsupportedOperationException();
    }
}

/**
 * A phrase with a single, chosen pronounciation for all the words.
 */
record PronouncedPhrase(List<PronouncedWord> words) {

}
