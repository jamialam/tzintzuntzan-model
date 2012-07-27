package tzintzuntzan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import flanagan.math.PsRandom;
import tzintzuntzan.Settings.EndorsementEvaluation;
import tzintzuntzan.Settings.EndorsementWeights;
import tzintzuntzan.Settings.ExchangeMode;
import tzintzuntzan.Settings.ExchangeType;
import tzintzuntzan.Settings.Gender;
import tzintzuntzan.Settings.Grid;
import tzintzuntzan.Settings.MaritalStatus;
import tzintzuntzan.Settings.Profession;
import tzintzuntzan.Settings.SharedInterests;
import tzintzuntzan.Settings.Tags;
import uchicago.src.collection.RangeMap;
import uchicago.src.sim.analysis.LocalDataRecorder;
import uchicago.src.sim.analysis.NetSequenceGraph;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Controller;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.OvalNetworkItem;
import uchicago.src.sim.network.DefaultDrawableNode;
import uchicago.src.sim.network.NetworkFactory;
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.util.Random;
import uchicago.src.sim.util.SimUtilities;

/**
 * 
 * @author shah
 *
 */

public class TzintzuntzanModel extends SimModelImpl {
	/** Default initial number of households - based on Foster/Kemper */
	private int initHouseholds = Settings.INITIAL_HOUSEHOLDS;
	/** This is an arbitrary number. We might just drop this - from Moss (2008) */
	private int numSharedInterests = SharedInterests.NUM_SHARED_INTERESTS;
	/** Default birth rate 47.3 - Birth rate in Michaoachan for 1970 (Lopez et al. 2006)
	 * 'Peasant emigration and land-use change at the watershed level: A GIS-based approach in Central Mexico' */
	//private double birthRate = 47.3;
	private double birthRate = Settings.BIRTH_RATE; 
	/** Chance factor for marriage*/
	private double elopementRate = Settings.ELOPEMENT_RATE;
	/** Rate - reduces the building of new households.*/
	private double buildNuclearHouseRate = Settings.BUILD_NUCLEAR_HOUSEHOLD_RATE;
	/** Positive static endorsements*/
	private String[] positiveStaticEndorsements = {"is-contractant", 
			"is-relative", "is-sibling", "is-neighbor", "is-compadre", "is-godparent", "is-god-child", "is-married",
			"is-share-interest", "is-same-gender", "is-same-age", "is-same-profession", "is-same-status", "is-above-status"};
	/** Negative static endorsements*/
	private String[] negativeStaticEndorsements = {"is-different-gender", "is-different-age",
			"is-different-profession", "is-below-status"};
	/** Positive dynamic endorsements*/
	private String[] positiveDynamicEndorsements = {"is-friend", "is-intense", "is-semi-intense", "is-prestigious", "is-similar", "expect-commercial-tie"};
	/** Negative dynamic endorsements*/
	private String[] negativeDynamicEndorsements = {"is-not-intense", "is-unprestigious", "is-dissimilar", "is-friend-in-past"};	
	/** Endorsement mode: {contextualized, random} */
//	private EndorsementWeights endorsementsWeights = EndorsementWeights.CONTEXTUALIZED;
	private EndorsementWeights endorsementsWeights = EndorsementWeights.RANDOM;
	/** Exchange mode*/
	private ExchangeMode exchangeMode = ExchangeMode.SEPARATE;
	/** Endorsement values evaluation : continuous or stepwise*/
	private EndorsementEvaluation endorsementEvaluation = EndorsementEvaluation.CONTINUOUS;
	//private EndorsementEvaluation endorsementEvaluation = EndorsementEvaluation.DISCRETE;
	/** Logistic distribution or Count recall */
	private boolean nonlinearRecall = Settings.NONLINEAR_RECALL;	
	/** Average number of activities resulting in an 'ordinary' exchange for a single agent in the population per month. */
	private double meanOrdinaryExchangeTime = Settings.MEAN_ORDINARY_EXCHANGE_TIME;
	/** Average number of activities resulting in an 'fiesta' exchange for a single agent in the population per month - on average6 per year */
	private double meanFiestaExchangeTime = Settings.MEAN_RITUAL_EXCHANGE_TIME; 
	/** Static endorsements - They do not change over time*/
	private ArrayList<String> staticEndorsements = new ArrayList<String>();
	/** Temporal endorsements */
	private ArrayList<String> dynamicEndorsements = new ArrayList<String>();
	/** Universal Set of endorsement labels */
	private ArrayList<String> combinedEndorsements = new ArrayList<String>();
	/** Mailbox for keeping record of sending and handling exchanges.*/
	protected ArrayList<ExchangeRecord> mailbox = new ArrayList<ExchangeRecord>();	
	/** Tag related parameters. - from Moss (2008)*/
	private double tagEvolutionPropensity = Tags.EVOLUTION_PROPENSITY;
	/** sm and rm used 7 - Moss (2008) used 7 */
	private int tagLength = Tags.LENGTH;
	/** Tag related parameters from Moss (2008)*/
	private int tagBase = Tags.BASE;	
	/** Tag related parameters from Moss (2008)*/
	private int minBaseValue = Tags.MIN_BASE_VALUE;
	/** Tag related parameters from Moss (2008)*/
	private int maxBaseValue = Tags.MAX_BASE_VALUE;
	/* Implementation variables */
	private DisplaySurface surface;
	protected Schedule schedule;
	private NetSequenceGraph graph;
	private boolean showPlot = false;
	/** 2D grid as representation of the village*/
	private Object2DGrid space;
	/** RM - Density factor for calculating neighborhood over the grid arbitrary assumption */
	private double densityFactor = Grid.DENSITY_FACTOR;
	/** RM - Neighborhood radius for calculating neighborhood over the grid arbitrary assumption */
	private double neighbourhoodRadius = Grid.RADIUS;
	/** X-grid size */
	private int gridSizeX = Grid.XSIZE;
	/** Y-grid size */
	private int gridSizeY = Grid.YSIZE;
	/* SM - variables for demographic processes */
	/** To store how many births will occur in which week of which month */
	private int[][] nextBirths = new int[12][4];
	/** Brandes (1983) - nuclear household with husband, wife and children */
	private Normal hhSizeDist = new Normal(5.0, 3.0, new MersenneTwister());  
	/** Arbitrary assumption for the husband's age distribution*/
	private Normal husbandAgeDist = new Normal(35, 7, new MersenneTwister());
	/** Arbitrary assumption for the household's head age distribution*/
	private Normal householdHeadAgeDist = new Normal(55, 5, new MersenneTwister());
	/** Age difference between married spouses (man-woman), approximated by the Normal distribution with mean = 8.43, sd = 6.576*/
	private Normal ageDiffDist = new Normal(5, 2, new MersenneTwister());	
	/**Average endorsement update rate for a single person; also updates the friendship network every 3rd week (1/3=0.3333) */
	private double endorsementUpdateRate = Settings.ENDORSEMENT_UPDATE_RATE;
	/* properties lists */ 
	private ArrayList<Person> personList = new ArrayList<Person>();  
	private ArrayList<Person> adultsList = new ArrayList<Person>();
	private ArrayList<Person> childrenList = new ArrayList<Person>();
	private ArrayList<Person> potentialMothers = new ArrayList<Person>();
	private ArrayList<Person> adultThisWeek = new ArrayList<Person>();
	private ArrayList<Person> eligibleMales = new ArrayList<Person>();
	private ArrayList<Person> eligibleFemales = new ArrayList<Person>();
	private ArrayList<Person> elopedCouples = new ArrayList<Person>();
	private HashMap<Integer, ArrayList<Couple>> couples = new HashMap<Integer, ArrayList<Couple>>();
	private HashMap<Integer, Person> personMap = new HashMap<Integer, Person>();
	private ArrayList<Household> householdList = new ArrayList<Household>();
	private HashMap<Integer, Household> householdMap = new HashMap<Integer, Household>();
	private ArrayList<SharedInterest> sharedInterestList;
	public PsRandom psr = new PsRandom();
	private int lastPersonID = -1;
	private int lastHouseholdID = -1;
	private int year = 0;
	private int month = 0;
	private int week = 0;
	private int currentTick = 0;
	private RangeMap exchangeMap = new RangeMap();

	protected int numPosStaticEndorsements = 0;
	protected int numNegStaticEndorsements = 0;	
	protected int numStaticEndorsments = 0;
	protected int numPosDyEndorsments = 0;
	protected int numNegDyEndorsments = 0;
	protected int totalEndorsements = 0;
	private int maxIterations = Settings.MAX_ITERATIONS;
	private LocalDataRecorder recorder, recorder2, recorder3;
	
	private int eM = 0, eF = 0;	
	private int populationSize = 0;
	private int households = 0;
	private int numberBirths = 0;
	private int numDeathsPerYear = 0;
	private int numJointHouseholds = 0;
	private int numPureNuclearHouseholds = 0;
	private int numNuclearHouseholds = 0;
	public double totalNegReliableEndorsements = 0;
	public double totalPosReliableEndorsements = 0;
	public double adults = 0;
	public double numCompadres = 0;
	public double numCompadresTotal = 0;
	public double numAdultsMarr = 0;
	public double medianCompadre = 0;
	public double compBoth = 0, compFr = 0, compSpFr = 0, compRel = 0, compSpRel = 0, compMisc = 0; 
	public double compBothYr = 0, compFrYr = 0, compRelYr = 0, compMiscYr = 0;

	public TzintzuntzanModel() {}

