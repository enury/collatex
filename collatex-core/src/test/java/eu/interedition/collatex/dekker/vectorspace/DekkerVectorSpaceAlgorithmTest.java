package eu.interedition.collatex.dekker.vectorspace;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import eu.interedition.collatex.AbstractTest;
import eu.interedition.collatex.Token;
import eu.interedition.collatex.VariantGraph;
import eu.interedition.collatex.dekker.vectorspace.VectorSpace.Vector;
import eu.interedition.collatex.jung.JungVariantGraph;
import eu.interedition.collatex.simple.SimpleToken;
import eu.interedition.collatex.simple.SimpleWitness;

public class DekkerVectorSpaceAlgorithmTest extends AbstractTest {

  private void assertPhrase(String expectedPhrase, List<Token> tokensFromVector) {
    assertEquals(expectedPhrase, SimpleToken.toString(tokensFromVector));
  }

  @Test
  public void testGetTokensFromVector() {
    SimpleWitness a = new SimpleWitness("A", "a b c x y z");
    SimpleWitness b = new SimpleWitness("B", "e a b c f g");
    DekkerVectorSpaceAlgorithm algo = new DekkerVectorSpaceAlgorithm();
    VectorSpace s = new VectorSpace();
    Vector v = s.new Vector(1, 2, 3);
    assertPhrase("a b c", algo.getTokensFromVector(v, 0, a));
    assertPhrase("a b c", algo.getTokensFromVector(v, 1, b));
  }
  
  @Test
  public void testCreationOfVGFromVectorSpace() {
    SimpleWitness a = new SimpleWitness("A", "a b c x y z");
    SimpleWitness b = new SimpleWitness("B", "e a b c f g");
    VariantGraph graph = new JungVariantGraph();
    DekkerVectorSpaceAlgorithm algo = new DekkerVectorSpaceAlgorithm();
    algo.collate(graph, a, b);
    // check the first witness
    VariantGraph.Vertex a1 = vertexWith(graph, "a", a);
    VariantGraph.Vertex a2 = vertexWith(graph, "b", a);
    VariantGraph.Vertex a3 = vertexWith(graph, "c", a);
    vertexWith(graph, "x", a);
    vertexWith(graph, "y", a);
    vertexWith(graph, "z", a);
    // check the second witness
    vertexWith(graph, "e", b);
    VariantGraph.Vertex b1 = vertexWith(graph, "a", b);
    VariantGraph.Vertex b2 = vertexWith(graph, "b", b);
    VariantGraph.Vertex b3 = vertexWith(graph, "c", b);
    vertexWith(graph, "f", b);
    vertexWith(graph, "g", b);
    // check alignment
    assertEquals(a1, b1);
    assertEquals(a2, b2);
    assertEquals(a3, b3);
  }
}