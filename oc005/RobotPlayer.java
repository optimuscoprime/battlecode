package oc005;

import java.util.*;

import battlecode.common.*;

import static oc005.Util.*;
import static battlecode.common.GameConstants.*;
import static battlecode.common.Direction.*;
import static battlecode.common.Team.*;
import static battlecode.common.RobotType.*;
import static battlecode.common.Upgrade.*;

public class RobotPlayer {

	private static final int LOTS_OF_EXCESS_POWER_THRESHOLD = 500;

	private static final int HQ_RAW_DISTANCE_BIG_DISTANCE = 6000;
	private static final int HQ_RAW_DISTANCE_MEDIUM_DISTANCE = HQ_RAW_DISTANCE_BIG_DISTANCE / 2;	
	private static final int HQ_RAW_DISTANCE_TINY_DISTANCE = HQ_RAW_DISTANCE_MEDIUM_DISTANCE / 2;	

	private static final int DEFAULT_SENSE_RADIUS_SQUARED = 14;
	private static final int UPGRADED_SENSE_RADIUS_SQUARED = 33;	
	private static final int ARTILLERY_SENSE_RADIUS_SQUARED = 63;	

	private static final int DIAGONALLY_ADJACENT_RADIUS = 2;
	private static final int DIRECTLY_ADJACENT_RADIUS = 1;

	private static final double HEAVILY_MINED_PERCENT_THRESHOLD = 0.8;
	private static final double LIGHTLY_MINED_PERCENT_THRESHOLD = 0.1;

	private static final int HUGE_RADIUS = 1000000;

	private static final double SHIELD_ARTILLERY_THRESHOLD = 65;

	private static final double MIN_POWER_THRESHOLD_FOR_SPAWNING = 10;

	private static final double AVERAGE_BYTECODES_USED = 9000;

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
	private static int numMapLocations;
	private static MapLocation myLocation;

	private static double myShields;

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

	private static int numAllies;

	private static Robot[] nearbyEnemyLocations;

	private static Robot[] nearbyAllyLocations;

	private static int numNearbyAllies;

	private static int numNearbyEnemies;

	private static double numEnemyEncampments;

	private static double numAvailableEncampments;

	private static int numAlliedEncampments;

	private static int numAlliedGenerators;

	private static int numAlliedSuppliers;

	private static int numUpgradesRemaining;

	private static int numAlliedSoldiers;

	private static int numPartialAlliedEncampments;

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
		RobotPlayer.numMapLocations = mapWidth * mapHeight;
		RobotPlayer.myLocation = rc.getLocation();
		RobotPlayer.random = new Random();
		RobotPlayer.rallyPoint = mapCenter;

