package tzintzuntzan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import cern.jet.random.Exponential;
import cern.jet.random.Uniform;
import tzintzuntzan.Settings.EndorsementEvaluation;
import tzintzuntzan.Settings.EndorsementScheme;
import tzintzuntzan.Settings.EndorsementWeights;
import tzintzuntzan.Settings.ExchangeMode;
import tzintzuntzan.Settings.ExchangeType;
import tzintzuntzan.Settings.Gender;
import tzintzuntzan.Settings.MaritalStatus;
import tzintzuntzan.Settings.Profession;
import tzintzuntzan.TzintzuntzanModel.RiteDePassage;
import uchicago.src.sim.network.DefaultDrawableNode;
import uchicago.src.sim.util.Random;
import uchicago.src.sim.util.SimUtilities;

/**
 * 
 * @author shah
 *
 */

public class Person {
	private int id = -1;
	private int age = -1;
	/** Gender:{female, male} */
	private Gender gender = Gender.FEMALE;
	/** Personality trait */	
	private int[] tag;
	private double tagEvolutionPropensity = -1;
	private double base = -1;
	/** An arbitrary proportion of similar tag-bits*/
	private double proportionTagSimilarity = 0;
	/** Is alive? */
	private boolean alive = true;
	/** Reference to household. */
	private Household household = null;
	/** Reference to maiden household for a wife married and moved to husband's household. */
	private Household maidenHousehold = null;
	/** Reference to father */
	private Person father = null;
	/** Reference to mother */
	private Person mother = null;
	/** Reference to spouse */
	private Person spouse = null;
	/** Father's ID */
	private int fatherID = -1;
	/** Mother's ID */
	private int motherID = -1;
	/** Spouse's ID */
	private int spouseID = -1;
	/** Is household head? */
	private boolean householdHead = false;
	/** maritalStatus: {single, eloped, married, widowed} */
	private MaritalStatus maritalStatus = MaritalStatus.SINGLE;
	/** Ego's birth god-father */
	private int birthGodFatherID = -1;
	/** Ego's birth god-mother */
	private int birthGodMotherID = -1;
	/** Ego's marriage god-father */
	private int marriageGodFatherID = -1;
	/** Ego's birth god-mother */
	private int marriageGodMotherID = -1;
	/* Friendship related */
	/** Weights specific to friendship - defines a different/'contextualized' endorsement scheme for making friends. */
	private double[] friendshipEndorsementWeights;
	/** Upper limit for friends - U(5,7) arbitrary assumption */
	private int maxFriends = 0;
	/** Endorsement comparator - may be have to move to the model class. */
	private Comparator<Person> friendshpEndorsementComparator = new FriendshipEndorsementComparator();
	/*  Dyadic contract related */
	/** Follows an exponential distribution to gets the next time this to respond if this person requires an ordinary good. */
	private int nextOrdinaryExchangeTime = -1;
	/** Follows an exponential distribution to gets the next time this to respond if this person requires an ordinary service. */
	private int nextRitualExchangeTime = -1;	
	/** Maximum number of contracts for ego (Upper limit) U(11, 17) - arbitrary assumption [needs calibration] */
	private int maxContracts = -1;
	/** Memory recall for dynamic endorsements */
	private int recallPeriod = -1;
	/** Person's endorsement update rate - currently, same for all persons - arbitrary assumption. */
	private double endorsementUpdateRate = 0; 
	/** Endorsement scheme for ego's routine and ritual exchanges - 'contextualized weights*/
	private double[] exchangeEndorsementWeights;
	private Comparator<Person> exchangeEndorsmentComparator = new ExchangeEndorsementComparator();
	/** Endorsement scheme for ego's compadre selection - 'contextualized weights*/
	private double[] compadreSelectionWeights;
	private Comparator<Person> compadreSelectionComparator = new CompadreSelectionComparator();
	/** This is the continuous implementation of endorsements*/
	private HashMap<Integer, Endorsements> endorsementsRecord = new  HashMap<Integer, Endorsements>();
	/** Record of the sent exchanges. */
	private HashMap<Integer, ArrayList<ExchangeRecord>> sentRecord = new HashMap<Integer, ArrayList<ExchangeRecord>>();
	/** Record of the received exchanges. */
	private HashMap<Integer, ArrayList<ExchangeRecord>> receivedRecord = new HashMap<Integer, ArrayList<ExchangeRecord>>();
	/** Week in the month that the person was born. */
	private int birthWeek = -1;
	/** Birth month for incrementing age. */
	private int birthMonth = -1;
	/** Death week */
	private int deathWeek = -1;
	/** Marriage time step */
	private int marriageTick = -1;
	/** Records tick of last delivery of a baby for females */
	private int lastDelivery = -1;
	/** Is pregnancy possible for this person? */
	private boolean pregnancyPossible = false;

	/** Ego's offsprings. */
	private ArrayList<Person> offsprings = new ArrayList<Person>();
	/** Ego's siblings. */
	private ArrayList<Person> siblings = new ArrayList<Person>();
	/** Ego's known persons - i.e. all the acquaintances. */
	private ArrayList<Person> knownPersons = new ArrayList<Person>();
	/** Ego's compadre */
	private ArrayList<Person> compadreList = new ArrayList<Person>();
	/** Ego's friends */
	private ArrayList<Person> friends = new ArrayList<Person>();	
	/** List of contractants */
	private ArrayList<Person> contractants = new ArrayList<Person>();
	private ExchangeType riteDePassage = ExchangeType.NONE;
	private ArrayList<RiteDePassage> invitations = new ArrayList<RiteDePassage>();
	private TzintzuntzanModel model = null;
	private DefaultDrawableNode node = new DefaultDrawableNode();
	double reliabilityRecall = 0;	
	public boolean coFr = false, coRel = false, coMisc = false, coBoth = false;	
	public boolean fGM = false, fGF = false;

	/**
	 * Constructor for Person class.
	 * @param _id
	 * @param _household
	 * @param _model
	 */
	public Person(int _id, Household _household, TzintzuntzanModel _model) {
		this.id = _id;
		this.household = _household;
		this.model = _model;
		initialize();
	}

	/**
	 * Initialize person's attributes.
	 */
	public void initialize() {
		gender = Math.random() <= 0.5 ? Gender.FEMALE : Gender.MALE;
		birthWeek = model.getCurrentTick();
		tagEvolutionPropensity = model.getTagEvolutionPropensity();
		base = Random.uniform.nextIntFromTo(model.getMinBaseValue(), model.getMaxBaseValue());
		int length = model.getTagLength();
		int tagBase = model.getTagBase();
		int[] newTag = new int[length];
		for (int i=0; i<length; i++) {
			newTag[i] = Random.uniform.nextIntFromTo(0, tagBase - 1);			
		}
		this.tag = newTag;
		/* Arbitrary assumption: we may want to replace it with a lognormal distribution should we want to retain it.*/
		proportionTagSimilarity = Uniform.staticNextDoubleFromTo(0.5, 1);
		/* This is an arbitrary assumption */
		maxFriends = Uniform.staticNextIntFromTo(11, 17);
		/* This is an arbitrary assumption - hardwired. */
		maxContracts = Math.max(Uniform.staticNextIntFromTo(11, 17), 2*maxFriends);
		/* Now assign the update endorsement rate - same for all persons currently.*/
		endorsementUpdateRate = model.getEndorsementUpdateRate();
		/* Now assign weights - Random or Contextualized depending upon the simulation setting */
		friendshipEndorsementWeights = new double[model.totalEndorsements];
		exchangeEndorsementWeights = new double[model.totalEndorsements];
		compadreSelectionWeights = new double[model.totalEndorsements];
		// Here initialize the endorsement weights
		if (model.getEndorsementsWeights() == EndorsementWeights.CONTEXTUALIZED) {
			setContextualizedEndorsmentWeights();
		}
		// Else assign the weights as random - U(0,3)
		else {
			for (int i=0; i<model.numPosStaticEndorsements; i++) {
				friendshipEndorsementWeights[i] = Uniform.staticNextIntFromTo(0, 3);
				exchangeEndorsementWeights[i] = Uniform.staticNextIntFromTo(0, 3);
				compadreSelectionWeights[i]= Uniform.staticNextIntFromTo(0, 3);			
				//model.print("Endorsement: " + model.returnStaticEndorsementLabel(i) + " friend weight: " + friendshipEndorsementWeights[i]);
			}
			for (int i=model.numPosStaticEndorsements; i<model.numStaticEndorsments; i++) {
				friendshipEndorsementWeights[i] = -1*Uniform.staticNextIntFromTo(0, 3);
				exchangeEndorsementWeights[i] = -1*Uniform.staticNextIntFromTo(0, 3);
				compadreSelectionWeights[i]= -1*Uniform.staticNextIntFromTo(0, 3);
				//model.print("Endorsement: " + model.returnStaticEndorsementLabel(i)  + " friend weight: " + friendshipEndorsementWeights[i]);
			}
			for (int i=model.numStaticEndorsments; i<model.numStaticEndorsments+model.numPosDyEndorsments; i++) {
				friendshipEndorsementWeights[i] = Uniform.staticNextIntFromTo(0, 3);
				exchangeEndorsementWeights[i] = Uniform.staticNextIntFromTo(0, 3);
				compadreSelectionWeights[i]= Uniform.staticNextIntFromTo(0, 3);
				//model.print("Endorsement: " + model.returnDynamicEndorsementLabel(i-model.numStaticEndorsments) + " friend weight: " + friendshipEndorsementWeights[i]);
			}
			for (int i=model.numStaticEndorsments+model.numPosDyEndorsments; i<model.totalEndorsements; i++) {
				friendshipEndorsementWeights[i] = -1*Uniform.staticNextIntFromTo(0, 3);
				exchangeEndorsementWeights[i] = -1*Uniform.staticNextIntFromTo(0, 3);
				compadreSelectionWeights[i]= -1*Uniform.staticNextIntFromTo(0, 3);
				//model.print("Endorsement: " + model.returnDynamicEndorsementLabel(i-model.numStaticEndorsments) + " friend weight: " + friendshipEndorsementWeights[i]);
			}
//			for (int i=0; i<model.totalEndorsements; i++) {				
//				friendshipEndorsementWeights[i] = Uniform.staticNextDoubleFromTo(0, 3);
//				exchangeEndorsementWeights[i] = Uniform.staticNextDoubleFromTo(0, 3);
//				compadreSelectionWeights[i]= Uniform.staticNextDoubleFromTo(0, 3);
//			}
		}		
		// Arbitrary assumption - avg. 10 years (480 weeks)
		do {
			//			recallPeriod = (int) model.getPsr().nextLogistic(373, 41);
			recallPeriod = (int) model.getPsr().nextLogistic(179, 37);
		} while (recallPeriod <= 96 || recallPeriod > 720);
		do {
			reliabilityRecall = model.getPsr().nextLogistic(61, 19);
		} while (reliabilityRecall > 72 || reliabilityRecall < 24);			
		//		do {
		//			reliabilityRecall = model.getPsr().nextLogistic(41, 23);
		//		} while (reliabilityRecall <= 36);		
	}

