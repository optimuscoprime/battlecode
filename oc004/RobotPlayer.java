package oc004;

import java.util.*;

import battlecode.common.*;

import static oc004.Debugging.*;
import static battlecode.common.GameConstants.*;
import static battlecode.common.Direction.*;
import static battlecode.common.RobotType.*;
import static battlecode.common.Upgrade.*;

public class RobotPlayer {

	private static final int LOTS_OF_EXCESS_POWER_THRESHOLD = 1000;

	private static final int HQ_RAW_DISTANCE_BIG_DISTANCE = 100;
	private static final int HQ_RAW_DISTANCE_MEDIUM_DISTANCE = HQ_RAW_DISTANCE_BIG_DISTANCE / 2;	
	private static final int HQ_RAW_DISTANCE_TINY_DISTANCE = HQ_RAW_DISTANCE_MEDIUM_DISTANCE / 2;	

	private static final int DEFAULT_SENSE_RADIUS_SQUARED = 14;
	private static final int UPGRADED_SENSE_RADIUS_SQUARED = 33;	

	private static final double HEAVILY_MINED_PERCENT_THRESHOLD = 0.4;
	private static final double LIGHTLY_MINED_PERCENT_THRESHOLD = 0.1;

	private static final int HUGE_RADIUS = 1000000;

	private static final double SHIELD_ARTILLERY_THRESHOLD = 65;

	private static final double MIN_POWER_THRESHOLD_FOR_SPAWNING = 10;

	private static final double AVERAGE_BYTECODES_USED = 5000;

	private static Direction[] GENUINE_DIRECTIONS = new Direction[] {
		Direction.NORTH,
		Direction.NORTH_EAST,
		Direction.NORTH_WEST,
		Direction.SOUTH,
		Direction.SOUTH_EAST,
		Direction.SOUTH_WEST,
		Direction.EAST,
		Direction.WEST
	};

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
	private static MapLocation rallyPoint;

	private static MapLocation[] allEncampmentLocations;

	private static MapLocation[] alliedEncampmentLocations;

	private static MapLocation closestNonAlliedEncampmentLocation;

	private static double percentAlliedMines;

	private static double percentNonAlliedMines;

	private static MapLocation closestAllyLocation;

	private static Robot[] allyLocations;

	private static int numAlliedRobots;

	private static Robot[] nearbyEnemyLocations;

	private static Robot[] nearbyAllyLocations;

	private static int numNearbyAllies;

	private static int numNearbyEnemies;

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
		RobotPlayer.rallyPoint = mapCenter;

