package oc003;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class RobotPlayer {    	
	private final static double MIN_TEAM_POWER = 60.0;

	private final static int ROUND_NUM_SELF_DESTRUCT = 2000;

	private final static int BIG_NUM_FRIENDLIES_ALIVE_ATTACK_THRESHOLD = 40;

	private final static int SMALL_RADIUS = 3;
	private final static int HUGE_RADIUS = 10000;
	private final static int DEFAULT_SENSE_ENEMY_RADIUS = 150;
	private final static int DEFAULT_SENSE_FRIENDLY_RADIUS = 1;	

	private static int NUM_ENEMIES_CHANNEL_1;
	private static int NUM_ENEMIES_CHANNEL_2;
	private static int STOP_NUKE_CHANNEL_1;
	private static int STOP_NUKE_CHANNEL_2;

	private final static int STOP_NUKE_MAGIC_NUMBER = 23892;

	private final static int NUM_ENEMIES_SEEN_RECENTLY_ATTACK_HQ_THRESHOLD = 20;
	private final static int MIN_NUM_FRIENDLIES_ALIVE_ATTACK_THRESHOLD = 20;
	private final static int MAGIC_CONSTANT = 900;

	private final static double MIN_POWER_TO_READ_BROADCAST = GameConstants.BROADCAST_READ_COST * 2;
	private final static double MIN_POWER_TO_SEND_BROADCAST = GameConstants.BROADCAST_SEND_COST * 2;

	// caching - each player gets its own private variables?
	private static Team MY_TEAM;
	private static Team OPPOSING_TEAM;
	private static int MAP_WIDTH;
	private static int MAP_HEIGHT;
	private static boolean SHOULD_STOP_NUKE = false;
	private static int numEnemiesSeen = 0;

	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {
			try {
				playOneTurn(rc);
				rc.yield();		
			} catch (Exception e) {
				debug_printf(e);
			}
		}
	}

	private static void initialise(RobotController rc) {
		MY_TEAM = rc.getTeam();
		OPPOSING_TEAM = MY_TEAM.opponent();
		MAP_WIDTH = rc.getMapWidth();
		MAP_HEIGHT = rc.getMapHeight();

		pickChannels(rc);
	}

	private static void pickChannels(RobotController rc) {		

		int crazyOffset = (7 * (MAP_WIDTH + (29 * (MAP_HEIGHT))));

		// maybe use current round too ?

		NUM_ENEMIES_CHANNEL_1 = 938 + crazyOffset;
		NUM_ENEMIES_CHANNEL_1 %= GameConstants.BROADCAST_MAX_CHANNELS;

		NUM_ENEMIES_CHANNEL_2 = 4187 + crazyOffset;
		NUM_ENEMIES_CHANNEL_2 %= GameConstants.BROADCAST_MAX_CHANNELS;

		STOP_NUKE_CHANNEL_1 = 543 + crazyOffset;
		STOP_NUKE_CHANNEL_1 %= GameConstants.BROADCAST_MAX_CHANNELS;

		STOP_NUKE_CHANNEL_2 = 1897 + crazyOffset;
		STOP_NUKE_CHANNEL_2 %= GameConstants.BROADCAST_MAX_CHANNELS;	
	}

	// System.setProperty("debugMethodsEnabled", "true");
	// use bc.conf to turn debug mode on	
	private static void debug_printf(String format, Object ... objects) {
		System.out.printf(format, objects);
		System.out.printf("\n");
	}	

	private static void debug_printf(Exception e) {
		System.out.printf("%s: %s", e.getStackTrace()[0].getMethodName(), e.getMessage());
		System.out.printf("\n");
	}	

	private static void playOneTurn(RobotController rc) {
		if (rc.isActive()) {
			switch(rc.getType()) {
				case HQ:
					playOneTurn_hq(rc);
					break;		
				case SOLDIER:
					playOneTurn_soldier(rc);
					break;			
				case SUPPLIER:
					playOneTurn_supplier(rc);
					break;
				case ARTILLERY:
					playOneTurn_artillery(rc);
					break;
				case GENERATOR:
					playOneTurn_generator(rc);
					break;
				case MEDBAY:
					playOneTurn_medbay(rc);
					break;
				case SHIELDS:
					playOneTurn_shields(rc);
					break;
			}
		}
	}

	private static void playOneTurn_hq(RobotController rc) {
		double power = rc.getTeamPower();

		// check if the enemy is building a nuke... sometimes
		try {
			if (!SHOULD_STOP_NUKE && rc.senseEnemyNukeHalfDone()) {
				tryBroadcastAttackSignal(rc);
			}
		} catch (GameActionException e) {
			debug_printf(e);
		}

		// try to spawn a soldier

		if (power >= MIN_TEAM_POWER) {
			Direction spawnDirection = findBestSpawnDirection(rc);
			if (spawnDirection != Direction.NONE && couldSpawnSoldier(rc, spawnDirection)) {
				return;
			}
		}

		// try to do research

		Upgrade bestUpgrade = findBestUpgrade(rc);
		try {
			rc.researchUpgrade(bestUpgrade);
		} catch (GameActionException e) {
			debug_printf(e);
		}
	}	

	private static void tryBroadcastAttackSignal(RobotController rc) {
		if (rc.getTeamPower() > (2 * MIN_POWER_TO_SEND_BROADCAST)) {
			trySendMessage(rc, STOP_NUKE_CHANNEL_1, STOP_NUKE_MAGIC_NUMBER);
			trySendMessage(rc, STOP_NUKE_CHANNEL_2, STOP_NUKE_MAGIC_NUMBER);
			SHOULD_STOP_NUKE = true;
			debug_printf("TOLD OTHERS TO STOP NUKE");
		}		
	}

	private static Upgrade findBestUpgrade(RobotController rc) {
		Upgrade bestUpgrade = Upgrade.NUKE;

		Upgrade upgradePriorities[] = new Upgrade[]{
				Upgrade.DEFUSION,
				Upgrade.VISION,
				Upgrade.PICKAXE,
				Upgrade.FUSION,
				Upgrade.NUKE
		};

		for (Upgrade upgrade: upgradePriorities) {
			if (!rc.hasUpgrade(upgrade)) {
				bestUpgrade = upgrade;
				break;
			}
		}
		return bestUpgrade;
	}

	private static Direction findBestSpawnDirection(RobotController rc) {
		Direction bestDirection = Direction.NONE;
		MapLocation currentLocation = rc.getLocation();

		for (Direction direction : Direction.values()) {
			MapLocation newLocation = currentLocation.add(direction);
			GameObject gameObject = null;
			try {
				gameObject = rc.senseObjectAtLocation(newLocation);
			} catch (GameActionException e) {
				debug_printf(e);
			}
			if (gameObject == null) {
				bestDirection = direction;
				break;
			}
		}
		return bestDirection;
	}

	private static boolean couldSpawnSoldier(RobotController rc, Direction direction) {
		boolean spawned = false;
		try {
			rc.spawn(direction);
			spawned = true;
		} catch (GameActionException e) {
			debug_printf(e);
			spawned = false;
		} 	
		return spawned;
	}	

	private static void playOneTurn_soldier(RobotController rc) {
		MapLocation myLocation = rc.getLocation();		

		Robot nearbyEnemyRobots[] = getNearbyEnemies(rc);

		Team myTeam = MY_TEAM;

		// this wastes power, so don't do it all the time
		if (Clock.getRoundNum() % 5 == 0) {
			if (nearbyEnemyRobots.length > 0) {
				increaseNumEnemiesSeenRecently(rc, nearbyEnemyRobots.length * MAGIC_CONSTANT);
			}
			// decay
			decreaseNumEnemiesSeenRecently(rc, 1);

			if (!SHOULD_STOP_NUKE) {
				checkIfShouldStopNuke(rc);
			}
		}

		Robot nearbyFriendlyRobots[] = getNearbyFriendlies(rc);
		Robot allFriendlyRobots[] = getAllFriendlies(rc);

		int numFriendlyRobots = allFriendlyRobots.length;
		int numEnemiesSeenRecently = getNumEnemiesSeenRecently(rc);

		boolean shouldCounterAttack = numEnemiesSeenRecently >= NUM_ENEMIES_SEEN_RECENTLY_ATTACK_HQ_THRESHOLD &&
				numFriendlyRobots > MIN_NUM_FRIENDLIES_ALIVE_ATTACK_THRESHOLD;

				// debug_printf("round %d: %s, %d enemies, %d friendlies\n", Clock.getRoundNum(), shouldCounterAttack,  numEnemiesSeenRecently, numFriendlyRobots);

				boolean shouldKamikaze = allFriendlyRobots.length > BIG_NUM_FRIENDLIES_ALIVE_ATTACK_THRESHOLD ||
						Clock.getRoundNum() >= ROUND_NUM_SELF_DESTRUCT;

						boolean shouldAttackHQ = SHOULD_STOP_NUKE || shouldCounterAttack || shouldKamikaze;

						boolean shouldExplore = Math.random() < 0.05;

						boolean shouldMove = nearbyEnemyRobots.length > 1 || 
								nearbyFriendlyRobots.length > 1 ||
								shouldAttackHQ ||
								shouldExplore;	

								List<Direction> prioritisedDirections = getPrioritisedDirections(rc, myLocation, myTeam, shouldAttackHQ);

								MapLocation preferredNewLocation = myLocation.add(prioritisedDirections.get(0));
								Team newLocationMineStatus = rc.senseMine(preferredNewLocation);
								boolean enemyMineAtNewLocation = (newLocationMineStatus != null && newLocationMineStatus != MY_TEAM);
								boolean bestDirectionIsThroughMinefield = enemyMineAtNewLocation && (Math.random() < 0.60);

								// if we would _probably_ prefer to walk through a minefield
								// or we are not supposed to be attacking the HQ
								// then try to defuse a nearby mine
								if (bestDirectionIsThroughMinefield || !shouldAttackHQ) {
									MapLocation mineLocations[] = rc.senseNonAlliedMineLocations(myLocation, SMALL_RADIUS);
									for (MapLocation mineLocation : mineLocations) {
										if (couldDefuseMine(rc, mineLocation)) {
											return;
										}				
									}
								}

								// try to move						 

								if (shouldMove) {
									Direction direction = findBestDirectionToMove(rc, myLocation, myTeam, nearbyEnemyRobots, shouldAttackHQ, prioritisedDirections);					 
									if (direction != null) {
										if (couldMove(rc, direction)) {
											return;
										}
									}
								}


								// try to build encampments

								if (rc.senseEncampmentSquare(myLocation)) {
									RobotType encampmentTypes[] = new RobotType[]{
											// prioritised best to worst
											RobotType.SUPPLIER,
											RobotType.GENERATOR,										
											RobotType.MEDBAY, 
											//RobotType.MEDBAY, 
											RobotType.SHIELDS,
											RobotType.ARTILLERY,
									};
									for (RobotType encampmentType: encampmentTypes) {
										if (shouldBuildEncampment(rc, encampmentType, myLocation, myTeam) && couldBuildEncampment(rc, encampmentType)) {
											return;
										}				
									}
								}	

								// mining

								Team mineStatus = rc.senseMine(myLocation);

								if (mineStatus == null) {			
									if (couldLayMine(rc)) {
										return;
									}
								} 	

	}	

	private static void checkIfShouldStopNuke(RobotController rc) {
		if (rc.getTeamPower() > (2 * MIN_POWER_TO_READ_BROADCAST)) {
			int message1 = tryGetMessage(rc, STOP_NUKE_CHANNEL_1);
			int message2 = tryGetMessage(rc, STOP_NUKE_CHANNEL_2);

			if (message1 == message2 && message2 == STOP_NUKE_MAGIC_NUMBER) {
				SHOULD_STOP_NUKE = true;
			}
		}		

		if (Math.random() > (Clock.getRoundNum() / 150.0)) {
			// build scouts at start of game
			SHOULD_STOP_NUKE = true;
		}
	}

	private static boolean couldDefuseMine(RobotController rc, MapLocation location) {
		boolean defused = false;
		try {
			rc.defuseMine(location);
			defused = true;
		} catch (GameActionException e) {
			debug_printf(e);
			defused = false;
		}
		return defused;
	}

	private static boolean shouldBuildEncampment(RobotController rc, RobotType encampmentType, MapLocation location, Team team) {		
		boolean shouldBuild = false;

		MapLocation encampmentSquares[] = {};
		try {
			encampmentSquares = rc.senseEncampmentSquares(location, SMALL_RADIUS, team);
		} catch (GameActionException e) {
			debug_printf(e);
		}

		if (encampmentSquares.length == 0) {
			shouldBuild = true;
		}

		return shouldBuild;
	}

	private static boolean couldBuildEncampment(RobotController rc, RobotType encampmentType) {
		boolean builtEncampment = false;
		try {
			rc.captureEncampment(encampmentType);
			builtEncampment = true;
		} catch (GameActionException e) {
			debug_printf(e);
			builtEncampment = false;
		}
		return builtEncampment;
	}

	private static boolean couldLayMine(RobotController rc) {
		boolean mined = false;
		try {
			rc.layMine();
			mined = true;
		} catch (GameActionException e) {
			debug_printf(e);
			mined = false;
		}
		return mined;
	}

	private static boolean couldMove(RobotController rc, Direction direction) {
		boolean moved = false;
		try {
			rc.move(direction);
			moved = true;
		} catch (GameActionException e) {
			debug_printf(e);
			moved = false;
		}
		return moved;
	}

	private static Direction findBestDirectionToMove(RobotController rc, MapLocation myLocation, Team myTeam, Robot nearbyEnemyRobots[], boolean shouldAttackHQ, List<Direction> prioritisedDirections) {
		Direction bestDirection = null;

		for (Direction direction: prioritisedDirections) {
			// causes exception
			if (direction != Direction.OMNI && direction != Direction.NONE) {
				MapLocation newLocation = myLocation.add(direction);
				Team mineTeam = rc.senseMine(newLocation);

				boolean safeDirection = (mineTeam == null || mineTeam == myTeam);

				if (safeDirection && rc.canMove(direction)) {
					bestDirection = direction;
					break;
				}
			}
		}	

		return bestDirection;
	}

	private static int getNumEnemiesSeenRecently(RobotController rc) {	
		int numEnemies = 0;

		if (rc.getTeamPower() > (2 * MIN_POWER_TO_READ_BROADCAST)) {
			int message1 = tryGetMessage(rc, NUM_ENEMIES_CHANNEL_1);
			int message2 = tryGetMessage(rc, NUM_ENEMIES_CHANNEL_2);

			if (message1 == message2) {
				numEnemies = message1;
			}
		}

		return numEnemies;
	}

	private static void increaseNumEnemiesSeenRecently(RobotController rc, int amount) {
		if (rc.getTeamPower() > (2 * MIN_POWER_TO_SEND_BROADCAST) + (2 * MIN_POWER_TO_READ_BROADCAST)) {

			int numEnemies = getNumEnemiesSeenRecently(rc);
			numEnemies += amount;

			trySendMessage(rc, NUM_ENEMIES_CHANNEL_1, numEnemies);
			trySendMessage(rc, NUM_ENEMIES_CHANNEL_2, numEnemies);
		}
	}

	private static void decreaseNumEnemiesSeenRecently(RobotController rc, int amount) {
		if (rc.getTeamPower() > (2 * MIN_POWER_TO_SEND_BROADCAST) + (2 * MIN_POWER_TO_READ_BROADCAST)) {
			int numEnemies = getNumEnemiesSeenRecently(rc);
			numEnemies -= amount;

			if (numEnemies < 0) {
				numEnemies = 0;
			}

			trySendMessage(rc, NUM_ENEMIES_CHANNEL_1, numEnemies);
			trySendMessage(rc, NUM_ENEMIES_CHANNEL_2, numEnemies);
		}
	}		

	private static void trySendMessage(RobotController rc, int channel, int message) {
		try {
			rc.broadcast(channel,  message);
		} catch (GameActionException e) {
			debug_printf(e);
		}
	}

	private static int tryGetMessage(RobotController rc, int channel) {
		int message = 0;
		try {
			message = rc.readBroadcast(channel);
		} catch (GameActionException e) {
			debug_printf(e);
		}
		return message;
	}

	private static List<Direction> getPrioritisedDirections(final RobotController rc, final MapLocation myLocation, final Team myTeam, boolean shouldAttackHQ) {

		List<Direction> directions = Arrays.asList(Direction.values());
		final Robot nearbyEnemies[] = getNearbyEnemies(rc);
		final MapLocation enemyHQLocation = rc.senseEnemyHQLocation();

		MapLocation mineLocations[] = rc.senseNonAlliedMineLocations(myLocation, HUGE_RADIUS);

		if (nearbyEnemies.length > 0) {	

			// if there are nearby enemy, go towards them

			final Map<Direction,Double> cachedDistances = new HashMap<Direction,Double>();
			for (Direction direction: directions) {
				MapLocation newLocation = myLocation.add(direction);
				double shortestDistance = -1;

				for (Robot robot: nearbyEnemies) {
					double distance = -1;
					try {
						distance = newLocation.distanceSquaredTo(rc.senseLocationOf(robot));
					} catch (GameActionException e) {
						debug_printf(e);
					}
					if (shortestDistance == -1 || distance < shortestDistance) {
						shortestDistance = distance;
					}
				}
				shortestDistance += numMinesAlongDirection(rc, newLocation, direction, mineLocations);
				cachedDistances.put(direction, shortestDistance);
			}			

			// sort based on distance to enemy
			Collections.sort(directions, new Comparator<Direction>() {
				@Override
				public int compare(Direction o1, Direction o2) {
					Double distance1 = distanceToNearbyEnemy(o1);
					Double distance2 = distanceToNearbyEnemy(o2);
					return distance1.compareTo(distance2);
				}

				private double distanceToNearbyEnemy(Direction direction) {
					return cachedDistances.get(direction);
				}
			});

		} else if (shouldAttackHQ) {

			// head to enemy HQ

			final Map<Direction,Double> cachedDistances = new HashMap<Direction,Double>();
			for (Direction direction: directions) {
				MapLocation newLocation = myLocation.add(direction);
				double distance = newLocation.distanceSquaredTo(enemyHQLocation);
				distance += numMinesAlongDirection(rc, newLocation, direction, mineLocations);
				cachedDistances.put(direction, distance);
			}

			// sort based on distance to enemy HQ
			Collections.sort(directions, new Comparator<Direction>() {
				@Override
				public int compare(Direction o1, Direction o2) {
					Double distance1 = distanceToEnemyHQ(o1);
					Double distance2 = distanceToEnemyHQ(o2);
					return distance1.compareTo(distance2);
				}

				private double distanceToEnemyHQ(Direction direction) {
					return cachedDistances.get(direction);
				}
			});

		} else {			
			// otherwise move randomly
			Collections.shuffle(directions);
		}

		return directions;
	}

	private static double numMinesAlongDirection(RobotController rc, MapLocation location, Direction direction, MapLocation mineLocations[]) {
		// heuristic to estimate how difficult a direction is to traverse

		int minesInThisDirection = 0;

		// simplify direction
		switch(direction) {
			case NORTH_EAST:
				direction = Direction.NORTH;
				break;
			case NORTH_WEST:
				direction = Direction.NORTH;
				break;
			case SOUTH_EAST:
				direction = Direction.SOUTH;
				break;
			case SOUTH_WEST:
				direction = Direction.SOUTH;
				break;
		}

		// checking all the mines is very expensive
		for (int minesChecked = 0; minesChecked < 10; minesChecked++) {
			MapLocation mineLocation = mineLocations[(int)(Math.random() * mineLocations.length)];
			switch(direction) {
				case NORTH:
					if (mineLocation.x == location.x && mineLocation.y < location.y) {
						minesInThisDirection++;
					}
					break;
				case SOUTH:
					if (mineLocation.x == location.x && mineLocation.y > location.y) {
						minesInThisDirection++;
					}					
					break;
				case EAST:
					if (mineLocation.x > location.x && mineLocation.y == location.y) {
						minesInThisDirection++;
					}					
					break;
				case WEST:
					if (mineLocation.x < location.x && mineLocation.y == location.y) {
						minesInThisDirection++;
					}					
					break;
				default:
					break;
			}			
		}

		int MAGIC_MINE_MULTIPLIER = 15;
		return minesInThisDirection * MAGIC_MINE_MULTIPLIER;
	}

	private static Robot[] getAllEnemies(RobotController rc) {
		return getNearbyEnemies(rc, HUGE_RADIUS);
	}	

	private static Robot[] getNearbyEnemies(RobotController rc) {
		return getNearbyEnemies(rc, DEFAULT_SENSE_ENEMY_RADIUS);
	}

	private static Robot[] getNearbyEnemies(RobotController rc, int radius) {
		return rc.senseNearbyGameObjects(Robot.class, radius, OPPOSING_TEAM);
	}

	private static Robot[] getAllFriendlies(RobotController rc) {
		return getNearbyFriendlies(rc, HUGE_RADIUS);
	}

	private static Robot[] getNearbyFriendlies(RobotController rc) {
		return getNearbyFriendlies(rc, DEFAULT_SENSE_FRIENDLY_RADIUS);
	}	

	private static Robot[] getNearbyFriendlies(RobotController rc, int radius) {
		return rc.senseNearbyGameObjects(Robot.class, radius);
	}	

	private static void playOneTurn_artillery(RobotController rc) {
		throw new RuntimeException("TODO: implement artillery logic");		
	}	

	private static void playOneTurn_shields(RobotController rc) {
		// no action possible (besides suicide?)	
	}

	private static void playOneTurn_medbay(RobotController rc) {
		// no action possible (besides suicide?)
	}

	private static void playOneTurn_generator(RobotController rc) {
		// no action possible (besides suicide?)
	}

	private static void playOneTurn_supplier(RobotController rc) {
		// no action possible (besides suicide?)
	}	
}

