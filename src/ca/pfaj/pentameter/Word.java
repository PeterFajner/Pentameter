package ca.pfaj.pentameter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A word that can be pronounced in one or more ways.
 */
public record Word(String name, Set<Pronounciation> pronounciationOptions) {
    /**
     * Get a set of PronouncedWords, representing each unique pronounciation of this word
     * @return the unique pronounciations of this word
     */
    public Set<PronouncedWord> uniquePronounciations() {
        return pronounciationOptions.stream().map(
                opt -> new PronouncedWord(name, opt)).collect(Collectors.toSet());
    }
}

/**
 * A word with one particular chosen pronounciation.
 */
record PronouncedWord(String name, Pronounciation pronounciation) {
}

/**
 * A single way to pronounce something.
 */
record Pronounciation(List<Stress> stress) {

}