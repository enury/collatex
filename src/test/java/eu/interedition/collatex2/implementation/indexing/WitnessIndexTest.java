package eu.interedition.collatex2.implementation.indexing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eu.interedition.collatex2.implementation.Factory;
import eu.interedition.collatex2.interfaces.IWitness;

public class WitnessIndexTest {
  private Factory factory;

  @Before
  public void setup() {
    factory = new Factory();
  }

  // Note: there are more combinations valid!
  @Ignore
  @Test
  public void test() {
    final IWitness witnessA = factory.createWitness("A", "the big black cat and the big black rat");
    final WitnessIndex index = new WitnessIndex(witnessA);
    assertTrue(index.contains("# the big black"));
    assertTrue(index.contains("the big black cat"));
    assertTrue(index.contains("and"));
    assertTrue(index.contains("and the big black"));
    assertTrue(index.contains("the big black rat"));
    assertEquals(5, index.size());
  }

}