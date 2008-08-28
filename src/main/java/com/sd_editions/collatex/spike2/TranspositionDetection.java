package com.sd_editions.collatex.spike2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class TranspositionDetection {
  private final List<Phrase> phrases;
  private final WitnessIndex witnessIndex;
  private final WitnessIndex witnessIndex2;

  public TranspositionDetection(WitnessIndex _witnessIndex, WitnessIndex _witnessIndex2) {
    this.witnessIndex = _witnessIndex;
    this.witnessIndex2 = _witnessIndex2;
    this.phrases = detectTranspositions();
  }

  private List<Phrase> detectTranspositions() {
    Set<Integer> matches = matches();
    List<Integer> matchesSequenceInBase = Lists.newArrayList(matches);
    List<Integer> matchesSequenceInWitness = sortMatchesByPosition(matches, witnessIndex2);
    //    System.out.println(matchesSequenceInBase);
    //    System.out.println(matchesSequenceInWitness);

    return calculatePhrases(matchesSequenceInBase, matchesSequenceInWitness, witnessIndex);
  }

  // Integers are word codes
  private Set<Integer> matches() {
    Set<Integer> matches = Sets.newLinkedHashSet(witnessIndex.getWordCodes());
    matches.retainAll(witnessIndex2.getWordCodes());
    //    System.out.println(matches);
    return matches;
  }

  // step 1 take the matches
  // step 2 walk over the witness index and filter away everything that is not a match

  protected static List<Integer> sortMatchesByPosition(final Set<Integer> matches, WitnessIndex witness) {
    List<Integer> wordCodesList = witness.getWordCodesList();
    List<Integer> onlyMatches = Lists.newArrayList(Iterables.filter(wordCodesList, new Predicate<Integer>() {
      public boolean apply(Integer wordCode) {
        return matches.contains(wordCode);
      }
    }));
    return onlyMatches;
  }

  @SuppressWarnings("boxing")
  private List<Phrase> calculatePhrases(List<Integer> matchesSequenceInBase, List<Integer> matchesSequenceInWitness, WitnessIndex base) {
    List<Phrase> _phrases = Lists.newArrayList();
    Map<Integer, Integer> expectations = calculateSequenceExpectations(matchesSequenceInBase);
    Integer expected = expectations.get(matchesSequenceInWitness.get(0));
    Integer beginPosition = 1;
    Integer position = 2;
    Iterator<Integer> i = matchesSequenceInWitness.iterator();
    i.next();
    while (i.hasNext()) {
      Integer actual = i.next();
      if (actual != expected) {
        // we detected a transposition
        _phrases.add(createPhrase(beginPosition, position - 1, matchesSequenceInWitness, base));
        beginPosition = position;
        expected = expectations.get(actual);
      } else {
        expected = expectations.get(expected);
      }
      position++;

    }
    _phrases.add(createPhrase(beginPosition, position - 1, matchesSequenceInWitness, base));
    return _phrases;
  }

  private Phrase createPhrase(Integer beginPosition, Integer endPosition, List<Integer> matchesSequenceInWitness, WitnessIndex base) {
    //    System.out.println("We detected a phrase from " + beginPosition + " to " + endPosition);
    Integer beginWordCode = matchesSequenceInWitness.get(beginPosition - 1);
    Integer endWordCode = matchesSequenceInWitness.get(endPosition - 1);
    int beginPositionInBase = base.getPosition(beginWordCode);
    int endPositionInBase = base.getPosition(endWordCode);
    Phrase phrase = base.createPhrase(beginPositionInBase, endPositionInBase);
    return phrase;
  }

  // TODO: in theory there could be no matches... that case should be handled earlier
  // or there could be only one match!
  protected static Map<Integer, Integer> calculateSequenceExpectations(List<Integer> matchesSequenceInBase) {
    // build expectation map
    Map<Integer, Integer> expectations = Maps.newHashMap();
    Iterator<Integer> i = matchesSequenceInBase.iterator();
    Integer previous = i.next();
    while (i.hasNext()) {
      Integer next = i.next();
      expectations.put(previous, next);
      previous = next;
    }
    return expectations;
  }

  public List<Phrase> getPhrases() {
    return phrases;
  }

}