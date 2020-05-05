package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Basic_Test_Custom1 {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
        j = -5;
        s.arrive(j);
	}
}
