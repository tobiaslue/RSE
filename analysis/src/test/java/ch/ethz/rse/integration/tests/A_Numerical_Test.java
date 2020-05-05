package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class A_Numerical_Test {
	public static void m1(int j, int k, int l) {
        TrainStation s = new TrainStation(10);
        TrainStation t = new TrainStation(10);
        TrainStation y = new TrainStation(10);
        t = s;
        t = y;
        
        t.arrive(5);
        y.arrive(j);
        s.arrive(7);
	}
}
