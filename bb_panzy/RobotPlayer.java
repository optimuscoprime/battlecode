package bb_panzy;

import battlecode.common.Clock; import battlecode.common.Direction; import
battlecode.common.GameConstants; import battlecode.common.MapLocation; import
battlecode.common.RobotController; import battlecode.common.RobotType; import
battlecode.common.Robot; import battlecode.common.Upgrade; import
battlecode.common.Team;

public class RobotPlayer {
   private final static int COMBAT_SUPERIORITY =4;
   private final static int CLOSE_RANGE =10;
   private final static int DEFAULT_SENSE_FIGHT_RADIUS =20;
   // radius is a squared number i.e. 16=dist 4 ?
   // caching - each player gets its own private variables?
   private static Team MY_TEAM;
   private static Team OPPOSING_TEAM;
   private static int MAP_WIDTH;
   private static int MAP_HEIGHT;
   private static boolean SHOULD_STOP_NUKE = false;
   private static int numEnemiesSeen = 0;
   private static MapLocation enemyHQloc;
   private static MapLocation myHQloc; 


   public static void run(RobotController rc) {
      initialise(rc);
      while (true) {
         try {
            if (rc.getType() == RobotType.HQ) {
               HQPlayer(rc);
            } else if (rc.getType() == RobotType.ARTILLERY){
               ArtilleryPlayer(rc);
            } else if (rc.getType() == RobotType.SOLDIER) {		    

               if(Clock.getRoundNum() < 50){
                  ScoutSldr(rc);
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
   private static void initialise(RobotController rc) {
      MY_TEAM = rc.getTeam();
      OPPOSING_TEAM = MY_TEAM.opponent();
      MAP_WIDTH = rc.getMapWidth();
      MAP_HEIGHT = rc.getMapHeight();
      enemyHQloc= rc.senseEnemyHQLocation();
      myHQloc = rc.senseHQLocation();

   }
   private static Robot[] getNearbyEnemies(RobotController rc, int radius) {
      return rc.senseNearbyGameObjects(Robot.class, radius, OPPOSING_TEAM);
   }
   private static Robot[] getNearbyFriendlies(RobotController rc, int radius) {
      return rc.senseNearbyGameObjects(Robot.class, radius, MY_TEAM);
   }
   private static MapLocation getNearbyMedbay(RobotController rc, int radius) throws Exception{
      MapLocation nearestMedbay= null;
      MapLocation[] nearEncamps=
      rc.senseEncampmentSquares(rc.getLocation(),radius, MY_TEAM);
      int nearest_dist=10000;
      for(int i=0; i < nearEncamps.length; i++){
         if(rc.getLocation().distanceSquaredTo(nearEncamps[i]) < nearest_dist){
            //&&  #FIXME - use radio or something to find where medbays are
         //(rc.senseRobotInfo(rc.senseObjectAtLocation(nearEncamps[i])).type == RobotType.MEDBAY)){
            nearest_dist=rc.getLocation().distanceSquaredTo(nearEncamps[i]);
            nearestMedbay=nearEncamps[i];
         }
      }
      return nearestMedbay;
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
         if (dir != Direction.OMNI && dir != Direction.NONE) {
         //the above avoids the exception
           if (rc.canMove(dir)) {
              if (rc.senseMine(location_in_dir) == null) {
                 rc.move(dir);
                 return;
              }
           }
        }else{
            rc.yield();
            return;
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
          moveToward(rc, enemyHQloc);
       }	
    }

    public static void avgSldr(RobotController rc) throws Exception {
		    
	/// MapLocation[] senseNonAlliedMineLocations(MapLocation center, int radiusSquared)
   //Fight any enemy present.
       if (rc.isActive()) {
          //Capture any encampments?
          if(GameConstants.CAPTURE_POWER_COST < rc.getTeamPower()){
             int searchDist=4;
             //System.out.println("engeron: " + rc.getEnergon());
             if(rc.getEnergon() < 30){
                 searchDist=10;
                 MapLocation nearbyMedbay=getNearbyMedbay(rc,30);
                 if(nearbyMedbay!= null){
                    moveToward(rc,nearbyMedbay);
                 }
             }
             MapLocation[]
             nearEncamps=rc.senseEncampmentSquares(rc.getLocation(),searchDist, Team.NEUTRAL);
             MapLocation nearestEncampment= null;
             int nearest_dist=100000000;
             int capping=0;
             for(int i=0; i < nearEncamps.length; i++){
                if(rc.getLocation().equals(nearEncamps[i]) ){ //we're ON encampment.
                   Team opponent = rc.getTeam().opponent();

                   if(searchDist ==10){
                      if(rc.isActive()){
                         rc.captureEncampment(RobotType.MEDBAY); 
                      }
                      rc.yield();
                   }else if (rc.getTeamPower() >  200){
                      rc.captureEncampment(RobotType.SUPPLIER);
                      //   }else if( 0 < (rc.senseNearbyGameObjects(RobotType.ARTILLERY, RobotType.ARTILLERY.attackRadiusMaxSquared, opponent).length()) ){
                      //    rc.captureEncampment(RobotType.SHIELDS); //if there's artillery within range build a shield?
                }else if(rc.getLocation().distanceSquaredTo(rc.senseEnemyHQLocation()) <= RobotType.ARTILLERY.attackRadiusMaxSquared){
                   rc.captureEncampment(RobotType.ARTILLERY); //if there's artillery within range build a shield?
                }else{
                   rc.captureEncampment(RobotType.GENERATOR);
                }
                capping=1;
                rc.yield();
                return; //somehow yield is broken.  Without returning will move twice error.
                }else{
                   if(rc.senseObjectAtLocation(nearEncamps[i])!= null ){
                      //might be me, or friendly robot?
                   }else if(rc.getLocation().distanceSquaredTo(nearEncamps[i]) < nearest_dist){
                      nearest_dist=rc.getLocation().distanceSquaredTo(nearEncamps[i]);
                      nearestEncampment=nearEncamps[i];
                   }

                }
             }
             //is enemy outnumbered?
             if(rc.isActive()){
                Robot nearbyEnemyRobots[] = getNearbyEnemies(rc,DEFAULT_SENSE_FIGHT_RADIUS);
                Robot nearbyFriendlyRobots[] = getNearbyFriendlies(rc,DEFAULT_SENSE_FIGHT_RADIUS);
                if(nearbyEnemyRobots.length > 0){
                   //if(nearbyEnemyRobots.length < nearbyFriendlyRobots.length){
                   if(nearbyEnemyRobots.length < (nearbyFriendlyRobots.length/ COMBAT_SUPERIORITY)){
                      //go for the kill
                      moveToward(rc,rc.senseLocationOf(nearbyEnemyRobots[0]));
                   }else{
                      //Run away!
                      //if(rc.getLocation().distanceSquaredTo(rc.senseLocationOf(nearbyEnemyRobots[0]))>CLOSE_RANGE ){
                        // rc.layMine();
                      //}else{
                      moveToward(rc,myHQloc);
                      //}
                   }
                }
             }
             rc.yield();
             if(rc.isActive()){
                // there was a robot there.  Maybe a friendly?
                if(nearestEncampment != null){
                   moveToward(rc,nearestEncampment);
                }else{
                   moveToward(rc, enemyHQloc);
                }
             }
             rc.yield();
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