		debug_endMethod();
	}

	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {
			try {
				decideMove();
			} catch (Exception e) {
				debug_printf("Unexpected exception: %s\n", e.toString());
				e.printStackTrace();
			}
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

			updateAllCaches();

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
		numNearbyEnemies = 0;

		for (Robot nearbyEnemy: nearbyEnemyLocations) {
			RobotInfo enemyInfo = null;
			try {
				enemyInfo = rc.senseRobotInfo(nearbyEnemy);
			} catch (GameActionException e) {
				debug_catch(e);
			}				
			if (enemyInfo != null) {
				if (enemyInfo.type == SOLDIER) {
					numNearbyEnemies++;
				}
			}			
		}

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
				teamPower > (UNIT_POWER_UPKEEP * numAllies) &&
				teamPower > (POWER_COST_PER_BYTECODE * AVERAGE_BYTECODES_USED * numAllies)) {
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
				if (mineStatus == NEUTRAL || mineStatus == enemyTeam) {
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

		boolean willTakeLongTimeForEnemyToReachUs = willTakeLongTimeForEnemyToReachUs();

		String macroReason = "";

		// TODO: enemy nuke progress

		if (Clock.getRoundNum() >= ROUND_MIN_LIMIT) {

			macroStrategy = MacroStrategy.ATTACK;

		} else if (ourBaseIsUnderAttack()) {

			macroStrategy = MacroStrategy.DEFEND;

		} else if (willTakeShortTimeToReachEnemy()) {

			macroStrategy = MacroStrategy.ATTACK;

		} else if (willTakeLongTimeForEnemyToReachUs && numAlliedSoldiers >= 10 && numUpgradesRemaining > 0) {

			macroStrategy = MacroStrategy.RESEARCH;			

		} else if (willTakeLongTimeForEnemyToReachUs && numAlliedSoldiers >= (2 + (2 * numAlliedEncampments + numPartialAlliedEncampments)) && numAvailableEncampments > 0 && lotsOfExcessPower()) {

			macroStrategy = MacroStrategy.EXPAND;

		} else if (Clock.getRoundNum() < 100) {

			macroReason = "round < 100";

			macroStrategy = MacroStrategy.DEFEND;

		} else if (numAlliedSoldiers >= 10 && numAlliedEncampments > 0 && numUpgradesRemaining > 0) {

			macroStrategy = MacroStrategy.RESEARCH;			

		} else if (numAlliedSoldiers >= (2 + (3 * numAlliedEncampments + numPartialAlliedEncampments)) && numAvailableEncampments > 0 && lotsOfExcessPower()) {

			macroStrategy = MacroStrategy.EXPAND;

		} else {

			macroStrategy = MacroStrategy.ATTACK; // default

		}

		debug_printf("MACRO STRATEGY IS: %s\n", macroStrategy.toString());
		rc.setIndicatorString(0, "MACRO: " + macroStrategy.toString() + " (" + macroReason + ")");

		debug_endMethod();
	}

	private static boolean lotsOfExcessPower() {
		debug_startMethod();

		double encampmentCaptureCost = CAPTURE_POWER_COST  * ( 1 + numAlliedEncampments);

		boolean lotsOfExcessPower = (rc.getTeamPower() > encampmentCaptureCost);

		debug_endMethod();

		return lotsOfExcessPower;
	}

	private static boolean willTakeLongTimeForEnemyToReachUs() {
		debug_startMethod();

		boolean willTakeLongTimeForEnemyToReachUs = false;

		int distanceFromOurHQToClosestEnemy = closestEnemyLocation.distanceSquaredTo(myHQLocation);

		debug_printf("distance from our HQ to closest enemy is %d\n", distanceFromOurHQToClosestEnemy);

		if (distanceFromOurHQToClosestEnemy > HQ_RAW_DISTANCE_BIG_DISTANCE) {

			willTakeLongTimeForEnemyToReachUs = true;

		} else if (distanceFromOurHQToClosestEnemy > HQ_RAW_DISTANCE_MEDIUM_DISTANCE) {

			if (percentAlliedMines > HEAVILY_MINED_PERCENT_THRESHOLD) {

				willTakeLongTimeForEnemyToReachUs = true;

			}
		}

		debug_endMethod();

		return willTakeLongTimeForEnemyToReachUs;

	}

	private static void updateMineLocations() {
		debug_startMethod();

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

		numAllies = allyLocations.length;

		int shortestDistance = -1;
		closestAllyLocation = rallyPoint;
		numAlliedSoldiers = 0;
		numPartialAlliedEncampments = 0;

		for (Robot ally: allyLocations) {
			RobotInfo allyInfo = null;
			try {
				if (rc.canSenseObject(ally)) {
					allyInfo = rc.senseRobotInfo(ally);
				}
			} catch (GameActionException e) {
				debug_catch(e);
			}
			if (allyInfo != null) {
				if (allyInfo.type == SOLDIER) {
					numAlliedSoldiers++;

					if (rc.canSenseSquare(allyInfo.location)) {
						if (rc.senseEncampmentSquare(allyInfo.location)) {
							numPartialAlliedEncampments++;
						}
					}
				}
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

		int distanceFromOurHQToClosestEnemy = closestEnemyLocation.distanceSquaredTo(myHQLocation);

		debug_printf("distance from our base to closest enemy: %d\n", distanceFromOurHQToClosestEnemy);

		if (distanceFromOurHQToClosestEnemy < HQ_RAW_DISTANCE_TINY_DISTANCE) {

			willTakeShortTimeToReachEnemy = true;

		} else if (distanceFromOurHQToClosestEnemy < HQ_RAW_DISTANCE_MEDIUM_DISTANCE) {

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
						DEFUSION,						
						NUKE,
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
						FUSION,						
						DEFUSION,
						PICKAXE,
						NUKE,						
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

			updateAllCaches();

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
						decideMove_soldier_expand();
						//decideMove_soldier_research();
						break;
				}
			}
		}

		debug_endMethod();		
	}

	private static void decideMicroStrategy() {
		debug_startMethod();

		microStrategy = MicroStrategy.NONE;

		int distanceToEnemyHQ = myLocation.distanceSquaredTo(enemyHQLocation);

		if (numNearbyEnemies > 0) {
			if (numNearbyEnemies <= numNearbyAllies) {
				microStrategy = MicroStrategy.ATTACK;
			} else if (numNearbyEnemies > 2 * numNearbyAllies) {
				microStrategy = MicroStrategy.DEFEND;
			}
		} else if (distanceToEnemyHQ < DEFAULT_SENSE_RADIUS_SQUARED) {
			microStrategy = MicroStrategy.ATTACK;
			closestEnemyLocation = enemyHQLocation;
		}

		debug_printf("MICRO STRATEGY IS: %s\n", microStrategy.toString());	
		rc.setIndicatorString(1, "MICRO: " + microStrategy.toString());

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

		// reset waypoint
		soldierWaypoint = myHQLocation;

		if (microStrategy == MicroStrategy.DEFEND) {
			moveToLocation(closestAllyLocation); 
		} else {

			int distanceToMyHQ = myHQLocation.distanceSquaredTo(myLocation);

			boolean layingMine = false;

			if (distanceToMyHQ <= UPGRADED_SENSE_RADIUS_SQUARED && numNearbyEnemies == 0) {
				Team mineStatus = rc.senseMine(myLocation);
				if (mineStatus == null) {
					layingMine = tryLayMine();
				}
			}

			if (!layingMine) {
				moveToLocation(myHQLocation);
			}
		}

		debug_endMethod();
	}	

	private static boolean tryLayMine() {
		debug_startMethod();

		boolean layingMine = false;

		try {
			rc.layMine();
			layingMine = true;
		} catch (GameActionException e) {
			debug_catch(e);
		}		

		debug_endMethod();

		return layingMine;
	}

	private static void decideMove_soldier_expand() {
		debug_startMethod();

		if (closestNonAlliedEncampmentLocation != null) {
			if (myLocation.equals(closestNonAlliedEncampmentLocation)) {

				// try to defuse
				boolean defusing = false;
				MapLocation[] adjacentNonAlliedMines = rc.senseNonAlliedMineLocations(myLocation, DIAGONALLY_ADJACENT_RADIUS);

				if (adjacentNonAlliedMines.length > 4) {
					defusing = tryDefuseMine(adjacentNonAlliedMines[0]);
				}

				boolean safeToCapture = numNearbyEnemies == 0;

				if (!defusing && safeToCapture) {
					RobotType bestEncampmentType = findBestEncampmentType();
					try {
						rc.captureEncampment(bestEncampmentType);
					} catch (GameActionException e) {
						debug_catch(e);
					}
				}
			} else {

				// try to lay
				boolean laying = false;

				MapLocation[] adjacentAlliedMines = rc.senseMineLocations(myLocation, DIAGONALLY_ADJACENT_RADIUS, myTeam);
				if (adjacentAlliedMines.length == 0) {
					laying = tryLayMine();
				}				
				if (!laying) {

					moveToLocation(closestNonAlliedEncampmentLocation);
				}
			}
		} else {
			moveToLocation(rallyPoint);
		}

		debug_endMethod();
	}	

	private static boolean tryDefuseMine(MapLocation location) {
		debug_startMethod();

		boolean defusing = false;

		try {
			rc.defuseMine(location);
			defusing = true;
		} catch (GameActionException e) {
			debug_catch(e);
		}

		debug_endMethod();		

		return defusing;
	}

	private static RobotType findBestEncampmentType() {
		debug_startMethod();

		RobotType bestEncampmentType = GENERATOR;

		if (artilleryUsefulAtLocation(myLocation)) {
			bestEncampmentType = ARTILLERY;
		} else {
			// favour suppliers
			if (numAlliedSuppliers <= numAlliedGenerators * 2) {
				bestEncampmentType = SUPPLIER;
			} else {
				bestEncampmentType = GENERATOR;
			}
		}

		// TODO
		// SHIELDS
		// MEDBAY

		debug_endMethod();

		return bestEncampmentType;
	}

	private static boolean artilleryUsefulAtLocation(MapLocation location) {
		debug_startMethod();

		boolean shouldBuildArtillery = false;

		if ( location.x >= 8 && 
				location.y >= 8 &&
				location.x <= mapWidth - 8 &&
				location.y <= mapHeight - 8) {

			if (location.distanceSquaredTo(myHQLocation) <= ARTILLERY_SENSE_RADIUS_SQUARED ||
					location.distanceSquaredTo(mapCenter) <= ARTILLERY_SENSE_RADIUS_SQUARED ||
					location.distanceSquaredTo(enemyHQLocation) <= ARTILLERY_SENSE_RADIUS_SQUARED) {

				// don't build artillery at edge of map, useless?

				shouldBuildArtillery = true;
			}
		}

		debug_endMethod();	

		return shouldBuildArtillery;
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

		numAlliedEncampments = alliedEncampmentLocations.length;

		closestNonAlliedEncampmentLocation = null;
		int shortestDistance = -1;

		numEnemyEncampments = 0;

		numAlliedGenerators = 0;
		numAlliedSuppliers = 0;

		for (MapLocation encampmentLocation : allEncampmentLocations) {
			GameObject gameObject = null;
			try {
				if (rc.canSenseSquare(encampmentLocation)) {
					gameObject = rc.senseObjectAtLocation(encampmentLocation);
				}
			} catch (GameActionException e) {
				debug_catch(e);
			}
			if (gameObject == null || gameObject.getTeam() != myTeam) {
				if (gameObject != null) {
					numEnemyEncampments++;
				} else {
					numEnemyEncampments += 0.5;
				}
				int distance = myLocation.distanceSquaredTo(encampmentLocation);

				if (shortestDistance == -1 || distance < shortestDistance) {
					shortestDistance = distance;
					closestNonAlliedEncampmentLocation = encampmentLocation;
				}				
			} else if (gameObject != null && gameObject.getTeam() == myTeam) {
				if (gameObject instanceof Robot) {
					RobotInfo info = null;
					try {
						info = rc.senseRobotInfo((Robot)gameObject);
					} catch (GameActionException e) {
						debug_catch(e);
					}
					if (info != null) {
						if (info.type == GENERATOR) {
							numAlliedGenerators ++;
						} else if (info.type == SUPPLIER) {
							numAlliedSuppliers ++;
						} else if (info.type == SOLDIER && gameObject.getID() == rc.getRobot().getID()) {
							int distance = myLocation.distanceSquaredTo(encampmentLocation);

							if (shortestDistance == -1 || distance < shortestDistance) {
								shortestDistance = distance;
								closestNonAlliedEncampmentLocation = encampmentLocation;
							}								
						}
					}	
				}
			}
		}

		numAvailableEncampments = allEncampmentLocations.length - numAlliedEncampments - numEnemyEncampments;

		debug_endMethod();
	}

	private static void moveToLocation(MapLocation destination) {
		debug_startMethod();

		Direction direction = bestDirectionToGetTo(destination);

		if (direction != null && rc.canMove(direction)) {
			MapLocation nextLocation = myLocation.add(direction);

			Team mineStatus = rc.senseMine(nextLocation);
			boolean defusing = false;

			if ( (mineStatus == enemyTeam || mineStatus == NEUTRAL) &&
					(numNearbyEnemies == 0 || (numNearbyEnemies < 5 && random.nextInt(2) == 0))) {
				defusing = tryDefuseMine(nextLocation);
			}

			if (!defusing) {
				try {
					rc.move(direction);
				} catch (GameActionException e) {
					debug_catch(e);
				}
			}
		}

		debug_endMethod();
	}

	private static Direction bestDirectionToGetTo(MapLocation destination) {
		debug_startMethod();

		Direction bestDirection = null;
		int shortestDistance = -1;

		boolean hasDefusion = rc.hasUpgrade(DEFUSION);

		boolean justDefuseTheGoddamnMine = (random.nextInt(5) == 0);

		for (Direction direction: GENUINE_DIRECTIONS) {
			if (rc.canMove(direction)) {
				MapLocation location = myLocation.add(direction);				
				Team mineStatus = rc.senseMine(location);
				int distance = location.distanceSquaredTo(destination);
				if (mineStatus == enemyTeam || mineStatus == NEUTRAL) {
					if (!justDefuseTheGoddamnMine) {
						if (hasDefusion) {
							distance += 5;
						} else {
							distance += 25;
						}
					}
				}
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

	private static void updateNumUpgradesRemaining() {
		debug_startMethod();

		numUpgradesRemaining = 5;
		if (rc.hasUpgrade(NUKE)) {
			numUpgradesRemaining--;
		}
		if (rc.hasUpgrade(DEFUSION)) {
			numUpgradesRemaining--;
		}
		if (rc.hasUpgrade(FUSION)) {
			numUpgradesRemaining--;
		}
		if (rc.hasUpgrade(PICKAXE)) {
			numUpgradesRemaining--;
		}
		if (rc.hasUpgrade(VISION)) {
			numUpgradesRemaining--;
		}		

		debug_endMethod();	
	}

	private static void updateAllCaches() {

		updateEnemyLocations();
		updateEncampmentLocations();
		updateMineLocations();
		updateAllyLocations();
		updateNumUpgradesRemaining();

	}

	private static void decideMove_supplier() {
		debug_startMethod();

		updateAllCaches();

		if (numAlliedSuppliers > numAlliedGenerators + 4) {
			//rc.suicide();
		}		

		debug_endMethod();
	}	

	private static void decideMove_artillery() {
		debug_startMethod();

		updateAllCaches();

		if (rc.isActive()) {

			int numAlliesAroundClosestEnemy = rc.senseNearbyGameObjects(Robot.class, closestEnemyLocation, DIAGONALLY_ADJACENT_RADIUS, myTeam).length;
			int numEnemiesAroundClosestEnemy = rc.senseNearbyGameObjects(Robot.class, closestEnemyLocation, DIAGONALLY_ADJACENT_RADIUS, enemyTeam).length;

			if (numAlliesAroundClosestEnemy < numEnemiesAroundClosestEnemy && rc.canAttackSquare(closestEnemyLocation)) {
				try {
					rc.attackSquare(closestEnemyLocation);			
				} catch (GameActionException e) {
					debug_catch(e);
				}
			}
		}

		debug_endMethod();
	}	

	private static void decideMove_generator() {
		debug_startMethod();

		updateAllCaches();

		if (numAlliedGenerators > numAlliedSuppliers + 4) {
			// rc.suicide();
		}

		debug_endMethod();		
	}	

	private static void decideMove_shields() {
		// TODO implement shields

		debug_startMethod();
		debug_endMethod();		
	}

	private static void decideMove_medbay() {
		// TODO implement medbay

		debug_startMethod();
		debug_endMethod();		
	}	

}
