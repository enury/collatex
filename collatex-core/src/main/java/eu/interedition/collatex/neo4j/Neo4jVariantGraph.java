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

package eu.interedition.collatex.neo4j;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import eu.interedition.collatex.Token;
import eu.interedition.collatex.VariantGraph;
import eu.interedition.collatex.Witness;
import eu.interedition.collatex.util.VariantGraphTraversal;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static eu.interedition.collatex.neo4j.Neo4jGraphRelationships.PATH;
import static java.util.Collections.singleton;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class Neo4jVariantGraph implements VariantGraph {
  private static final Logger LOG = Logger.getLogger(Neo4jVariantGraph.class.getName());

  final GraphDatabaseService database;
  final Neo4jVariantGraphAdapter adapter;

  final Neo4jVariantGraphVertex start;
  final Neo4jVariantGraphVertex end;

  public Neo4jVariantGraph(GraphDatabaseService database, Neo4jVariantGraphAdapter adapter) {
    this(database, database.createNode(), database.createNode(), adapter);
    connect(start, end, Collections.<Witness>emptySet());
  }

  public Neo4jVariantGraph(GraphDatabaseService database, Node start, Node end, Neo4jVariantGraphAdapter adapter) {
    this.database = database;
    this.adapter = adapter;
    this.start = (Neo4jVariantGraphVertex) vertexWrapper.apply(start);
    this.end = (Neo4jVariantGraphVertex) vertexWrapper.apply(end);
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
    final Set<Transposition> transpositions = Sets.newHashSet();
    for (Vertex v : vertices()) {
      Iterables.addAll(transpositions, v.transpositions());
    }
    return transpositions;
  }

  @Override
  public Iterable<Vertex> vertices() {
    return vertices(null);
  }

  @Override
  public Iterable<Vertex> vertices(final Set<Witness> witnesses) {
    return VariantGraphTraversal.of(this, witnesses);
  }

  @Override
  public Iterable<Edge> edges() {
    return edges(null);
  }

  @Override
  public Iterable<Edge> edges(final Set<Witness> witnesses) {
    return VariantGraphTraversal.of(this, witnesses).edges();
  }

  @Override
  public Neo4jVariantGraphVertex add(Token token) {
    if (LOG.isLoggable(Level.FINER)) {
      LOG.log(Level.FINER, "Creating new vertex with {0}", token);
    }
    return new Neo4jVariantGraphVertex(this, singleton(token));
  }

  @Override
  public Edge connect(VariantGraph.Vertex from, VariantGraph.Vertex to, Set<Witness> witnesses) {
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
    return new Neo4jVariantGraphEdge(this, (Neo4jVariantGraphVertex) from, (Neo4jVariantGraphVertex) to, witnesses);
  }

  @Override
  public Edge register(Witness witness) {
    return connect(start, end, Collections.singleton(witness));
  }

  @Override
  public Transposition transpose(Set<VariantGraph.Vertex> vertices) {
    Preconditions.checkArgument(!vertices.isEmpty());
    for (Transposition transposition : vertices.iterator().next().transpositions()) {
      if (Sets.newHashSet(transposition).equals(vertices)) {
        return transposition;
      }
    }
    return new Neo4jVariantGraphTransposition(this, vertices);
  }

  @Override
  public Edge edgeBetween(Vertex a, Vertex b) {
    final Node aNode = ((Neo4jVariantGraphVertex)a).getNode();
    final Node bNode = ((Neo4jVariantGraphVertex)b).getNode();
    for (Relationship r : aNode.getRelationships(PATH)) {
      if (r.getOtherNode(aNode).equals(bNode)) {
        return new Neo4jVariantGraphEdge(this, r);
      }
    }
    return null;
  }

  @Override
  public Set<Witness> witnesses() {
    final Set<Witness> witnesses = Sets.newHashSet();
    for (Edge e : start.outgoing()) {
      witnesses.addAll(e.witnesses());
    }
    return witnesses;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj != null && obj instanceof Neo4jVariantGraph) {
      return start.equals(((Neo4jVariantGraph) obj).start);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return start.hashCode();
  }

  @Override
  public String toString() {
    return Iterables.toString(witnesses());
  }

  final Function<Node, Vertex> vertexWrapper = new Function<Node, VariantGraph.Vertex>() {
    @Override
    public VariantGraph.Vertex apply(Node input) {
      return new Neo4jVariantGraphVertex(Neo4jVariantGraph.this, input);
    }
  };

  final Function<Relationship, Edge> edgeWrapper = new Function<Relationship, VariantGraph.Edge>() {
    @Override
    public VariantGraph.Edge apply(Relationship input) {
      return new Neo4jVariantGraphEdge(Neo4jVariantGraph.this, input);
    }
  };

  final Function<Node, Transposition> transpositionWrapper = new Function<Node, VariantGraph.Transposition>() {
    @Override
    public VariantGraph.Transposition apply(Node input) {
      return new Neo4jVariantGraphTransposition(Neo4jVariantGraph.this, input);
    }
  };
}
