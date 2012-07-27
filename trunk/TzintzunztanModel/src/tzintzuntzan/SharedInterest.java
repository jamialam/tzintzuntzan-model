package tzintzuntzan;

import java.util.ArrayList;

/**
 * @author scott moss  
 *  
 */

public class SharedInterest {
	private int activityID = -1;
	private ArrayList<Person> participants = new ArrayList<Person>();
	private int activityType = -1;
	
	SharedInterest(int _activityID, int _activityType) {
		activityType = _activityType;
	}
	
	SharedInterest(int _activityID, int _activityType, ArrayList<Person> _participants) {
		activityID = _activityID;
		activityType = _activityType;
		participants = _participants;
	}

	public int getActivityType() {
		return activityType;
	}

	public void setActivityType(int activityType) {
		this.activityType = activityType;
	}

	public int getActivityID() {
		return activityID;
	}

	public void setActivityID(int activityID) {
		this.activityID = activityID;
	}

	public ArrayList<Person> getParticipants() {
		return participants;
	}

	public void setParticipants(ArrayList<Person> participants) {
		this.participants = participants;
	}	
}