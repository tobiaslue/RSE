package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Moodle_Test_11 {
    public void m(int j) {
        TrainStation s = new TrainStation(10);
           s.arrive(5);
           if (j >= 0 && j < 10) {
              s.arrive(j);
           }
        }
}
