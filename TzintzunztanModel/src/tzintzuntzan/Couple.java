package tzintzuntzan;

public class Couple {
	public int elopementTick = -1;
	public int marriageTick = -1;
	public Person male;
	public Person female;

	public Couple(int _elopementWeek) {
		elopementTick = _elopementWeek;
	}

	public Couple(int _elopementWeek, Person _male, Person _female) {
		male = _male;
		female = _female;
		elopementTick = _elopementWeek;
	}
}
