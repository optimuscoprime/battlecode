package scBlindRush;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class RobotPlayer {    
    public static void run(RobotController rc) {
	while (true) {
	    try {
		int clock_round = Clock.getRoundNum();

		if (rc.getType() == RobotType.HQ) {
		    HQPlayer(rc);
		} else if (rc.getType() == RobotType.SOLDIER) {
		    ScoutPlayer(rc);
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
	}
    }
    
    private static int closest_settlement_x = -1;
    private static int closest_settlement_y = -1;
    
    public static void SupplierPlayer(RobotController rc) throws Exception {
	
	if (closest_settlement_y < 0) {
	    int map_width = rc.getMapWidth();
	    int map_height = rc.getMapHeight();
	    MapLocation center = new MapLocation(map_width/2, map_height/2);
	    int approx_map_size = (map_width + map_height) / 2;
	    
	    MapLocation [] settlements = rc.senseEncampmentSquares(center, approx_map_size/2, Team.NEUTRAL);
	    
	    for (MapLocation s : settlements) {
		
		// find closest and save into static vars
		
	    }
	    
	    // move towards closest and then cap if there
	}
	
	
	
	
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
	    
	    Team mine_obj_at_left = rc.senseMine(location_in_dir_left);
	    Team mine_obj_at_right = rc.senseMine(location_in_dir_right);
	    
	    boolean mine_at_left = (mine_obj_at_left != null) && (! mine_obj_at_left.equals(rc.getTeam()));
	    boolean mine_at_right = (mine_obj_at_right != null) && (! mine_obj_at_right.equals(rc.getTeam()));
	    
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
		Team mine_obj_in_dir = rc.senseMine(location_of_dir);
		
		if (mine_obj_in_dir != null && (! mine_obj_in_dir.equals(rc.getTeam()))) {
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
    	trySendBroadcast(rc, HASH1 * robot_id, task * HASH3);
    	trySendBroadcast(rc, HASH2 * robot_id, task * HASH1);
    	trySendBroadcast(rc, HASH3 * robot_id, task * HASH2);	
    }

    public static int getRobotTask(RobotController rc, int robot_id) throws Exception {		
	int val1 = tryReadBroadcast(rc, robot_id * HASH1) / HASH3;
	int val2 = tryReadBroadcast(rc, robot_id * HASH2) / HASH1;
	int val3 = tryReadBroadcast(rc, robot_id * HASH3) / HASH2;

	if (val1 == val2) return val1;
	if (val1 == val3) return val1;
	if (val2 == val3) return val2;
	
	return ROBOT_TASK_ERROR;
    }
    
    private static void trySendBroadcast(RobotController rc, int channel, int message) {
    	if (channel >= 0 && channel < GameConstants.BROADCAST_MAX_CHANNELS) {
    		try {
				rc.broadcast(channel, message);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
    	}
    }
    
    private static int tryReadBroadcast(RobotController rc, int channel) {
    	int broadcast = 0;
    	if (channel >= 0 && channel < GameConstants.BROADCAST_MAX_CHANNELS) {
    		try {
				broadcast = rc.readBroadcast(channel);
			} catch (GameActionException e) {
				e.printStackTrace();
			}
    	}
    	return broadcast;
    }
    
}
