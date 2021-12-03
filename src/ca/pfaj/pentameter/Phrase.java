package ca.pfaj.pentameter;

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
    public String colour() {
        // get all unique pronounciations for this phrase
        var pronounciationOptions = uniqueProunounciations();
        // try to find iambic penameter, just iambic, or not iambic, in that order
        PronouncedPhrase firstIambicPentameter = null;
        PronouncedPhrase firstIambic = null;
        PronouncedPhrase chosen = null;
        for (var option : pronounciationOptions) {
            if (option.isIambicPentameter()) {
                firstIambicPentameter = option;
                break;
            } else if (option.isIambic()) {
                firstIambic = option;
            } else {
                chosen = option;
            }
        }
        if (firstIambicPentameter != null) {
            chosen = firstIambicPentameter
        } else if (firstIambic != null) {
            chosen = firstIambic;
        }
        // colour the phrase
        throw new UnsupportedOperationException();
    }

    /**
     * Get the phrase's iambic pronounciation, if it exists. If multiple exist, choose one.
     * @return the phrase's iambic pronounciation, if it exists
     */
    public Optional<PronouncedPhrase> getIambicPronounciation() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the phrase's iambic pentameter pronounciation, if it exists. If multiple exists, choose one.
     * @return the phrase's iambic pentameter pronounciation, if it exists
     */
    public Optional<PronouncedPhrase> getIambicPentameterPronounciation() {
        throw new UnsupportedOperationException();
    }
}

/**
 * A phrase with a single, chosen pronounciation for all the words.
 */
record PronouncedPhrase(List<PronouncedWord> words) {
    public boolean isIambic() {
        throw new UnsupportedOperationException();
    }

    public boolean isIambicPentameter() {
        throw new UnsupportedOperationException();
    }

    public String colour() {
        throw new UnsupportedOperationException();
    }
}
