package tzintzuntzan;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author shah
 *
 */

public class Endorsements {
	private int numStaticEndorsements = 0;	
	private boolean[] staticEndorsements;
	/** Implemented as a dynamic ArrayList of strings. */ 
	private HashMap<Integer, ArrayList<Integer>> dynamicEndorsements = new HashMap<Integer, ArrayList<Integer>>();
	
	/**
	 * Constructor for Endorsements class
	 * @param _numStaticEndorsments
	 */
	public Endorsements(int _numStaticEndorsments) {
		numStaticEndorsements = _numStaticEndorsments;		
		staticEndorsements = new boolean[numStaticEndorsements];
		for (int i=0; i<numStaticEndorsements; i++) {
			staticEndorsements[i] = false;
		}				
	}	
	
	/**
	 * Toggle the static endorsement as true
	 * @param index
	 */
	public void setStaticEndorsement(int index) {
		staticEndorsements[index] = true;
	}
	
	/**
	 * Sets the static endorsement for the given index as false 
	 * @param index
	 */
	public void removeStaticEndorsement(int index) {
		staticEndorsements[index] = false;
	}
	
	public void setDynamicEndorsement(int timestep, Integer index) {
		try {
			if (!dynamicEndorsements.containsKey(index)) {
				dynamicEndorsements.put(index, new ArrayList<Integer>());
			}
			if (!dynamicEndorsements.get(index).contains(timestep)) {
				dynamicEndorsements.get(index).add(new Integer(timestep));
			}			
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public void setDynamicEndorsementAll(int timestep, ArrayList<Integer> indices) {
		for (Integer index : indices) {
			setDynamicEndorsement(timestep, index);
		}
	}
	
	public boolean[] getStaticEndorsements() {
		return staticEndorsements;
	}

	public void setStaticEndorsements(boolean[] staticEndorsements) {
		this.staticEndorsements = staticEndorsements;
	}

	public HashMap<Integer, ArrayList<Integer>> getDynamicEndorsements() {
		return dynamicEndorsements;
	}

	public void setDynamicEndorsements(HashMap<Integer, ArrayList<Integer>> dynamicEndorsements) {
		this.dynamicEndorsements = dynamicEndorsements;
	}
}