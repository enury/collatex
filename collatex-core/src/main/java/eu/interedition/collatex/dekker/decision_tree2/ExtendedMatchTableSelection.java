package eu.interedition.collatex.dekker.decision_tree2;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.interedition.collatex.dekker.matrix.Island;
import eu.interedition.collatex.dekker.matrix.MatchTable;
import eu.interedition.collatex.dekker.matrix.MatchTableSelection;

/*
 * Extended MatchTableSelection class,
 * which allows the caller to select vectors from the MatchTable 
 * based on different requirements compared to the default MatchTableSelection
 * class, which always returns the largest vectors.
 * 
 * @author: Ronald Haentjens Dekker
 */
public class ExtendedMatchTableSelection extends MatchTableSelection {
  private Set<Island> possibleIslands;
  
  public ExtendedMatchTableSelection(MatchTable table) {
    super(table);
    this.possibleIslands = table.getIslands();
  }
  
  //NOTE: copy constructor
  public ExtendedMatchTableSelection(ExtendedMatchTableSelection orig) {
    super(orig);
    this.possibleIslands = Sets.newHashSet(orig.possibleIslands);
  }

  public DecisionTreeNode selectFirstVectorFromGraph() {
    Island i = getFirstVectorFromGraph();
    addIsland(i);
    return new DecisionTreeNode(this);
  }

  //TODO: check precondition (possibleIslands can be empty)
  public Island getFirstVectorFromGraph() {
    Comparator<Island> comp = new Comparator<Island> () {
      @Override
      public int compare(Island arg0, Island arg1) {
        // first sort on column
        // TODO: second sort on size
        return arg0.getLeftEnd().getColumn() - arg1.getLeftEnd().getColumn();
      }
    };
    List<Island> vectorsSortedOnGraphOrder = Lists.newArrayList(possibleIslands);
    Collections.sort(vectorsSortedOnGraphOrder, comp);
    return vectorsSortedOnGraphOrder.get(0);
  }
  
  @Override
  public void addIsland(Island isl) {
    possibleIslands.remove(isl);
    super.addIsland(isl);
  }

  public DecisionTreeNode selectFirstVectorFromWitness() {
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  public DecisionTreeNode skipFirstVectorFromGraph() {
    throw new UnsupportedOperationException("Not yet implemented!");
  }

  public DecisionTreeNode skipFirstVectorFromWitness() {
    throw new UnsupportedOperationException("Not yet implemented!");
  }
  

}
