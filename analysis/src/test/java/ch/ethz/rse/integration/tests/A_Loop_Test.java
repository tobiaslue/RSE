package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH SAFE

public class A_Loop_Test {
	public static void m1(int j, int k) {
		TrainStation s = new TrainStation(10);
		for(int i = 0; i < j; i++){
            k = k + 1;
        }
        s.arrive(k);
	}
}
