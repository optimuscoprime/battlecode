package bbRushDefuse;

import java.util.HashSet;
import java.util.Set;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class SoldierPlayer extends BasePlayer {

	public SoldierPlayer(RobotController rc) {
		// code to run once
		super(rc);

	}
	public void run() throws GameActionException{
		// run once every turn.
		if (rc.isActive()) {

			boolean rotating_left_on_cant_move = (Math.random() < 0.5);

			// aim to move directly towards enemy HQ
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());

			for (int i = 0; i < 8; i++) {

				if (rc.isActive()) {
					MapLocation location_of_dir = rc.getLocation().add(dir);

					if (rc.senseMine(location_of_dir) != null) {
						rc.defuseMine(location_of_dir);
						break;
					} else {

						if (rc.canMove(dir)) {
							//rc.setIndicatorString(0, "Last direction moved:"+dir.toString());
							//System.out.println("About to move" +  rc.getLocation().toString() + dir.toString() );
							rc.move(dir);
							//System.out.println("Moved" +  rc.getLocation().toString() + dir.toString() );
							//rc.breakpoint();
							i=8;
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
}
