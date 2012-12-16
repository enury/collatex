package eu.interedition.collatex.io;

import com.google.common.io.Closeables;
import eu.interedition.collatex.neo4j.Neo4jVariantGraph;
import eu.interedition.collatex.neo4j.Neo4jVariantGraphEdge;
import eu.interedition.collatex.neo4j.Neo4jVariantGraphTransposition;
import eu.interedition.collatex.neo4j.Neo4jVariantGraphVertex;
import org.neo4j.graphdb.Transaction;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Provider
@Produces(MediaType.TEXT_PLAIN)
public class VariantGraphDotMessageBodyWriter implements MessageBodyWriter<Neo4jVariantGraph> {

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return Neo4jVariantGraph.class.isAssignableFrom(type);
  }

  @Override
  public long getSize(Neo4jVariantGraph variantGraph, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(Neo4jVariantGraph graph, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
    final Transaction tx = graph.getDatabase().beginTx();
    try {
      final PrintWriter out = new PrintWriter(new OutputStreamWriter(entityStream, "UTF-8"));
      try {
        final String indent = "  ";
        final String connector = " -> ";

        out.println("digraph G {");

        for (Neo4jVariantGraphVertex v : graph.vertices()) {
          out.print(indent + "v" + v.getNode().getId());
          out.print(" [label = \"" + toLabel(v) + "\"]");
          out.println(";");
        }

        for (Neo4jVariantGraphEdge e : graph.edges()) {
          out.print(indent + "v" + e.from().getNode().getId() + connector + "v" + e.to().getNode().getId());
          out.print(" [label = \"" + toLabel(e) + "\"]");
          out.println(";");
        }

        for (Neo4jVariantGraphTransposition t : graph.transpositions()) {
          out.print(indent + "v" + t.from().getNode().getId() + connector + "v" + t.to().getNode().getId());
          out.print(" [color = \"lightgray\", style = \"dashed\" arrowhead = \"none\", arrowtail = \"none\" ]");
          out.println(";");
        }

        out.println("}");
      } finally {
        Closeables.close(out, false);
      }
    } finally {
      tx.finish();
    }
  }

  private String toLabel(Neo4jVariantGraphEdge e) {
    return Neo4jVariantGraphEdge.TO_CONTENTS.apply(e).replaceAll("\"", "\\\"");
  }

  private String toLabel(Neo4jVariantGraphVertex v) {
    return Neo4jVariantGraphVertex.TO_CONTENTS.apply(v).replaceAll("\"", "\\\"");
  }

}