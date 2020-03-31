package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE UNSAFE
// TRACK_IN_RANGE UNSAFE
// NO_CRASH UNSAFE

public class Basic_Test_Unsafe {
	public void m2(int j) {
		TrainStation c = new TrainStation(10);
		if (-1 <= j && j <= 10) {
			// -1<=j<=10
			// c points to an object with 10 tracks
			c.arrive(j);
			c.arrive(j);
		}
	}
}
