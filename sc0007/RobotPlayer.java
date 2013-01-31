package sc0007;

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

						boolean rotating_left_on_cant_move = (Math.random() < 0.5);

						// aim to move directly towards enemy HQ
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());

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

				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
