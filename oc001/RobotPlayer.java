package oc001;

import battlecode.common.RobotController;

public class RobotPlayer {    
	
	public static void run(RobotController rc) {
		//	System.setProperty("debugMethodsEnabled", "true");
		// use bc.conf to turn debug mode on
		
		while(true) {
			try {
				
				throw new Exception("TODO: make strategy");
				
			} catch (Exception e) {
				debug_printf(e.toString());
			}
		}
	}
	
	public static void debug_printf(String format, Object ... objects) {
		System.out.printf(format, objects);
		System.out.printf("\n");
	}	
}

