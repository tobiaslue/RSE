package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Moodle_Test_10 {
    public static void m1(int j) {
        TrainStation s = new TrainStation(10);
    
        while (j >= 0) {     
            s.arrive(5);
            j--;
        }
    }
}
