package bbRushDefuse;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class HQPlayer extends BasePlayer {

	//declare local variables
	
	public HQPlayer(RobotController rc) {
		super(rc);
                //code to execute one time
                
        }
	public void run() throws GameActionException {
		//code to execute for the whole match
            if (rc.isActive()) {  // Believe me, things get ugly without.
                Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());

                if(Clock.getRoundNum() < (50)){
                    if (rc.canMove(dir)) {
                        rc.spawn(dir);
                    }
                }else{
                    if(rc.hasUpgrade(Upgrade.valueOf("DEFUSION")) ){
                        rc.researchUpgrade(Upgrade.valueOf("NUKE"));
                    }else{
                        rc.researchUpgrade(Upgrade.valueOf("DEFUSION"));
                    }
                }
            }
            rc.yield();
        }

}
