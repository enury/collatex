/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.collatex.simple;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import eu.interedition.collatex.Token;
import eu.interedition.collatex.Witness;
import eu.interedition.collatex.util.VertexMatch;

import javax.annotation.Nullable;
import java.util.SortedSet;

public class SimpleToken implements Token, Comparable<SimpleToken> {
  private final SimpleWitness witness;
  private final String content;
  private final String normalized;

  public SimpleToken(SimpleWitness witness, String content, String normalized) {
    this.witness = witness;
    this.content = content;
    this.normalized = normalized;
  }

  public String getContent() {
    return content;
  }

  @Override
  public Witness getWitness() {
    return witness;
  }

  public String getNormalized() {
    return normalized;
  }

  @Override
  public String toString() {
    return new StringBuilder(witness.toString()).append(":").append(witness.getTokens().indexOf(this)).append(":'").append(normalized).append("'").toString();
  }

  public static String toString(Iterable<? extends Token> tokens) {
    final StringBuilder normalized = new StringBuilder();
    for (SimpleToken token : Iterables.filter(tokens, SimpleToken.class)) {
      normalized.append(token.getContent());
    }
    return normalized.toString().trim();
  }

  @Override
  public int compareTo(SimpleToken o) {
    return witness.compare(this, o);
  }

  public static final Function<SortedSet<VertexMatch.WithToken>, Integer> TOKEN_MATCH_EVALUATOR = new Function<SortedSet<VertexMatch.WithToken>, Integer>() {
    @Override
    public Integer apply(@Nullable SortedSet<VertexMatch.WithToken> input) {
      int value = 0;
      for (VertexMatch.WithToken match : input) {
        value += ((SimpleToken) match.token).getContent().length();
      }
      return value;
    }
  };
}