	/** 
	 * We assume no extra-marital or unmarried pregnant persons in the model.
	 * Arbitrary assumption for starting age to be eligible for marriage for both females and males as well as for fertility.
	 * @return
	 */
	public void update(int currentTick) {
		if (!alive) {
			return;
		}
		if (model.getMonth() == birthMonth
				&& model.getWeek() == birthWeek) {
			age++;
		}
		if (gender.equals(Gender.FEMALE)) {
			if (maritalStatus.equals(MaritalStatus.SINGLE)
				&& isAdult()
				//assumption: for female marriage limit - same as fertility
				&& age <= Settings.FEMALE_MARRIAGE_LIMIT
			) {
				model.getEligibleFemales().add(this);
			}
			else if (maritalStatus.equals(MaritalStatus.MARRIED)
				&& spouse != null
				&& spouse.isAlive()
				&& age >= Settings.ADULT_STARTING_AGE + 1 
				&& age <= Settings.FEMALE_FERTILITY_LIMIT
				&& (lastDelivery == -1 || lastDelivery < (currentTick - 48))) {
				pregnancyPossible = true;
				model.getPotentialMothers().add(this);
			}
		}
		else if (gender.equals(Gender.MALE)) {
			if (maritalStatus.equals(MaritalStatus.SINGLE)
				&& isAdult()
				// assumption: for male marriage limit
				&& age <= Settings.MALE_MARRIAGE_LIMIT
				/* Adding another condition restricting  only one male per household to be eloped at a time. See household.elopedMaleID. */
				&& household.getElopedMaleID() == -1
			) {
				model.getEligibleMales().add(this);
			}
			else if (maritalStatus.equals(MaritalStatus.MARRIED)) {
				if (household.getHead().getId() != id
						&& spouse != null
						&& spouse.isAlive()
						&& Math.random() <= model.getBuildNuclearHouseRate()
						&& !household.isBuildNewHousehold()
						&& !isOnlySonInHousehold()
				) {
					household.setHouseholdBuildTime(id);
				}
			}
		}
		if (model.getNumSharedInterests() >= 1
				&& isAdult()
				&& birthMonth == model.getMonth()
				&& birthWeek == model.getWeek()) {
			model.getAdultThisWeek().add(this);
			model.assignSharedInterests(this);	
		}
		if (isAdult()) {
			model.getAdultsList().add(this);
			updateExcangeTime(currentTick);
		} 
		removeRandomFriend();
	}

	/**
	 * Invite adult known persons as potential contractants to contribute to the service for Rite de Passage. 
	 * Arbitrary assumptions - to be validated.
	 * @param invitation
	 */
	public ArrayList<Person> sendInvitationRiteDePassage(RiteDePassage invitation) {
		ArrayList<Person> invitees = new ArrayList<Person>();
		for (Person person : knownPersons) {
			if (person != null
					&& person.isAlive()
					&& person.isAdult()
			) {
				switch(invitation.serviceType) {
				case BIRTH_SERVICE:
				case MARRIAGE_SERVICE:
					if (isRelative(person)
							|| friends.contains(person)
							|| compadreList.contains(person)) {
						invitees.add(person);
					}
					break;
				case ELOPEMENT_SERVICE:
					if (compadreList.contains(person)) {
						invitees.add(person);
					}
					break;									
				case DEATH_SERVICE:
					invitees.add(person);
				default:break;
				}
				if (isGodParentOrGodChild(person)
						&& !invitees.contains(person)) {
					invitees.add(person);
				}
			}
		}
		if (father != null && father.isAlive() && !invitees.contains(father)) {
			invitees.add(father);
		}
		if (mother != null && mother.isAlive() && !invitees.contains(mother)) {
			invitees.add(mother);
		}
		return invitees;
	}

	/**
	 * Is this person endorsed as a relative? includes siblings
	 * @param person
	 * @return
	 */
	public boolean isRelative(Person person) {
		int index = model.returnStaticEndorsementIndex("is-relative");
		try {
			if (endorsementsRecord.isEmpty()) {
				return false;
			}
			if (endorsementsRecord.containsKey(person.getId())) {
				if (siblings.contains(person)
						|| endorsementsRecord.get(person.getId()).getStaticEndorsements()[index]) {
					return true;
				}
			}			
		} catch (Exception e) {e.printStackTrace();}
		return false;
	}