	public void mainAction() {
		refreshVariables();
		updateDateValues();
		/* Births trigger selection of god parents and services from compadre to reinforce ties.*/
		updatePersons();
		/* Elopements trigger decisions to build a nuclear house in future. */
		elopements();
		/* Marriages trigger finding marriage of god parents - feasts arranged by both sides - endorsements*/
		marriages();
		/* Updating/change of endorsements for neighbors*/
		updateHouseholds();
		updateEndorsements();
		/* Perform riteDePassage services for this week.*/
		riteDePassageService();
		/* Perform ordinary and occasionally fiesta/ritual services and goods exchange for this week. */
		routineExchanges();
		if (year % 15 == 0) {
			System.gc();
		}
		setPopulation(personList.size());
		setHouseholds(householdList.size());
		adults += (double) adultsList.size();
		print("tick: " + currentTick + " - year: " + year + " - population: " + populationSize + " households: " + households);
		schedule.scheduleActionAt((double) maxIterations, this, "stop", Schedule.LAST);
		if (currentTick % 48 == 0) {
			 //Utilities.contractNetworkPajek("contracts-"+currentTick, adultsList);
		}
	}

	/** */
	public void riteDePassageService() {
		for (Person adult : adultsList) {
			if (!adult.getInvitations().isEmpty()) {
				adult.handleRiteDePassageInvitations();
			}
		}
	}

	/**
	 * Contains arbitrary assumption - for finding new friends  every 3rd week on average. 
	 */
	public void updateEndorsements() {
		SimUtilities.shuffle(adultsList);
		if (numSharedInterests >= 1
				&& year % 48 == 0
				&& !adultThisWeek.isEmpty()) {
			assignSharedInterestEndorsmenets();
		}
		for (Person adult : adultsList) {
			if (Math.random() <= Tags.EVOLUTION_PROBABILITY
					&& !adult.getKnownPersons().isEmpty()) {
				adult.evolveTag();
			}
			if (Math.random() <= adult.getEndorsementUpdateRate()
					|| adultThisWeek.contains(adult)) {
				adult.updateDynamicEndorsements();
				adult.findFriends();
				//TODO: arbitrary assumption in the findContractSeparate/findContractCombined call.
				adult.getContractants().clear();
				if (exchangeMode == ExchangeMode.SEPARATE) {
					adult.findContractsSeparate();
				}
				else if (exchangeMode == ExchangeMode.COMBINED) {
					adult.findContractsCombined();
				}
			}
		}
		adultThisWeek.clear();
	}

	/** We assume that only adults participate in the dyadic exchange process.*/
	public void routineExchanges() {
		for (Person adult : adultsList) {
			if(adult.getNextRitualExchangeTime() == currentTick
					|| adult.getNextOrdinaryExchangeTime() == currentTick
			) {
				adult.doDyadicExchange(currentTick);
			}			
			if (adult.getMaritalStatus().equals(MaritalStatus.SINGLE) == false
					&& adult.getMaritalStatus().equals(MaritalStatus.ELOPED) == false
					&& !adult.getCompadreList().isEmpty()) {
				numAdultsMarr++;
				numCompadresTotal += adult.getCompadreList().size();
				compBothYr += compBoth;
				compFrYr += compFr;
				compRelYr += compRel;
				compMiscYr += compMisc;
				//adult.updateCompRelations();
			}
		}
				
		for (ExchangeRecord exchangeRecord : mailbox) {
			Person receiver = (Person) personMap.get(exchangeRecord.getReceiverID());
			if (receiver != null
					&& receiver.isAlive()) {
				receiver.handleExchangeRecord(exchangeRecord);
			}
		}
		mailbox.clear();		
		medianCompadre = Utilities.calculateMedianCompadre(numAdultsMarr, adultsList);		
	}

	/** Update households every week.*/
	public void updateHouseholds() {
		SimUtilities.shuffle(householdList);
		ArrayList<Household> buildNewHouseholds = new ArrayList<Household>();
		ArrayList<Household> dissolvedHouseholds = new ArrayList<Household>();
		for (Household household : householdList) {
			household.update();			
			if (household.isShouldDissolve()) {
				dissolvedHouseholds.add(household);
			}
			else if (currentTick >= 1
					&& household.isBuildNewHousehold()
					&& household.getTimeBuildNuclearHousehold() == currentTick) {
				buildNewHouseholds.add(household);
			}
		}
		for (Household household : buildNewHouseholds) {
			createNewHouseholds(household);
		}
		for (Household household : dissolvedHouseholds) {
			dissolveHousehold(household);
		}
		for (Household household : householdList) {
			if (isNuclearHousehold(household)) {
				numNuclearHouseholds++;
			}
			else {
				numJointHouseholds++;
			}
		}
	}

	/** */
	private boolean isNuclearHousehold(Household household) {
		Person head = (Person) household.getHead();
		boolean flag = true;
		for (Person member : household.getMembers()) {
			if (member != null
					&& member.isAlive()
					&& member.getId() != head.getId()) {				
				if (!head.isPureNuclearRelation(member)) {
					flag = false;
					break;
				}
			}
		}
		return flag;
	}
		
	/**
	 * Create new nuclear households for married couples in their respective parent households.
	 * A nuclear household can be created either by an independent 
	 *  decision of a married couple residing in the husband's parental household;
	 * OR triggered by elopement of a {single; currently} husband's brother.
	 * @param parentHouseholds
	 */
	public void createNewHouseholds (Household parentHousehold) {	
		try {
			Person husband = (Person) personMap.get(parentHousehold.getMemberBuildingNuclearHousehold());
			Person wife = (Person) husband.getSpouse();
			if (husband == null 
					|| wife == null
					|| !husband.isAlive()
					|| !wife.isAlive()) {
				parentHousehold.resetBuildingNuclearHousehold();
				return;				
			}
			//Total number of husband's siblings
			int numHusbansSiblings = husband.getSiblings().size();
			if (numHusbansSiblings == 0
					|| husband.isOnlySonInHousehold()
			) {
				parentHousehold.resetBuildingNuclearHousehold();
				return;
			}			
			Household household = new Household(++lastHouseholdID, this);
			householdList.add(household);
			householdMap.put(new Integer(household.getId()), household);

			household.setProfession(parentHousehold.getProfession());

			parentHousehold.removeMember(husband);
			parentHousehold.removeMember(wife);
			household.addMember(husband);
			household.addMember(wife);
			for (Person child : husband.getOffsprings()) {
				household.addMember(child);
				parentHousehold.removeMember(child);
			}
			household.setHead(husband);
			/* First decide if we want to be in the neighborhood of the parent's household
			 * then find a location for the household.
			 * If we don't decide or find a location in the vicinity of parents' house,
			 * we find a random location.
			 * We assume that we always find a location in the village - which may be constrained
			 * in the next iteration. 
			 * Then update the endorsements. 
			 */									
			boolean flag = false;
			if (household.shouldNeighborParentalHousehold(parentHousehold.getId())) {
				flag = household.findNeighborhoodOf(parentHousehold);
			}
			if (!flag) {
				findRandomLocation(household);
			}
			// Now we update endorsements for neighbors and relative households.
			household.updateNeighborLinks(parentHousehold.getNeighbors());
			household.updateKinshipLinks(parentHousehold.getRelativeHouseholds());
			parentHousehold.resetBuildingNuclearHousehold();
		}  catch (Exception e) {e.printStackTrace();}		
	}

	/** Distribute households on random positions inside village.*/
	public void populateVillage() {			
		int radius = (int)(Math.sqrt(initHouseholds)/2.0 * densityFactor);
		int[] centre = new int[2];
		space = new Object2DGrid(gridSizeX, gridSizeY);
		for (int i=0; i<gridSizeX; i++) {
			for (int j=0; j<gridSizeY; j++) {
				space.putObjectAt(i,j, null);
			}
		}
		centre[0] = Random.uniform.nextIntFromTo(radius+1, this.gridSizeX-radius-1);
		centre[1] = Random.uniform.nextIntFromTo(radius+1, this.gridSizeY-radius-1);
		for (int i=0; i<initHouseholds; i++) {
			Household household = new Household(++lastHouseholdID, this);
			householdList.add(household);
			householdMap.put(new Integer(household.getId()), household);
			/* Create a nuclear-type household */
			if (Math.random() <= Settings.INITIAL_NUCLEAR_HOUSEHOLDS_PROP) {
				populateNuclearHousehold(household);
			}
			/* Create a joint-type household */ 
			else {
				populateJointHousehold(household);
			}
			/* Now endorse close relatives within the household.*/
			household.endorseCloseRelatives();
			/*
			 * Now assign professions to household. Note we are just assigning
			 * randomly for now (arbitrary assumption). The distribution c adapted from Foster (1969)  
			 */
			double rand = Math.random();
			if (rand <= 0.65) {
				household.setProfession(Profession.POTTERY);
			}
			else if (rand > 0.65 && rand <= 0.9) {
				household.setProfession(Profession.FARMING);
			}
			else {
				household.setProfession(Profession.OTHER);
			}
			findRandomLocation(household);
		}		
	}

	/**
	 * Finds a random location for this household.
	 * @param household
	 */
	public void findRandomLocation(Household household) {
		int x=0, y=0;
		do {
			x = Random.uniform.nextIntFromTo(0, gridSizeX-1);
			y = Random.uniform.nextIntFromTo(0, gridSizeY-1);
		} while (space.getObjectAt(x, y) != null);
		household.setXY(x, y);
		space.putObjectAt(x, y, household);
	}

	/** Adapted from Brandes (1983) - Household Development Cycle - ~70% nuclear households
	 * 	Actual: Nuclear Households:
		- Woman alone - 5% households
		- Widow and Children - 5% households
		- Man and Wife - 10 % households
		- Man, Wife and Children - 50% households [average size: 5 persons; 3 persons  children per household]

		For the time being: Nuclear Households:
		- Man and Wife - 20 % households
		- Man, Wife and Children - 50% households [average size: 5 persons; 3 persons  children per household]

	 * @param household
	 */
	public void populateNuclearHousehold(Household household) {
		Person husband = new Person(++lastPersonID, household, this);
		int husbandAge = 0, wifeAge = 0;
		do {
			husbandAge = (int)husbandAgeDist.nextDouble();
			//arbitrary assumption - the range
		} while (husbandAge < 25 || husbandAge > 55);
		husband.setAge(husbandAge);
		husband.setGender(Gender.MALE);
		addPerson(husband, household);
		
		Person wife = new Person(++lastPersonID, household, this);
		wife.setGender(Gender.FEMALE);
		do {
			wifeAge = husband.getAge() - (int)ageDiffDist.nextDouble();
			//arbitrary assumption - the range
		} while (wifeAge < 20);
		wife.setAge(wifeAge);
		husband.setMaritalStatus(MaritalStatus.MARRIED);
		husband.setSpouse(wife);
		wife.setMaritalStatus(MaritalStatus.MARRIED);
		wife.setSpouse(husband);
		addPerson(wife, household);
		
		household.setHead(husband);
		// Only create a nuclear household with a married couple. {we might set the couple's age to be low in that case} 
		if (Math.random() <= 0.3) {
			return;
		}		
		int numChildren = 0;
		do {
			numChildren = (int) (hhSizeDist.nextDouble() - 2);
			//arbitrary assumption
		} while (numChildren <=2 || numChildren >= 5);
		for (int i=0; i<numChildren; i++) {
			createChild(wife);
		}					
	}

