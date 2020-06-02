package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH UNSAFE

public class Moodle_Test_9 {
    public void foo(int j) {
        TrainStation st = new TrainStation(50);
        TrainStation st1 = new TrainStation(500);
        TrainStation st2 = null;
        for (int i = 10; i >= 0; i--) {
          st.arrive(i);
        }
        if (j == 10) { 
          st = new TrainStation(400);
          st2 = st;
        } else {
          st2 = st1;
        }
        st1.arrive(499);
        st2.arrive(50);
        st.arrive(49);
      }
}
