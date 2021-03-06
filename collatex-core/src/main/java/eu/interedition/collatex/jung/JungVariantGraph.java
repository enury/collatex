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

package eu.interedition.collatex.jung;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import eu.interedition.collatex.Token;
import eu.interedition.collatex.VariantGraph;
import eu.interedition.collatex.Witness;
import eu.interedition.collatex.util.VariantGraphTraversal;

import java.util.Collections;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class JungVariantGraph extends DirectedSparseGraph<JungVariantGraphVertex, JungVariantGraphEdge> implements VariantGraph {

  final JungVariantGraphVertex start;
  final JungVariantGraphVertex end;
  final Multimap<VariantGraph.Vertex, VariantGraph.Transposition> transpositionIndex = HashMultimap.create();

  public JungVariantGraph() {
    super();
    addVertex(this.start = new JungVariantGraphVertex(this, Collections.<Token>emptySet()));
    addVertex(this.end = new JungVariantGraphVertex(this, Collections.<Token>emptySet()));
    connect(this.start, this.end, Collections.<Witness>emptySet());
  }

  @Override
  public Vertex getStart() {
    return start;
  }

  @Override
  public Vertex getEnd() {
    return end;
  }

  @Override
  public Set<Transposition> transpositions() {
    return Sets.newHashSet(transpositionIndex.values());
  }

  @Override
  public Iterable<Vertex> vertices() {
    return vertices(null);
  }

  @Override
  public Iterable<Vertex> vertices(Set<Witness> witnesses) {
    return VariantGraphTraversal.of(this, witnesses);
  }

  @Override
  public Iterable<Edge> edges() {
    return edges(null);
  }

  @Override
  public Iterable<Edge> edges(Set<Witness> witnesses) {
    return VariantGraphTraversal.of(this, witnesses).edges();
  }

  @Override
  public Vertex add(Token token) {
    final JungVariantGraphVertex vertex = new JungVariantGraphVertex(this, Collections.singleton(token));
    addVertex(vertex);
    return vertex;
  }

  @Override
  public Edge connect(Vertex from, Vertex to, Set<Witness> witnesses) {
    Preconditions.checkArgument(!from.equals(to));

    if (from.equals(start)) {
      final Edge startEndEdge = edgeBetween(start, end);
      if (startEndEdge != null) {
        if (to.equals(end)) {
          witnesses = Sets.newHashSet(witnesses);
          witnesses.addAll(startEndEdge.witnesses());
        }
        startEndEdge.delete();
      }
    }

    for (Edge e : from.outgoing()) {
      if (to.equals(e.to())) {
        return e.add(witnesses);
      }
    }

    final JungVariantGraphEdge edge = new JungVariantGraphEdge(this, witnesses);
    addEdge(edge, (JungVariantGraphVertex) from, (JungVariantGraphVertex) to);
    return edge;
  }

  @Override
  public Edge register(Witness witness) {
    return connect(start, end, Collections.singleton(witness));
  }

  @Override
  public Transposition transpose(Set<Vertex> vertices) {
    Preconditions.checkArgument(!vertices.isEmpty());
    for (Transposition transposition : vertices.iterator().next().transpositions()) {
      if (Sets.newHashSet(transposition).equals(vertices)) {
        return transposition;
      }
    }
    return new JungVariantGraphTransposition(this, vertices);
  }

  @Override
  public Edge edgeBetween(Vertex a, Vertex b) {
    return findEdge((JungVariantGraphVertex) a, (JungVariantGraphVertex) b);
  }

  @Override
  public Set<Witness> witnesses() {
    Set<Witness> witnesses = Sets.newHashSet();
    for (Edge edge : start.outgoing()) {
      witnesses.addAll(edge.witnesses());
    }
    return witnesses;
  }

  @Override
  public String toString() {
    return Iterables.toString(witnesses());
  }
}
