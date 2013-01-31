package sc0005;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir)) {
							rc.spawn(dir);
						}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {

						// Choose a random direction, and move that way if possible
						Direction dir = Direction.values()[(int)(Math.random()*8)];

						// but, every so often, greedy to enemy HQ
						if (Math.random() < 0.25) {
							dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						}			

						MapLocation location_of_dir = rc.getLocation().add(dir);

						if (rc.senseMine(location_of_dir) != null) {
							rc.defuseMine(location_of_dir);
						} else {
							if (rc.canMove(dir)) {
								rc.move(dir);
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
}
