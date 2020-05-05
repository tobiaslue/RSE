package ch.ethz.rse.integration.tests;

import ch.ethz.rse.TrainStation;

// expected results:
// TRACK_NON_NEGATIVE SAFE
// TRACK_IN_RANGE SAFE
// NO_CRASH SAFE

public class A_Numerical_Test {
	public static void m1(int j, int k, int l) {
               TrainStation s = new TrainStation(10);
               
                if(j == k){
                       j = 7;
                       k = l;
                       l = 8;
               }else{
                        l = 5;
                        k = l;
                        j = 3;
               }

               s.arrive(j);
               s.arrive(k);

               s.arrive(l);

	}
}
