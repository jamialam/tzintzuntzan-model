package tzintzuntzan;

/**
 * 
 * @author shah
 *
 */
public class Settings {
	protected static final int MAX_ITERATIONS = 1500;
	protected static boolean NONLINEAR_RECALL = false;
	protected static final int INITIAL_HOUSEHOLDS = 100;
	protected static final double INITIAL_NUCLEAR_HOUSEHOLDS_PROP = 0.65; 
	protected static final double ENDORSEMENT_UPDATE_RATE = 0.083333;
	protected static enum EndorsementEvaluation {DISCRETE, CONTINUOUS}
	protected static enum EndorsementScheme {FRIEND_SELECTION, COMPADRE_SELECTION, DYADIC_EXCHANGE}
	protected static enum EndorsementWeights {RANDOM, CONTEXTUALIZED}
	protected static enum ExchangeMode {COMBINED, SEPARATE}	
	protected static final int MEAN_ORDINARY_EXCHANGE_TIME = 2;
	protected static final int MEAN_RITUAL_EXCHANGE_TIME = 8;
	/** Starting age for an adult - Alam (2008) */
	protected static final int ADULT_STARTING_AGE = 15;		
	protected static final int FEMALE_MARRIAGE_LIMIT = 50;
	protected static final int FEMALE_FERTILITY_LIMIT = 43;
	protected static final int MALE_MARRIAGE_LIMIT = 63;
	protected static final double BIRTH_RATE = 57;
	protected static final double ELOPEMENT_RATE = 0.005;
	protected static final double BUILD_NUCLEAR_HOUSEHOLD_RATE = 0.005;
	/** Agent's gender */
	protected static enum Gender {FEMALE, MALE}
	/** Agent's marital:{single, eloped, married, widowed} */ 
	protected static enum MaritalStatus {SINGLE, ELOPED, MARRIED, WIDOWED}
	/** Shared interests from Moss (2008)*/
	protected static class SharedInterests {
		protected static final int NUM_SHARED_INTERESTS = 100;
		protected static final int YOUTH = 0;
		protected static final int ADULT = 1;
		/**Arbitrary assumption */
		protected static final int MAX_YOUTH_ACTIVITIES = 3;
		/**Arbitrary assumption */
		protected static final int MAX_ADULT_SHARED_INTERESTS = 5;		
	}
	/** Tag related parameters from Moss (2008)*/
	protected static class Tags {
		protected static final double EVOLUTION_PROBABILITY = 0.05;
		protected static final double EVOLUTION_PROPENSITY = 0.005;
		protected static final int LENGTH = 11;
		protected static final int BASE = 5;
		protected static final int MIN_BASE_VALUE = 1;
		protected static final int MAX_BASE_VALUE = 1;
	}
	/** Grid related parameters*/
	protected static class Grid {
		protected static final int XSIZE = 20;
		protected static final int YSIZE = 20;
		protected static final int RADIUS = 4;
		protected static final int DENSITY_FACTOR = 2;
	}
	/** Profession type: it determines that of a household. A son inherits the profession of his household.*/
	protected static enum Profession {OTHER, POTTERY, FARMING}				
	/** From Brandes (1983) */
	protected static enum ExchangeType {NONE, ORDINARY_SERVICE, ORDINARY_GOOD, FIESTA_SERVICE,
		FIESTA_GOOD, BIRTH_SERVICE, ELOPEMENT_SERVICE, MARRIAGE_SERVICE, DEATH_SERVICE}   
}