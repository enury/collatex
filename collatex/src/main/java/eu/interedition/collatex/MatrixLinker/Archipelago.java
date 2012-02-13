package eu.interedition.collatex.MatrixLinker;

import java.util.ArrayList;

public class Archipelago {

	protected ArrayList<DirectedIsland> islands;

	public Archipelago() {
		islands = new ArrayList<DirectedIsland>();
	}

	public Archipelago(DirectedIsland isl) {
		islands = new ArrayList<DirectedIsland>();
		islands.add(isl);
  }
	public void add(DirectedIsland island) {
		for(Island i : islands) {
			if(island.size()>i.size()) {
				islands.add(islands.indexOf(i), island);
				return;
			} else
				try {
					DirectedIsland disl = (DirectedIsland) island;
					DirectedIsland di   = (DirectedIsland) i;
				if(island.size()>i.size() && disl.direction()>di.direction()) {
					islands.add(islands.indexOf(i), island);
					return;
				}
				} catch (Exception e) {
				}
		}
		islands.add(island);
  }

	// this is not a real iterator implementation but it works...
  public ArrayList<DirectedIsland> iterator() {
	  return islands;
  }


	protected void remove(int i) {
		islands.remove(i);
  }

	public int size() {
	  return islands.size();
  }

	public void mergeIslands() {
		int i=0;
		int j=1;
		int[] rr = new int[size()];
		for(i=0; i<size(); i++) {
			for(j=i+1; j<size(); j++) {
				if(islands.get(i).overlap(islands.get(j))) {
					(islands.get(i)).merge(islands.get(j));
					islands.get(j).clear();
					rr[j] = 1;
				}
			}
		}
		for(i=(rr.length-1); i>0; i--) {
			if(rr[i]==1)
			  islands.remove(i);
		}
  }

	public Object numOfConflicts() {
		int result = 0;
		int num = islands.size();
		for(int i=0; i<num; i++)
			for(int j=i+1; j<num; j++) {
//				System.out.println("compare "+islands.get(j)+" with "+islands.get(i));				
				if(islands.get(j).isCompetitor(islands.get(i)))
					result++;
			}
	  return result;
  }
	
	public Island get(int i) {
		return islands.get(i);
	}

	public Archipelago copy() {
		Archipelago result = new Archipelago();
		for(DirectedIsland isl: islands) {
			result.add((DirectedIsland) isl.copy());
		}
	  return result;
  }

	public boolean conflictsWith(Island island) {
		for(Island isl : islands) {
			if(isl.isCompetitor(island))
				return true;
		}
	  return false;
  }

	public String toString() {
		String result = "";
		for(Island island : islands) {
		  if(result.isEmpty())
		  	result = "[ " + island;
		  else
		  	result += ", " + island;
		}
		result += " ]";
		return result;
	}

}