	/**
	 * 
	 * @param person
	 * @param household
	 */
	public void addPerson(Person person, Household household) {
		person.setBirthMonth(Random.uniform.nextIntFromTo(0,11));
		person.setBirthWeek(Random.uniform.nextIntFromTo(0,3));			
		household.addMember(person);
		person.setNode(new DefaultDrawableNode(""+person.getId(),new OvalNetworkItem(1, 10)));
		personList.add(person);
		personMap.put(new Integer(person.getId()), person);
		if (person.isAdult() && currentTick < 1) {
			adultsList.add(person);
		}
	}

	/**
	 * 	Joint Households
		- Man, Wife, Children, with Married Son and his Wife, and Children - 15% households [average size: 10 persons; 6 children to married son AND/OR married son's siblings]
		- Widow, Married Son and Married Son's Wife and Children - 5%
		- Two Married Brothers and Family - 5% [7 persons per household - 5 children per couple on average]
		- Man, Wife, Daughter and Daughter's Children - 5% [Widowed - Young: 5 children per household]

		We use: 
		- Case-I: Man, Wife, Children, with Married Son and his Wife, and Children 
			20% households [average size: 10 persons; 6 children to married son AND/OR married son's siblings]
		- Case-II: Two Married Brothers and Family - 10% [7 persons per household - 5 children per couple on average]

	 * @param household
	 */
	public void populateJointHousehold(Household household) {
		// 80% with case-I 
		if (Math.random() <= 0.8) {
			populateJointHouseholdCaseI(household);
		}
		else {
			populateJointHouseholdCaseII(household);
		}
	}
	
	/**
	 * 
	 * @param household
	 */
	public void populateJointHouseholdCaseI(Household household) {
		Person husband = new Person(++lastPersonID, household, this);
		int husbandAge = 0, wifeAge = 0;
		do {
			husbandAge = (int)householdHeadAgeDist.nextDouble();
			//arbitrary assumption - the range
		} while (husbandAge < 50 || husbandAge > 75);
		husband.setAge(husbandAge);
		husband.setGender(Gender.MALE);
		addPerson(husband, household);
		
		Person wife = new Person(++lastPersonID, household, this);
		wife.setGender(Gender.FEMALE);
		do {
			wifeAge = husband.getAge() - (int)ageDiffDist.nextDouble();
			//arbitrary assumption - the range
		} while (wifeAge < 40);
		wife.setAge(wifeAge);
		addPerson(wife, household);

		husband.setMaritalStatus(MaritalStatus.MARRIED);
		husband.setSpouse(wife);
		wife.setMaritalStatus(MaritalStatus.MARRIED);
		wife.setSpouse(husband);
		household.setHead(husband);

		Person marriedSon = new Person(++lastPersonID, household, this);
		do {
			husbandAge = (int)husbandAgeDist.nextDouble();
			//arbitrary assumption - the range
		} while (husbandAge < 25 || husbandAge > 45 || husbandAge > (husband.getAge() - 20));
		marriedSon.setAge(husbandAge);
		marriedSon.setGender(Gender.MALE);
		addPerson(marriedSon, household);
		
		Person sonsWife = new Person(++lastPersonID, household, this);
		sonsWife.setGender(Gender.FEMALE);
		do {
			wifeAge = marriedSon.getAge() - (int)ageDiffDist.nextDouble();
			//arbitrary assumption - the range
		} while (wifeAge < 20);
		sonsWife.setAge(wifeAge);
		addPerson(sonsWife, household);

		marriedSon.setMaritalStatus(MaritalStatus.MARRIED);
		marriedSon.setSpouse(sonsWife);
		sonsWife.setMaritalStatus(MaritalStatus.MARRIED);
		sonsWife.setSpouse(marriedSon);

		int numChildren = Uniform.staticNextIntFromTo(1, 3);		 
		for (int i=0; i<numChildren; i++) {
			createChild(sonsWife);
		}

		int numAdults = Uniform.staticNextIntFromTo(1, 3);		 
		for (int i=0; i<numAdults; i++) {
			createUnmarriedAdult(wife);
		}
	}

	/**
	 * Create an unmarried adult for this mother. Needs revising in next iteration.
	 * @param mother
	 */
	public void createUnmarriedAdult(Person mother) {						
		Person unmarriedAdult = new Person(++lastPersonID, mother.getHousehold(), this);		
		unmarriedAdult.setFather(mother.getSpouse());
		unmarriedAdult.getFather().getOffsprings().add(unmarriedAdult);		
		unmarriedAdult.setMother(mother);
		mother.getOffsprings().add(unmarriedAdult);
		unmarriedAdult.addSiblings();	
		unmarriedAdult.setAge(Uniform.staticNextIntFromTo(10, mother.getAge() - Settings.ADULT_STARTING_AGE));
		addPerson(unmarriedAdult, mother.getHousehold());
	}

	/**
	 * Two married brothers and family - 1-3 children per couple
	 * @param household
	 */
	public void populateJointHouseholdCaseII(Household household) {
		Person husband1 = new Person(++lastPersonID, household, this);
		int husbandAge = 0, wifeAge = 0;
		do {
			husbandAge = (int)householdHeadAgeDist.nextDouble();
			//arbitrary assumption - the range
		} while (husbandAge < 25 || husbandAge > 45);
		husband1.setAge(husbandAge);
		husband1.setGender(Gender.MALE);
		addPerson(husband1, household);

		Person wife1 = new Person(++lastPersonID, household, this);
		wife1.setGender(Gender.FEMALE);
		do {
			wifeAge = husband1.getAge() - (int)ageDiffDist.nextDouble();
			//arbitrary assumption - the range
		} while (wifeAge < 20);
		wife1.setAge(wifeAge);
		addPerson(wife1, household);

		husband1.setMaritalStatus(MaritalStatus.MARRIED);
		husband1.setSpouse(wife1);
		wife1.setMaritalStatus(MaritalStatus.MARRIED);
		wife1.setSpouse(husband1);
		household.setHead(husband1);

		int numChildren = Uniform.staticNextIntFromTo(1, 3);		 
		for (int i=0; i<numChildren; i++) {
			createChild(wife1);
		}

		Person husband2 = new Person(++lastPersonID, household, this);
		do {
			husbandAge = (int)householdHeadAgeDist.nextDouble();
			//arbitrary assumption - the range
		} while (husbandAge < 25 || husbandAge > 45);
		husband2.setAge(husbandAge);
		husband2.setGender(Gender.MALE);
		addPerson(husband2, household);

		Person wife2 = new Person(++lastPersonID, household, this);
		wife2.setGender(Gender.FEMALE);
		do {
			wifeAge = husband2.getAge() - (int)ageDiffDist.nextDouble();
			//arbitrary assumption - the range
		} while (wifeAge < 20);
		wife2.setAge(wifeAge);
		addPerson(wife2, household);
		
		husband2.setMaritalStatus(MaritalStatus.MARRIED);
		husband2.setSpouse(wife2);
		wife2.setMaritalStatus(MaritalStatus.MARRIED);
		wife2.setSpouse(husband2);
		household.setHead(husband2);
		//arbitrary assumption - the range
		numChildren = Uniform.staticNextIntFromTo(1, 3);		 
		for (int i=0; i<numChildren; i++) {
			createChild(wife2);
		}		
		husband1.getSiblings().add(husband2);
		husband2.getSiblings().add(husband1);
	}

	/**
	 * Creating "is-compadre"; "is-godparent", "is-godchild" endorsements for children in the initial population setting.
	 * This method is only called once at the beginning.
	 */
	public void createCompadreLinks() {
		SimUtilities.shuffle(childrenList);
		for (Person child : childrenList) {
			SimUtilities.shuffle(adultsList);
			Person godMother = null, godFather = null;
			Boolean foundGM = false, foundGF = false; 
			for (Person adult : adultsList) {
				if (adult.getHousehold().getId() != child.getHousehold().getId()
						&& !adult.isNuclearRelation(child)) {
					if (foundGM && foundGF) {
						assignCompdreship(child, godMother, godFather);
						break;
					}
					if (adult.getGender().equals(Gender.FEMALE)) {
						godMother = adult;
						foundGM = true;
						if (adult.getSpouse() != null) {							
							godFather = adult.getSpouse();
							foundGF = true;
						}
					}
					else {
						godFather = adult;
						foundGF = true;
						if (adult.getSpouse() != null) {							
							godMother = adult.getSpouse();
							foundGM = true;
						}				
					}
				}
			}
			
		}
	}

