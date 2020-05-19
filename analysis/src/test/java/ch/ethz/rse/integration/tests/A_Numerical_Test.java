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
                
                if (j == k) {
                        j = 1;
                        l = 3;
                        k = 5;

                } else {
                        l = 4;
                        j = 2;
                        k = 6;
                }



                t.arrive(j);
                t.arrive(k);
                t.arrive(l);
        }
}