		debug_endMethod();
	}

	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {
			debug_printf("START OF TURN");

			try {
				decideMove();
			} catch (Exception e) {
				debug_printf("Unexpected exception: %s\n", e.toString());
				e.printStackTrace();
			}

			debug_printf("END OF TURN");

			rc.yield();
		}
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
			updateAllyLocations();
			updateMineLocations();
			updateEncampmentLocations();

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
		nearbyEnemyLocations = rc.senseNearbyGameObjects(Robot.class, myLocation, UPGRADED_SENSE_RADIUS_SQUARED, enemyTeam);
		numNearbyEnemies = nearbyEnemyLocations.length;
		
		closestEnemyLocation = enemyHQLocation;
		int smallestDistance = myLocation.distanceSquaredTo(closestEnemyLocation);
		
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
					if (distanceSquared < smallestDistance) {
						smallestDistance = distanceSquared;
						closestEnemyLocation = enemyInfo.location;
					}				
				}	
			}

		}		

		debug_endMethod();
	}

	private static boolean decideMove_HQ_spawn() {
		debug_startMethod();

		boolean spawned = false;

		/* only if we can pay the upkeep */

		double teamPower = rc.getTeamPower();

		if (teamPower > MIN_POWER_THRESHOLD_FOR_SPAWNING &&
				teamPower > (UNIT_POWER_UPKEEP * numAlliedRobots) &&
				teamPower > (POWER_COST_PER_BYTECODE * AVERAGE_BYTECODES_USED * numAlliedRobots)) {
			Direction bestSpawnDirection = findBestSpawnDirection();

			spawned = didSpawn(bestSpawnDirection);				
		}

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

	private static void updateMineLocations() {
		debug_startMethod();

		myMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, myTeam);
		enemyMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, enemyTeam);
		nonAlliedMineLocations = rc.senseNonAlliedMineLocations(mapCenter, HUGE_RADIUS);			
		allMineLocations = rc.senseMineLocations(mapCenter, HUGE_RADIUS, null);

		percentAlliedMines = (allMineLocations.length - enemyMineLocations.length) / numMapLocations;
		percentNonAlliedMines = nonAlliedMineLocations.length / numMapLocations;

		debug_endMethod();
	}

	private static void updateAllyLocations() {
		allyLocations = rc.senseNearbyGameObjects(Robot.class, HUGE_RADIUS, myTeam);
		nearbyAllyLocations = rc.senseNearbyGameObjects(Robot.class, UPGRADED_SENSE_RADIUS_SQUARED, myTeam);
		numNearbyAllies = nearbyAllyLocations.length;

		numAlliedRobots = allyLocations.length;

		int shortestDistance = -1;
		closestAllyLocation = rallyPoint;

		for (Robot ally: allyLocations) {
			RobotInfo allyInfo = null;
			try {
				allyInfo = rc.senseRobotInfo(ally);
			} catch (GameActionException e) {
				debug_catch(e);
			}
			if (allyInfo != null) {
				int distance = myLocation.distanceSquaredTo(allyInfo.location);
				if (shortestDistance == -1 || distance < shortestDistance) {
					shortestDistance = distance;
					closestAllyLocation = allyInfo.location;
				}
			}

		}		
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

		Robot[] enemiesNearOurHQ = rc.senseNearbyGameObjects(Robot.class, myHQLocation, UPGRADED_SENSE_RADIUS_SQUARED, enemyTeam);
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

	private static void decideMove_solder() {
		debug_startMethod();

		if (rc.isActive()) {	
			myLocation = rc.getLocation();
			myShields = rc.getShields();	

			updateEnemyLocations();
			updateEncampmentLocations();
			updateMineLocations();
			updateAllyLocations();

			decideMicroStrategy();

			boolean doMacro = false;

			switch(microStrategy) {
				case ATTACK:
					decideMove_soldier_attack();
					break;
				case DEFEND:
					decideMove_soldier_defend();
					break;
				case NONE:
					doMacro = true;
					break;					
			}

			if (doMacro) {
				decideMacroStrategy();

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
		}

		debug_endMethod();		
	}

	private static void decideMicroStrategy() {
		debug_startMethod();

		microStrategy = MicroStrategy.NONE;

		if (numNearbyEnemies > 0) {
			if (numNearbyEnemies < numNearbyAllies) {
				microStrategy = MicroStrategy.ATTACK;
			} else if (numNearbyEnemies > 2 * numNearbyAllies) {
				microStrategy = MicroStrategy.DEFEND;
			}
		}

		debug_printf("MICRO STRATEGY IS: %s\n", microStrategy.toString());			

		debug_endMethod();
	}

	private static void decideMove_soldier_attack() {
		debug_startMethod();

		if (microStrategy == MicroStrategy.ATTACK) {
			moveToLocation(closestEnemyLocation); 
		} else {
			moveToLocation(closestEnemyLocation); 
		}

		debug_endMethod();
	}	

	private static void decideMove_soldier_defend() {
		debug_startMethod();

		if (microStrategy == MicroStrategy.DEFEND) {
			moveToLocation(closestAllyLocation); 
		} else {
			moveToLocation(myHQLocation);
		}

		debug_endMethod();
	}	

	private static void decideMove_soldier_expand() {
		debug_startMethod();

		if (closestNonAlliedEncampmentLocation != null) {
			moveToLocation(closestNonAlliedEncampmentLocation);
		} else {
			moveToLocation(rallyPoint);
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


	private static void moveToLocation(MapLocation destination) {
		debug_startMethod();

		Direction direction = bestDirectionToGetTo(destination);

		if (direction != null && rc.canMove(direction)) {
			try {
				rc.move(direction);
			} catch (GameActionException e) {
				debug_catch(e);
			}
		}

		debug_endMethod();
	}

	private static Direction bestDirectionToGetTo(MapLocation destination) {
		debug_startMethod();

		Direction bestDirection = null;
		int shortestDistance = -1;

		for (Direction direction: GENUINE_DIRECTIONS) {
			if (rc.canMove(direction)) {
				MapLocation location = myLocation.add(direction);
				int distance = location.distanceSquaredTo(destination);
				if (shortestDistance == -1 || distance < shortestDistance) {
					shortestDistance = distance;
					bestDirection = direction;
				}
			}
		}

		debug_endMethod();

		return bestDirection;
	}

	private static void pickNewSoldierWaypoint() {
		debug_startMethod();

		int x = random.nextInt(mapHeight);
		int y = random.nextInt(mapWidth);
		soldierWaypoint = new MapLocation(x,y);

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
