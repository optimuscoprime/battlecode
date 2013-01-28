package oc004;

import java.util.*;

import battlecode.common.*;

import static oc004.MacroStrategy.*;
import static oc004.Debugging.*;
import static battlecode.common.Direction.*;
import static battlecode.common.RobotType.*;

public class RobotPlayer {

	private static final int LOTS_OF_EXCESS_POWER_THRESHOLD = 1000;

	private static final int HQ_RAW_DISTANCE_BIG_DISTANCE = 100;
	private static final int HQ_RAW_DISTANCE_MEDIUM_DISTANCE = HQ_RAW_DISTANCE_BIG_DISTANCE / 2;	
	private static final int HQ_RAW_DISTANCE_TINY_DISTANCE = HQ_RAW_DISTANCE_MEDIUM_DISTANCE / 2;	

	private static final int DEFAULT_SENSE_RADIUS_SQUARED = 14;

	private static final double HEAVILY_MINED_PERCENT_THRESHOLD = 0.4;
	private static final double LIGHTLY_MINED_PERCENT_THRESHOLD = 0.1;
	
	private static final int HUGE_RADIUS = 1000000;

	private static final double SHIELD_ARTILLERY_THRESHOLD = 65;


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
	private static MapLocation myLocation;
	
	private static double myShields;

	private static MapLocation[] myMineLocations;
	private static MapLocation[] enemyMineLocations;
	private static MapLocation[] allMineLocations;
	private static MapLocation[] nonAlliedMineLocations;

	private static MapLocation closestEnemyLocation;

	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {

			debug_printf("START OF TURN");
			
			try {
				decideMove();
			} catch (Exception e) {
				debug_catch(e);
			}

			debug_printf("END OF TURN");
			
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
		RobotPlayer.myLocation = rc.getLocation();

		debug_endMethod();
	}

	private static void decideMove() {
		debug_startMethod();

		switch(rc.getType()) {
			case ARTILLERY:
				decideMove_artillery();
				break;
			case GENERATOR:
				decideMove_generator();
				break;
			case HQ:
				decideMove_HQ();
				break;
			case MEDBAY:
				decideMove_medbay();
				break;
			case SHIELDS:
				decideMove_shields();
				break;
			case SOLDIER:
				decideMove_solder();
				break;
			case SUPPLIER:
				decideMove_supplier();
				break;
		}

		debug_endMethod();
	}

	private static void decideMove_HQ() {
		debug_startMethod();

		updateClosestEnemyLocation();
		
		decideMacroStrategy();		

		if (rc.isActive()) {

			debug_printf("MACRO STRATEGY IS: %s\n", macroStrategy.toString());			
			
			switch(macroStrategy) {
				case ATTACK:
					decideMove_HQ_attack();
					break;
				case DEFEND:
					decideMove_HQ_defend();
					break;
				case EXPAND:
					decideMove_HQ_expand();
					break;
				case RESEARCH:
					decideMove_HQ_research();
					break;
			}

			// what should everyone else do?
		}

		debug_endMethod();		
	}

	private static void updateClosestEnemyLocation() {
		debug_startMethod();
		
		Robot[] enemies = rc.senseNearbyGameObjects(Robot.class, myLocation, HUGE_RADIUS, enemyTeam);
		
		int smallestDistance = -1;
		
		for (Robot enemy: enemies) {
			try {
				RobotInfo enemyInfo = rc.senseRobotInfo(enemy);
				if (enemyInfo.type != ARTILLERY || myShields > SHIELD_ARTILLERY_THRESHOLD) {
					int distanceSquared = myLocation.distanceSquaredTo(enemyInfo.location);
					if (smallestDistance == -1 || distanceSquared < smallestDistance) {
						smallestDistance = distanceSquared;
						closestEnemyLocation = enemyInfo.location;
					}				
				}				
			} catch (GameActionException e) {
				debug_catch(e);
			}
		}
		
		if (closestEnemyLocation == null) {
			closestEnemyLocation = enemyHQLocation;
		}
			
		debug_endMethod();
	}

	private static void decideMove_HQ_attack() {
		debug_startMethod();
		
		// build more units
		
		Direction bestSpawnDirection = findBestSpawnDirection();
		
		if (!didSpawn(bestSpawnDirection)) {
			// we wanted to spawn but couldn't
			decideMove_HQ_research();
		}
		
		debug_endMethod();
	}