	/**
	 * Is this person god parent or god child
	 * @param person
	 * @return
	 */
	public boolean isGodParentOrGodChild(Person person) {
		int index1 = model.returnStaticEndorsementIndex("is-god-child");
		int index2 = model.returnStaticEndorsementIndex("is-godparent");
		if (endorsementsRecord.containsKey(person.getId())) {
			if (endorsementsRecord.get(person.getId()).getStaticEndorsements()[index1]
			                || endorsementsRecord.get(person.getId()).getStaticEndorsements()[index2]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is this person endorsed as a relative?
	 * @param person
	 * @return
	 */
	public boolean isNeighbor(Person person) {
		int index = model.returnStaticEndorsementIndex("is-neighbor");
		if (endorsementsRecord.containsKey(person.getId())) {
			if (endorsementsRecord.get(person.getId()).getStaticEndorsements()[index]) {
				return true;
			}
		}
		return false;
	}

	/** Handle an invitation for a service concerning the rite de passage of a person.*/
	public void handleRiteDePassageInvitations() {
		ArrayList<Person> temp = new ArrayList<Person>();
		SimUtilities.shuffle(invitations);		
		for (RiteDePassage invitation : invitations) {
			if (invitation.person != null
					&& id != invitation.person.getId()
					&& !temp.contains(invitation.person) 
					&& !invitation.serviceType.equals(ExchangeType.NONE)
			) {
				temp.add(invitation.person);
				String godParentType = isGodparent(invitation.person);
				if (godParentType != "") {
					doGodParentService(invitation, godParentType);
				}
				/* We assume that only the birth god parents play their role in settling arguments/disputes following elopements
				 * It may be a hard assumption here.*/
				else if (shouldSendRiteDePassage(invitation)) {
					if (gender.equals(Gender.MALE)
						&& invitation.person.getFather() != null
						&& invitation.person.getFather().isAlive()
					) {
						sendExchange(invitation.person.getFather(), invitation.serviceType);
					}
					else if (gender.equals(Gender.FEMALE)
						&& invitation.person.getMother() != null
						&& invitation.person.getMother().isAlive()) {
						sendExchange(invitation.person.getMother(), invitation.serviceType);
					}
				}
			}
		}
	}

	/**
	 * We assume that only the birth god parents play their role in settling arguments/disputes following elopements.
	 * It may be a hard assumption here.
	 * @param invitation
	 * @return
	 */
	private boolean shouldSendRiteDePassage(RiteDePassage invitation) {
		Person personFather=null, personMother=null;
		if (invitation.person.getFatherID() != -1) {
			personFather = (Person) model.getPersonMap().get(invitation.person.getFatherID());
		}
		if (invitation.person.getMotherID() != -1) {
			personMother = (Person) model.getPersonMap().get(invitation.person.getMotherID());
		}		
		if (personFather != null 
				&& personFather.isAlive()) {
			if (personFather.getId() == id
					|| contractants.contains(personFather)) {
				return true;
			}
		}
		if (personMother != null
				&& personMother.isAlive()) {
			if (personMother.getId() == id
					|| contractants.contains(personMother)) {
				return true;
			}
		}						
		if (invitation.person.getSpouse() != null
				&& invitation.person.getSpouse().isAlive()) {
			if (contractants.contains(invitation.person.getSpouse())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check for adult age - inline function.
	 * @param person
	 * @return
	 */
	public boolean isAdult() {
		return age >= Settings.ADULT_STARTING_AGE ? true : false; 
	}

	/**
	 * Contains arbitrary assumption for the value of service/good given by a god parent.
	 * TODO: Just a token money - to be incorporated once we introduce economic constrains 
	 * @param invitation
	 * @param godParentType
	 */
	private void doGodParentService(RiteDePassage invitation, String godParentType) {
		double factor = 1;
		int recipientID = -1;
		if (gender.equals(Gender.FEMALE)) {
			if (invitation.person.getMother() != null
					&& invitation.person.getId() != id
					&& invitation.person.getMother().isAlive()) {
				recipientID = invitation.person.getMother().getId();
				// arbitrary assumption - 1/3 of the value from the god mother.
				factor = 0.3333;
			}
		}
		else if (gender.equals(Gender.MALE)) {
			if (invitation.person.getFather() != null
					&& invitation.person.getId() != id
					&& invitation.person.getFather().isAlive()) {
				recipientID = invitation.person.getFather().getId();
			}			
		}
		if (recipientID == -1) {
			return;
		}
		double value = 0;
		/* TODO: Just a token money - to be incorporated once we introduce economic constrains */
		switch(invitation.serviceType) {
		case BIRTH_SERVICE:
			value = model.getPsr().nextLogNormal(3, 0.7);
			break;
		case ELOPEMENT_SERVICE:
			value = model.getPsr().nextLogNormal(2, 0.11);
			break;
		case MARRIAGE_SERVICE:
			if (godParentType == "marriageGodParent") {
				value = model.getPsr().nextLogNormal(4, 0.13);	
			}
			else {
				value = model.getPsr().nextLogNormal(2, 0.7);	
			}
			break;
		case DEATH_SERVICE:
			if (invitation.person.isAdult()) {
				value = model.getPsr().nextLogNormal(3, 0.7);	
			}
			else {
				value = model.getPsr().nextLogNormal(2, 0.11);	
			}
			break;
		default:break;
		}	
		ExchangeRecord exchangeRecord = new ExchangeRecord();
		exchangeRecord.setTimestep(model.getCurrentTick());
		exchangeRecord.setExchangeType(invitation.serviceType);
		exchangeRecord.setExchangeValue((int)(value*factor));
		exchangeRecord.setSenderID(id);
		exchangeRecord.setReceiverID(recipientID);
		if (!sentRecord.containsKey(recipientID)) {
			sentRecord.put(recipientID, new ArrayList<ExchangeRecord>());
		}
		sentRecord.get(recipientID).add(exchangeRecord);
		model.mailbox.add(exchangeRecord);				
	}

	/**
	 * Check if I am a god-parent of this person.
	 * We assume that being a marriage god parent is more significant than baptismal god parent.
	 * Of course, if the person isn't married and/or young, they would only have a birth god parent.
	 * @param person
	 * @return
	 */
	public String isGodparent(Person person) {
		String gp = "";
		if (id == person.getBirthGodMotherID()
				|| id == person.getBirthGodFatherID()) {
			gp = "birthGodParent";
		}
		else if (id == person.getBirthGodMotherID()
				|| id == person.getBirthGodFatherID()) {
			gp = "marriageGodParent";
		}
		return gp;
	}

	/**
	 * Method that sends a service or a good (Exchange) to a person with the associated type.
	 * @param person
	 * @param type
	 */
	public void sendExchange(Person person, ExchangeType type) {
		ExchangeRecord exchangeRecord = new ExchangeRecord();
		exchangeRecord.setTimestep(model.getCurrentTick());
		exchangeRecord.setExchangeType(type);
		exchangeRecord.setExchangeValue((int)model.returnExchangeValue(type));
		exchangeRecord.setSenderID(id);
		exchangeRecord.setReceiverID(person.getId());
		if (!sentRecord.containsKey(person.getId())) {
			sentRecord.put(person.getId(), new ArrayList<ExchangeRecord>());
		}
		sentRecord.get(person.getId()).add(exchangeRecord);
		model.mailbox.add(exchangeRecord);				
	}

	/** Remove a friend randomly and endorse them as 'is-friend-in-the-past */
	public void removeRandomFriend() {
		String label = "is-friend-in-past";
		ArrayList<Person> temp = new ArrayList<Person>(friends);
		for (Person friend : temp) {
			if (Math.random() <= (double) model.getElopementRate()
					&& friend != null
					&& friend.isAlive()) {
				friends.remove(friend);
				friend.getFriends().remove(this);
				endorsePerson(friend, label);
				friend.endorsePerson(this, label);
			}
		}
	}

	/**
	 * Check if I am the only son remaining in the household?
	 * @return
	 */
	public boolean isOnlySonInHousehold() {
		boolean isOnlySonInHousehold = true;
		try {
			for (Person sibling : siblings) {
				if (sibling != null
						&& sibling.getGender().equals(Gender.MALE)
							&& sibling.isAlive()
							&& sibling.getHousehold().getId() == household.getId()
				) {
					isOnlySonInHousehold = false;
					break;
				}
			}			
		} catch (Exception e) {e.printStackTrace();}
		return isOnlySonInHousehold;
	}

	/**
	 * 	We use a random activation regime, following Axtell (2000) with each agent's next activity wait time is based on the exponential distribution. 
	 * @param currentTick	 
	 */
	public void updateExcangeTime(int currentTick) {
		if (nextOrdinaryExchangeTime == -1
				|| nextOrdinaryExchangeTime < currentTick) {
			nextOrdinaryExchangeTime = (int) (currentTick 
					+ Exponential.staticNextDouble((double)1/model.getMeanOrdinaryExchangeTime()) + 1);
		}
		if (nextRitualExchangeTime == -1
				|| nextRitualExchangeTime < currentTick) {
			nextRitualExchangeTime = (int) (currentTick 
					+ Exponential.staticNextDouble((double)1/model.getMeanFiestaExchangeTime()) + 1);
		}		
	}

	/**
	 * 
	 * @param currentTick
	 */
	public void doDyadicExchange(int currentTick) {
		if (model.getExchangeMode() == ExchangeMode.COMBINED) {
			doDyadicExchangeCombined(currentTick);
		}
		else if (model.getExchangeMode() == ExchangeMode.SEPARATE) {
			doDyadicExchangeSeparate(currentTick);
		}
	}

	/**
	 * Perform dyadic exchange.
	 * We select individuals to send a routine good or service.
	 * They are selected from
	 * - compadre
	 * - kin
	 * - friends
	 * We anticipate reciprocation in future.
	 * It takes some exchanges to establish contracts and once they are established they go on.
	 * If not enough exchanges are going on from both sides, ego will look for others in the known persons list
	 * or expand its network through building compadre.
	 * 
	 * There is at least one type of exchange for this person at this tick. 
	 * The two exchanges below are considered independent.
	 */
	public void doDyadicExchangeCombined(int currentTick) {
		ArrayList<Person> knownAdults = new ArrayList<Person>();
		for (Person person : knownPersons) {
			if (person != null
					&& person.isAlive()
					&& person.isAdult()) {
				knownAdults.add(person);
			}
		}
		sortExchangeEndorsments();
		if (nextOrdinaryExchangeTime == currentTick) {
			doOrdinaryExchangeCombined(currentTick, knownAdults);
		}
		if (nextRitualExchangeTime == currentTick) {
			doRitualExchangeCombined(currentTick, knownAdults);
		}						
	}

	/**
	 * Do dyadic exchange for separate social institutions.
	 * @param currentTick
	 */
	public void doDyadicExchangeSeparate(int currentTick) {
		ArrayList<Person> ordinaryExchangeList = new ArrayList<Person>(), ritualExchangeList = new ArrayList<Person>(); 
		for (Person person : knownPersons) {
			if (person != null
					&& person.isAlive()
					&& person.isAdult()) {
				if (isNeighbor(person)
						|| friends.contains(person)) {
					if (!ordinaryExchangeList.contains(person)) {
						ordinaryExchangeList.add(person);
					}

				}
				if (isRelative(person)
						|| compadreList.contains(person)) {
					if (!ritualExchangeList.contains(person)) {
						ritualExchangeList.add(person);
					}					
				}
			}		
		}
		if (nextOrdinaryExchangeTime == currentTick) {
			doOrdinaryExchangeSeparate(ordinaryExchangeList);
		}
		if (nextRitualExchangeTime == currentTick) {
			doRitualExchangeSeparate(ritualExchangeList);
		}						
	}

	/**
	 * 
	 * @param ordinaryExchangeList
	 */
	public void doOrdinaryExchangeSeparate(ArrayList<Person> ordinaryExchangeList) {
		Collections.sort(ordinaryExchangeList, exchangeEndorsmentComparator);
		//arbitrary assumption
		//SJA
		//		int numSentExchanges = (int) ((double) Uniform.staticNextDoubleFromTo(0.2, 0.4) * ordinaryExchangeList.size());
		int numSentExchanges = (int) ((double) Uniform.staticNextDoubleFromTo(0.4, 0.6) * ordinaryExchangeList.size());
		ExchangeType type = ExchangeType.NONE;
		for (int i=0; i<numSentExchanges; i++) {
			Person person = (Person) ordinaryExchangeList.get(i);
			if (person != null) {
				type = Math.random() <= 0.5 ? ExchangeType.ORDINARY_GOOD : ExchangeType.ORDINARY_SERVICE;
				sendExchange(person, type);				
			}
		}
	}
	
	/**
	 * 
	 * @param ritualExchangeList
	 */
	public void doRitualExchangeSeparate(ArrayList<Person> ritualExchangeList) {
		Collections.sort(ritualExchangeList, exchangeEndorsmentComparator);
		//arbitrary assumption
		int numSentExchanges = (int) ((double) Uniform.staticNextDoubleFromTo(0.2, 0.4) * ritualExchangeList.size());
		ExchangeType type = ExchangeType.NONE;
		for (int i=0; i<numSentExchanges; i++) {
			Person person = (Person) ritualExchangeList.get(i);
			if (person != null) {
				type = Math.random() <= 0.5 ? ExchangeType.FIESTA_SERVICE : ExchangeType.FIESTA_SERVICE;
				sendExchange(person, type);				
			}
		}
	}						

	/**
	 * TODO: arbitrary assumption for calculating numSentExchanges for ordinary exchange.
	 * @param currentTick
	 */
	public void doOrdinaryExchangeCombined(int currentTick, ArrayList<Person> knownAdults) {
		// arbitrary assumption
		int numSentExchanges = (int) Math.max(maxContracts, 0.25*knownPersons.size());			
		for (int i=0; i<numSentExchanges; i++) {
			ExchangeType type = ExchangeType.NONE;
			try {
				Person person = (Person) knownAdults.get(i);
				// arbitrary assumption
				type = Math.random() <= 0.5 ? ExchangeType.ORDINARY_GOOD : ExchangeType.ORDINARY_SERVICE;
				sendExchange(person, type);
			} catch (Exception e) {e.printStackTrace();}		
		}
	}

	/**
	 * Handle ordinary and ritual exchange records received from known persons.
	 * @param exchangeRecord
	 */
	public void handleExchangeRecord(ExchangeRecord exchangeRecord) {
		int senderID = exchangeRecord.getSenderID(); 
		if (senderID != id
				&& exchangeRecord.getReceiverID() == id) {
			if (!receivedRecord.containsKey(senderID)) {
				receivedRecord.put(senderID, new ArrayList<ExchangeRecord>());
			}
			receivedRecord.get(senderID).add(exchangeRecord);
		}
	}

	/**
	 * TODO: arbitrary assumption for calculating numSentExchanges for ritual exchange
	 * Currently same implementation as that of the ordinary exchange. 
	 * The only difference is in the type of ritual exchange and its 'value'.
	 * @param currentTick
	 */
	public void doRitualExchangeCombined(int currentTick, ArrayList<Person> knownAdults) {
		//arbitrary assumption 
		int numSentExchanges = (int) Math.max(maxContracts, 0.25*knownPersons.size());			
		for (int i=0; i<numSentExchanges; i++) {
			ExchangeType type = ExchangeType.NONE;
			try {
				Person person = (Person) knownAdults.get(i);
				ExchangeRecord exchangeRecord = new ExchangeRecord();
				exchangeRecord.setTimestep(currentTick);
				type = Math.random() <= 0.5 ? ExchangeType.FIESTA_GOOD : ExchangeType.FIESTA_SERVICE;
				exchangeRecord.setExchangeType(type);
				exchangeRecord.setExchangeValue((int)model.returnExchangeValue(type));
				exchangeRecord.setSenderID(id);
				exchangeRecord.setReceiverID(person.getId());
				if (!sentRecord.containsKey(person.getId())) {
					sentRecord.put(person.getId(), new ArrayList<ExchangeRecord>());
				}
				sentRecord.get(person.getId()).add(exchangeRecord);
				model.mailbox.add(exchangeRecord);				
			} catch (Exception e) {e.printStackTrace();}		
		}
	}

	/**
	 * The birth god parents are found by the parents by this person.
	 * Called by baby's object at the time of birth.
	 * The first entry to the returned array is the god mother and the second entry is the god father.
	 */
	public Person[] findGodParents() {
		Person[] godParents = new Person[2];
		godParents[0] = null; godParents[1] = null;
		try {
			ArrayList<Person> faPotGP = new ArrayList<Person>(), 
			moPotGP = new ArrayList<Person>();
			Person fat = null, mot = null;
			if (father != null && father.isAlive()) {
				faPotGP = father.returnPotentialCompadre();
				fat = father;
			}
			else {
				Person birthGF = (Person) model.getPersonMap().get(birthGodFatherID);
				if (birthGF != null
						&& birthGF.isAlive()) {
					faPotGP = birthGF.returnPotentialCompadre();
					fat = birthGF;
				}
			}			
			if (mother != null && mother.isAlive()) {
				moPotGP = mother.returnPotentialCompadre();
				mot = mother;
			}
			else {
				Person birthGM = (Person) model.getPersonMap().get(birthGodMotherID);
				if (birthGM != null	&& birthGM.isAlive()) {
					moPotGP = birthGM.returnPotentialCompadre();
					mot = birthGM;
				}
			}
			if (faPotGP.isEmpty() && moPotGP.isEmpty()) {
				return godParents;
			}
			Person gP = null, gM = null, gF = null;
			int totalNum = Math.max(faPotGP.size(), moPotGP.size());
			boolean fa = false, mo = false;
			for (int i=0; i<totalNum; i++) {			
				fa = false; mo = false;
				gP = null;
				if (i < faPotGP.size()) {
					gP = faPotGP.get(i);
					fa = true;					
				}
				if (i < moPotGP.size()) {
					gP = moPotGP.get(i);
					mo = true;
				}
				if (fa && mo) {
					if (Math.random() <= 0.5) {
						gP = moPotGP.get(i);
						mot.getCompRelations(gP);
					} else {
						gP = faPotGP.get(i);
						fat.getCompRelations(gP);
					}
					//gP = Math.random() <= 0.5 ? moPotGP.get(i) : faPotGP.get(i);
				}
				if (gP.getMaritalStatus().equals(MaritalStatus.MARRIED)
					&& gP.getSpouse() != null
					&& gP.getSpouse().isAlive()) {
					if (gP.getGender().equals(Gender.FEMALE)) {
						gF = gP;
						gM = gP.getSpouse();
					} 
					else {
						gM = gP;
						gF = gP.getSpouse();						
					}
					break;
				}
				else {
					if (gP.getGender().equals(Gender.FEMALE)) {
						gF = gP;
					}
					else {
						gM = gP;
					}					
				}
				if (gM != null && gF != null) {
					break;
				}	
			}
			if (gM != null && gF != null) {
				godParents[0] = gM;
				godParents[1] = gF;
			}
			else {
				if (gM != null) {
					godParents[0] = gM;
				}
				else if (gF != null){
					godParents[1] = gF;
				}
			}
		} catch (Exception e){e.printStackTrace();}
		return godParents;
	}

	/**
	 * Called by mother or father of the person to pick potential compadre as their daughter/son's birth or marriage god parents.
	 * TODO: Preference of friends over relatives for birth god parents.
	 * @return
	 */
	public ArrayList<Person> returnPotentialCompadre() {
		/* Sort known persons' list based on the compadre endorsements*/
		findFriends();
		sortCompadreSelection();	
		//int size = Uniform.staticNextIntFromTo(maxContracts, maxContracts*2);
		int size = knownPersons.size();
		ArrayList<Person> potentialCompadre = new ArrayList<Person>(size);
		try {
			for (Person person : knownPersons) {
				if (potentialCompadre.size() == size) {
					break;
				}			
				if (person != null
						&& person.isAlive()
						&& person.isAdult()
				) {
					potentialCompadre.add(person);
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return potentialCompadre;
	}		

	/**
	 * Functions that checks the possibility of elopement of a couple - 
	 * Note that, we assume random marriage pattern with a high bias for similar age and checking for incest. 
	 * @param potentialWife
	 * @return
	 */
	public boolean shouldElope(Person potentialWife) {
		/* First check if already taken */
		if (potentialWife.getMaritalStatus().equals(MaritalStatus.SINGLE) == false
			|| potentialWife.getHousehold().getId() == household.getId()) {
			return false;
		}
		/* Then check for prohibitions, if any.*/
		if (isIncestProhibition(potentialWife)
				|| potentialWife.isIncestProhibition(this)) {
			return false;			
		}
		/* We now select with a bias for similar age but low probability for a larger difference in age - arbitrary assumption. */
		double ageFactor = 1;
		int diff = age - potentialWife.getAge();
		if (diff <= 5) {
			ageFactor = 1;		
		}
		else if (diff > 5 && diff <= 9) {
			ageFactor = 0.9;
		}
		else if (diff > 9 && diff <= 15) {
			ageFactor = 0.75;
		}
		else if (diff > 15 && diff <= 20) {
			ageFactor = 0.5;
		}
		else if (diff > 20 || (diff <= 0 && diff >= -3)) {
			ageFactor = 0.2;	
		}
		else {
			ageFactor = 0;
		}
		return Math.random() <= model.getElopementRate() * ageFactor ? true : false;
	}

	/**
	 * Returns true if prohibition rule applies, i.e. 
	 * marriage is impossible;
	 * false, otherwise.
	 * 
	 * We only check for mother as we assume no extra-marital relations, no polygamy,
	 * no polyandry, no divorce, no re-marriages of widows/widowers.
	 * We first check with nuclear family and then for uncles and aunts.
	 * 
	 * For this version, we are not checking for grandparents in this function.
	 * 	 
	 * @param person
	 * @return
	 */
	public boolean isIncestProhibition(Person person) {
		if (motherID == person.getId() 
				|| fatherID == person.getId()
				|| siblings.contains(person)) {
			return true;
		}
		if (fatherID != -1
				&& father != null
				&& father.getFatherID() != -1
				&& father.getFatherID() == person.getFatherID()) {
			return true;
		}
		if (motherID != -1
				&& mother != null
				&& mother.getFatherID() != -1
				&& mother.getFatherID() == person.getFatherID()) {
			return true;
		}
		if (spouseID == person.getId()
				|| offsprings.contains(person)
				|| person.getOffsprings().contains(this)) {
			return true;
		}
		if(birthGodFatherID == person.getId()
				|| birthGodMotherID == person.getId()
				|| marriageGodFatherID == person.getId()
				|| marriageGodMotherID == person.getId()
		) {
			return true;
		}
		return false;
	}

	/**
	 * We include parents, offsprings and spouse as nuclear relation.
	 * Note that siblings are endorsed as known persons and endorsed as 'is-sibling'
	 * @param person
	 * @return
	 */
	public boolean isNuclearRelation(Person person) {
		if (fatherID == person.getId()
				|| motherID == person.getId()
				|| spouseID == person.getId()
				|| offsprings.contains(person)) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Return pure nuclear check only.  
	 * @param person
	 * @return
	 */
	public boolean isPureNuclearRelation(Person person) {
		if (spouseID == person.getId()
				|| offsprings.contains(person)) {
			return true;
		}
		else {
			return false;
		}		
	}

	/** For a newly-born or a newly inducted person in the system, we assign the static endorsements: 
	 * "is-relative", "is-closeRelative", "is-neighbor".   
	 */
	public void initializeStaticEndorsments() {
		String label = "is-neighbor";
		for (Integer neighborHouseholdID : household.getNeighbors()) {
			try {
				Household neighborHousehold = (Household) model.getHouseholdMap().get(neighborHouseholdID);
				if (neighborHousehold != null) {
					for (Person neighbor : neighborHousehold.getMembers()) {
						if (neighbor != null
								&& neighbor.isAlive()) {
							endorsePerson(neighbor, label);
							neighbor.endorsePerson(this, label);
						}
					}
				}
			} catch (Exception e) {e.printStackTrace();}		
		}
		label = "is-relative";
		for (Integer relativeHouseholdID : household.getRelativeHouseholds()) {
			try {
				Household relativeHousehold = (Household) model.getHouseholdMap().get(relativeHouseholdID);
				if (relativeHousehold != null) {
					for (Person relative : relativeHousehold.getMembers()) {
						if (relative != null
								&& relative.isAlive()) {
							endorsePerson(relative, label);
							relative.endorsePerson(this, label);	
						}
					}
				}
			} catch (Exception e) {e.printStackTrace();}		
		}			
		label = "";
		for (Person person : household.getMembers()) {
			label = "";
			if (person != null
					&& person.isAlive()
					&& id != person.getId()) { 
				if (!isNuclearRelation(person)) {
					label = "is-relative";	
				}
				if (siblings.contains(person)) {
					label = "is-sibling";
				}	
				endorsePerson(person, label);
				person.endorsePerson(this, label);				
			}
		}
	}

	/** Add siblings to the new born and the other way round. */
	public void addSiblings() {
		for (Person sibling : mother.getOffsprings()) {
			if (id != sibling.getId()) {
				if (!siblings.contains(sibling)) {
					siblings.add(sibling);
				}
				if (!sibling.getSiblings().contains(this)) {
					sibling.getSiblings().add(this);
				}
			}
		}
	}

	/** Find friends.*/
	public void findFriends() {
		friends.clear();
		sortFriendsEndorsements();
		for (int i=0; i<maxFriends; i++) {
			if (i >= knownPersons.size()) {
				break;
			}
			Person potentialFriend = (Person) knownPersons.get(i);
			if (potentialFriend != null
					&& potentialFriend.isAlive()
					&& potentialFriend.isAdult()
					&& !pastFriendThisWeek(potentialFriend)
					&& !friends.contains(potentialFriend)) {
				endorsePerson(potentialFriend, "is-friend");
				friends.add(potentialFriend);
			}
		}
		// arbitrary assumption
		//int counter = 0;
		/*		double rnd = Uniform.staticNextDoubleFromTo(1, 1.5);
		do {
			if (counter < knownPersons.size() 
					&& Math.random() <= 1/rnd
			) {
				Person potentialFriend = (Person) knownPersons.get(counter);
				if (potentialFriend != null
						&& potentialFriend.isAlive()
						&& !pastFriendThisWeek(potentialFriend)) {
					endorsePerson(potentialFriend, "is-friend");
					friends.add(potentialFriend);					
				}
			}
			counter++;			
		} while (friends.size() <= maxFriends);
		 */	
	}

	/**
	 * Was this a friend a week ago?
	 * @param potentialFriend
	 * @return
	*/
	public boolean pastFriendThisWeek(Person potentialFriend) {
		Integer pfID = new Integer(potentialFriend.getId());
		boolean flag = false;
		int index = model.returnDynamicEndorsementIndex("is-friend-in-past");
		if (endorsementsRecord.containsKey(pfID)
				&& checkDynamicEndorsement(pfID, model.getCurrentTick())) {
			if (endorsementsRecord.get(pfID).getDynamicEndorsements().get(model.getCurrentTick()).contains(index)) {
				flag = true;
			}

		}
		return flag;
	}

	/**
	 * 
	 * @param personID
	 * @param timestep
	 * @return
	 */
	private boolean checkDynamicEndorsement(int personID, int timestep) {
		if (endorsementsRecord.get(personID).getDynamicEndorsements().containsKey(timestep)) {
			return true;
		}
		else {
			return false;
		}
	}

	public void findContractsSeparate() {
		contractants.addAll(friends);
		ArrayList<Person> temp = new ArrayList<Person>();
		contractants = (ArrayList<Person>)Utilities.Union(contractants, compadreList);
		for (Household neighborHousehold : household.returnRelativesHouseholds()) {
			for (Person person : neighborHousehold.getMembers()) {
				if (person != null
						&& person.isAlive()
						&& !temp.contains(person)
						&& person.isAdult()
				) {
					temp.add(person);
				}				
			}			
		}
		sortExchangeEndorsments(temp);		
		int numPotentialContracts = (int) Uniform.staticNextDoubleFromTo(0.25, 0.4) * temp.size();		
		contractants = (ArrayList<Person>)Utilities.Union(contractants, temp.subList(0, numPotentialContracts));
	}

	/**
	 * Create contract.
	 */
	public void findContractsCombined() {
		int timeLim = model.getCurrentTick() - recallPeriod;
		if (timeLim <= 0) {
			timeLim = model.getCurrentTick();
		}
		try {
			ArrayList<Person> temp = new ArrayList<Person>();
			for (Person person : knownPersons) {
				if (person != null
						&& person.isAlive()
						&& person.isAdult()
				) {
					temp.add(person);
				}
			}
			Collections.sort(temp, exchangeEndorsmentComparator);			
			int limit = Math.max(maxContracts, temp.size());
			for (int i=0; i<limit; i++) {
				endorsePerson(temp.get(i), "is-contractant");
			}			
		} catch (Exception e) {e.printStackTrace();}		
	}

	/** Comparator for friendship endorsements for ego's known persons */
	class FriendshipEndorsementComparator implements Comparator<Person> {
		public int compare(Person person1, Person person2) {
			double e1 = 0, e2 = 0;
			EndorsementScheme scheme = EndorsementScheme.FRIEND_SELECTION;
			if (model.getEndorsementEvaluation() == EndorsementEvaluation.DISCRETE) {
				e1 = calculateDiscreteEndorsement(person1, scheme); 
				e2 = calculateDiscreteEndorsement(person2, scheme);				
			}
			else if (model.getEndorsementEvaluation() == EndorsementEvaluation.CONTINUOUS) {
				e1 = calculateContinuousEndorsement(person1, scheme);
				e2 = calculateContinuousEndorsement(person2, scheme);
			}
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

	/** Comparator for compadre selection from ego's known persons or the persons needed to be sorted based on their endorsement values.*/
	class CompadreSelectionComparator implements Comparator<Person> {
		public int compare(Person person1, Person person2) {
			double e1 = 0, e2 = 0;
			EndorsementScheme scheme = EndorsementScheme.COMPADRE_SELECTION;
			if (model.getEndorsementEvaluation() == EndorsementEvaluation.DISCRETE) {
				e1 = calculateDiscreteEndorsement(person1, scheme); 
				e2 = calculateDiscreteEndorsement(person2, scheme);					
			}
			else if (model.getEndorsementEvaluation() == EndorsementEvaluation.CONTINUOUS) {
				e1 = calculateContinuousEndorsement(person1, scheme);
				e2 = calculateContinuousEndorsement(person2, scheme);
			}
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

	/** Comparator for exchange endorsements for ego's known persons or the persons needed to be sorted based on their endorsement values. */
	class ExchangeEndorsementComparator implements Comparator<Person> {
		public int compare(Person person1, Person person2) {
			double e1 = 0, e2 = 0;
			EndorsementScheme scheme = EndorsementScheme.DYADIC_EXCHANGE;
			if (model.getEndorsementEvaluation() == EndorsementEvaluation.DISCRETE) {
				e1 = calculateDiscreteEndorsement(person1, scheme); 
				e2 = calculateDiscreteEndorsement(person2, scheme);
			}
			else if (model.getEndorsementEvaluation() == EndorsementEvaluation.CONTINUOUS) {
				e1 = calculateContinuousEndorsement(person1, EndorsementScheme.DYADIC_EXCHANGE);
				e2 = calculateContinuousEndorsement(person2, EndorsementScheme.DYADIC_EXCHANGE);
			}
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

	public void sortFriendsEndorsements() { 
		Collections.sort(knownPersons, friendshpEndorsementComparator);
	}

	public ArrayList<Person> sortExchangeEndorsments(ArrayList<Person> persons) {
		Collections.sort(persons, exchangeEndorsmentComparator);
		return persons;
	}

	public void sortExchangeEndorsments() {
		Collections.sort(knownPersons, exchangeEndorsmentComparator);
	}	

	public void sortCompadreSelection() {
		Collections.sort(knownPersons, compadreSelectionComparator);
	}

	/** Assigns and updates dynamic endorsements for the known persons. */
	public void updateDynamicEndorsements() {
		try {
			ArrayList<Person> temp = new ArrayList<Person>();
			for (Person person : knownPersons) {
				if (person != null
						&& person.isAlive()) {
					// We do the endorsement for 'similar'/'dissimilar'
					similarityEndorsement(person);
					// We do the endorsement for 'commercial-ties'
					commercialtiesEndorsement(person);
					// We do the endorsement for intensity of exchange
					intensityEndorsement(person);		
					// TODO: We do the endorsement for 'prestigious'
					//reputationEndorsment(person);					
				}
				else {
					temp.add(person);
				}
			}
			knownPersons.removeAll(temp);			
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * TODO: Based on Fiestas according to the anthropologists and services at births, marriages, and deaths.
	 * @param person
	 */
	public void reputationEndorsment(Person person) {
	}

	/**
	 * Crudely, if the person was of the same-status or above and with the same profession
	 * and was reliable in the past time-step, we may assume that we have hope for a commercial tie.
	 * TODO: This is an arbitrary assumption - needs checking. 
	 * @param person
	 */
	public void commercialtiesEndorsement(Person person) {
		Integer personID = new Integer(person.getId());
		int sameProfIndex = model.returnStaticEndorsementIndex("is-same-profession");
		int sameStatusIndex = model.returnStaticEndorsementIndex("is-same-status");
		int aboveStatusIndex = model.returnStaticEndorsementIndex("is-above-status");
		int friendPastIndex = model.returnDynamicEndorsementIndex("is-friend-in-past");
		if (endorsementsRecord.containsKey(personID)) {
			if (endorsementsRecord.get(personID)
					.getStaticEndorsements()[sameProfIndex]
					                         &&(endorsementsRecord.get(personID).
					                        		 getStaticEndorsements()[sameStatusIndex]
					                        		                         ||endorsementsRecord.get(personID).getStaticEndorsements()[aboveStatusIndex])			                                                                     
					                        		                         && checkDynamicEndorsement(personID, model.getCurrentTick()-1)
					                        		                         && endorsementsRecord.get(personID).getDynamicEndorsements().get(model.getCurrentTick()-1).contains(friendPastIndex)
			) 
			{
				endorsePerson(person, "expect-commercial-tie");				
			}
		}
	}

	/**
	 * TODO: This function has got several arbitrary assumptions and numbers
	 * Contains period for reliability.
	 * Recall is for a year.
	 * @param person
	 */
	public void intensityEndorsement(Person person) {
		if (model.getCurrentTick() < 24) {
			return;
		}
		//double reliabilityRecall = 0;
		/*		do {
		reliabilityRecall = model.getPsr().nextLogistic(61, 23);
	} while (reliabilityRecall <= 72);*/

		/*		do {
			reliabilityRecall = model.getPsr().nextLogistic(41, 23);
		} while (reliabilityRecall <= 36);*/
		/*		do {
			reliabilityRecall = model.getPsr().nextLogistic(31, 17);
		} while (reliabilityRecall <= 24);
		 */		
		/*		do {
			reliabilityRecall = model.getPsr().nextLogistic(13, 7);
		} while (reliabilityRecall <= 8);
		 */
		Integer personID = new Integer(person.getId());
		int numSentItems = 0;
		int numReceivedItems = 0;		
		int timeLim = model.getCurrentTick() <= reliabilityRecall ? 0 : (int) (model.getTickCount()-reliabilityRecall);
		if (sentRecord.containsKey(personID)) {
			for (int i=sentRecord.get(personID).size()-1; i>=0; i--) {
				ExchangeRecord sentItem = (ExchangeRecord) sentRecord.get(personID).get(i);
				if (sentItem.getTimestep() >= timeLim) {
					if (model.isNonlinearRecall()) {
						if (timeLim == 0) {
							numSentItems += model.returnLogisticProbability(sentItem.getTimestep());	
						}
						else {							
							numSentItems += model.returnLogisticProbability(sentItem.getTimestep());	
						}						
					}
					else {
						numSentItems++;
					}
				}
				else {
					break;
				}
			}
		}
		if (receivedRecord.containsKey(personID)) {		
			for (int i=receivedRecord.get(personID).size()-1; i>=0; i--) {
				ExchangeRecord receivedItem = (ExchangeRecord) receivedRecord.get(personID).get(i);
				if (receivedItem.getTimestep() >= timeLim) {
					if (model.isNonlinearRecall()) {
						if (timeLim == 0) {
							numReceivedItems += model.returnLogisticProbability(receivedItem.getTimestep());	
						}
						else {
							numReceivedItems += model.returnLogisticProbability(receivedItem.getTimestep());
						}

					}
					else {
						numReceivedItems++;
					}
				}
				else {
					break;
				}
			}
		}
		/* TODO: check Arbitrary assumption */ //PARAM
		/*		
 		double factor = 0.5 * timeLim;
		 Arbitrary assumption
		if (numReceivedItems >= factor
				&& numReceivedItems >= numSentItems) {
			endorsePerson(person, "is-reliable");
		}
		 TODO: check Arbitrary assumption 
		else if (numReceivedItems <= 0.05 * timeLim
				&& numSentItems > 0.05 * timeLim) {
			endorsePerson(person, "is-unreliable");
		}

		 */
		//		double factor = 0.1 * timeLim;
		//double factor = 0.05 * reliabilityRecall;
		//		model.print("tick: " + model.getCurrentTick() 
		//				+ "agent: " + id + " numSent: " + numSentItems 
		//				+ " numRcvd: " + numReceivedItems);

		double intensityFactor = model.getTickCount() < reliabilityRecall ? model.getTickCount() : reliabilityRecall;
		intensityFactor *= 0.1;
		if (numReceivedItems > intensityFactor) {
			endorsePerson(person, "is-intense");
			model.totalPosReliableEndorsements++;
		}
		else if (numReceivedItems <= 0
				&& numSentItems > intensityFactor) {
			endorsePerson(person, "is-not-intense");
			model.totalNegReliableEndorsements++;
		}
	}

	/**
	 * Endorsements that are not specific to a an agent's relation, propinquity or dyadic tie. 
	 * That is,
	 * - age (same age: the proportion is an arbitrary assumption 25% difference)
	 * - gender
	 * - similarity
	 * - profession/status
	 */
	public ArrayList<String> assignStaticEndorsements(Person person) {
		ArrayList<String> labels = new ArrayList<String>();
		if (person == null
			|| !person.isAlive()
			|| person.getId() == id
		) {
			return labels;
		}
		if (gender.equals(person.getGender())) {
			labels.add("is-same-gender");
		}
		else {
			labels.add("is-different-gender");
		}
		if (Math.abs(age - person.getAge()) <= (double) age*0.25) {
			labels.add("is-same-age");
		}
		else {
			labels.add("is-different-age");
		}				
		labels.addAll(statusAndProfessionEndorsment(person));
		return labels;
	}

	/**
	 * Assign and update status and profession endorsements for this person.
	 * @param person
	 */
	public ArrayList<String> statusAndProfessionEndorsment(Person person) {
		ArrayList<String> labels = new ArrayList<String>();
		boolean sameProfession = false;
		if (person == null
				|| person.getHousehold() == null) {
			person.printProfile();
		}
		if (household == null) {
			model.print("Me");
			printProfile();
		}
		if (person.getHousehold().getProfession() == household.getProfession()) {
			labels.add("is-same-profession");
			sameProfession = true;
		}
		else {
			labels.add("is-different-profession");
		}		
		if (sameProfession) {
			labels.add("is-same-status");
		}
		else {
			switch(household.getProfession()) {
			case FARMING:
				labels.add("is-below-status");	
				break; 
			case POTTERY:
				if (person.getHousehold().getProfession() == Profession.FARMING) {
					labels.add("is-above-status");	
				}
				else {
					labels.add("is-below-status");
				}
				break;
			case OTHER:
				labels.add("is-above-status");
			default:break;
			}	
		}
		return labels;
	}

	/**
	 * @author sm/rm/sja
	 * TODO: Implement similarity - endorsement criteria may vary from person to person for 'is-similar' OR 'is-dissimilar'. * 
	 * TODO: Dissimilar: need a different criteria
	 */
	public String similarityEndorsement(Person person) {
		String label = "";
		int counter = 0;
		for (int i=0; i<tag.length; i++) {
			if (tag[i] == person.getTag()[i]) {
				counter++;
			}
		}
		if (counter >= (int)proportionTagSimilarity * tag.length) {
			label = "is-similar";
		}
		else {
			label = "is-dissimilar";
		}
		endorsePerson(person, label);
		return label; 
	}

	/**
	 * Endorse the person with the label given as the second argument.
	 * @param person
	 * @param label
	 */
	public void endorsePerson(Person person, String label) {		
		if (person.getId() == id) {
			return;
		}
		try{
			ArrayList<String> labels = new ArrayList<String>();
			int index = -1;
			Integer personID = new Integer(person.getId());
			if (!knownPersons.contains(person)) {
				knownPersons.add(person);
				labels = assignStaticEndorsements(person);
			}
			if (!endorsementsRecord.containsKey(personID)) {
				Endorsements endorsements = new Endorsements(model.getStaticEndorsements().size());
				endorsementsRecord.put(person.getId(), endorsements);
			}
			if (!labels.contains(label)) {
				labels.add(label);
			}
			for (String l : labels) {
				//This is a static endorsement label.
				if (model.getStaticEndorsements().contains(l)) {
					index = model.returnStaticEndorsementIndex(l);
					endorsementsRecord.get(personID).setStaticEndorsement(index);
				}
				//This is a dynamic endorsement label.
				else if (model.getDynamicEndorsements().contains(l)) {
					index = model.returnDynamicEndorsementIndex(l);
					endorsementsRecord.get(personID).setDynamicEndorsement(model.getCurrentTick(), index);
				}			
				if (label == "is-compadre"
					&& !compadreList.contains(person)) {
					compadreList.add(person);
					getCompRelations(person);
				}
			}			
			index = model.returnStaticEndorsementIndex("is-married");
			if (person.getMaritalStatus().equals(MaritalStatus.MARRIED)) {				
				endorsementsRecord.get(personID).setStaticEndorsement(index);
			}
			else {
				endorsementsRecord.get(personID).removeStaticEndorsement(index);
			}
		} catch (Exception e) {e.printStackTrace();}
	}

	/** Clears all data structures storing information about other individuals in the system.*/
	public void clearMemory() {
		endorsementsRecord.clear();
		sentRecord.clear();
		receivedRecord.clear();
		Integer myID = new Integer(id);
		if (household != null
				&& model.getHouseholdList().contains(household)) {
			household.removeMember(this);
		}
		for (Person sibling : siblings) {
			if (sibling != null) {
				sibling.getSiblings().remove(this);
			}
		}
		siblings.clear();
		for (Person compadre : compadreList) {
			if (compadre != null) {
				compadre.getCompadreList().remove(this);
			}
		}
		compadreList.clear();
		for (Person knownPerson : knownPersons) {
			if (knownPerson != null) {
				knownPerson.getKnownPersons().remove(this);
				if (knownPerson.getEndorsements().containsKey(myID)) {
					knownPerson.getEndorsements().remove(myID);	
				}
			}
		}
		knownPersons.clear();
		for (Person friend : friends) {
			if (friend != null) {
				friend.getFriends().remove(this);
			}
		}
		friends.clear();		
		if (father != null
				&& fatherID != -1) {
			father.getOffsprings().remove(this);
		}
		if (mother != null
				&& motherID != -1) {
			mother.getOffsprings().remove(this);
		} 
		if (model.getNumSharedInterests() >= 1) {
			for (SharedInterest sharedInterest : model.getSharedInterestList()) {
				if (sharedInterest.getParticipants().contains(this)) {
					sharedInterest.getParticipants().remove(this);
				}
			}                       
		}
		endorsementsRecord = null;
		sentRecord = null;
		receivedRecord = null;
		household = null;
		/*        compadreList = null;
        siblings = null;
        knownPersons = null;        
        spouse = null;
        father = null; 
        mother = null;*/
	}

	/**
	 * For this version, the way endorsement values are calculated is kept same for the compadre friendship and dyadic contract schemes from Moss (1995). 
	 * We might want to use different mechanisms for calculating the weights in subsequent iterations of the model.
	 * @param person
	 */	
	public double calculateDiscreteEndorsement(Person person, EndorsementScheme scheme) {
		Integer personID = new Integer(person.getId());
		double value = 0;
		if (person != null
				&& person.isAlive()
				&& endorsementsRecord.containsKey(personID)) {
			try {
				Endorsements endorsements = (Endorsements) endorsementsRecord.get(personID);
				Integer currentTime = new Integer(model.getCurrentTick());
				for (int i=0; i<model.totalEndorsements; i++) {
					int sign = 0;
					if (i < model.numStaticEndorsments) {
						if (endorsements.getStaticEndorsements()[i]) {
							if (getWeights(scheme)[i] < 0) {
								sign = -1;
							}
							else {
								sign = 1;
							}
						}						
					}
					else {
						Integer key = new Integer(i-model.numStaticEndorsments);
						if (endorsements.getDynamicEndorsements().containsKey(key)
								&& !endorsements.getDynamicEndorsements().get(key).isEmpty()){
							int lastIndex = endorsements.getDynamicEndorsements().get(key).size()-1;
							if(endorsements.getDynamicEndorsements().get(key).get(lastIndex) == currentTime) {
								if (getWeights(scheme)[i] < 0) {
									sign = -1;
								}
								else {
									sign = 1;
								}	
							}
						}
					}
					value += sign*Math.pow(base, Math.abs(getWeights(scheme)[i]));
				}
			} catch (Exception e) {e.printStackTrace();}
		}
		else {
			//returning a significantly small value.
			value = -1E-11d;			
		}
		return value;
	}
	
	/**
	 * Calculates the endorsement value for this person over the recall period.
	 * @param person
	 * @return
	 */
	public double calculateContinuousEndorsement(Person person, EndorsementScheme scheme) {
		Integer personID = new Integer(person.getId());
		double endorsedValue = -1E-11;
		if (!endorsementsRecord.containsKey(personID)) {
			return endorsedValue;
		}
		try {
			Endorsements endorsements = (Endorsements) endorsementsRecord.get(personID);
			int sign = 0;
			double staticEndorsementValue = 0;
			double value = 0;
			for (int i=0; i<model.numStaticEndorsments; i++) {
				sign = 0;
				if (endorsements.getStaticEndorsements()[i]) {
					if (getWeights(scheme)[i] < 0) {
						sign = -1;
					}
					else {
						sign = 1;
					}
				}
				staticEndorsementValue += sign*Math.pow(base, Math.abs(friendshipEndorsementWeights[i]));
			}		
			int timeLim = model.getCurrentTick() - recallPeriod;
			if (timeLim <= 0) {
				timeLim = model.getCurrentTick();
			}
			int entries = 0;			
			for (Integer key : endorsements.getDynamicEndorsements().keySet()) {
				sign = 0;
				if (!endorsements.getDynamicEndorsements().get(key).isEmpty()) {
					int index = model.numStaticEndorsments+key.intValue();
					if (getWeights(scheme)[index] < 0) {
						sign = -1;
					}
					else {
						sign = 1;
					}
					for (int i=endorsements.getDynamicEndorsements().get(key).size()-1; i>=0; i--) {
						if (endorsements.getDynamicEndorsements().get(key).get(i) >= timeLim) {
							value += sign*Math.pow(base, Math.abs(getWeights(scheme)[index]));
							entries++;
						}
						else {
							break;
						}
					}				
				}
			}
			endorsedValue = (double) (entries*staticEndorsementValue)+value;
		} catch (Exception e) {e.printStackTrace();}
		return (endorsedValue);
	}
	
	public void printProfile() {
		System.out.println("ID: " + id + " gender: " + gender + " age: " + age + " alive: " + alive + " marital status: "
				+ maritalStatus + " spouse: " + spouseID);
	}

	/**
	 * Returns weights sets corresponding to the EndorsementScheme
	 * @param scheme
	 * @return
	 */
	private double[] getWeights (EndorsementScheme scheme) {
		if (scheme == EndorsementScheme.COMPADRE_SELECTION) {
			return compadreSelectionWeights; 
		}
		else if (scheme == EndorsementScheme.DYADIC_EXCHANGE) {
			return exchangeEndorsementWeights;
		}
		else if (scheme == EndorsementScheme.FRIEND_SELECTION) {
			return friendshipEndorsementWeights;
		}				
		else {
			return null;
		}
	}

	/**
	 * SM/RM/AG
	 */
	public void evolveTag() {
		try{
			Person person = (Person) knownPersons
			.get(Uniform.staticNextIntFromTo(0, knownPersons.size()-1));
			for (int i=0; i<this.tag.length; i++){
				if ((this.tag[i] != person.getTag()[i]) 
						&& (Random.uniform.nextDoubleFromTo(0, 1) <= tagEvolutionPropensity)){
					if (this.tag[i] > person.getTag()[i])
						--this.tag[i];
					else ++this.tag[i];
				}
			}
		} catch (Exception e) {e.printStackTrace();}
	}

	/**
	 * Assigns contextualized weights for the compadre selection, friendship and dyadic exchange endorsement schemes.
	 * Need to revise and improve the implementation. 
	 */
	public void setContextualizedEndorsmentWeights() {
		String label = "";
		double friendMin = 0, friendMax = 0;
		double compadreMin = 0, compadreMax = 0;
		double dyadicExchangeMin = 0, dyadicExchangeMax = 0;
		for (int i=0; i<model.getCombinedEndorsements().size(); i++) {
			label = model.getCombinedEndorsements().get(i);
			friendMin = 0; friendMax = 0;
			compadreMin = 0; compadreMax = 0;
			dyadicExchangeMin = 0; dyadicExchangeMax = 0;
			if (label == "is-contractant") {
				friendMin = 0; friendMax = 2;
				compadreMin = 3; compadreMax = 5;
				dyadicExchangeMin = 3; dyadicExchangeMax = 5;
			}
			else if (label == "is-relative") {
				friendMin = -1; friendMax = 1;
				compadreMin = -1; compadreMax = 2;
				//				compadreMin = -5; compadreMax = -2;
				dyadicExchangeMin = 0; dyadicExchangeMax = 2;
			}
			else if (label == "is-sibling") {
				friendMin = -1; friendMax = 1;
				compadreMin = 0; compadreMax = 2;
				dyadicExchangeMin = 0; dyadicExchangeMax = 2;
			}
			else if (label == "is-neighbor") {
				friendMin = 0; friendMax = 3;
				compadreMin = -2; compadreMax = 1;
			}
			else if (label == "is-compadre") {
				friendMin = 1; friendMax = 3;
				//				compadreMin = -20; compadreMax = -10;
				dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			else if (label == "is-godparent") {
				//dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			else if (label == "is-god-child") {
				//dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			//"is-married"
			else if (label == "is-married") {
				compadreMin = 2; compadreMax = 3;
			}
			else if (label == "is-share-interest") {
				friendMin = 1; friendMax = 3;
				compadreMin = 0; compadreMax = 2;	
			}
			else if (label == "is-same-age") {	
				friendMin = 3; friendMax = 5;
				compadreMin = 1; compadreMax = 3;
			}
			else if (label == "is-same-gender") {
				friendMin = 2; friendMax = 5;
				//				friendMin = 1; friendMax = 5;
				compadreMin = 1; compadreMax = 3;
				dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			else if (label == "is-same-profession") {
				compadreMin = 0; compadreMax = 3;
				dyadicExchangeMin = 0; dyadicExchangeMax = 3;
			}
			else if (label == "is-same-status") {	
				compadreMin = 2; compadreMax = 5;
				dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			else if (label == "is-above-status") {
				compadreMin = 0; compadreMax = 2;
				dyadicExchangeMin = 0; dyadicExchangeMax = 2;
			}
			else if (label == "is-friend") {
				compadreMin = 2; compadreMax = 5;
				dyadicExchangeMin = 2; dyadicExchangeMax = 4;
			}
			else if (label == "is-intense") {
				friendMin = 1; friendMax = 3;
				//sja: added below
				compadreMin = 1; compadreMax = 3;
				dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			else if (label == "is-prestigious") {
				compadreMin = 1; compadreMax = 3;
				dyadicExchangeMin = 1; dyadicExchangeMax = 2;
			}
			else if (label == "is-similar") {
				friendMin = 1; friendMax = 3;
			}
			else if (label == "is-expect-commercial-tie") {
				compadreMin = 0; compadreMax = 3;
				dyadicExchangeMin = 1; dyadicExchangeMax = 3;
			}
			else if (label == "is-different-gender") {
				friendMin = -3; friendMax = 0;
				compadreMin = -3; compadreMax = -1;
				dyadicExchangeMin = -3; dyadicExchangeMax = -1;
			}
			else if (label == "is-different-age") {
				friendMin = -5; friendMax = -2;
				//sja
				//compadreMin = -3; compadreMax = -1;
			}
			else if (label == "is-different-profession") {
				compadreMin = -3; compadreMax = -1;
				dyadicExchangeMin = -3; dyadicExchangeMax = -1;
			}
			else if (label == "is-below-status") {	
				compadreMin = -3; compadreMax = -2;
				dyadicExchangeMin = -3; dyadicExchangeMax = -2;
			}
			else if (label == "is-not-intense") {				
				//sja compadreMin = -2; compadreMax = 0;
				compadreMin = -1; compadreMax = 1;
				//dyadicExchangeMin = -3; dyadicExchangeMax = -1;
				dyadicExchangeMin = -2; dyadicExchangeMax = 0;
			}
			else if (label == "is-unprestigious") {
				dyadicExchangeMin = -5; dyadicExchangeMax = -2;
			}
			else if (label == "is-dissimilar") {	
				friendMin = -1; friendMax = 0;
			}
			else if (label == "is-friend-in-past") {
				//compadreMin = 1; compadreMax = 2;				
				//				friendMin = -3; friendMax = -1;
				friendMin = -1; friendMax = 0;
			}
			friendshipEndorsementWeights[i] = Uniform.staticNextIntFromTo((int)friendMin, (int)friendMax);
			compadreSelectionWeights[i] = Uniform.staticNextIntFromTo((int)compadreMin, (int) compadreMax);
			exchangeEndorsementWeights[i] = Uniform.staticNextIntFromTo((int)dyadicExchangeMin, (int)dyadicExchangeMax);						
		}		
	}

	public void getCompRelations(Person compadre) {
		boolean flag1=false, flag2=false;
		model.numCompadres++;
		if (isRelative(compadre)) {
			model.compRel++;
			flag1 = true;
		}
		if (friends.contains(compadre)) {
			model.compFr++;
			flag2 = true;
		}
		if (!flag1 && !flag2) {
			model.compMisc++;
		}
		else if (flag1 && flag2) {
			model.compBoth++;
		}		
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public int[] getTag() {
		return tag;
	}

	public void setTag(int[] tag) {
		this.tag = tag;
	}

	public double getTagEvolutionPropensity() {
		return tagEvolutionPropensity;
	}

	public void setTagEvolutionPropensity(double tagEvolutionPropensity) {
		this.tagEvolutionPropensity = tagEvolutionPropensity;
	}

	public double getBase() {
		return base;
	}

	public void setBase(double base) {
		this.base = base;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public Household getHousehold() {
		return household;
	}

	public void setHousehold(Household household) {
		this.household = household;
	}

	public Person getFather() {
		return father;
	}

	public void setFather(Person father) {
		this.father = father;
		this.fatherID = father.getId();
	}

	public Person getMother() {
		return mother;
	}

	public void setMother(Person mother) {
		this.mother = mother;
		this.motherID = mother.getId();
	}

	public Person getSpouse() {
		return spouse;
	}

	public void setSpouse(Person spouse) {
		this.spouse = spouse;
		this.spouseID = spouse.getId();
		if (knownPersons.contains(spouse)) {
			knownPersons.remove(spouse);			
		}
		if (endorsementsRecord.containsKey(new Integer(spouse.getId()))) {
			endorsementsRecord.remove(new Integer(spouse.getId()));
		}
	}

	public boolean isHouseholdHead() {
		return householdHead;
	}

	public void setHouseholdHead(boolean householdHead) {
		this.householdHead = householdHead;
	}

	public MaritalStatus getMaritalStatus() {
		return maritalStatus;
	}

	public void setMaritalStatus(MaritalStatus maritalStatus) {
		this.maritalStatus = maritalStatus;
	}

	public ArrayList<Person> getOffsprings() {
		return offsprings;
	}

	public void setOffsprings(ArrayList<Person> offsprings) {
		this.offsprings = offsprings;
	}

	public TzintzuntzanModel getModel() {
		return model;
	}

	public void setModel(TzintzuntzanModel model) {
		this.model = model;
	}

	public DefaultDrawableNode getNode() {
		return node;
	}

	public void setNode(DefaultDrawableNode node) {
		this.node = node;
	}

	public int getBirthWeek() {
		return birthWeek;
	}

	public void setBirthWeek(int birthWeek) {
		this.birthWeek = birthWeek;
	}

	public int getBirthMonth() {
		return birthMonth;
	}

	public void setBirthMonth(int birthMonth) {
		this.birthMonth = birthMonth;
	}

	public int getDeathWeek() {
		return deathWeek;
	}

	public void setDeathWeek(int deathWeek) {
		this.deathWeek = deathWeek;
	}

	public int getLastDelivery() {
		return lastDelivery;
	}

	public void setLastDelivery(int lastDelivery) {
		this.lastDelivery = lastDelivery;
	}

	public boolean isPregnancyPossible() {
		return pregnancyPossible;
	}

	public void setPregnancyPossible(boolean pregnancyPossible) {
		this.pregnancyPossible = pregnancyPossible;
	}

	public ArrayList<Person> getSiblings() {
		return siblings;
	}

	public void setSiblings(ArrayList<Person> siblings) {
		this.siblings = siblings;
	}

	public int getFatherID() {
		return fatherID;
	}

	public void setFatherID(int fatherID) {
		this.fatherID = fatherID;
	}

	public int getMotherID() {
		return motherID;
	}

	public void setMotherID(int motherID) {
		this.motherID = motherID;
	}

	public Household getMaidenHousehold() {
		return maidenHousehold;
	}

	public void setMaidenHousehold(Household maidenHousehold) {
		this.maidenHousehold = maidenHousehold;
	}

	public int getMarriageTick() {
		return marriageTick;
	}

	public void setMarriageTick(int marriageWeek) {
		this.marriageTick = marriageWeek;
	}

	public void setEndorsements(HashMap<Integer, Endorsements> endorsements) {
		this.endorsementsRecord = endorsements;
	}

	public HashMap<Integer, Endorsements> getEndorsements() {
		return endorsementsRecord;
	}

	public ArrayList<Person> getKnownPersons() {
		return knownPersons;
	}

	public void setKnownPersons(ArrayList<Person> knownPersons) {
		this.knownPersons = knownPersons;
	}

	public int getSpouseID() {
		return spouseID;
	}

	public void setSpouseID(int spouseID) {
		this.spouseID = spouseID;
	}

	public int getBirthGodFatherID() {
		return birthGodFatherID;
	}

	public void setBirthGodFatherID(int birthGodFatherID) {
		this.birthGodFatherID = birthGodFatherID;
	}

	public int getBirthGodMotherID() {
		return birthGodMotherID;
	}

	public void setBirthGodMotherID(int birthGodMotherID) {
		this.birthGodMotherID = birthGodMotherID;
	}

	public int getMarriageGodFatherID() {
		return marriageGodFatherID;
	}

	public void setMarriageGodFatherID(int marriageGodFatherID) {
		this.marriageGodFatherID = marriageGodFatherID;
	}

	public int getMarriageGodMotherID() {
		return marriageGodMotherID;
	}

	public void setMarriageGodMotherID(int marriageGodMotherID) {
		this.marriageGodMotherID = marriageGodMotherID;
	}

	public ArrayList<Person> getCompadreList() {
		return compadreList;
	}

	public void setCompadreList(ArrayList<Person> compadre) {
		this.compadreList = compadre;
	}

	public HashMap<Integer, Endorsements> getEndorsementsRecord() {
		return endorsementsRecord;
	}

	public void setEndorsementsRecord(
			HashMap<Integer, Endorsements> endorsementsRecord) {
		this.endorsementsRecord = endorsementsRecord;
	}

	public double[] getFriendshipEndorsementWeights() {
		return friendshipEndorsementWeights;
	}

	public void setFriendshipEndorsementWeights(
			double[] friendshipEndorsementWeights) {
		this.friendshipEndorsementWeights = friendshipEndorsementWeights;
	}

	public int getMaxFriends() {
		return maxFriends;
	}

	public void setMaxFriends(int maxFriends) {
		this.maxFriends = maxFriends;
	}

	public int getMaxContracts() {
		return maxContracts;
	}

	public void setMaxContracts(int maxContracts) {
		this.maxContracts = maxContracts;
	}

	public double[] getExchangeEndorsementWeights() {
		return exchangeEndorsementWeights;
	}

	public void setExchangeEndorsementWeights(
			double[] dyadicContractEndorsementWeights) {
		this.exchangeEndorsementWeights = dyadicContractEndorsementWeights;
	}

	public int getNextRitualExchangeTime() {
		return nextRitualExchangeTime;
	}

	public void setNextRitualExchangeTime(int nextRitualExchangeTime) {
		this.nextRitualExchangeTime = nextRitualExchangeTime;
	}

	public int getNextOrdinaryExchangeTime() {
		return nextOrdinaryExchangeTime;
	}

	public void setNextOrdinaryExchangeTime(int nextOrdinaryExchangeTime) {
		this.nextOrdinaryExchangeTime = nextOrdinaryExchangeTime;
	}

	public double getEndorsementUpdateRate() {
		return endorsementUpdateRate;
	}

	public void setEndorsementUpdateRate(double endorsementUpdateRate) {
		this.endorsementUpdateRate = endorsementUpdateRate;
	}

	public ArrayList<Person> getFriends() {
		return friends;
	}

	public void setFriends(ArrayList<Person> friends) {
		this.friends = friends;
	}

	public ArrayList<Person> getContractants() {
		return contractants;
	}

	public void setContractants(ArrayList<Person> contractants) {
		this.contractants = contractants;
	}

	public void setRiteDePassage(ExchangeType riteDePassage) {
		this.riteDePassage = riteDePassage;
	}

	public ExchangeType getRiteDePassage() {
		return riteDePassage;
	}

	public void setInvitations(ArrayList<RiteDePassage> invitations) {
		this.invitations = invitations;
	}

	public ArrayList<RiteDePassage> getInvitations() {
		return invitations;
	}
}