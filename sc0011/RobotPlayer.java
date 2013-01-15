package sc0011;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

public class RobotPlayer {    
    public static void run(RobotController rc) {
	while (true) {
	    try {
		if (rc.getType() == RobotType.HQ) {
		    HQPlayer(rc);
		} else if (rc.getType() == RobotType.SOLDIER) {
		    int global_task = getRobotTask(rc, 1);		    
		    int task = getRobotTask(rc, rc.getRobot().getID());
		    
		    if (Clock.getRoundNum() > 600) {
			ScoutPlayer(rc);
			
		    } else {

			if (global_task == ROBOT_TASK_UNKNOWN || global_task == ROBOT_TASK_SCOUT) {
			    if (task == ROBOT_TASK_UNKNOWN) {
				System.out.println("Made SCOUT (crisis)");
				setRobotTask(rc, rc.getRobot().getID(), ROBOT_TASK_SCOUT);
				ScoutPlayer(rc);
			    } else if (task == ROBOT_TASK_ERROR) {
				ScoutPlayer(rc);
			    }
			} else {
			    if (task == ROBOT_TASK_UNKNOWN) {
				int rand = getRobotTask(rc, 0);
				setRobotTask(rc, 0, rand+1);
				
				if (rand % 3 == 0) {
				    System.out.println("Made SCOUT");
				    setRobotTask(rc, rc.getRobot().getID(), ROBOT_TASK_SCOUT);
				    ScoutPlayer(rc);
				} else {
				    System.out.println("Made MINER");
				    setRobotTask(rc, rc.getRobot().getID(), ROBOT_TASK_MINER);
				    MinerPlayer(rc);
				}
			    } else if (task == ROBOT_TASK_MINER) {
				MinerPlayer(rc);
			    } else {
				ScoutPlayer(rc);
			    }
			}
		    }
		}

		// End turn
		rc.yield();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }
    
    public static void HQPlayer(RobotController rc) throws Exception {
	if (rc.isActive()) {
		
	    int clock_round = Clock.getRoundNum();

	    if (clock_round % 50 == 0) {
		if (rc.senseEnemyNukeHalfDone()) {
		    setRobotTask(rc, 1, ROBOT_TASK_SCOUT);
		} else {
		    setRobotTask(rc, 1, ROBOT_TASK_MINER);
		}
	    }
	
	    if (rc.hasUpgrade(Upgrade.valueOf("DEFUSION")) || clock_round < 10) {
			    
		// Spawn a soldier
		Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			    
		for (int i = 0; i < 8; i++) {			    
		    if (rc.canMove(dir)) {
			rc.spawn(dir);
			break;
		    } else {
			dir = dir.rotateLeft();
		    }
		}
	    } else {
			    
		rc.researchUpgrade(Upgrade.valueOf("DEFUSION"));
			    
	    }
	}
	//if (Clock.getRoundNum() % 100 == 0) {
	//MapLocation [] rc.senseNonAlliedMineLocations(new MapLocation(35, 35), 35);
	//}

    }
    
    public static void ScoutPlayer(RobotController rc) throws Exception {
		    
	/// MapLocation[] senseNonAlliedMineLocations(MapLocation center, int radiusSquared)
		    
	if (rc.isActive()) {
	    MapLocation enemy_hq = rc.senseEnemyHQLocation();

	    boolean rotating_left_on_cant_move = (Math.random() < 0.5);
				    	    	    
	    // aim to move directly towards enemy HQ
	    Direction dir = rc.getLocation().directionTo(enemy_hq);
	    MapLocation location_in_dir = rc.getLocation().add(dir);

	    if (rc.canMove(dir)) {
		if (rc.senseMine(location_in_dir) == null) {
		    rc.move(dir);
		    return;
		}
	    }
	    	    	    	    
	    // but also give yourself a few options if you can't move where you want
	    Direction dir_left = dir.rotateLeft();
	    Direction dir_right = dir.rotateRight();
	    
	    MapLocation location_in_dir_left = rc.getLocation().add(dir_left);
	    MapLocation location_in_dir_right = rc.getLocation().add(dir_right);
	    
	    int dir_dist_to_enemy_hq_left = location_in_dir_left.distanceSquaredTo(enemy_hq);
	    int dir_dist_to_enemy_hq_right = location_in_dir_right.distanceSquaredTo(enemy_hq);
	    
	    boolean mine_at_left = rc.senseMine(location_in_dir_left) != null;
	    boolean mine_at_right = rc.senseMine(location_in_dir_right) != null;
	    
	    // if no mines, then pick closest
	    if (!mine_at_left && !mine_at_right) {
		if (dir_dist_to_enemy_hq_left < dir_dist_to_enemy_hq_right) {
		    if (rc.canMove(dir_left)) {
			rc.move(dir_left);
			return;
		    }
		} else {
		    if (rc.canMove(dir_right)) {
			rc.move(dir_right);
			return;
		    }
		}
	    } 

	    if (mine_at_left && !mine_at_right) {
		if (rc.canMove(dir_right)) {
		    rc.move(dir_right);
		    return;
		}
	    }

	    if (mine_at_right && !mine_at_left) {
		if (rc.canMove(dir_left)) {
		    rc.move(dir_left);
		    return;
		}
	    }	    

	    for (int i = 0; i < 8; i++) {

		MapLocation location_of_dir = rc.getLocation().add(dir);
		
		if (rc.senseMine(location_of_dir) != null) {
		    rc.defuseMine(location_of_dir);
		    break;
		} else {
				
		    if (rc.canMove(dir)) {
			rc.move(dir);
			break;
		    } else {
			if (rotating_left_on_cant_move) {
			    dir = dir.rotateLeft();
			} else {
			    dir = dir.rotateRight();
			}
		    }
		}
	    }			
	}	
    }
    
    public static void MinerPlayer(RobotController rc) throws Exception {
	if (rc.isActive()) {
	    MapLocation hq = rc.senseHQLocation();	    
	    
	    int dist_to_hq = rc.getLocation().distanceSquaredTo(hq);
	    
	    int clock_round = Clock.getRoundNum();
	    
	    int desired_radius = (clock_round / 100) + 1;

	    if (inRange(dist_to_hq-1, desired_radius * desired_radius, dist_to_hq+1)) {
		if (rc.senseMine(rc.getLocation()) == null) {
		    //rc.layMine();
		    return;
		}
	    }
	    
	    Direction dir = rc.getLocation().directionTo(hq);
	    dir = dir.rotateLeft().rotateLeft();
	    
	    for (int i = 0; i < 8; i++) {
	    
		MapLocation dir_loc = rc.getLocation().add(dir);	       
		
		if (rc.senseMine(dir_loc) == null) {
		    if (rc.canMove(dir)) {
			rc.move(dir);
			return;
		    }
		}
		
		dir = dir.rotateRight();
	    }
	}
    }
    
    public static boolean inRange(int a, int n, int b) {
	return a<=n && n <= b;
    }
    
    public static final int HASH1 = 13;
    public static final int HASH2 = 29;
    public static final int HASH3 = 131;
    
    public static final int ROBOT_TASK_ERROR = -1;
    public static final int ROBOT_TASK_UNKNOWN = 0;
    public static final int ROBOT_TASK_SCOUT = 1;
    public static final int ROBOT_TASK_MINER = 2;
    public static final int ROBOT_TASK_GUARD = 3;

    public static String taskToString(int val) {
	if (val == ROBOT_TASK_ERROR) return "ROBOT_TASK_ERROR";
	if (val == ROBOT_TASK_UNKNOWN) return "ROBOT_TASK_UNKNOWN";
	if (val == ROBOT_TASK_SCOUT) return "ROBOT_TASK_SCOUT";
	if (val == ROBOT_TASK_MINER) return "ROBOT_TASK_MINER";
	if (val == ROBOT_TASK_GUARD) return "ROBOT_TASK_GUARD";
	return "Unknown";
    }

    public static void setRobotTask(RobotController rc, int robot_id, int task) throws Exception {	
	rc.broadcast(HASH1 * robot_id, task * HASH3);
	rc.broadcast(HASH2 * robot_id, task * HASH1);
	rc.broadcast(HASH3 * robot_id, task * HASH2);	
    }

    public static int getRobotTask(RobotController rc, int robot_id) throws Exception {		
	int val1 = rc.readBroadcast(robot_id * HASH1) / HASH3;
	int val2 = rc.readBroadcast(robot_id * HASH2) / HASH1;
	int val3 = rc.readBroadcast(robot_id * HASH3) / HASH2;

	if (val1 == val2) return val1;
	if (val1 == val3) return val1;
	if (val2 == val3) return val2;
	
	return ROBOT_TASK_ERROR;
    }
    
}
