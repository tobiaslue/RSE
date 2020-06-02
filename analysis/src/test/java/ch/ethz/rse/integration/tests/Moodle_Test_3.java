package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Moodle_Test_3 {
    public static void example() {
        TrainStation s = new TrainStation(10);
        int i = 3; 
    
        while(i != 0) {
            s.arrive(i);
            i--;
        }
    }
}
