package tzintzuntzan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import tzintzuntzan.Settings.Gender;
import tzintzuntzan.Settings.MaritalStatus;
import tzintzuntzan.Settings.Profession;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.util.SimUtilities;

/**
 * 
 * @author shah
 *
 */

public class Household implements Drawable {
	/** Household ID */
	private int id = -1;
	/** Household head */
	private Person head = null;
	private TzintzuntzanModel model;
	/** List of household members */
	private ArrayList<Person> members = new ArrayList<Person>();
	/** List of IDs of neighbor households */
	private ArrayList<Integer> neighbors = new ArrayList<Integer>();
	/** List of IDs of relative households */
	private ArrayList<Integer> relativeHouseholds = new ArrayList<Integer>();
	/** X-Coordinate of the household's location on the grid*/
	private int xCoord = -1;
	/** Y-Coordinate of the household's location on the grid*/
	private int yCoord = -1;
	private NetworkNode node = null;
	/** Household profession. A son inherits his father's profession (assumption) */
	private Profession profession = Profession.OTHER;
	/** Time when a married couple decides to get a new nuclear household of their own*/
	private int timeBuildNuclearHousehold = -1;
	/** Married member of the household who builds a new nuclear household of their own*/
	private int memberBuildingNuclearHousehold = -1;
	/**if a married couple should build a household. */
	private boolean buildNewHousehold = false;
	/**Id of the male member of the household, who is recently eloped.
	 * We restrict a single male to be eloped at a time until they get married. It is an arbitrary assumption. 
	*/
	private int elopedMaleID = -1;
	private int elopedTick = -1;	
	/**Flag to check if the household is still viable or needs to be resolved.*/
	private boolean shouldDissolve = false;
	private Comparator<Person> ageComparator = new AgeComparator();

	/**
	 * Constructor for the Household class
	 * @param _id
	 * @param _model
	 */
	public Household(int _id, TzintzuntzanModel _model) {
		this.id = _id;
		this.model = _model;
	}

	/** Reset variables about the male member of the household who was eloped before */
	public void resetElopedMaleMember() {
		elopedMaleID = -1;
		elopedTick = -1;
	}

	/** Reset variables about the new nuclear households being created*/
	public void resetBuildingNuclearHousehold() {
		buildNewHousehold = false;
		memberBuildingNuclearHousehold = -1;
		timeBuildNuclearHousehold = -1;
	}

	/** Relocate children when without adult members to a randomly chosen relative household*/
	public void relocateMembers() {
		if (!members.isEmpty()
				&& !relativeHouseholds.isEmpty()) {
			Integer relativeHouseholdID = (Integer) relativeHouseholds.get(Uniform.staticNextIntFromTo(0, relativeHouseholds.size()-1));
			Household relativeHousehold = (Household) model.getHouseholdMap().get(relativeHouseholdID);
			if (relativeHousehold != null
					&& model.getHouseholdList().contains(relativeHousehold)) {
				ArrayList<Person> temp = new ArrayList<Person>(members);
				for (Person member : temp) {
					relativeHousehold.addMember(member);
				}				
				members.clear();
				temp = null;
			}
		}
	}

	/**
	 * Check if a married couple should build a household. 
	 * Set timing for that. 
	 * This is a strict model assumption, which need to be relaxed.
	 * @return
	 */
	public void update() {
		if (xCoord < 0 
				|| yCoord < 0
				|| xCoord >= model.getGridSizeX()
				|| yCoord >= model.getGridSizeY()) {
			System.err.println("Error - household coordinates: " + id);
		}
		if (members.isEmpty()
				|| (head != null
						&& !head.isAdult())
		) {
			relocateMembers();
			shouldDissolve = true;
			return;
		}				
		//check if to build a new nuclear household
		if (elopedMaleID != -1
				&& !buildNewHousehold
				&& elopedTick == model.getCurrentTick()
		) {			
			try{
				Person member = (Person) model.getPersonMap().get(elopedMaleID);
				if (member != null
						&& member.isAlive()) {
					for (Person sibling : member.getSiblings()) {
						if (sibling.getHousehold().getId() == id
								&& sibling.getGender().equals(Gender.MALE)
								&& sibling.getMaritalStatus().equals(MaritalStatus.MARRIED)
								&& sibling.isAlive()
								&& members.contains(sibling)									
						) {
							setHouseholdBuildTime(sibling.getId());
						}
					}
				}
			} catch (Exception e) {e.printStackTrace();}
		}
	}

