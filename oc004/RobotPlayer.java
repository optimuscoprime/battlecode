package oc004;

import java.util.*;

import battlecode.common.*;

import static oc004.Debugging.*;
import static battlecode.common.Direction.*;
import static battlecode.common.RobotType.*;
import static battlecode.common.Upgrade.*;

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

	private static Random random;

	private static RobotController rc;
	private static Team myTeam;
	private static Team enemyTeam;
	private static int mapWidth;
	private static int mapHeight;
	private static MacroStrategy macroStrategy;
	private static MicroStrategy microStrategy;
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

	private static Robot[] allEnemyLocations;

	private static MapLocation soldierWaypoint;

	private static MapLocation[] allEncampmentLocations;

	private static MapLocation[] alliedEncampmentLocations;

	private static MapLocation closestNonAlliedEncampmentLocation;

	private static double percentAlliedMines;

	private static double percentNonAlliedMines;

	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {
			debug_printf("START OF TURN");

			try {
				decideMove();
			} catch (Exception e) {
				debug_printf("Unexpected exception: %s\n", e.toString());
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
		RobotPlayer.mapCenter = new MapLocation((myHQLocation.x + enemyHQLocation.x) / 2, (myHQLocation.y + enemyHQLocation.y) / 2);
		RobotPlayer.rawDistanceBetweenHQs = myHQLocation.distanceSquaredTo(enemyHQLocation);
		RobotPlayer.numMapLocations = mapWidth * mapHeight;
		RobotPlayer.myLocation = rc.getLocation();
		RobotPlayer.random = new Random();

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

		if (rc.isActive()) {

			updateEnemyLocations();			

			decideMacroStrategy();					

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
		}

		debug_endMethod();		
	}

	private static void updateEnemyLocations() {
		debug_startMethod();

		allEnemyLocations = rc.senseNearbyGameObjects(Robot.class, myLocation, HUGE_RADIUS, enemyTeam);

		int smallestDistance = -1;

		for (Robot enemy: allEnemyLocations) {
			RobotInfo enemyInfo = null;
			try {
				enemyInfo = rc.senseRobotInfo(enemy);
			} catch (GameActionException e) {
				debug_catch(e);
			}				
			if (enemyInfo != null) {
				if (enemyInfo.type != ARTILLERY || myShields > SHIELD_ARTILLERY_THRESHOLD) {
					int distanceSquared = myLocation.distanceSquaredTo(enemyInfo.location);
					if (smallestDistance == -1 || distanceSquared < smallestDistance) {
						smallestDistance = distanceSquared;
						closestEnemyLocation = enemyInfo.location;
					}				
				}	
			}

		}

		if (closestEnemyLocation == null) {
			closestEnemyLocation = enemyHQLocation;
		}

		debug_endMethod();
	}

	private static boolean decideMove_HQ_spawn() {
		debug_startMethod();

		Direction bestSpawnDirection = findBestSpawnDirection();

		boolean spawned = didSpawn(bestSpawnDirection);

		debug_endMethod();

		return spawned;
	}

	private static void decideMove_HQ_attack() {
		debug_startMethod();

		if (!decideMove_HQ_spawn()) {
			decideMove_HQ_research();
		}

		debug_endMethod();
	}

	private static boolean didSpawn(Direction spawnDirection) {
		debug_startMethod();

		boolean spawned = false;

		if (spawnDirection != NONE && spawnDirection != OMNI) {
			MapLocation spawnLocation = myHQLocation.add(spawnDirection);
			GameObject gameObject = null;

			try {
				gameObject = rc.senseObjectAtLocation(spawnLocation);	
			} catch(GameActionException e) {
				debug_catch(e);
			}

			if (gameObject == null) {
				try {
					rc.spawn(spawnDirection);
					spawned = true;
				} catch (GameActionException e) {
					debug_catch(e);
				}
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
			GameObject gameObject = null;

			try {
				gameObject = rc.senseObjectAtLocation(newLocation);
			} catch (GameActionException e) {
				debug_catch(e);
			}

			if (gameObject == null) {
				int distanceSquared = newLocation.distanceSquaredTo(closestEnemyLocation);

				Team mineStatus = rc.senseMine(newLocation);
				if (mineStatus == null || mineStatus != myTeam) {
					distanceSquared *= 2; // penalise distances that hurt us
				}

				if (smallestDistance == -1 || distanceSquared < smallestDistance) {
					smallestDistance = distanceSquared;
					bestDirection = direction;
				}
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

			macroStrategy = MacroStrategy.DEFEND;

		} else if (willTakeShortTimeToReachEnemy()) {

			macroStrategy = MacroStrategy.ATTACK;

		} else if (willTakeLongTimeForEnemyToReachUs()) {

			macroStrategy = MacroStrategy.EXPAND;

		} else if (lotsOfExcessPower()) {

			macroStrategy = MacroStrategy.RESEARCH;

		}

		if (macroStrategy == null) {
			macroStrategy = MacroStrategy.ATTACK;
		}

		debug_printf("MACRO STRATEGY IS: %s\n", macroStrategy.toString());					

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

			if (percentAlliedMines > HEAVILY_MINED_PERCENT_THRESHOLD) {

				willTakeLongTimeForEnemyToReachUs = true;

			}
		}

		debug_endMethod();

		return willTakeLongTimeForEnemyToReachUs;

	}

	private static void maybeUpdateMineLocations() {
		debug_startMethod();

		// do this occasioanlly if the cost is prohibitive

		myMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, myTeam);
		enemyMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, enemyTeam);
		nonAlliedMineLocations = rc.senseNonAlliedMineLocations(mapCenter, HUGE_RADIUS);			
		allMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, null);

		percentAlliedMines = (allMineLocations.length - enemyMineLocations.length) / numMapLocations;
		percentNonAlliedMines = nonAlliedMineLocations.length / numMapLocations;

		debug_endMethod();
	}

	private static boolean willTakeShortTimeToReachEnemy() {
		debug_startMethod();

		boolean willTakeShortTimeToReachEnemy = false;

		if (rawDistanceBetweenHQs < HQ_RAW_DISTANCE_TINY_DISTANCE) {
			willTakeShortTimeToReachEnemy = true;

		} else if (rawDistanceBetweenHQs < HQ_RAW_DISTANCE_MEDIUM_DISTANCE) {

			if (percentNonAlliedMines < LIGHTLY_MINED_PERCENT_THRESHOLD) {
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

	private static void decideMove_HQ_expand() {
		debug_startMethod();

		decideMove_HQ_attack();

		debug_endMethod();
	}

	private static void decideMove_HQ_defend() {
		debug_startMethod();

		decideMove_HQ_attack();

		debug_endMethod();
	}	

	private static void decideMove_HQ_research() {
		debug_startMethod();

		debug_printf("RESEARCHING\n");

		Upgrade upgrade = findBestUpgrade();
		if (upgrade != null) {
			if (!couldResearch(upgrade)) {
				decideMove_HQ_spawn();
			}
		}

		debug_endMethod();
	}	


	private static boolean couldResearch(Upgrade upgrade) {
		debug_startMethod();

		boolean researched = false;

		try {
			rc.researchUpgrade(upgrade);
			researched = true;
		} catch (GameActionException e) {
			debug_catch(e);
		}

		debug_endMethod();

		return researched;
	}

	private static Upgrade findBestUpgrade() {
		debug_startMethod();

		Upgrade[] upgradePriorities = new Upgrade[0];

		switch(macroStrategy) {
			case ATTACK:
				upgradePriorities = new Upgrade[] {
						FUSION,
						DEFUSION,
						NUKE,
						VISION,												
						PICKAXE,
				};
				break;
			case DEFEND:
				upgradePriorities = new Upgrade[] {
						FUSION,						
						PICKAXE,
						NUKE,
						DEFUSION,						
						VISION					
				};				
				break;
			case EXPAND:
				upgradePriorities = new Upgrade[] {
						FUSION,
						DEFUSION,
						VISION,												
						NUKE,
						PICKAXE,
				};				
				break;
			case RESEARCH:
				upgradePriorities = new Upgrade[] {
						NUKE,
						FUSION,						
						PICKAXE,						
						DEFUSION,
						VISION						
				};				
				break;
		}

		Upgrade bestUpgrade = null;

		for (Upgrade upgrade: upgradePriorities) {
			if (upgrade == DEFUSION && percentNonAlliedMines < LIGHTLY_MINED_PERCENT_THRESHOLD) {
				continue; // don't bother if there are not many mines
			}			
			if (!rc.hasUpgrade(upgrade)) {
				bestUpgrade = upgrade;
				break;
			}
		}

		debug_endMethod();		

		return bestUpgrade;
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

	private static void decideMove_solder() {
		debug_startMethod();

		if (rc.isActive()) {	
			myLocation = rc.getLocation();
			myShields = rc.getShields();	

			updateEnemyLocations();
			updateEncampmentLocations();

			decideMicroStrategy();
			decideMacroStrategy();

			switch(microStrategy) {
				case ATTACK:
					break;
				case CONCAVE:
					break;
				case MAKE_SPACE:
					break;
				case RETREAT:
					break;
				case SUICIDE:
					break;
				case NONE:
					break;					
			}

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

	private static void decideMicroStrategy() {
		debug_startMethod();
		debug_endMethod();

	}

	private static void updateEncampmentLocations() {
		debug_startMethod();

		allEncampmentLocations = rc.senseAllEncampmentSquares();
		alliedEncampmentLocations = rc.senseAlliedEncampmentSquares();

		closestNonAlliedEncampmentLocation = null;
		int shortestDistance = -1;

		for (MapLocation encampmentLocation : allEncampmentLocations) {
			GameObject gameObject = null;
			try {
				gameObject = rc.senseObjectAtLocation(encampmentLocation);
			} catch (GameActionException e) {
				debug_catch(e);
			}
			if (gameObject == null || gameObject.getTeam() != myTeam) {
				int distance = myLocation.distanceSquaredTo(encampmentLocation);
				if (shortestDistance == -1 || distance < shortestDistance) {
					distance = shortestDistance;
					closestNonAlliedEncampmentLocation = encampmentLocation;
				}
			}
		}

		debug_endMethod();
	}

	private static void decideMove_soldier_research() {
		debug_startMethod();

		if (soldierWaypoint != null) {
			if (myLocation.equals(soldierWaypoint)) {
				soldierWaypoint = null;
			}
		}

		if (soldierWaypoint == null) {
			pickNewSoldierWaypoint();
		}

		moveToLocation(soldierWaypoint);

		debug_endMethod();
	}

	private static void moveToLocation(MapLocation destination) {
		debug_startMethod();

		Direction direction = myLocation.directionTo(destination);

		if (rc.canMove(direction)) {
			try {
				rc.move(direction);
			} catch (GameActionException e) {
				debug_catch(e);
			}
		}

		debug_endMethod();
	}

	private static void pickNewSoldierWaypoint() {
		debug_startMethod();

		int x = random.nextInt(mapHeight);
		int y = random.nextInt(mapWidth);
		soldierWaypoint = new MapLocation(x,y);

		debug_endMethod();
	}

	private static void decideMove_soldier_expand() {
		debug_startMethod();

		if (closestNonAlliedEncampmentLocation != null) {
			moveToLocation(closestNonAlliedEncampmentLocation);
		}

		debug_endMethod();
	}

	private static void decideMove_soldier_defend() {
		debug_startMethod();

		moveToLocation(myHQLocation);

		debug_endMethod();

	}

	private static void decideMove_soldier_attack() {
		debug_startMethod();

		moveToLocation(closestEnemyLocation);

		debug_endMethod();
	}	

}
