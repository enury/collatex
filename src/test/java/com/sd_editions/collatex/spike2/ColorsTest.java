package com.sd_editions.collatex.spike2;

import junit.framework.TestCase;

import com.google.common.collect.Sets;

public class ColorsTest extends TestCase {
  public void testVerySimple() {
    String[] witnesses = new String[] { "very simple", "simple indeed" };
    Colors colors = new Colors(witnesses);
    assertEquals(3, colors.numberOfColors());
  }

  @SuppressWarnings("boxing")
  public void testFirstUseCasePeter() {
    String[] witnesses = new String[] { "The black cat", "The black and white cat", "The black and green cat" };
    Colors colors = new Colors(witnesses);
    assertEquals(6, colors.numberOfColors());
    assertEquals(Sets.newHashSet(1, 2, 3), colors.getColorsPerWitness(1));
    assertEquals(Sets.newHashSet(1, 2, 4, 5, 3), colors.getColorsPerWitness(2));
    assertEquals(Sets.newHashSet(1, 2, 4, 6, 3), colors.getColorsPerWitness(3));
  }

  //  public void testColors() {
  //    String[] witnesses = new String[] { "A black cat.", "A black dog", "One white dog" };
  //    Colors colors = new Colors(witnesses);
  //  }
  //  //  public final void testGetColorMatrixPermutations1() {
  //  //    //    Set<ColorMatrix> colorMatrixPermutations = makePermutations(new String[] { "A black cat.", "A black dog", "One white dog" });
  //  //    //    assertEquals(1, colorMatrixPermutations.size());
  //  //    //    ColorMatrix cm1 = new ColorMatrix(new int[][] { { 1, 2, 3 }, { 1, 2, 4 }, { 5, 6, 4 } });
  //  //    //    assertEquals(cm1, colorMatrixPermutations.iterator().next());
  //  //    //  }

}
