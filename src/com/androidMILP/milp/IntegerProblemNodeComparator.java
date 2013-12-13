package com.androidMILP.milp;

import java.util.Comparator;

public class IntegerProblemNodeComparator implements Comparator<IntegerProblemNode>{

	@Override
	public int compare(IntegerProblemNode o1, IntegerProblemNode o2) {
		if (o1.lowerBnd < o2.lowerBnd){
			return -1;
		}else{
			return 1;
		}		
	}

}
