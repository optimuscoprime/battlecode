package bbSwarm;
/*
Based largely on the example swarm2 player fromm lecture slides.
- Also try to use medbays though if injured.
- And use artillery if lots of enemies around but not too close.
- artillery strategy is a bit simple.
- first unit or two are scouts incase we're facing a nukebot.

*/

import battlecode.common.*;

public class RobotPlayer{
	
	static RobotController rc;
	static int mult = 145;   //hopefully unique for clean comms.
	static int status = 1;//1 is don't lay mines, 2 is lay mines
	static int injured_health = 20;
   static int SUPERIORITY = 15;
   static int CAPTURE_PRIVACY_RADIUS = 5;
	public static void run(RobotController myRC){
		rc = myRC;
		if (rc.getTeam()==Team.A)
			mult = 314;   //so we can play ourselves needs to differ fromm above.
		
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
               if(rc.getRobot().getID()<=103){ //make the first few scout
                  scoutCode();//this may finish if enemies are seen.
                  soldierCode();
               }else{
                  soldierCode();
               }
				}else if (rc.getType()==RobotType.HQ){
					hqCode();
				}else if (rc.getType()==RobotType.MEDBAY){
					medbayCode();
				}else if (rc.getType()==RobotType.ARTILLERY){
					artilleryCode();
				}

			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	private static void soldierCode(){
		MapLocation rallyPt = rc.getLocation();
      boolean injured=false;
		while(true){
			try{

            Robot[] allies = rc.senseNearbyGameObjects(Robot.class,14,rc.getTeam());
            Robot[] enemies = rc.senseNearbyGameObjects(Robot.class,10000000,rc.getTeam().opponent());
            Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,14,rc.getTeam().opponent());

            if(rc.getEnergon() < injured_health)
               injured=true;
            if(rc.getEnergon() ==  40) //we're healed.  maxEnergon didn'tt work?
               injured=false;
               // lets deviate if low health.
            if(injured){
               if (rc.isActive()){
                  int medChannel=getChannel()+2;
                  MapLocation medbayLoc= IntToMaplocation(rc.readBroadcast(medChannel));
                  if(medbayLoc!= null){
                     MapLocation myLoc=rc.getLocation();
                     Direction finalDir = myLoc.directionTo(medbayLoc);
                     if (Math.random()<.1)
                        finalDir = finalDir.rotateRight();
                     simpleMove(finalDir, myLoc,true);
                  
                  }else{
                     //head for nearest encampment. (it'll cap as medbay)
                     MapLocation futureMedbay=getNearbyMedbay(rc,50);
                     if(futureMedbay!= null){
                        freeGo(futureMedbay, allies, enemies, nearbyEnemies);
                     }else{
                        freeGo(rc.senseEnemyHQLocation(),allies,enemies,nearbyEnemies);
                     }
                  }
               }
            }else{
               rc.setIndicatorString(0,"goal: "+rallyPt.toString());
               //receive rally point from HQ
               MapLocation received = IntToMaplocation(rc.readBroadcast(getChannel()));
               if (received!= null)
                  rallyPt = received;
               //receive mining command
               int ir = rc.readBroadcast(getChannel()+1);
               if (ir!=0&&ir<=2)
                  status = ir;


               if (rc.isActive()){
                  if (status == 1){//don't lay mines
                     //move toward received goal, using swarm behavior
                     freeGo(rallyPt,allies,enemies,nearbyEnemies);
                  }else if (status == 2){//lay mines!
                     if (goodPlace(rc.getLocation())&&rc.senseMine(rc.getLocation())==null){
                        rc.layMine();
                     }else{
                        freeGo(rallyPt,allies,enemies,nearbyEnemies);
                     }
                  }
               }
            }
         }catch (Exception e){
            System.out.println("Soldier Exception");
            e.printStackTrace();
			}
			rc.yield();
		}
	}


	private static boolean goodPlace(MapLocation location) {
//		return ((3*location.x+location.y)%8==0);//pickaxe with gaps
//		return ((2*location.x+location.y)%5==0);//pickaxe without gaps
		return ((location.x+location.y)%2==0);//checkerboard
	}
	//Movement system
	private static void freeGo(MapLocation target, Robot[] allies,Robot[] enemies,Robot[] nearbyEnemies) throws GameActionException {
		//This robot will be attracted to the goal and repulsed from other things
		MapLocation myLoc = rc.getLocation();
		Direction toTarget = myLoc.directionTo(target);
		int targetWeighting = targetWeight(myLoc.distanceSquaredTo(target));
		MapLocation goalLoc = myLoc.add(toTarget,targetWeighting);//toward target, TODO weighted by the distance?
		
		if (enemies.length==0){
			//find closest allied robot. repel away from that robot.
			if(allies.length>0){
				MapLocation closestAlly = findClosest(allies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestAlly),-3);
			}
		}else if (allies.length<nearbyEnemies.length+3){
			if(allies.length>0){//find closest allied robot. attract to that robot.
				MapLocation closestAlly = findClosest(allies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestAlly),5);
			}
			if(nearbyEnemies.length>0){//avoid enemy
				MapLocation closestEnemy = findClosest(nearbyEnemies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestEnemy),-10);
			}
		}else if (allies.length>=nearbyEnemies.length*3){
			if(allies.length>0){
				MapLocation closestAlly = findClosest(allies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestAlly),5);
			}
			if(nearbyEnemies.length>0){
				MapLocation closestEnemy = findClosest(nearbyEnemies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestEnemy),10);
         }else{// no nearby enemies; go toward far enemy
            if(enemies.length>0){
               MapLocation closestEnemy = findClosest(enemies);
               goalLoc = goalLoc.add(myLoc.directionTo(closestEnemy),10);
            }else{
               goalLoc = goalLoc.add(myLoc.directionTo( rc.senseEnemyHQLocation()),9);
            }
			}
		}
		//TODO repel from allied mines?
		//now use that direction
		Direction finalDir = myLoc.directionTo(goalLoc);
		if (Math.random()<.1)
			finalDir = finalDir.rotateRight();
		simpleMove(finalDir, myLoc,true);
	}
	private static int targetWeight(int dSquared){
		if (dSquared>100){
			return 5;
		}else if (dSquared>9){
			return 2;
		}else{
			return 1;
		}
	}
   private static MapLocation getNearbyMedbay(RobotController rc, int radius) throws Exception{
      MapLocation nearestMedbay= null;
      MapLocation[] nearEncamps=
      rc.senseEncampmentSquares(rc.getLocation(),radius, Team.NEUTRAL);
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

	private static void simpleMove(Direction dir, MapLocation myLoc, boolean defuseMines) throws GameActionException {
		//first try to capture an encampment
      int numEnemies =
                     rc.senseNearbyGameObjects(Robot.class,CAPTURE_PRIVACY_RADIUS,rc.getTeam().opponent()).length;
      int numArtilleryTargets=rc.senseNearbyGameObjects(Robot.class, RobotType.ARTILLERY.attackRadiusMaxSquared, rc.getTeam().opponent()).length;

		if
      (defuseMines && (rc.getTeamPower() > rc.senseCaptureCost()) && rc.senseEncampmentSquare(myLoc) &&
      (numEnemies<1)){//leisure indicator
			if(rc.getEnergon() < injured_health){
            //check if a medbay is a adjacent first.  If it is, build artillery. lols.
            int medChannel=getChannel()+2;
            MapLocation medbayLoc= IntToMaplocation(rc.readBroadcast(medChannel));
            if(medbayLoc!= null){
               if(rc.getLocation().isAdjacentTo(medbayLoc)){
                  rc.captureEncampment(RobotType.ARTILLERY);
               }else{
                  rc.captureEncampment(RobotType.MEDBAY);
               }
            }else{
               rc.captureEncampment(RobotType.MEDBAY);
            }
         }else if(numArtilleryTargets>3){
                  rc.captureEncampment(RobotType.ARTILLERY);
         }else if(Math.random()<.6){
				rc.captureEncampment(RobotType.GENERATOR);
			}else{
				rc.captureEncampment(RobotType.SUPPLIER);
			}
		}else{
			//then consider moving
			int[] directionOffsets = {0,1,-1,2,-2};
			Direction lookingAtCurrently = null;
			lookAround: for (int d:directionOffsets){
				lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
				Team currentMine = rc.senseMine(myLoc.add(lookingAtCurrently));
				if(rc.canMove(lookingAtCurrently)&&(defuseMines||(!defuseMines&&(currentMine==rc.getTeam()||currentMine==null)))){
					moveOrDefuse(lookingAtCurrently);
					break lookAround;
				}
			}
		}
	}
	private static void moveOrDefuse(Direction dir) throws GameActionException{
		MapLocation ahead = rc.getLocation().add(dir);
		Team mineAhead = rc.senseMine(ahead);
		if(mineAhead!=null&&mineAhead!= rc.getTeam()){
			rc.defuseMine(ahead);
		}else{
			rc.move(dir);			
		}
	}
	private static MapLocation findClosest(Robot[] enemyRobots) throws GameActionException {
		int closestDist = 1000000;
		MapLocation closestEnemy=null;
		for (int i=0;i<enemyRobots.length;i++){
			Robot arobot = enemyRobots[i];
			RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
			int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
			if (dist<closestDist){
				closestDist = dist;
				closestEnemy = arobotInfo.location;
			}
		}
		return closestEnemy;
	}
//HQ
	private static void hqCode(){
		MapLocation myLoc = rc.getLocation();
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation rallyPt = myLoc.add(myLoc.directionTo(enemyLoc),5);
		while(true){
			try{
				
				if (rc.isActive()) {
               int numFriendlies=rc.senseNearbyGameObjects(Robot.class,1000,rc.getTeam()).length;
               int numEnemies =
               rc.senseNearbyGameObjects(Robot.class,10000000,rc.getTeam().opponent()).length;
					// Spawn a soldier
					//			Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
               int beGreaterBy= rc.hasUpgrade(Upgrade.FUSION) ? SUPERIORITY*3:SUPERIORITY;

					if((rc.getTeamPower()-40>10) &&(numFriendlies < (numEnemies +beGreaterBy))){
						lookAround: for (Direction d:Direction.values()){
                     if(d == Direction.OMNI)
								break lookAround;
                     if(d == Direction.NONE)
								break lookAround;
							if (rc.canMove(d)&&(d != Direction.OMNI)&&(d != Direction.NONE)){
								rc.spawn(d);
                        //System.out.println("friendlies" + numFriendlies + "spawning");
								break lookAround;
							}
						}
					}else if (!rc.hasUpgrade(Upgrade.PICKAXE)){
						rc.researchUpgrade(Upgrade.PICKAXE);
					}else if (!rc.hasUpgrade(Upgrade.DEFUSION)){
						rc.researchUpgrade(Upgrade.DEFUSION);
					}else if (!rc.hasUpgrade(Upgrade.FUSION)){
						rc.researchUpgrade(Upgrade.FUSION);
					}else if (!rc.hasUpgrade(Upgrade.NUKE)){
						rc.researchUpgrade(Upgrade.NUKE);
					}

				}
				
				//move the rally point if it is a capfutureencampment
				MapLocation[] alliedEncampments = rc.senseAlliedEncampmentSquares();
				if (alliedEncampments.length>0&&among(alliedEncampments,rallyPt)){
					MapLocation closestEncampment = captureEncampments(alliedEncampments);
					if (closestEncampment!= null){
						rallyPt = closestEncampment;
					}
				}
				
				if(rc.getEnergon()<300||Clock.getRoundNum()>400||rc.senseEnemyNukeHalfDone()){//kill enemy if nearing round limit or injured
					rallyPt = enemyLoc;
				}
				
				//message allies about where to go
				int channel = getChannel();
				int msg = MapLocationToInt(rallyPt);
				rc.broadcast(channel, msg);
				rc.setIndicatorString(0,"Posted "+msg+" to "+channel);
				
				//message allies about whether to mine
            int nearbyEnemies=rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent()).length;
				if
            ( (rc.hasUpgrade(Upgrade.PICKAXE) && (nearbyEnemies<2) )|| 
            ( (nearbyEnemies<3)&&(Clock.getRoundNum()<150) )){
					rc.broadcast(getChannel()+1, 2);
				}else{
					rc.broadcast(getChannel()+1, 1);
            }
				
			}catch (Exception e){
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	private static void medbayCode(){
		MapLocation myLoc = rc.getLocation();
		while(true){
			try{
				
            if (rc.isActive()) {

               //move the rally point if it is a capfutureencampment

               //message allies about where to go
               int medChannel = getChannel() + 2;
               int msg = MapLocationToInt(myLoc);
               rc.broadcast(medChannel, msg);
               rc.setIndicatorString(0,"Posted "+msg+" to "+ (medChannel));

            }
			}catch (Exception e){
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
   private static void artilleryCode(){
      MapLocation myLoc = rc.getLocation();
      while(true){
         try{

            if (rc.isActive()) {

               Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,RobotType.ARTILLERY.attackRadiusMaxSquared,rc.getTeam().opponent());
               
               if(nearbyEnemies.length > 0){
                  rc.attackSquare(findClosest(nearbyEnemies));
               }
            }
         }catch (Exception e){
            System.out.println("Soldier Exception");
            e.printStackTrace();
         }
         rc.yield();
      }
   }

	//Messaging functions
	public static int getChannel(){
		int channel = ((Clock.getRoundNum()/5)*mult)%GameConstants.BROADCAST_MAX_CHANNELS;
		//int channel = (5*mult)%GameConstants.BROADCAST_MAX_CHANNELS;
		return channel;
	}
	public static int MapLocationToInt(MapLocation loc){
		return loc.x*1000+loc.y;
	}
	public static MapLocation IntToMaplocation(int mint){
		int y = mint%1000;
		int x = (mint-y)/1000;
		if(x==0&&y==0){
			return null;
		}else{
			return new MapLocation(x,y);
		}
	}
//locating encampment
	public static MapLocation captureEncampments(MapLocation[] alliedEncampments) throws GameActionException{
		MapLocation[] allEncampments = rc.senseAllEncampmentSquares();
		//locate uncaptured encampments within a certain radius
		MapLocation[] neutralEncampments = new MapLocation[allEncampments.length];
		int neInd = 0;
		
		// Compute nearest encampment (counting the enemy HQ)
		outer: for(MapLocation enc: allEncampments) {
			for(MapLocation aenc: alliedEncampments) 
				if(aenc.equals(enc))
					continue outer;
			if(rc.senseHQLocation().distanceSquaredTo(enc)<= Math.pow(Clock.getRoundNum()/10, 2)){
				//add to neutral encampments list
				neutralEncampments[neInd]=enc;
				neInd=neInd+1;
			}
		}
		rc.setIndicatorString(2, "neutral enc det "+neInd+" round "+Clock.getRoundNum());
		
		if (neInd>0){
			//proceed to an encampment and capture it
			int which = (int) ((Math.random()*100)%neInd);
			MapLocation campLoc = neutralEncampments[which];
			return campLoc;
		}else{//no encampments to capture; change state
			return null;
		}
	}
	private static boolean among(MapLocation[] alliedEncampments,
			MapLocation rallyPt) {
		for(MapLocation enc:alliedEncampments){
			if(enc.equals(rallyPt))
				return true;
		}
		return false;
	}

   private static void scoutCode(){
      MapLocation myLoc = rc.getLocation();
      while(true){
         try{

            if (rc.isActive()) { 
               
               Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class, 25,rc.getTeam().opponent());
               if(nearbyEnemies.length>1)
                  return;//scouting's done.


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
         }catch (Exception e){
            System.out.println("Soldier Exception");
            e.printStackTrace();
         }
         rc.yield();
      }
   }

}

