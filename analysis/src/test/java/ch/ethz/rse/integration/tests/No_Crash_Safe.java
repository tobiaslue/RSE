package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class No_Crash_Safe {
	public static void m1(int j) {
		TrainStation s = new TrainStation(10);
		if(0 <= j && j < 10){
            if(j < 5){
                s.arrive(5);
            }else{
                s.arrive(5);
            }
		}
	}
}