	/**
	 * The male member of the household - i.e. the married son decides the time for his his new nuclear household in future.
	 * Contains arbitrary assumption - needs verification and calibration.
	 * @param male
	 */
	public void setHouseholdBuildTime(int maleID) {
		buildNewHousehold = true;
		memberBuildingNuclearHousehold = maleID;
		int timeLag = 0;
		do {
			timeLag = (int) Normal.staticNextDouble(72, 24);		
		} while (timeLag < 24 || timeLag > 120);
		timeBuildNuclearHousehold = model.getCurrentTick()+timeLag;
	}

	/**
	 * Add this member to the household
	 * @param member
	 */
	public void addMember(Person member) {
		if (!members.contains(member)) {
			members.add(member);			
		}		
		member.setHousehold(this);
	}

	/**
	 * Remove this member from the household
	 * @param member
	 */
	public void removeMember(Person member) {
		if (members.contains(member)) {
			members.remove(member);	
		}
		/* If member was a household head then find a new head for the household. */
		if (head.getId() == member.getId()) {			
			determineNewHead(member);
			member.setHouseholdHead(false);
		}
	}

	/**
	 * Decide if we want to find a house near the parents' household
	 * @return
	 */
	public boolean shouldNeighborParentalHousehold(int parentalHouseholdID) {
		/* Flag to decide if we want to find a house near the parents' household.*/ 
		boolean flag = false;
		try {
			if (head == null
					|| head.getSpouse() == null) {
				return false;
			}
			Person husband = (Person) head;
			Person wife = (Person) husband.getSpouse();
			String label = "is-similar";
			for (Person sibling : husband.getSiblings()) {				
				if (sibling.isAlive()
						&& sibling.getHousehold().getId() == parentalHouseholdID
						&& husband.similarityEndorsement(sibling) == label) {
					flag = true;
					break;				
				}									
			}
			if (!flag
					&& husband.getMotherID() != -1
					&& husband.getMother().isAlive()) {
				Person motherInLaw = (Person) husband.getMother();
				if (wife.similarityEndorsement(motherInLaw) == label) {
					flag = true;
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return flag;
	}

	/**
	 * Add parents' kin households to this new household.
	 * We 'endorse' wife's household as relative at the time of marriage. 
	 */
	public void updateKinshipLinks(ArrayList<Integer> kinHouseholds) {
		try {
			Integer ID = new Integer(this.id);
			for (Integer kinHouseholdID : kinHouseholds) {
				Household kinHousehold = (Household) model.getHouseholdMap().get(kinHouseholdID);
				if (kinHousehold != null
						&& model.getHouseholdList().contains(kinHousehold)) {
					kinHousehold.getRelativeHouseholds().add(ID);
					relativeHouseholds.add(kinHouseholdID);										
				}
			}
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * Find new neighbors and endorse them. Also, clear off previous endorsements for neighbors. 
	 * Called when a new nuclear household is created.
	 */
	public void updateNeighborLinks(ArrayList<Integer> prevNeighborIDs) {
		String label = "is-neighbor";
		int index = model.returnStaticEndorsementIndex(label);
		for (Integer prevNeighborHHID : prevNeighborIDs) {
			try {
				Household previousNeighborHH = (Household) model.getHouseholdMap().get(prevNeighborHHID);
				if (previousNeighborHH != null
						&& model.getHouseholdList().contains(prevNeighborHHID)) {
					for (Person member : members) {
						for (Person prevNeighbor : previousNeighborHH.getMembers()) {
							if (member.getEndorsementsRecord().containsKey(prevNeighbor.getId())) {
								member.getEndorsementsRecord().get(prevNeighbor.getId()).removeStaticEndorsement(index);
							}
							if (prevNeighbor.getEndorsementsRecord().containsKey(member.getId())) {
								prevNeighbor.getEndorsementsRecord().get(member.getId()).removeStaticEndorsement(index);
							}																																						
						}
					}
				}
			} catch (Exception e) {e.printStackTrace();}					
		}
		Integer ID = new Integer(this.id);
		for (Household household : model.getHouseholdList()){
			if (this.id != household.getId()
					&& household != null){
				double distance =
					Math.sqrt((getX() - household.getX()) * (getX() - household.getX())
							+ (getY() - household.getY()) * (getY() - household.getY()));
				if (distance <= model.getNeighbourhoodRadius()){
					try {
						Integer hhID = new Integer(household.getId());
						if (!neighbors.contains(hhID)
								&& !household.getNeighbors().contains(ID)) {
							neighbors.add(hhID);
							household.getNeighbors().add(ID);
							model.householdsMutualEndorsments(this, household, label);
						}					
					} catch (Exception e) {e.printStackTrace();}
				}
			}
		}
	}

	/**
	 * Locate a neighborhood for this household and see if we can find a location. Returns true if successful; false, otherwise.
	 * Look at the xExt and yExt as extensions to the Moore Neighborhood. Adapted from Repast 3.1 implementation. 
	 * @param household
	 */
	public boolean findNeighborhoodOf(Household household) {
		boolean flag = false;
		try {
			ArrayList<int[]> vacantPositions = model
			.returnVacantNeighborhoodLocations(household.getXCoord(), household.getYCoord());
			if (!vacantPositions.isEmpty()) {
				int size = vacantPositions.size();
				SimUtilities.shuffle(vacantPositions);
				for (int i=0; i<size; i++) {
					int[] location = vacantPositions.get(i);
					if (location[0] >= 0 
							&& location[1] >= 0
							&& location[0] < model.getGridSizeX()
							&& location[1] < model.getGridSizeY()
							&& model.getSpace().getObjectAt(location[0], location[1]) == null) {
						setXY(location[0], location[1]);
						model.getSpace().putObjectAt(location[0], location[1], this);
						flag = true;
						break;
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return flag;
	}

	/** Clear neighbor and relative links with relevant households. Called at the time of dissolution. */
	public void clearAll() {
		Integer ID = new Integer(this.id);
		for (Integer neighborID : neighbors) {
			Household neighbor = (Household) model.getHouseholdMap().get(neighborID);			
			if (neighbor != null
					&& neighbor.getNeighbors().contains(ID)) {
				neighbor.getNeighbors().remove(ID);
			}					
		}
		for (Integer kinID : relativeHouseholds) {
			Household kinHH = (Household) model.getHouseholdMap().get(kinID);
			if (kinHH != null
					&& kinHH.getRelativeHouseholds().contains(ID)){
				kinHH.getRelativeHouseholds().remove(ID);				
			}			
		}
		neighbors.clear();
		relativeHouseholds.clear();
		members.clear();
	}

	/**
	 * We endorse household members that are close relatives 'is-relative' to one another.
	 * Called when the model is built.  
	 */
	public void endorseCloseRelatives() {
		String label = "";
		for (Person m1 : members) {
			label = "";
			for (Person m2 : members) {
				if (m1.getId() != m2.getId()
						&& !m1.isNuclearRelation(m2)) {
					label = "is-relative";
				}
				if (m1.getSiblings().contains(m2)) {
					label = "is-sibling";
				}
				if (label != ""
					&& m1 != null && m2 != null
					&& m1.isAlive() && m2.isAlive()) {
					m1.endorsePerson(m2, label);
					m2.endorsePerson(m1, label);					
				}
			}
		}
	}

	/**
	 * Determine new head of the household
	 * @param formerHead
	 */
	public void determineNewHead(Person formerHead) {
		if (!members.isEmpty()) {
			if (formerHead.getMaritalStatus().equals(MaritalStatus.MARRIED)
				&& formerHead.getSpouse() != null
				&& formerHead.getSpouse().isAlive()) {
				setHead(formerHead.getSpouse());
			}
			else {
				Collections.sort(members, ageComparator);
				Person eldest = members.get(0);
				for (Person offspring : formerHead.getOffsprings()) {
					if (offspring != null
							&& offspring.isAlive()
							&& offspring.isAdult()
							&& offspring.getHousehold().getId() == this.id
							&& offspring.getAge() >= eldest.getAge()) {
						eldest = offspring;	
					}
				}
				setHead(eldest);
			}			
		}
	}		
	
	class AgeComparator implements Comparator<Person> {
		public int compare(Person person1, Person person2) {
			int e1 = 0, e2 = 0;
			e1 = person1.getAge();
			e2 = person2.getAge();
			if (e1 > e2) {
				return -1;
			}
			else if (e1 < e2) {
				return 1;
			}
			else {
				return 0;
			}
		}
	}

	/** Find the eldest surviving member in the household as the new head. Not called by default in the code. */
	public void determineNewHead() {
		if (!members.isEmpty()) {
			Person eldest = members.get(0);
			for (Person person : members) {
				if (person.getAge() > eldest.getAge()) {
					eldest = person;
				}
			}
			setHead(eldest);
		}
	}

	/** Returns a list of households who are identified as neighbors */
	public ArrayList<Household> returnNeighborHouseholds() {
		ArrayList<Household> households = new ArrayList<Household>();
		try {
			for (Integer householdID : neighbors) {
				if (model.getHouseholdMap().containsKey(householdID)) {
					Household household = (Household) model.getHouseholdMap().get(householdID);
					if (!households.contains(household)) {
						households.add(household);
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return households;
	}

	/** Returns a list of households who are identified as extended family */
	public ArrayList<Household> returnRelativesHouseholds() {
		ArrayList<Household> households = new ArrayList<Household>();
		try {
			for (Integer householdID : relativeHouseholds) {
				if (model.getHouseholdMap().containsKey(householdID)) {
					Household household = (Household) model.getHouseholdMap().get(householdID);
					if (!households.contains(household)) {
						households.add(household);
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return households;		
	}

	public void setNode(NetworkNode _node){
		this.node = _node;
		this.node.setId(this.id);
	}

	public void draw(SimGraphics g){	
		g.drawFastCircle(java.awt.Color.black);
	}

	public int getXCoord() {
		return xCoord;
	}

	public void setXCoord(int coord) {
		xCoord = coord;
	}

	public int getYCoord() {
		return yCoord;
	}

	public void setYCoord(int coord) {
		yCoord = coord;
	}

	public int getX(){
		return this.xCoord;
	}

	public int getY(){
		return this.yCoord;
	}

	public void setX(int x){
		this.xCoord = x;
	}

	public void setY(int y){
		this.yCoord = y;
	}

	public void setXY(int x, int y){
		setXCoord(x);
		setYCoord(y);
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Person getHead() {
		return head;
	}

	public void setHead(Person head) {
		this.head = head;
	}

	public TzintzuntzanModel getModel() {
		return model;
	}

	public void setModel(TzintzuntzanModel model) {
		this.model = model;
	}

	public ArrayList<Person> getMembers() {
		return members;
	}

	public void setMembers(ArrayList<Person> members) {
		this.members = members;
	}

	public NetworkNode getNode() {
		return node;
	}

	public ArrayList<Integer> getNeighbors() {
		return neighbors;
	}

	public void setNeighbors(ArrayList<Integer> neighbors) {
		this.neighbors = neighbors;
	}

	public ArrayList<Integer> getRelativeHouseholds() {
		return relativeHouseholds;
	}

	public void setRelativeHouseholds(ArrayList<Integer> relativeHouseholds) {
		this.relativeHouseholds = relativeHouseholds;
	}

	public Profession getProfession() {
		return profession;
	}

	public void setProfession(Profession profession) {
		this.profession = profession;
	}

	public void setNeighborHouseholds(ArrayList<Integer> neighborHouseholds) {
		this.neighbors = neighborHouseholds;
	}

	public int getTimeBuildNuclearHousehold() {
		return timeBuildNuclearHousehold;
	}

	public void setTimeBuildNuclearHousehold(int timeBuildNuclearHousehold) {
		this.timeBuildNuclearHousehold = timeBuildNuclearHousehold;
	}

	public boolean isBuildNewHousehold() {
		return buildNewHousehold;
	}

	public void setBuildNewHousehold(boolean buildNewHousehold) {
		this.buildNewHousehold = buildNewHousehold;
	}

	public int getElopedMaleID() {
		return elopedMaleID;
	}

	public void setElopedMaleID(int elopedMaleID) {
		this.elopedMaleID = elopedMaleID;
		elopedTick = model.getCurrentTick();
	}

	public int getMemberBuildingNuclearHousehold() {
		return memberBuildingNuclearHousehold;
	}

	public void setMemberBuildingNuclearHousehold(int memberBuildingNuclearHousehold) {
		this.memberBuildingNuclearHousehold = memberBuildingNuclearHousehold;
	}

	public boolean isShouldDissolve() {
		return shouldDissolve;
	}

	public void setShouldDissolve(boolean shouldDissolve) {
		this.shouldDissolve = shouldDissolve;
	}
}