	private static boolean didSpawn(Direction spawnDirection) {
		debug_startMethod();
		
		boolean spawned = false;
		
		if (spawnDirection != NONE && spawnDirection != OMNI) {
			MapLocation spawnLocation = myHQLocation.add(spawnDirection);
			try {
				GameObject gameObject = rc.senseObjectAtLocation(spawnLocation);	
				if (gameObject == null) {
					rc.spawn(spawnDirection);
					spawned = true;
				}
			} catch(GameActionException e) {
				debug_catch(e);
			}
		}
		
		debug_endMethod();
		
		return spawned;
	}

	private static Direction findBestSpawnDirection() {
		debug_startMethod();
		
		Direction bestDirection = NONE;
		int smallestDistance = -1;
		
		for (Direction direction: Direction.values()) {
			MapLocation newLocation = myHQLocation.add(direction);
			try {
				GameObject gameObject = rc.senseObjectAtLocation(newLocation);
				if (gameObject == null) {
					int distanceSquared = newLocation.distanceSquaredTo(closestEnemyLocation);
					if (smallestDistance == -1 || distanceSquared < smallestDistance) {
						smallestDistance = distanceSquared;
						bestDirection = direction;
					}
				}
			} catch (GameActionException e) {
				debug_catch(e);
			}
		}
		
		debug_endMethod();				
		
		return bestDirection;
	}

	private static void decideMacroStrategy() {
		debug_startMethod();

		macroStrategy = null;

		maybeUpdateMineLocations();		

		if (ourBaseIsUnderAttack()) {
			macroStrategy = DEFEND;
		} else if (willTakeShortTimeToReachEnemy()) {
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

		// always do this for now
		
		myMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, myTeam);
		enemyMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, enemyTeam);
		nonAlliedMineLocations = rc.senseNonAlliedMineLocations(mapCenter, HUGE_RADIUS);			
		allMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, null);

		debug_endMethod();
	}

	private static boolean willTakeShortTimeToReachEnemy() {
		debug_startMethod();

		boolean willTakeShortTimeToReachEnemy = false;

		if (rawDistanceBetweenHQs < HQ_RAW_DISTANCE_TINY_DISTANCE) {
			willTakeShortTimeToReachEnemy = true;

		} else if (rawDistanceBetweenHQs < HQ_RAW_DISTANCE_MEDIUM_DISTANCE) {
			double percentNonAlliedMInes = nonAlliedMineLocations.length / numMapLocations;

			if (percentNonAlliedMInes < LIGHTLY_MINED_PERCENT_THRESHOLD) {

				willTakeShortTimeToReachEnemy = true;

			}			
		}

		debug_endMethod();

		return willTakeShortTimeToReachEnemy;
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

	private static void decideMove_solder() {
		debug_startMethod();

		if (rc.isActive()) {
			
			myLocation = rc.getLocation();
			myShields = rc.getShields();	
			
			updateClosestEnemyLocation();
			
			decideMacroStrategy();

			debug_printf("MACRO STRATEGY IS: %s\n", macroStrategy.toString());			
			
			switch(macroStrategy) {
				case ATTACK:
					decideMove_soldier_attack();
					break;
				case DEFEND:
					decideMove_soldier_defend();
					break;
				case EXPAND:
					decideMove_soldier_expand();
					break;
				case RESEARCH:
					decideMove_soldier_research();
					break;
			}
		}
		
		debug_endMethod();		
	}

	private static void decideMove_soldier_research() {
		debug_startMethod();
		debug_endMethod();
		
	}

	private static void decideMove_soldier_expand() {
		debug_startMethod();
		debug_endMethod();
		
	}

	private static void decideMove_soldier_defend() {
		debug_startMethod();
		debug_endMethod();
		
	}

	private static void decideMove_soldier_attack() {
		debug_startMethod();
		debug_endMethod();
		
	}

	private static void decideMove_HQ_expand() {
		debug_startMethod();
		
		decideMove_HQ_attack();
		
		debug_endMethod();
	}

	private static void decideMove_HQ_defend() {
		debug_startMethod();
		
		// if army is small, spawn
		decideMove_HQ_attack();
		
		// if army is big, research
		// decideMove_HQ_research();
		
		debug_endMethod();
	}	
	
	private static void decideMove_HQ_research() {
		debug_startMethod();
		
		debug_printf("RESEARCHING\n");
		
		debug_endMethod();
	}	
	

	private static void decideMove_shields() {
		debug_startMethod();
		debug_endMethod();		
	}		
	

	private static void decideMove_medbay() {
		debug_startMethod();
		debug_endMethod();		
	}

	private static void decideMove_generator() {
		debug_startMethod();
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

}
