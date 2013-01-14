package bb_panzy;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;
import battlecode.common.Team;

public class RobotPlayer {
   public static void run(RobotController rc) {
      while (true) {
         try {
            if (rc.getType() == RobotType.HQ) {
               HQPlayer(rc);
            } else if (rc.getType() == RobotType.ARTILLERY){
               ArtilleryPlayer(rc);
            } else if (rc.getType() == RobotType.SOLDIER) {		    


               //  int task = getRobotTask(rc, rc.getRobot().getID());

               //if (task == ROBOT_TASK_UNKNOWN) {
               //	setRobotTask(rc, rc.getRobot().getID(), ROBOT_TASK_SCOUT);

               if(Clock.getRoundNum() < 50){
                  //}else if(numGuards > 6)
                  //    } else if (task == ROBOT_TASK_ERROR) {
                  ScoutSldr(rc);
               //  }
               }else{
                  avgSldr(rc);
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
   public static void ArtilleryPlayer(RobotController rc) throws Exception {
      if (rc.isActive()) {
         Team opponent = rc.getTeam().opponent();
         //RobotType[] target=rc.senseNearbyGameObjects(RobotType.SOLDIER, RobotType.ARTILLERY.attackRadiusMaxSquared, opponent);
         
         //for(int i=0; i < targets.length; i++){
            rc.attackSquare(rc.senseEnemyHQLocation()); //target[i].getLocation());
            rc.yield();
         //}
      }
      rc.yield();
   }
    
    public static void moveToward(RobotController rc, MapLocation location_x)
    throws Exception{
        boolean rotating_left_on_cant_move = (Math.random() < 0.5);

        // aim to move directly towards target
        Direction dir = rc.getLocation().directionTo(location_x);
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

        int dir_dist_to_location_x_left = location_in_dir_left.distanceSquaredTo(location_x);
        int dir_dist_to_location_x_right = location_in_dir_right.distanceSquaredTo(location_x);

        boolean mine_at_left = rc.senseMine(location_in_dir_left) != null;
        boolean mine_at_right = rc.senseMine(location_in_dir_right) != null;

        // if no mines, then pick closest
        if (!mine_at_left && !mine_at_right) {
            if (dir_dist_to_location_x_left < dir_dist_to_location_x_right) {
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
    public static void ScoutSldr(RobotController rc) throws Exception {
		    
	/// MapLocation[] senseNonAlliedMineLocations(MapLocation center, int radiusSquared)
		    
       if (rc.isActive()) {
          MapLocation enemy_hq = rc.senseEnemyHQLocation();
          moveToward(rc, enemy_hq);
       }	
    }

    public static void avgSldr(RobotController rc) throws Exception {
		    
	/// MapLocation[] senseNonAlliedMineLocations(MapLocation center, int radiusSquared)
   //Fight any enemy present.
       if (rc.isActive()) {
          //Capture any encampments?
          if(GameConstants.CAPTURE_POWER_COST < rc.getTeamPower()){
             MapLocation[] nearEncamps=rc.senseEncampmentSquares(rc.getLocation(),2, Team.NEUTRAL);
             MapLocation nearest= null;
             int nearest_dist=100000000;
             int capping=0;
             for(int i=0; i < nearEncamps.length; i++){
                if(rc.getLocation().equals(nearEncamps[i]) ){ //we're ON encampment.
                   Team opponent = rc.getTeam().opponent();

                   if (rc.getTeamPower() < (GameConstants.CAPTURE_POWER_COST * 5)){
                      rc.captureEncampment(RobotType.GENERATOR);
                      //   }else if( 0 < (rc.senseNearbyGameObjects(RobotType.ARTILLERY, RobotType.ARTILLERY.attackRadiusMaxSquared, opponent).length()) ){
                      //    rc.captureEncampment(RobotType.SHIELDS); //if there's artillery within range build a shield?
                     }else if(rc.getLocation().distanceSquaredTo(rc.senseEnemyHQLocation()) <= RobotType.ARTILLERY.attackRadiusMaxSquared){
                        rc.captureEncampment(RobotType.ARTILLERY); //if there's artillery within range build a shield?
                     }else{
                        rc.captureEncampment(RobotType.SUPPLIER);
                     }
                     capping=1;
                     rc.yield();
                     return; //somehow yield is broken.  Without returning will move twice error.
                }else{
                   if(rc.senseObjectAtLocation(nearEncamps[i])!= null ){
                      //might be me, or friendly robot?
                   }else if(rc.getLocation().distanceSquaredTo(nearEncamps[i]) < nearest_dist){
                      nearest_dist=rc.getLocation().distanceSquaredTo(nearEncamps[i]);
                      nearest=nearEncamps[i];
                   }

                }
             }

             // there was a robot there.  Maybe a friendly?
             if(nearest != null){
                if(rc.isActive()){
                   moveToward(rc,nearest);
                }
             }else{
                if(rc.getLocation().distanceSquaredTo(rc.senseEnemyHQLocation()) > 10){
                   MapLocation enemy_hq = rc.senseEnemyHQLocation();
                   moveToward(rc, enemy_hq);
                }
                rc.yield();
             }
             //  If(GameConstants.CAPTURE_POWER_COST < getTeamPower() 
             //  If getTeamPower() < (GameConstants.CAPTURE_POWER_COST * 5)
             // make a generator.
             //  If(enemyArtillery)
             // buildShield
             //If (enemyHQ within range)
             // build artillery
             // else build supply ?

             //MapLocation enemy_hq = rc.senseEnemyHQLocation();
             //moveToward(rc, enemy_hq);
          }	
       }
    }    

//
//    public static final int HASH1 = 13;
//    public static final int HASH2 = 29;
//    public static final int HASH3 = 131;
//    
//    public static final int ROBOT_TASK_ERROR = -1;
//    public static final int ROBOT_TASK_UNKNOWN = 0;
//    public static final int ROBOT_TASK_SCOUT = 1;
//    public static final int ROBOT_TASK_MINER = 2;
//
//    public static void setRobotTask(RobotController rc, int robot_id, int task) throws Exception {
//	rc.broadcast(HASH1 * robot_id, task * HASH3);
//	rc.broadcast(HASH2 * robot_id, task * HASH1);
//	rc.broadcast(HASH3 * robot_id, task * HASH2);
//    }
//
//    public static int getRobotTask(RobotController rc, int robot_id) throws Exception {
//	int val1 = rc.readBroadcast(robot_id / HASH1) / HASH3;
//	int val2 = rc.readBroadcast(robot_id / HASH2) / HASH1;
//	int val3 = rc.readBroadcast(robot_id / HASH3) / HASH2;
//	
//	if (val1 == val2 && val2 == val3) {
//	    return val1;
//	} else {
//	    return ROBOT_TASK_ERROR;
//	}
//    }
//    
}
