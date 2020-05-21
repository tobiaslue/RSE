package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Non_Negative_Unsafe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if(j >= 0){
			j = 5;
		} else {
			j = -5;
		}
        s.arrive(j);
	}
}