	/**
	 * 
	 * @param child
	 * @param godMother
	 * @param godFather
	 */
	private void assignCompdreship(Person child, Person godMother, Person godFather) {
		try {
			Person father = null, mother = null;
			godFather.endorsePerson(child, "is-god-child");
			godMother.endorsePerson(child, "is-god-child");
			child.endorsePerson(godFather, "is-godparent");
			child.endorsePerson(godMother, "is-godparent");
			child.setBirthGodFatherID(godFather.getId());
			child.setBirthGodMotherID(godMother.getId());

			String label = "is-compadre";			
			if (child.getFather() != null) {
				father = child.getFather();
				father.endorsePerson(godFather, label);
				father.endorsePerson(godMother, label);
				godFather.endorsePerson(father, label);
				godMother.endorsePerson(father, label);
			}
			if (child.getMother() != null) {
				mother = child.getMother();
				mother.endorsePerson(godFather, label);
				mother.endorsePerson(godMother, label);
				godFather.endorsePerson(mother, label);
				godMother.endorsePerson(mother, label);
			}
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * Creating "is-relative" endorsements among members of the extended family in the village, across households. 
	 * This method is only called once at the beginning. 
	 */
	@SuppressWarnings("unchecked")
	public void createRelativesLinks() {
		List<?> list = NetworkFactory.createWattsStrogatzNetwork(initHouseholds, 2, 0.2, NetworkNode.class, NetworkLink.class);
		for (int i=0; i<initHouseholds; i++) {
			NetworkNode node = (NetworkNode) list.get(i);
			Household household = householdMap.get(new Integer(i));
			household.setNode(node);
			node.setId(household.getId());
		}
		for (Household h1 : householdList) {
			ArrayList<NetworkNode> relatives = h1.getNode().getOutNodes();
			for (NetworkNode node : relatives) {
				Household h2 = householdMap.get(new Integer(node.getID()));
				try {
					if (h2 != null
							&& householdList.contains(h2)) {
						kinshipEndorsement(h1, h2);						
					}
				} catch (Exception e) {e.printStackTrace();}
			}
		}
	}

	/**
	 * Members of households household1 and household2 endorse each other as relatives. 
	 * @param household1
	 * @param household2
	 */
	public void kinshipEndorsement(Household household1, Household household2) {
		String label = "is-relative";
		if (household1.getId() != household2.getId()
				&& !household1.getRelativeHouseholds().contains(household2.getId())
				&& !household2.getRelativeHouseholds().contains(household1.getId())
		) {
			household1.getRelativeHouseholds().add(household2.getId());
			household2.getRelativeHouseholds().add(household1.getId());
			householdsMutualEndorsments(household1, household2, label);
		}		
	}

	/**
	 * Creating "is-neighbor" endorsements among members of the extended family in 
	 * the village, across households. This method is only called once at the beginning.
	 */
	public void createNeighbourhoodLinks() {
		for (Household h1 : householdList){
			for (Household h2 : householdList){
				if (h1.getId() != h2.getId()){
					double distance =
						Math.sqrt((h1.getX() - h2.getX()) * (h1.getX() - h2.getX())
								+ (h1.getY() - h2.getY()) * (h1.getY() - h2.getY()));
					if (distance <= neighbourhoodRadius){
						try {
							neighborEndorsements(h1, h2);
						} catch (Exception e) {e.printStackTrace();}
					}
				}
			}
		}
	}

	/**
	 * Members of households household1 and household2 endorse each other as neighbors.
	 * @param household1
	 * @param household2
	 */
	public void neighborEndorsements(Household household1, Household household2) {
		String label = "is-neighbor";
		if (!household1.getNeighbors().contains(household2.getId())
				&& !household2.getNeighbors().contains(household1.getId())
		) {
			household1.getNeighbors().add(household2.getId());
			household2.getNeighbors().add(household1.getId());
			householdsMutualEndorsments(household1, household2, label);
		}					
	}

	/**
	 * Members of households household1 and household2 endorse each other for the endorsement label.
	 * @param household1
	 * @param household2
	 * @param label
	 */
	public void householdsMutualEndorsments(Household household1, Household household2, String label) {
		for (Person m1 : household1.getMembers()) {
			for (Person m2 : household2.getMembers()) {
				if (m1 != null 
						&& m2 != null
						&& m1.isAlive()
						&& m2.isAlive()) {
					m1.endorsePerson(m2, label);
					m2.endorsePerson(m1, label);						
				}
			}
		}
	}

	/** 
	 * Update every individual agent's age and death check - 
	 * Also, determine the 'week' for the agent's death. 
	 * Remove agents who are dead from the simulation.
	 * Check for new births and assign birth weeks to new agents. 
	 */
	public void updatePersons() {
		ArrayList<Person> deadPersons = new ArrayList<Person>();
		potentialMothers.clear();
		adultsList.clear();	
		eligibleFemales.clear();
		eligibleMales.clear();
		for (Person person : personList) {
			person.getInvitations().clear();
			if (person.getDeathWeek() == -1) {
				checkDeath(person);
			}
			if ((person.getDeathWeek() != -1 
					&& person.getDeathWeek() == currentTick)
					|| !person.isAlive()) {
				person.setAlive(false);
				deadPersons.add(person);
			}
			else {
				/* Update person */
				person.update(currentTick);
			}			
		}
		/* At the beginning of year determine births for that year. */
		if (month == 0 && week == 0) {			
			calculateNextBirths();
		}
		/* Create a new person and find parents for them. */
		for (int i=0; i<nextBirths[month][week]; i++){
			if (!potentialMothers.isEmpty()){
				makeNewBornPerson();			
			}
		}
		for (Person deadPerson : deadPersons) {
			removePerson(deadPerson);
			numDeathsPerYear++;
		}				
		deadPersons = null;
	}

	/**
	 * Use birth rate and size of population to calculate number of births this year
	 */
	void calculateNextBirths(){
		int numBirths = (int)(birthRate / 1000.0 * personList.size());
		// distribute them uniformly over the weeks of the year
		resetNextBirths();
		for (int i = 0; i < numBirths; i++){
			int month = Random.uniform.nextIntFromTo(0, 11);
			int week = Random.uniform.nextIntFromTo(0, 3);
			nextBirths[month][week]++;
		}
	}

	public void resetNextBirths(){
		for (int i = 0; i < nextBirths.length; i++){
			for (int j = 0; j < nextBirths[i].length; j++){
				nextBirths[i][j] = 0;
			}
		}
	}

	public void makeNewBornPerson(){	
		Person mother = potentialMothers.get(Random.uniform.nextIntFromTo(0, potentialMothers.size()-1));
		createChild(mother);
		numberBirths++;		
	}

	/**Create new child agent for this mother.
	 * Assuming that no first pregnancy occurs before 16 years of age for the mother.  
	 * @param mother
	 */
	public void createChild(Person mother){
		Person father = (Person) mother.getSpouse();
		Household household = (Household) father.getHousehold();
		Person baby = new Person(++lastPersonID, household, this);
		if (getTickCount() < 1) {
			baby.setAge(Uniform.staticNextIntFromTo(1, mother.getAge()-Settings.ADULT_STARTING_AGE));
			childrenList.add(baby);
		}
		else {
			baby.setAge(0);
		}
		baby.setBirthMonth(month);
		baby.setBirthWeek(week);				
		baby.setFather(father);
		father.getOffsprings().add(baby);		
		baby.setMother(mother);
		mother.getOffsprings().add(baby);
		mother.setLastDelivery(currentTick);
		household.addMember(baby);
		baby.addSiblings();				
		personList.add(baby);
		personMap.put(new Integer(baby.getId()), baby);
		//That is, if the baby is born during the simulation and not at the start.
		if (currentTick >= 1) {
			baby.initializeStaticEndorsments();
			Person[] godParents = baby.findGodParents();
			String label = "is-compadre";
			if (godParents[0] != null) {
				baby.setBirthGodFatherID(godParents[0].getId());
				mother.endorsePerson(godParents[0], label);
				father.endorsePerson(godParents[0], label);
				godParents[0].endorsePerson(mother, label);
				godParents[0].endorsePerson(father, label);
				godParents[0].endorsePerson(baby, "is-god-child");
				baby.endorsePerson(godParents[0], "is-godparent");
			}
			if (godParents[1] != null) {
				baby.setBirthGodMotherID(godParents[1].getId());
				mother.endorsePerson(godParents[1], label);
				father.endorsePerson(godParents[1], label);
				godParents[1].endorsePerson(mother, label);
				godParents[1].endorsePerson(father, label);
				godParents[1].endorsePerson(baby, "is-god-child");
				baby.endorsePerson(godParents[1], "is-godparent");
			}
			addRiteDePassage(baby, ExchangeType.BIRTH_SERVICE);
		}		
	}

	/**
	 * Announce this service to known persons.
	 * Invite adult known persons as potential contractants to contribute to the service for Rite de Passage. 
	 * @param person
	 * @param serviceType
	 */
	public void addRiteDePassage(Person person, ExchangeType serviceType) {
		if (person == null || serviceType == ExchangeType.NONE) {
			return;
		}
		RiteDePassage invitation = new RiteDePassage(person, serviceType);
		person.setRiteDePassage(serviceType);
		Person father = (Person) person.getFather();
		Person mother = (Person) person.getMother();
		ArrayList<Person> invitees = new ArrayList<Person>(); 
		if (father != null && father.isAlive()) {
			//			invitees = (ArrayList<Person>)Settings.Union(invitees, father.sendInvitationRiteDePassage(invitation));
			invitees = (ArrayList<Person>) father.sendInvitationRiteDePassage(invitation);
		}
		if (mother != null && mother.isAlive()) {
			invitees = (ArrayList<Person>)Utilities.Union(invitees, mother.sendInvitationRiteDePassage(invitation));
		}
		for (Person invitee : invitees) {
			invitee.getInvitations().add(invitation);
		}
		invitees = null;
	}

	public void elopements() {
		SimUtilities.shuffle(eligibleFemales);
		setEF(eligibleFemales.size());
		SimUtilities.shuffle(eligibleMales);
		setEM(eligibleMales.size());
		for (Person male : eligibleMales) {
			if (male.isAlive()) {
				for (Person female : eligibleFemales) {
					if (female.isAlive()) {
						if (male.shouldElope(female)) {
							addElopedCouples(male, female);					
						}						
					}
				}				
			}
		}	
	}

	public void marriages() {
		Integer key = new Integer(currentTick);
		if (!couples.containsKey(key)) {
			return;
		}		
		ArrayList<Couple> marryingCouples = new ArrayList<Couple>(couples.get(key));
		SimUtilities.shuffle(marryingCouples);
		for (Couple couple : marryingCouples) {
			if (couple.male.isAlive() && couple.female.isAlive()) {
				marry(couple.male, couple.female);	
			}
			else {
				couple.male.setMaritalStatus(MaritalStatus.SINGLE);
				couple.female.setMaritalStatus(MaritalStatus.SINGLE);				
			}			
		}
		couples.get(key).clear();
		couples.remove(key);
	}

	/** We include wife's household as part of the extended family. 
	 * Update endorsements for both sides. 
	 * Birth god parents of both become known to the both sides.
	 * Both marrying households add each other and recognize as relatives. 
	 * We assume half a chance for either side's parents to find a marriage god parent, consistent with Foster. 
	 * 
	 * TODO: There is a chance where people get to know each other.
	 * 
	 * @param husband
	 * @param wife
	 */
	public void marry(Person husband, Person wife) {
		husband.setMaritalStatus(MaritalStatus.MARRIED);
		husband.setSpouse(wife);
		husband.getHousehold().resetElopedMaleMember();
		wife.setMaritalStatus(MaritalStatus.MARRIED);
		wife.setSpouse(husband);
		//Moving to husband's household
		Household maidenHousehold = (Household) wife.getHousehold();
		Household mh = (Household) wife.getHousehold();
		if (wife.getMaidenHousehold() == null) {
			//print("1.null");
		}
		if (wife.getHousehold() == null) {
			print("2.null");
			wife.printProfile();
			System.exit(1);
		}			
		
		wife.setHousehold(husband.getHousehold());
		wife.getHousehold().addMember(wife);
		if (husband.getHousehold() == null) {
			print("4.null");
			husband.printProfile();
			System.exit(1);
		}
		if (maidenHousehold != null
				&& householdList.contains(maidenHousehold)) {
			maidenHousehold.removeMember(wife);		
			wife.setMaidenHousehold(maidenHousehold);			
		}
		if (wife.getHousehold() == null) {
			print("5.null");	
		}
		if (wife.getMaidenHousehold() == null) {
			print("6.null");
		}
		if (mh == null) {
			print("x. null");
		}
		kinshipEndorsement(wife.getHousehold(), wife.getMaidenHousehold());
		//Find marriage god parents for the married couple.
		findMarriageGodParents(wife, husband);
		//Marriage service for god parents of husband and wife.		
		addRiteDePassage(husband, ExchangeType.MARRIAGE_SERVICE);
		addRiteDePassage(wife, ExchangeType.MARRIAGE_SERVICE);
	}

	/**
	 * We assume half a chance for either side's parents to find a marriage god parent, consistent with Foster. 
	 * @param husband
	 * @param wife
	 */
	public Person[] findMarriageGodParents(Person husband, Person wife) {
		Person[] godParents = Math.random() <= 0.5 ? husband.findGodParents() : wife.findGodParents();
		if (godParents[0] == null && godParents[1] == null) {
			return null;
		}		
		String label = "is-compadre";
		Person wifeFather = (Person) wife.getFather();
		Person wifeMother = (Person) wife.getMother();
		Person husbandFather = (Person) husband.getFather();
		Person husbandMother = (Person) husband.getMother();
		if (godParents[0] != null
				&& godParents[0].isAlive()) {
			if (husbandMother != null 
					&& husbandMother.isAlive()) {
				husbandMother.endorsePerson(godParents[0], label);
				godParents[0].endorsePerson(husbandMother, label);
			}
			if (husbandFather != null
					&& husbandFather.isAlive()) {
				husbandFather.endorsePerson(godParents[0], label);
				godParents[0].endorsePerson(husbandFather, label);
			}
			if (wifeMother != null
					&& wifeMother.isAlive()) {
				wifeMother.endorsePerson(godParents[0], label);
				godParents[0].endorsePerson(wifeMother, label);
			}
			if (wifeFather != null
					&& wifeFather.isAlive()) {
				wifeFather.endorsePerson(godParents[0], label);
				godParents[0].endorsePerson(wifeFather, label);
			}
			husband.setMarriageGodFatherID(godParents[0].getId());
			wife.setMarriageGodFatherID(godParents[0].getId());			
			husband.endorsePerson(godParents[0], "is-godparent");
			wife.endorsePerson(godParents[0], "is-godparent");
			godParents[0].endorsePerson(husband, "is-god-child");
			godParents[0].endorsePerson(wife, "is-god-child");
		}
		if (godParents[1] != null
				&& godParents[1].isAlive()) {
			if (husbandMother != null
					&& husbandMother.isAlive()) {
				husbandMother.endorsePerson(godParents[1], label);
				godParents[1].endorsePerson(husbandMother, label);
			}
			if (husbandFather != null
					&& husbandFather.isAlive()) {
				husbandFather.endorsePerson(godParents[1], label);
				godParents[1].endorsePerson(husbandFather, label);
			}
			if (wifeMother != null
					&& wifeMother.isAlive()) {
				wifeMother.endorsePerson(godParents[1], label);
				godParents[1].endorsePerson(wifeMother, label);
			}
			if (wifeFather != null
					&& wifeFather.isAlive()) {
				wifeFather.endorsePerson(godParents[1], label);
				godParents[1].endorsePerson(wifeFather, label);
			}
			husband.setMarriageGodMotherID(godParents[1].getId());
			wife.setMarriageGodMotherID(godParents[1].getId());
			husband.endorsePerson(godParents[1], "is-godparent");
			wife.endorsePerson(godParents[1], "is-godparent");
			godParents[1].endorsePerson(husband, "is-god-child");
			godParents[1].endorsePerson(wife, "is-god-child");
		}
		return godParents;
	}

	/**
	 * Average of 20 weeks from now - with standard deviation of 8 weeks -
	 * This is to be calibrated with Brandes (1983) 
	 * @param male
	 * @param female
	 */
	public void addElopedCouples(Person male, Person female) {
		male.setMaritalStatus(MaritalStatus.ELOPED);
		female.setMaritalStatus(MaritalStatus.ELOPED);
		Couple couple = new Couple(currentTick);
		couple.male = male;
		couple.female = female;
		Integer marriageTick = -1; int lag = -1;
		do {
			lag = (int) Normal.staticNextDouble(24, 8);	
		} while (lag <= 8 || lag >= 72);
		marriageTick = currentTick + lag; 
		couple.marriageTick = marriageTick;
		if (!couples.containsKey(marriageTick)) {
			couples.put(marriageTick, new ArrayList<Couple>());
		}
		couples.get(marriageTick).add(couple);
		male.getHousehold().setElopedMaleID(male.getId());
		male.setMarriageTick(marriageTick);
		female.setMarriageTick(marriageTick);
		//Elopement service by birth god parents and/or relatives
		addRiteDePassage(male, ExchangeType.ELOPEMENT_SERVICE);
		addRiteDePassage(female, ExchangeType.ELOPEMENT_SERVICE);
	}

	/**
	 * Remove the person from the simulation. 
	 * We are keeping the person object in the personMap for the time being.
	 * @param person
	 */
	public void removePerson(Person person) {
		//First of all funeral/death service for the person. 
		addRiteDePassage(person, ExchangeType.DEATH_SERVICE);
		//Then remove them from the simulation.
		if (person.getSpouse() != null) {
			person.getSpouse().setMaritalStatus(MaritalStatus.WIDOWED);
		}
		if (adultsList.contains(person)) {
			adultsList.remove(person);
		}
		person.clearMemory();
		personList.remove(person);		
	}

	/**
	 * Dissolve the household and clear all neighborhood and kinship links. 
	 * Clear space for new household to occupy that location.
	 */
	public void dissolveHousehold(Household household) {
		try {
			int xLoc = household.getXCoord(),
			yLoc = household.getYCoord();			
			if (xLoc > getGridSizeX()
					|| yLoc > getGridSizeY()) {
				System.exit(1);
			}
			space.putObjectAt(household.getXCoord(), household.getYCoord(), null);
			household.clearAll();
			if (householdList.contains(household)) {
				householdList.remove(household);
				householdMap.remove(household);			
			}
			System.gc();
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * The values are calculated arbitrarily based on just arbitrary assumptions.
	 * @param exchangeType
	 * @return
	 */
	public double returnExchangeValue(ExchangeType exchangeType) {
		double value = 0;
		switch(exchangeType) {
		case ORDINARY_GOOD:
			value = psr.nextLogNormal(0.3, 0.3);
			break;
		case ORDINARY_SERVICE:
			value = psr.nextLogNormal(0.3, 0.7);
			break;
		case FIESTA_GOOD:
			value = psr.nextLogNormal(0.5, 0.7);
			break;
		case FIESTA_SERVICE:
			value = psr.nextLogNormal(0.5, 0.9);
			break;
		case BIRTH_SERVICE:
			value = psr.nextLogNormal(0.5, 0.9);
			break;
		case ELOPEMENT_SERVICE:
			value = psr.nextLogNormal(0.5, 0.9);
			break;
		case MARRIAGE_SERVICE:
			value = psr.nextLogNormal(0.5, 0.9);
			break;
		case DEATH_SERVICE:
			value = psr.nextLogNormal(0.5, 0.9);
			break;
		default:break;
		}
		return value;
	}

	public void updateDateValues(){
		currentTick++;
		setYear(currentTick / 48);
		setMonth((currentTick - (48 * year)) / 4);
		setWeek(currentTick % 4);
	}

	public void buildModel() {
		recordData();
		buildModelProperties();
		populateVillage();
		print("village populated");
		createNeighbourhoodLinks();
		print("neighborhood links created households");
		createRelativesLinks();
		print("kinship links created households");
		createCompadreLinks();
		print("compadre links created households");
		childrenList = null;
		if (numSharedInterests >= 1) {
			createSharedInterests();
			print("activities created households");
		}
		for (Person person : personList) {
			person.updateDynamicEndorsements();
			person.findFriends();
		}	
		print("friendship links created among agents");
		System.gc();
	}

	/**  */
	public void createSharedInterests() {
		sharedInterestList = new ArrayList<SharedInterest>(numSharedInterests);
		for (int i=0; i<numSharedInterests; i++) {
			int type = Math.random() <= 0.1 ? SharedInterests.YOUTH : SharedInterests.ADULT;
			SharedInterest activity = new SharedInterest(i, type);
			sharedInterestList.add(activity);
		}
		SimUtilities.shuffle(personList);
		for (Person person : personList) {
			int age = person.getAge();
			int numActs = 0;
			int activityType = -1;
			if (age >= Settings.ADULT_STARTING_AGE){
				numActs = Random.uniform.nextIntFromTo(1, SharedInterests.MAX_ADULT_SHARED_INTERESTS);
				activityType = SharedInterests.ADULT;
			}
			else {
				numActs = 0;
				activityType = -1;
			}
			for (int i=0; i<numActs; i++) {
				int index = Random.uniform.nextIntFromTo(0, numSharedInterests - 1);
				SharedInterest activity = sharedInterestList.get(index);
				while (activity.getActivityType() != activityType 
				) {
					index = Random.uniform.nextIntFromTo(0, numSharedInterests - 1);
					activity = sharedInterestList.get(index);
				}
				activity.getParticipants().add(person);
			}
			assignSharedInterestEndorsmenets();
		}
	}

	/**
	 * Called when a child becomes adult during a simulation run.
	 */
	public void assignSharedInterests(Person person) {
		int numSharedInterests = 0;
		int activityType = 0;
		if (person.isAdult()){
			numSharedInterests = Random.uniform.nextIntFromTo(1, SharedInterests.MAX_ADULT_SHARED_INTERESTS);
			activityType = SharedInterests.ADULT;
		}
		for (int i=0; i<numSharedInterests; i++) {
			int index = Random.uniform.nextIntFromTo(0, numSharedInterests - 1);
			SharedInterest activity = sharedInterestList.get(index);
			while (activity.getActivityType() != activityType 
			) {
				index = Random.uniform.nextIntFromTo(0, numSharedInterests - 1);
				activity = sharedInterestList.get(index);
			}
			activity.getParticipants().add(person);
		}			
	}

	/**
	 * This can be implemented in a much faster way. Needs to revise the function implementation.
	 */
	public void assignSharedInterestEndorsmenets() {
		String label = "is-share-interest";
		//bad implementation
		if (currentTick > 1) {
			for (SharedInterest sharedInterest : sharedInterestList) {
				for (Person person1 : sharedInterest.getParticipants()) {
					if (adultThisWeek.contains(person1)) {
						for (Person person2 : sharedInterest.getParticipants()) {
							if (person1.getId() != person2.getId()
									&& person1 != null && person2 != null
									&& person1.isAlive() && person2.isAlive()) {
								person1.endorsePerson(person2, label);
								person2.endorsePerson(person1, label);
							}							
						}
					}
				}
			}
			return;
		}
		for (SharedInterest sharedInterest : sharedInterestList) {
			for (Person person1 : sharedInterest.getParticipants()) {
				for (Person person2 : sharedInterest.getParticipants()) {
					if (person1.getId() != person2.getId()) {
						if (person1 != null && person2 != null
								&& person1.isAlive() && person2.isAlive()) {
							person1.endorsePerson(person2, label);
							person2.endorsePerson(person1, label);

						}
					}
				}
			}
		}
	}

	protected void buildSchedule() {
		print("in schedule");
		schedule.scheduleActionBeginning(1, this, "mainAction");		
		schedule.scheduleActionAtInterval(1, new BasicAction() {
			public void execute() {
				recorder.record();
				recorder2.record();
			}
		}, Schedule.LAST);
		//12th monthly - reliability endorsements
		schedule.scheduleActionAtInterval(48, new BasicAction() {
			public void execute() {				
				recorder3.record();
				if (currentTick%48 == 0) {
					totalPosReliableEndorsements = 0;
					totalNegReliableEndorsements = 0;
					adults = 0;
					compBothYr = 0;
					compFrYr = 0;
					compRelYr = 0;
					compMiscYr = 0;
				}
			}
		}, Schedule.LAST);
//		schedule.scheduleActionAtInterval(48, new BasicAction() {
//			public void execute() {				
//				recorder2.record();
//				if (currentTick%48 == 0) {
//					numberBirths = 0;
//					numDeathsPerYear = 0;
//				}
//			}
//		}, Schedule.LAST);
		schedule.scheduleActionAtEnd(new BasicAction() {
			public void execute() {
				recorder.write();
				recorder2.write();
				recorder3.write();
			}
		});
	}

	public void buildModelProperties() {
		numPosStaticEndorsements = positiveStaticEndorsements.length;
		numNegStaticEndorsements = negativeStaticEndorsements.length;
		numStaticEndorsments = numPosStaticEndorsements+numNegStaticEndorsements;
		numPosDyEndorsments = positiveDynamicEndorsements.length;
		numNegDyEndorsments = negativeDynamicEndorsements.length;			

		for (int i=0; i<numPosStaticEndorsements; i++) {
			staticEndorsements.add(positiveStaticEndorsements[i]);
		}
		for (int i=0; i<numNegStaticEndorsements; i++) {
			staticEndorsements.add(negativeStaticEndorsements[i]);
		}
		for (int i=0; i<numPosDyEndorsments; i++) {
			dynamicEndorsements.add(positiveDynamicEndorsements[i]);
		}
		for (int i=0; i<numNegDyEndorsments; i++) {
			dynamicEndorsements.add(negativeDynamicEndorsements[i]);
		}
		totalEndorsements = numStaticEndorsments+numPosDyEndorsments+numNegDyEndorsments;

		combinedEndorsements.addAll(staticEndorsements);
		combinedEndorsements.addAll(dynamicEndorsements);

		//		ordinaryExchangeDist = new Exponential((double)(1/meanOrdinaryExchangeTime), new MersenneTwister());
		//		ritualExchangeDist = new Exponential((double)(1/meanRitualExchangeTime), new MersenneTwister());
	}

	protected int returnLogisticProbability(double x) {
		x /= getTickCount();
		x = (2*x - 1)*10;
		double prob = 1/( 1 + Math.pow(Math.E,(-1*x)));
		int i = Math.random() <= prob ? 1 : 0;
		return i;
		//		return Math.random() <= 1/( 1 + Math.pow(Math.E,(-1*(2*(x/max)-1)))) ? 1 : 0;
	}

	public void print(String str) {
		System.out.println(""+str);
	}

	public void begin() {		
		buildModel();
		buildSchedule();
		if (showPlot) {
			graph.display();
		}
	}

	public void setup() {
		Random.createUniform();
		if (surface != null) surface.dispose();
		if (graph != null) graph.dispose();
		surface = null;
		schedule = null;
		graph = null;
		personList = null;
		householdList = null;
		personMap = null;
		householdMap = null;
		potentialMothers = null;
		adultThisWeek = null;
		adultsList = null;
		eligibleMales = null;
		eligibleFemales = null;
		elopedCouples = null;
		staticEndorsements = null;
		dynamicEndorsements = null;
		combinedEndorsements = null;
		exchangeMap = null;
		System.gc();
		schedule = new Schedule();
		resetAllVariables();

		Random.createUniform();
		surface = new DisplaySurface(this, "Tzintzuntzan Display");
		registerDisplaySurface("Main Display", surface);
		schedule = new Schedule();
		Controller.CONSOLE_ERR = false;
		Controller.CONSOLE_OUT = false;
		setRngSeed(new Long(1199995797250l));
	}
	
	/**
	 * This is the code from Repast 3.1  MooreNeighborhooder class with minor modifications.
	 * Default values: 3
	 * @param x
	 * @param y
	 * @param extents
	 * @param returnNulls
	 */
	public ArrayList<int[]> returnVacantNeighborhoodLocations(int x, int y) {
		int xExtent = 3, yExtent = 3;		
		ArrayList<int[]> vacantPositions = new ArrayList<int[]>(xExtent*yExtent*4 + (xExtent*2) + (yExtent*2));
		int xLeft = xExtent;
		int xRight = xExtent;
		if (x+xRight > space.getSizeX()-1) {
			xRight = space.getSizeX()-1-x;
		}            
		if (x-xLeft < 0) {
			xLeft = x;
		}            
		int yTop = yExtent;
		int yBottom = yExtent;
		if (y+yBottom > space.getSizeY()- 1) {
			yBottom = space.getSizeY()-1-y;        	
		}            
		if (y-yTop < 0) {
			yTop = y;
		}
		for (int j=y-yTop; j <=y+yBottom; j++) {
			for (int i=x-xLeft; i<=x+xRight; i++) {
				if (!(j==y && i==x)) {
					if (space.getObjectAt(i,j) == null) {
						int[] pos = new int[2];
						pos[0] = i; pos[1] = j;
						vacantPositions.add(pos);
					}
				}
			}
		}
		return vacantPositions;
	}

	/** RM/SM
	 *  Uses WHO life tables to determine probability of death at current age for the person
		http://www3.who.int/whosis/life/life_tables/life_tables_process.cfm?country=zaf&language=en#
	 * @param person
	 */
	void checkDeath(Person person){
		WHOLifeTable table;
		table = (person.getGender().toString() == "female") ? femalesTable : malesTable;
		//Need only calculate anything when person's age is lower bound of an age interval
		int age = person.getAge();
		if (!table.isLowerBound(age)) return;
		//Retrieve probability
		double prob = table.getDeathProbability(age);
		if (Random.uniform.nextDouble() <= prob){
			//Person will die in current age interval
			// choose a random week from within this interval
			int timeToDie = Random.uniform.nextIntFromTo(1,table.getAgeIntervalSize(age)*48);
			//Assign it to person as tick to die in
			person.setDeathWeek(this.currentTick + timeToDie - 1);
		}
	}

	/**
	 * @author Ruth Meyer
	 * Need to check that length of values and probability is the same and that all the probabilities add up to 1.
	 */
	class EmpiricalDiscreteDist{
		RandomEngine randomGenerator;
		double[] probabilities;	

		public EmpiricalDiscreteDist(double[] probabilities, RandomEngine randomGenerator){
			this.randomGenerator = randomGenerator;
			this.probabilities = new double[probabilities.length]; 
			// turn probabilities into cumulative probabilities
			this.probabilities[0] = probabilities[0];
			for (int i = 1; i < probabilities.length; i++){
				this.probabilities[i] = this.probabilities[i-1] + probabilities[i]; 
			}			
		}

		public int nextInt(){
			int index = 0;
			double randomNumber = randomGenerator.nextDouble();
			while (index < this.probabilities.length && randomNumber > this.probabilities[index]){
				index++;
			}
			return index;
		}
	}

	/**
	 * @author Ruth Meyer	  
	 */
	class WHOLifeTable{
		int[] ageIntervals;  // the age range classes, given as (inclusive) lower bounds 
		double[] deathProbabilities;  // the probability to die within the current age range (nqx)

		public WHOLifeTable(int[] ageIntervals, double[] deathProbabilities){
			this.ageIntervals = ageIntervals;
			this.deathProbabilities = deathProbabilities;
		}

		public double getDeathProbability(int age){
			// lookup age in ageIntervals, then use that index to find corresponding death probability
			int index = findAgeInterval(age);
			if (index >= 0 ) return this.deathProbabilities[index];
			return 1.0;
		}

		public int getAgeIntervalSize(int age){
			int i = findAgeInterval(age);
			if (i == this.ageIntervals.length-1) return 3; // assume last interval has size 3
			else if (i >= 0) return (this.ageIntervals[i+1] - this.ageIntervals[i]);
			return -1;
		}

		public boolean isLowerBound(int age){
			int i = findAgeInterval(age);
			if (i > 0 && this.ageIntervals[i] == age) return true;
			return false;
		}

		private int findAgeInterval(int age){
			if (age < 0) return -1;
			// linear search
			int index = 0;
			while (index < this.ageIntervals.length 
					&& this.ageIntervals[index] <= age){
				index++;
			}
			return index-1;
		}
	}

	/**
	 * WHO Life Tables for males in Mexico for the year 2006
	 */
	WHOLifeTable malesTable = new WHOLifeTable(
			new int[]{0, 1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 
					70, 75, 80, 85, 90, 95, 100},
					new double[]{
					0.03186,
					0.00677,
					0.00157,
					0.0018,
					0.00453,
					0.00664,
					0.00796,
					0.00995,
					0.01232,
					0.01641,
					0.02297,
					0.03395,
					0.05075,
					0.07474,
					0.10864,
					0.16305,
					0.24191,
					0.37271,
					0.53898,
					0.68047,
					0.78337,
					1
			});

	/**
	 * WHO Life Tables for females in Mexico for the year 2006
	 */
	WHOLifeTable femalesTable = new WHOLifeTable(
			new int[]{0, 1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 
					70, 75, 80, 85, 90, 95, 100},
					new double[]{
					0.02620,
					0.00588,
					0.00125,
					0.00126,
					0.00204,
					0.00244,
					0.00292,
					0.0037,
					0.00498,
					0.008,
					0.01311,
					0.02089,
					0.03466,
					0.05441,
					0.08096,
					0.12333,
					0.18966,
					0.29869,
					0.44813,
					0.59793,
					0.72636,
					1
			});

	public class RiteDePassage {
		public RiteDePassage(Person _person, ExchangeType _serviceType) {
			person = _person;
			serviceType = _serviceType;
		}
		protected Person person = null; 
		protected ExchangeType serviceType = ExchangeType.NONE;
	}

	public String[] getInitParam() {
		String[] params = {"initHouseholds", "endorsementsWeights", "exchangeMode", "endorsementEvaluation", "nonlinearRecall", 
				"endorsementUpdateRate"};
		return params;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void recordData() {
		recorder = new LocalDataRecorder("Data-.txt", this);
		recorder.createNumericDataSource("Population", this, "getPopulation");
		recorder.createNumericDataSource("Households", this, "getHouseholds");
		recorder.createNumericDataSource("Compadre", this, "getCompadresTotal");
		recorder.createNumericDataSource("Adults", this, "getNumAdults");
		recorder.createNumericDataSource("Median", this, "getMedianCompadre");					
		recorder.createNumericDataSource("NuclearHH", this, "getNumNuclearHouseholds");
		recorder.createNumericDataSource("PureNuclearHH", this, "getNumPureNuclearHouseholds");
		recorder.createNumericDataSource("JointHH", this, "getNumJointHouseholds");			

//		recorder2 = new LocalDataRecorder("Annual-.txt", this);
//		recorder2.createNumericDataSource("BirthsPY", this, "getNumberBirths");
//		recorder2.createNumericDataSource("DeathsPY", this, "getNumDeathsPerYear");
		
		recorder2 = new LocalDataRecorder("compadre-.txt", this);
		recorder2.createNumericDataSource("numCompadres", this, "getNumCompadres");
		recorder2.createNumericDataSource("compFr", this, "getCompFr");
		recorder2.createNumericDataSource("compRel", this, "getCompRel");
		recorder2.createNumericDataSource("compMisc", this, "getCompMisc");
		recorder2.createNumericDataSource("compBoth", this, "getCompBoth");

		recorder3 = new LocalDataRecorder("Reliability-.txt", this);
		recorder3.createNumericDataSource("totReliabile", this, "getTotalPosReliableEndorsements");
		recorder3.createNumericDataSource("totUnReliable", this, "getTotalNegReliableEndorsements");
		recorder3.createNumericDataSource("adults", this, "getAdults");
		recorder3.createNumericDataSource("compFr", this, "getCompFrYr");
		recorder3.createNumericDataSource("compRel", this, "getCompRelYr");
		recorder3.createNumericDataSource("compMisc", this, "getCompMiscYr");
		recorder3.createNumericDataSource("compBoth", this, "getCompBothYr");		
	}

	private void refreshVariables() {
		populationSize = 0;
		households = 0;
		numNuclearHouseholds = 0;
		numJointHouseholds = 0;
		numAdultsMarr = 0;
		numCompadres = 0;
		numCompadresTotal = 0;		
		compBoth = 0;
		compFr = 0;
		compRel = 0;
		compMisc = 0;
	}
	
	private void resetAllVariables() {
		personList = new ArrayList<Person>();
		personMap = new HashMap<Integer, Person>();
		householdList = new ArrayList<Household>();
		householdMap = new HashMap<Integer, Household>();
		childrenList = new ArrayList<Person>();
		potentialMothers = new ArrayList<Person>();
		adultThisWeek = new ArrayList<Person>();
		eligibleMales = new ArrayList<Person>();
		eligibleFemales = new ArrayList<Person>();
		elopedCouples = new ArrayList<Person>();
		combinedEndorsements = new ArrayList<String>();
		staticEndorsements = new ArrayList<String>();
		dynamicEndorsements = new ArrayList<String>();
		adultsList = new ArrayList<Person>();
		year = 0; month = 0; week = 0; currentTick = 0;
		lastPersonID = -1;
		lastHouseholdID = -1;
		numPosStaticEndorsements = 0; numNegStaticEndorsements = 0;	
		numStaticEndorsments = 0; numPosDyEndorsments = 0;
		numNegDyEndorsments = 0; totalEndorsements = 0;
		eM = 0; eF = 0;
		populationSize = 0;
		households = 0;
		numberBirths = 0;
		numDeathsPerYear = 0;
		numJointHouseholds = 0;
		numPureNuclearHouseholds = 0;
		numNuclearHouseholds = 0;
		totalNegReliableEndorsements = 0;
		totalPosReliableEndorsements = 0;
		adults = 0;
		numCompadres = 0;
		numCompadresTotal = 0;
		numAdultsMarr = 0;
		medianCompadre = 0;
		compBoth = 0; compFr = 0; compSpFr = 0; compRel = 0; compSpRel = 0; compMisc = 0; 
		compBothYr = 0; compFrYr = 0; compRelYr = 0; compMiscYr = 0;
	}

	public String getName() {
		return "Contextualized Reasoning in a model of the Tzintzuntzan village based on Foster et al.";
	}

	public static void main(String[] args) {
		uchicago.src.sim.engine.SimInit init = new uchicago.src.sim.engine.SimInit();
		TzintzuntzanModel model = new TzintzuntzanModel();
		init.loadModel(model, null, false);
	}	

	public int returnStaticEndorsementIndex(String label) {
		return staticEndorsements.indexOf(label);
	}

	public int returnDynamicEndorsementIndex(String label) {
		return dynamicEndorsements.indexOf(label);
	}

	public String returnStaticEndorsementLabel (int index) {
		return staticEndorsements.get(index);
	}

	public String returnDynamicEndorsementLabel (int index) {
		return dynamicEndorsements.get(index);
	}

	public int getCurrentTick() {
		return currentTick;
	}

	public void setCurrentTick(int currentTick) {
		this.currentTick = currentTick;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getWeek() {
		return week;
	}

	public void setWeek(int week) {
		this.week = week;
	}

	public int getInitHouseholds() {
		return initHouseholds;
	}

	public void setInitHouseholds(int initHouseholds) {
		this.initHouseholds = initHouseholds;
	}

	public double getBirthRate() {
		return birthRate;
	}

	public void setBirthRate(double birthRate) {
		this.birthRate = birthRate;
	}

	public double getTagEvolutionPropensity() {
		return tagEvolutionPropensity;
	}

	public void setTagEvolutionPropensity(double tagEvolutionPropensity) {
		this.tagEvolutionPropensity = tagEvolutionPropensity;
	}

	public int getTagLength() {
		return tagLength;
	}

	public void setTagLength(int tagLength) {
		this.tagLength = tagLength;
	}

	public int getTagBase() {
		return tagBase;
	}

	public void setTagBase(int tagBase) {
		this.tagBase = tagBase;
	}

	public int getMinBaseValue() {
		return minBaseValue;
	}

	public void setMinBaseValue(int minBaseValue) {
		this.minBaseValue = minBaseValue;
	}

	public int getMaxBaseValue() {
		return maxBaseValue;
	}

	public void setMaxBaseValue(int maxBaseValue) {
		this.maxBaseValue = maxBaseValue;
	}

	public ArrayList<Person> getPersonList() {
		return personList;
	}

	public void setPersonList(ArrayList<Person> personList) {
		this.personList = personList;
	}

	public ArrayList<Person> getAdultsList() {
		return adultsList;
	}

	public void setAdultsList(ArrayList<Person> adultsList) {
		this.adultsList = adultsList;
	}

	public ArrayList<Household> getHouseholdList() {
		return householdList;
	}

	public void setHouseholdList(ArrayList<Household> householdList) {
		this.householdList = householdList;
	}

	public PsRandom getPsr() {
		return psr;
	}

	public void setPsr(PsRandom psr) {
		this.psr = psr;
	}

	public int[][] getNextBirths() {
		return nextBirths;
	}

	public void setNextBirths(int[][] nextBirths) {
		this.nextBirths = nextBirths;
	}

	public void setPotentialMothers(ArrayList<Person> possibleMothers) {
		this.potentialMothers = possibleMothers;
	}

	public void setEligibleMales(ArrayList<Person> eligibleMales) {
		this.eligibleMales = eligibleMales;
	}

	public ArrayList<Person> getEligibleMales() {
		return eligibleMales;
	}

	public void setEligibleFemales(ArrayList<Person> eligibleFemales) {
		this.eligibleFemales = eligibleFemales;
	}

	public ArrayList<Person> getEligibleFemales() {
		return eligibleFemales;
	}

	public void setElopedCouples(ArrayList<Person> elopedCouples) {
		this.elopedCouples = elopedCouples;
	}

	public ArrayList<Person> getElopedCouples() {
		return elopedCouples;
	}

	public void setElopementRate(double marriageRate) {
		this.elopementRate = marriageRate;
	}

	public double getElopementRate() {
		return elopementRate;
	}

	public void setCouples(HashMap<Integer, ArrayList<Couple>> couples) {
		this.couples = couples;
	}

	public HashMap<Integer, ArrayList<Couple>> getCouples() {
		return couples;
	}

	public void setEM(int eM) {
		this.eM = eM;
	}

	public int getEM() {
		return eM;
	}

	public void setEF(int eF) {
		this.eF = eF;
	}

	public int getEF() {
		return eF;
	}

	public void setPopulation(int populationSize) {
		this.populationSize = populationSize;
	}

	public int getPopulation() {
		return populationSize;
	}

	public HashMap<Integer, Household> getHouseholdMap() {
		return householdMap;
	}

	public void setHouseholdMap(HashMap<Integer, Household> householdMap) {
		this.householdMap = householdMap;
	}

	public HashMap<Integer, Person> getPersonMap() {
		return personMap;
	}

	public void setPersonMap(HashMap<Integer, Person> personMap) {
		this.personMap = personMap;
	}

	public ArrayList<String> getStaticEndorsements() {
		return staticEndorsements;
	}

	public void setStaticEndorsements(ArrayList<String> staticEndorsements) {
		this.staticEndorsements = staticEndorsements;
	}

	public ArrayList<String> getDynamicEndorsements() {
		return dynamicEndorsements;
	}

	public void setDynamicEndorsements(ArrayList<String> dynamicEndorsements) {
		this.dynamicEndorsements = dynamicEndorsements;
	}

	public void setSharedInterestList(ArrayList<SharedInterest> activitiesList) {
		this.sharedInterestList = activitiesList;
	}

	public ArrayList<SharedInterest> getSharedInterestList() {
		return sharedInterestList;
	}

	public Object2DGrid getSpace() {
		return space;
	}

	public void setSpace(Object2DGrid space) {
		this.space = space;
	}

	public double getNeighbourhoodRadius() {
		return neighbourhoodRadius;
	}

	public void setNeighbourhoodRadius(double neighbourhoodRadius) {
		this.neighbourhoodRadius = neighbourhoodRadius;
	}

	public double getBuildNuclearHouseRate() {
		return buildNuclearHouseRate;
	}

	public void setBuildNuclearHouseRate(double buildNuclearHouse) {
		this.buildNuclearHouseRate = buildNuclearHouse;
	}

	public int getGridSizeX() {
		return gridSizeX;
	}

	public void setGridSizeX(int gridSizeX) {
		this.gridSizeX = gridSizeX;
	}

	public int getGridSizeY() {
		return gridSizeY;
	}

	public void setGridSizeY(int gridSizeY) {
		this.gridSizeY = gridSizeY;
	}

	public ArrayList<Person> getPotentialMothers() {
		return potentialMothers;
	}

	public ArrayList<Person> getAdultThisWeek() {
		return adultThisWeek;
	}

	public void setAdultThisWeek(ArrayList<Person> adultThisWeek) {
		this.adultThisWeek = adultThisWeek;
	}

	public ArrayList<String> getCombinedEndorsements() {
		return combinedEndorsements;
	}

	public void setCombinedEndorsements(ArrayList<String> combinedEndorsements) {
		this.combinedEndorsements = combinedEndorsements;
	}

	public int getNumSharedInterests() {
		return numSharedInterests;
	}

	public void setNumSharedInterests(int numSharedInterests) {
		this.numSharedInterests = numSharedInterests;
	}

	public double getEndorsementUpdateRate() {
		return endorsementUpdateRate;
	}

	public void setEndorsementUpdateRate(double endorsementUpdateRate) {
		this.endorsementUpdateRate = endorsementUpdateRate;
	}

	public double getMeanOrdinaryExchangeTime() {
		return meanOrdinaryExchangeTime;
	}

	public void setMeanOrdinaryExchangeTime(double meanOrdinaryExchangeTime) {
		this.meanOrdinaryExchangeTime = meanOrdinaryExchangeTime;
	}

	public double getMeanFiestaExchangeTime() {
		return meanFiestaExchangeTime;
	}

	public void setMeanFiestaExchangeTime(double meanRitualExchangeTime) {
		this.meanFiestaExchangeTime = meanRitualExchangeTime;
	}

	public int getHouseholds() {
		return households;
	}

	public void setHouseholds(int households) {
		this.households = households;
	}

	public int getNumDeathsPerYear() {
		return numDeathsPerYear;
	}

	public void setNumDeathsPerYear(int numDeathsPerYear) {
		this.numDeathsPerYear = numDeathsPerYear;
	}

	public int getNumberBirths() {
		return numberBirths;
	}

	public void setNumberBirths(int numberBirths) {
		this.numberBirths = numberBirths;
	}

	public int getNumJointHouseholds() {
		return numJointHouseholds;
	}

	public void setNumJointHouseholds(int numJointHouseholds) {
		this.numJointHouseholds = numJointHouseholds;
	}

	public int getNumNuclearHouseholds() {
		return numNuclearHouseholds;
	}

	public void setNumNuclearHouseholds(int numNuclearHouseholds) {
		this.numNuclearHouseholds = numNuclearHouseholds;
	}

	public int getNumPureNuclearHouseholds() {
		return numPureNuclearHouseholds;
	}

	public void setNumPureNuclearHouseholds(int numPureNuclearHouseholds) {
		this.numPureNuclearHouseholds = numPureNuclearHouseholds;
	}

	public void setExchangeMap(RangeMap exchangeMap) {
		this.exchangeMap = exchangeMap;
	}

	public RangeMap getExchangeMap() {
		return exchangeMap;
	}

	public double getTotalNegReliableEndorsements() {
		return totalNegReliableEndorsements;
	}

	public void setTotalNegReliableEndorsements(double totalNegReliableEndorsements) {
		this.totalNegReliableEndorsements = totalNegReliableEndorsements;
	}

	public double getTotalPosReliableEndorsements() {
		return totalPosReliableEndorsements;
	}
	
	public double getAdults() {
		return adults;
	}

	public void setTotalPosReliableEndorsements(double totalPosReliableEndorsements) {
		this.totalPosReliableEndorsements = totalPosReliableEndorsements;
	}
	
	public double getNumAdults() {
		return numAdultsMarr;
	}
	
	public double getCompadres() {
		return numCompadres;
	}
	
	public double getMedianCompadre() {
		return medianCompadre;
	}
	
	public double getNumCompadres() {
		return numCompadres;
	}
	
	public double getCompFr() {
		return compFr;
	}

	public double getCompBoth() {
		return compBoth;
	}
	
	public double getCompMisc() {
		return compMisc;
	}
	
	public double getCompRel() {
		return compRel;
	}

	public boolean isNonlinearRecall() {
		return nonlinearRecall;
	}

	public void setNonlinearRecall(boolean sigmoidRecall) {
		this.nonlinearRecall = sigmoidRecall;
	}
	
	public double getCompadresTotal() {
		return numCompadresTotal;
	}
	
	public double getCompBothYr() {
		return compBothYr;
	}
	
	public double getCompMiscYr() {
		return compMiscYr;
	}
	
	public double getCompFrYr() {
		return compFrYr;
	}
	
	public double getCompRelYr() {
		return compRelYr;
	}

	public EndorsementEvaluation getEndorsementEvaluation() {
		return endorsementEvaluation;
	}

	public void setEndorsementEvaluation(EndorsementEvaluation endorsementEvaluation) {
		this.endorsementEvaluation = endorsementEvaluation;
	}

	public EndorsementWeights getEndorsementsWeights() {
		return endorsementsWeights;
	}

	public void setEndorsementsWeights(EndorsementWeights endorsementsWeights) {
		this.endorsementsWeights = endorsementsWeights;
	}

	public ExchangeMode getExchangeMode() {
		return exchangeMode;
	}

	public void setExchangeMode(ExchangeMode exchangeMode) {
		this.exchangeMode = exchangeMode;
	}	
}