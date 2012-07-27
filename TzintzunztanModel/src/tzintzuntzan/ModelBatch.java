package tzintzuntzan;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;

public class ModelBatch extends TzintzuntzanModel {

	public ModelBatch() {
	}

	public final void begin() {
		super.begin();

	}
	public final void setup() {
		// Initializing the original model first
		super.setup(); 
		System.gc();
	}

	public final void buildModel() {
		// Build the original model last
		super.buildModel();
	}
	
	public final void buildSchedule() {
		// Do the original model's schedules first
		super.buildSchedule();
		schedule.scheduleActionAtInterval(Settings.MAX_ITERATIONS, this, "stop",Schedule.LAST);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final SimInit init = new SimInit();
		// We MUST create a ModelBatch object instead of an instance of Model
		// or that of a ModelGUI, in order to get the Batch functionality.
		final TzintzuntzanModel m = new ModelBatch();
		// Note the differences between the parameters when loading a batch-mode
		// model (compared to that of a GUI-mode one, @see ModelGUI).
		// The second parameter specifies the name of the parameter file, while
		// the third one declares that the model is to be run in batch-mode.
		init.loadModel(m, "param.xml", true);
	}
}