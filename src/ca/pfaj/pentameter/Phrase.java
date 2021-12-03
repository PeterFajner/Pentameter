package ca.pfaj.pentameter;

import java.util.List;
import java.util.Optional;

/**
 * A list of words
 */
public class Phrase {
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

record PronouncedPhrase(List<PronouncedWord> words) {

}
