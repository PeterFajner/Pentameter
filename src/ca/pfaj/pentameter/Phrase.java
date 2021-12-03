package ca.pfaj.pentameter;

import java.util.List;
import java.util.Optional;

/**
 * A list of words
 */
public record Phrase(List<Word> words, Dictionary dictionary) {
    public Phrase(String words, Dictionary dictionary) {
        this(Phrase.parseWords(words, dictionary), dictionary);
    }

    static List<Word> parseWords(String words, Dictionary dictionary) {
        throw new UnsupportedOperationException();
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
