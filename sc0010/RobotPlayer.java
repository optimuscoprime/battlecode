package sc0010;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					HQPlayer(rc);
				} else if (rc.getType() == RobotType.SOLDIER) {
					SoldierPlayer(rc);
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

			if (rc.hasUpgrade(Upgrade.valueOf("DEFUSION")) || Clock.getRoundNum() < 10) {

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

	public static void SoldierPlayer(RobotController rc) throws Exception {

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
}
