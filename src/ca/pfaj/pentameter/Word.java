package ca.pfaj.pentameter;

import java.util.List;
import java.util.Set;

/**
 * A word that can be pronounced in one or more ways.
 */
public record Word(String name, Set<Pronounciation> pronounciationOptions) {

}

/**
 * A word with one particular chosen pronounciation.
 */
record PronouncedWord(String name, List<Stress> pronounciation) {

}

/**
 * A single way to pronounce something.
 */
record Pronounciation(List<Stress> stress) {

}