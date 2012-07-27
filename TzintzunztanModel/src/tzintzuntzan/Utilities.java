package tzintzuntzan;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import tzintzuntzan.Settings.Gender;
import tzintzuntzan.Settings.MaritalStatus;

public class Utilities {
	public static double calculateMedianCompadre(double numAdultsMarr, ArrayList<Person> adultsList) {
		int size = (int) numAdultsMarr;
		double[] data = new double[size];
		double med1 = 0, med2 = 0;
		double median = 0;
		int index = 0;
		for (Person adult : adultsList) {
			if (adult.getMaritalStatus().equals(MaritalStatus.SINGLE) == false
					&& adult.getMaritalStatus().equals(MaritalStatus.ELOPED) == false
					&& !adult.getCompadreList().isEmpty()) {
				data[index] = adult.getCompadreList().size();
				index++;
			}
		}
		double[] sortedData = sort(data);
		if (size % 2 == 0) {
			median = sortedData[size/2];
		}
		else {
			med1 = sortedData[size/2];
			med2 = sortedData[(size-1)/2];
			median = (med1+med2)/2;
		}
		return median;
	}
	
	public static double[] sort(double[] a) {
		QuickSort(a, 0, a.length-1);
		return a;
	}

	/**Quicksort  http://www.koders.com/java/fid18DC52F02D2E4B02E04E6CE32F297C028F485366.aspx */
	private static void QuickSort(double a[],int lo0, int hi0) {
		int lo = lo0;
		int hi = hi0;
		double mid;
		if ( hi0 > lo0) {
			//Arbitrarily establishing partition element as the midpoint of the array.			 
			mid = a[ ( lo0 + hi0 ) / 2 ];
			// loop through the array until indices cross
			while( lo <= hi ) {
				/* find the first element that is greater than or equal to
				 * the partition element starting from the left Index.
				 */
				while( ( lo < hi0 ) && ( a[lo] < mid ))  {
					++lo;
				}
				/* find an element that is smaller than or equal to
				 * the partition element starting from the right Index.
				 */
				while( ( hi > lo0 ) && ( a[hi] > mid )) {
					--hi;
				}
				// if the indexes have not crossed, swap
				if( lo <= hi ) {
					double temp = 0; 
					temp = a[lo];
					a[lo] = a[hi];
					a[hi] = temp;
					++lo;
					--hi;
				} 
			}
			/* If the right index has not reached the left side of array
			 * must now sort the left partition.
			 */
			if( lo0 < hi ) {
				QuickSort( a, lo0, hi );
			}
			/* If the left index has not reached the right side of array
			 * must now sort the right partition. */
			if( lo < hi0 ) {
				QuickSort( a, lo, hi0 );
			}
		}		
	}

	
	/**
	 * Union of two collections. source: Web
	 * @param collection1
	 * @param collection2
	 * @return
	 */
	public static Collection<Person> Union(Collection<Person> collection1, Collection<Person> collection2) {
		Set<Person> union = new HashSet<Person>(collection1);
		union.addAll(new HashSet<Person>(collection2));
		return new ArrayList<Person>(union);
	}
	
	public static void contractNetworkPajek(String prefix, ArrayList<Person> adultsList) {
		PrintWriter netRecorder;
		HashMap<Integer, ArrayList<Person>> contractNetwork = new  HashMap<Integer, ArrayList<Person>>();
		HashMap<Integer, Boolean> verticesMale = new HashMap<Integer, Boolean>();
		HashMap<Integer, Boolean> verticesFemale = new HashMap<Integer, Boolean>();
		ArrayList<Person> temp = new ArrayList<Person>();
		for (Person person : adultsList) {
			if (person != null
					&& person.isAlive()) {
				temp.add(person);
			}
		}
		for (Person person : temp) {
			contractNetwork.put(new Integer(person.getId()), 
					new ArrayList<Person>(person.getContractants()));
			if (person.getGender().equals(Gender.MALE)) {
				verticesMale.put(new Integer(person.getId()), true);
			}
			else {
				verticesFemale.put(new Integer(person.getId()), true);
			}			 
		}	
		try {
			netRecorder = new PrintWriter(new BufferedWriter(new FileWriter(""+ prefix + ".net")));
			HashMap<Integer, Integer> vertices = new HashMap<Integer, Integer>();
			int counter = 1;
			netRecorder.println("*Vertices " + (verticesMale.size() + verticesFemale.size()));		
			for (Iterator<Integer> it = verticesMale.keySet().iterator(); it.hasNext(); ){
				Integer id = it.next();
				netRecorder.println(counter + " p-" + id.intValue() + " ic " + "RoyalBlue");
				vertices.put(id, new Integer(counter));
				counter++;
			}
			for (Iterator<Integer> it = verticesFemale.keySet().iterator(); it.hasNext(); ){
				Integer id = it.next();
				netRecorder.println(counter + " p-" + id.intValue() + " ic " + "Dandelion");
				vertices.put(id, new Integer(counter));
				counter++;

			}
			netRecorder.println("*Edges");		 
			for (Iterator<Integer> it = contractNetwork.keySet().iterator(); it.hasNext(); ){
				Integer id = it.next();
				ArrayList<Person> partners = contractNetwork.get(id);
				for (Person p : partners) {
					if (p != null
							&& p.isAlive()
							&& vertices.containsKey(new Integer(p.getId()))) {
						netRecorder.println(vertices.get(id) + " " + vertices.get(new Integer(p.getId())));	
					}
				}				 				 
			}

			netRecorder.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}