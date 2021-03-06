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

package eu.interedition.collatex;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.RowSortedTable;
import com.google.common.collect.Sets;
import eu.interedition.collatex.dekker.DekkerAlgorithm;
import eu.interedition.collatex.dekker.Match;
import eu.interedition.collatex.jung.JungVariantGraph;
import eu.interedition.collatex.matching.EqualityTokenComparator;
import eu.interedition.collatex.simple.SimpleToken;
import eu.interedition.collatex.simple.SimpleWitness;
import eu.interedition.collatex.util.VariantGraphRanking;
import org.junit.Assert;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Logger;

import static eu.interedition.collatex.dekker.Match.PHRASE_MATCH_TO_TOKENS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public abstract class AbstractTest {
  public static final char[] SIGLA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

  protected final Logger LOG = Logger.getLogger(getClass().getName());

  protected CollationAlgorithm collationAlgorithm;

  @Before
  public void initAlgorithm() {
    collationAlgorithm = CollationAlgorithmFactory.dekkerMatchMatrix(new EqualityTokenComparator(), 2);
  }

  protected SimpleWitness[] createWitnesses(String... contents) {
    Assert.assertTrue("Not enough sigla", contents.length <= SIGLA.length);
    final SimpleWitness[] witnesses = new SimpleWitness[contents.length];
    for (int wc = 0; wc < contents.length; wc++) {
      witnesses[wc] = new SimpleWitness(Character.toString(SIGLA[wc]), contents[wc]);
    }
    return witnesses;
  }

  protected VariantGraph collate(SimpleWitness... witnesses) {
    final VariantGraph graph = new JungVariantGraph();
    collate(graph, witnesses);
    return graph;
  }

  protected void collate(VariantGraph graph, SimpleWitness... witnesses) {
    collationAlgorithm.collate(graph, witnesses);
  }

  protected VariantGraph collate(String... witnesses) {
    return collate(createWitnesses(witnesses));
  }

  protected static RowSortedTable<Integer, Witness, Set<Token>> table(VariantGraph graph) {
    return VariantGraphRanking.of(graph).asTable();
  }

  protected static SortedSet<String> extractPhrases(VariantGraph graph, Witness witness) {
    return extractPhrases(Sets.<String> newTreeSet(), graph, witness);
  }

  protected static SortedSet<String> extractPhrases(SortedSet<String> phrases, VariantGraph graph, Witness witness) {
    for (VariantGraph.Vertex v : graph.vertices(Collections.singleton(witness))) {
      phrases.add(toString(v, witness));
    }
    return phrases;
  }

  protected static String toString(VariantGraph.Vertex vertex, Witness... witnesses) {
    final Multimap<Witness, Token> tokens = Multimaps.index(vertex.tokens(Sets.newHashSet(Arrays.asList(witnesses))), Token.TO_WITNESS);
    List<String> tokenContents = Lists.newArrayListWithExpectedSize(tokens.size());
    for (Witness witness : Ordering.from(Witness.SIGIL_COMPARATOR).sortedCopy(tokens.keySet())) {
      for (Token token : Ordering.natural().sortedCopy(Iterables.filter(tokens.get(witness), SimpleToken.class))) {
        tokenContents.add(((SimpleToken) token).getNormalized());
      }
    }
    return Joiner.on(' ').join(tokenContents);
  }

  protected static void assertHasWitnesses(VariantGraph.Edge edge, Witness... witnesses) {
    assertEquals(Sets.newHashSet(Arrays.asList(witnesses)), edge.witnesses());
  }

  protected static VariantGraph.Edge edgeBetween(VariantGraph.Vertex start, VariantGraph.Vertex end) {
    final VariantGraph.Edge edge = start.graph().edgeBetween(start, end);
    Assert.assertNotNull(String.format("No edge between %s and %s", start, end), edge);
    return edge;
  }

  protected static void assertVertexEquals(String expected, VariantGraph.Vertex vertex) {
    assertEquals(expected, ((SimpleToken) Iterables.getFirst(vertex.tokens(), null)).getNormalized());
  }

  protected static void assertTokenEquals(String expected, Token token) {
    assertEquals(expected, ((SimpleToken) token).getContent());
  }

  protected static void assertVertexHasContent(VariantGraph.Vertex vertex, String content, Witness in) {
    Assert.assertEquals(String.format("%s does not has expected content for %s", vertex, in), content, toString(vertex, in));
  }

  protected static VariantGraph.Vertex vertexWith(VariantGraph graph, String content, Witness in) {
    for (VariantGraph.Vertex v : graph.vertices(Collections.singleton(in))) {
      if (content.equals(toString(v, in))) {
        return v;
      }
    }
    fail(String.format("No vertex with content '%s' in witness %s", content, in));
    return null;
  }

  protected static String toString(RowSortedTable<Integer, Witness, Set<Token>> table) {
    final StringBuilder tableStr = new StringBuilder();
    for (Witness witness : table.columnKeySet()) {
      tableStr.append(witness.getSigil()).append(": ").append(toString(table, witness)).append("\n");
    }
    return tableStr.toString();
  }

  protected static String toString(RowSortedTable<Integer, Witness, Set<Token>> table, Witness witness) {
    final StringBuilder tableRowStr = new StringBuilder("|");
    for (Integer row : table.rowKeySet()) {
      final Set<Token> tokens = table.get(row, witness);
      if (tokens == null) {
        tableRowStr.append(" |");
      } else {
        final List<SimpleToken> simpleTokens = Ordering.natural().sortedCopy(Iterables.filter(tokens, SimpleToken.class));
        tableRowStr.append(Joiner.on(" ").join(Iterables.transform(simpleTokens, new Function<Token, String>() {
          @Override
          public String apply(Token input) {
            return ((SimpleToken) input).getNormalized();
          }
        }))).append("|");
      }
    }
    return tableRowStr.toString();
  }

  protected void assertPhraseMatches(String... expectedPhrases) {
    List<List<Match>> phraseMatches = ((DekkerAlgorithm) collationAlgorithm).getPhraseMatches();
    int i = 0;
    for (List<Match> phraseMatch : phraseMatches) {
      Assert.assertEquals(expectedPhrases[i], SimpleToken.toString(PHRASE_MATCH_TO_TOKENS.apply(phraseMatch)));
      i++;
    }
  }

  protected void setCollationAlgorithm(CollationAlgorithm collationAlgorithm) {
    this.collationAlgorithm = collationAlgorithm;
  }

}
