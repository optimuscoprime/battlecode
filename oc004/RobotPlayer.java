package oc004;

import java.util.*;

import battlecode.common.*;

import static oc004.MacroStrategy.*;
import static oc004.Debugging.*;

public class RobotPlayer {

	private static final int LOTS_OF_EXCESS_POWER_THRESHOLD = 1000;

	private static final int HQ_RAW_DISTANCE_BIG_DISTANCE = 100;
	private static final int HQ_RAW_DISTANCE_MEDIUM_DISTANCE = HQ_RAW_DISTANCE_BIG_DISTANCE / 2;	
	private static final int HQ_RAW_DISTANCE_TINY_DISTANCE = HQ_RAW_DISTANCE_MEDIUM_DISTANCE / 2;	

	private static final int DEFAULT_SENSE_RADIUS_SQUARED = 14;

	private static final double HEAVILY_MINED_PERCENT_THRESHOLD = 0.3;

	private static final int HUGE_RADIUS = 1000000;

	private static RobotController rc;
	private static Team myTeam;
	private static Team enemyTeam;
	private static int mapWidth;
	private static int mapHeight;
	private static MacroStrategy macroStrategy;
	private static MapLocation myHQLocation;
	private static MapLocation enemyHQLocation;
	private static MapLocation mapCenter;
	private static int rawDistanceBetweenHQs;
	private static int numMapLocations;

	private static MapLocation[] myMineLocations;
	private static MapLocation[] enemyMineLocations;
	private static MapLocation[] allMineLocations;
	private static MapLocation[] nonAlliedMineLocations;



	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {

			try {
				decideMove();
			} catch (Exception e) {
				debug_catch(e);
			}

			rc.yield();
		}
	}

	private static void initialise(RobotController rc) {
		debug_startMethod();

		RobotPlayer.rc = rc;
		RobotPlayer.myTeam = rc.getTeam();
		RobotPlayer.enemyTeam = myTeam.opponent();
		RobotPlayer.mapHeight = rc.getMapHeight();
		RobotPlayer.mapWidth = rc.getMapWidth();
		RobotPlayer.myHQLocation = rc.senseHQLocation();
		RobotPlayer.enemyHQLocation = rc.senseEnemyHQLocation();
		RobotPlayer.mapCenter = new MapLocation((myHQLocation.x + enemyHQLocation.x) / 2, 
				(myHQLocation.y + enemyHQLocation.y) / 2);
		RobotPlayer.rawDistanceBetweenHQs = myHQLocation.distanceSquaredTo(enemyHQLocation);
		RobotPlayer.numMapLocations = mapWidth * mapHeight;

		debug_endMethod();
	}

	private static void decideMove() {
		debug_startMethod();

		switch(rc.getType()) {
			case ARTILLERY:
				decideMove_artillery();
			case GENERATOR:
				decideMove_generator();
			case HQ:
				decideMove_HQ();
			case MEDBAY:
				decideMove_medbay();
			case SHIELDS:
				decideMove_shields();
			case SOLDIER:
				decideMove_solder();
			case SUPPLIER:
				decideMove_supplier();
				break;
		}

		debug_endMethod();
	}

	private static void decideMove_artillery() {
		debug_startMethod();
		debug_endMethod();
	}

	private static void decideMove_supplier() {
		debug_startMethod();
		debug_endMethod();
	}

	private static void decideMove_solder() {
		debug_startMethod();
		debug_endMethod();		
	}

	private static void decideMove_shields() {
		debug_startMethod();
		debug_endMethod();		
	}

	private static void decideMove_HQ() {
		debug_startMethod();

		decideMacroStrategy();		

		if (rc.isActive()) {

			switch(macroStrategy) {
				case ATTACK:
					break;
				case DEFEND:
					break;
				case EXPAND:
					break;
				case RESEARCH:
					break;
			}

			debug_printf(macroStrategy.toString());

			// what should everyone else do?
		}

		debug_endMethod();		
	}

	private static void decideMacroStrategy() {
		debug_startMethod();

		macroStrategy = null;

		maybeUpdateMineLocations();		

		if (ourBaseIsUnderAttack()) {
			macroStrategy = DEFEND;
		} else if (willTakeLongTimeToReachEnemy()) {
			macroStrategy = ATTACK;
		} else if (willTakeLongTimeForEnemyToReachUs()) {
			macroStrategy = EXPAND;
		} else if (lotsOfExcessPower()) {
			macroStrategy = RESEARCH;
		}

		if (macroStrategy == null) {
			macroStrategy = ATTACK;
		}

		debug_endMethod();
	}

	private static boolean lotsOfExcessPower() {
		debug_startMethod();

		boolean lotsOfExcessPower = (rc.getTeamPower() > LOTS_OF_EXCESS_POWER_THRESHOLD);

		debug_endMethod();

		return lotsOfExcessPower;

	}

	private static boolean willTakeLongTimeForEnemyToReachUs() {
		debug_startMethod();

		boolean willTakeLongTimeForEnemyToReachUs = false;

		if (rawDistanceBetweenHQs > HQ_RAW_DISTANCE_BIG_DISTANCE) {

			willTakeLongTimeForEnemyToReachUs = true;

		} else if (rawDistanceBetweenHQs > HQ_RAW_DISTANCE_MEDIUM_DISTANCE) {

			double percentAlliedMines = (allMineLocations.length - enemyMineLocations.length) / numMapLocations;

			if (percentAlliedMines > HEAVILY_MINED_PERCENT_THRESHOLD) {

				willTakeLongTimeForEnemyToReachUs = true;

			}
		}

		debug_endMethod();

		return willTakeLongTimeForEnemyToReachUs;

	}

	private static void maybeUpdateMineLocations() {
		debug_startMethod();

		if (Clock.getRoundNum() % 10 == 0) {
			myMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, myTeam);
			enemyMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, enemyTeam);
			nonAlliedMineLocations = rc.senseNonAlliedMineLocations(mapCenter, HUGE_RADIUS);			
			allMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, null);
		}

		debug_endMethod();
	}

	private static boolean willTakeLongTimeToReachEnemy() {
		debug_startMethod();

		boolean willTakeLongTimeToReachEnemy = true;

		if (rawDistanceBetweenHQs < HQ_RAW_DISTANCE_TINY_DISTANCE) {
			willTakeLongTimeToReachEnemy = false;

		} else if (rawDistanceBetweenHQs < HQ_RAW_DISTANCE_MEDIUM_DISTANCE) {
			double percentNonAlliedMInes = nonAlliedMineLocations.length / numMapLocations;

			if (percentNonAlliedMInes > HEAVILY_MINED_PERCENT_THRESHOLD) {

				willTakeLongTimeToReachEnemy = true;

			}			
		}

		debug_endMethod();

		return willTakeLongTimeToReachEnemy;
	}

	private static boolean ourBaseIsUnderAttack() {
		debug_startMethod();

		boolean ourBaseIsUnderAttack = false;

		Robot[] enemiesNearOurHQ = rc.senseNearbyGameObjects(Robot.class, myHQLocation, DEFAULT_SENSE_RADIUS_SQUARED, enemyTeam);
		if (enemiesNearOurHQ.length > 0) {
			ourBaseIsUnderAttack = true;
		}

		debug_endMethod();

		return ourBaseIsUnderAttack;
	}

	private static void decideMove_medbay() {
		debug_startMethod();
		debug_endMethod();		
	}

	private static void decideMove_generator() {
		debug_startMethod();
		debug_endMethod();		
	}

}
