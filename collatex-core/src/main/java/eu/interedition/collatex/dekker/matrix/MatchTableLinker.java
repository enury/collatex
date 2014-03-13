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

package eu.interedition.collatex.dekker.matrix;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import eu.interedition.collatex.Token;
import eu.interedition.collatex.VariantGraph;
import eu.interedition.collatex.dekker.Match;
import eu.interedition.collatex.dekker.TokenLinker;

public class MatchTableLinker implements TokenLinker {
	static Logger LOG = Logger.getLogger(MatchTableLinker.class.getName());
  private final int outlierTranspositionsSizeLimit;

  public MatchTableLinker(int outlierTranspositionsSizeLimit) {
    super();
    this.outlierTranspositionsSizeLimit = outlierTranspositionsSizeLimit;
  }

  @Override
  public Map<Token, VariantGraph.Vertex> link(VariantGraph base, Iterable<Token> witness, Comparator<Token> comparator) {
    // create MatchTable and fill it with matches
    LOG.finer("create MatchTable and fill it with matches");
    MatchTable table = MatchTable.create(base, witness, comparator);
    
    if (LOG.isLoggable(Level.FINE)) {
      // skip the merging of the first witness
      if (!base.witnesses().isEmpty()) {
        // transform islands into a list of Matches
        List<List<Match>> potentialPhraseMatches = Lists.newArrayList();
        for (Island i : table.getIslands()) {
          List<Match> phrase = Lists.newArrayList();
          for (Coordinate c : i) {
            Match match = new Match(table.vertexAt(c.row, c.column), table.tokenAt(c.row, c.column));
            phrase.add(match);
          }
          potentialPhraseMatches.add(phrase);
        }
        LOG.log(Level.FINE, "Potential matches: {0}", Iterables.toString(potentialPhraseMatches));
      }
    }
    
    // create IslandConflictResolver
    LOG.finer("create island conflict resolver");
	  IslandConflictResolver resolver = new IslandConflictResolver(table, outlierTranspositionsSizeLimit);
	
	  // The IslandConflictResolver createNonConflictingVersion() method
	  // selects the optimal islands
	  LOG.finer("select the optimal islands");
	  MatchTableSelection preferredIslands = resolver.createNonConflictingVersion();
	  if (LOG.isLoggable(Level.FINEST)) {
	    LOG.log(Level.FINEST, "Number of preferred Islands: {0}", preferredIslands.size());
	  }
	
	  // Here the result is put in a map
	  Map<Token, VariantGraph.Vertex> map = Maps.newHashMap();
	  for (Island island : preferredIslands.getIslands()) {
	    for (Coordinate c : island) {
	      map.put(table.tokenAt(c.row, c.column), table.vertexAt(c.row, c.column));
	    }
	  }
	  return map;
  }
}
