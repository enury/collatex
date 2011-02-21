package eu.interedition.collatex2.implementation.containers.witness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import eu.interedition.collatex2.implementation.input.Phrase;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.IPhrase;
import eu.interedition.collatex2.interfaces.IToken;
import eu.interedition.collatex2.interfaces.ITokenIndex;
import eu.interedition.collatex2.interfaces.IWitness;

public class Witness implements Iterable<INormalizedToken>, IWitness {
  private String sigil;
  private List<INormalizedToken> tokens;

  public Witness() {}

  public Witness(final String sigil, final List<INormalizedToken> tokens) {
    this.sigil = sigil;
    this.tokens = tokens;
  }

  public Witness(final String sigil) {
    this.sigil = sigil;
    this.tokens = Lists.newArrayList();
  }

  // Note: not pleased with this method! implement Iterable!
  @Override
  public List<INormalizedToken> getTokens() {
    return tokens;
  }

  public void setTokens(List<INormalizedToken> tokens) {
    this.tokens = tokens;
  }

  @Override
  public String getSigil() {
    return sigil;
  }

  public void setSigil(String sigil) {
    this.sigil = sigil;
  }

  // TODO check whether iterator.remove() throws exception!
  @Override
  public Iterator<INormalizedToken> iterator() {
    return tokens.iterator();
  }

  @Override
  public IPhrase createPhrase(final int startPosition, final int endPosition) {
    // TODO this problemCase shouldn't occur
    final boolean problemCase = (startPosition - 1 > endPosition);
    final List<INormalizedToken> subList = problemCase ? new ArrayList<INormalizedToken>() : tokens.subList(startPosition - 1, endPosition);
    return new Phrase(subList);
  }

  @Override
  public int size() {
    return tokens.size();
  }

  @Override
  public List<String> getRepeatedTokens() {
    final Multimap<String, INormalizedToken> normalizedTokenMap = ArrayListMultimap.create();
    for (final INormalizedToken token : getTokens()) {
      normalizedTokenMap.put(token.getNormalized(), token);
    }
    final List<String> repeatingNormalizedTokens = Lists.newArrayList();
    for (final String key : normalizedTokenMap.keySet()) {
      final Collection<INormalizedToken> tokenCollection = normalizedTokenMap.get(key);
      if (tokenCollection.size() > 1) {
        repeatingNormalizedTokens.add(key);
      }
    }
    return repeatingNormalizedTokens;
  }

  @Override
  public ITokenIndex getTokenIndex(List<String> repeatedTokens) {
    return new WitnessIndex(this, repeatedTokens);
  }


  @Override
  public String toString() {
    return getSigil();
  }

  @Override
  public boolean isNear(IToken a, IToken b) {
    // sanity check!
    if (!(a instanceof WitnessToken)) {
      throw new RuntimeException("Token a is not a NormalizedToken!");
    }
    if (!(b instanceof WitnessToken)) {
      throw new RuntimeException("Token b is not a NormalizedToken!");
    }
    return ((WitnessToken)b).position - ((WitnessToken)a).position == 1;
  }

  @Override
  public Iterator<INormalizedToken> tokenIterator() {
    return iterator();
  }
}