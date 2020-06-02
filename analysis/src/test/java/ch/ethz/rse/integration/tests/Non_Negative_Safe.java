package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class Non_Negative_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if(j < 0){
			j = 5;
		} 
        s.arrive(j);
	}
}
