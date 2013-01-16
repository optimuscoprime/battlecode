package oc001;

import battlecode.common.RobotController;

public class RobotPlayer {    		
	public static void run(RobotController rc) {
		while(true) {
			try {
				playOneTurn(rc);
				rc.yield();		
			} catch (Exception e) {
				debug_printf(e.toString());
			}
		}
	}

	//	System.setProperty("debugMethodsEnabled", "true");
	// use bc.conf to turn debug mode on	
	private static void debug_printf(String format, Object ... objects) {
		System.out.printf(format, objects);
		System.out.printf("\n");
	}	

	private static void playOneTurn(RobotController rc) {
		throw new RuntimeException("TODO: make strategy");
	}	
}

