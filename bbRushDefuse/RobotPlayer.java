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

/** Haitao's first attempt at a good macro bot.
 */
public class RobotPlayer {
	public static void run(RobotController rc) {
		BasePlayer br;
		switch(rc.getType()) {
			case HQ:
				br = new HQPlayer(rc);
				break;
			case SOLDIER:
				br = new SoldierPlayer(rc);
				break;
			default:
				br = new EncampmentPlayer(rc);
				break;
		}

		br.loop();
	}

	public static RobotInfo nearestEnemy(RobotController rc, int distThreshold) throws GameActionException {
		// functinos written here will be available to each player file
		return null;
	}
}
