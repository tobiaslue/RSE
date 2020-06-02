package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class Moodle_Test_2 {
    public void m2(int j) {
        TrainStation c = new TrainStation(10);
        if (0 <= j && j < 10) {
            if (j >= 5)
                c.arrive(5);
            else
                c.arrive(5);
        }
    }
}
