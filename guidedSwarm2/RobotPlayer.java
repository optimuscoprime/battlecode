package guidedSwarm2;

import battlecode.common.*;

public class RobotPlayer{
	
	static RobotController rc;
	static int mult = 234;
	static int status = 1;//1 is don't lay mines, 2 is lay mines
	
	public static void run(RobotController myRC){
		rc = myRC;
		if (rc.getTeam()==Team.A)
			mult = 114;
		
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					soldierCode();
				}else if (rc.getType()==RobotType.HQ){
					hqCode();
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
		while(true){
			try{
				rc.setIndicatorString(0,"goal: "+rallyPt.toString());
				
				//receive rally point from HQ
				MapLocation received = IntToMaplocation(rc.readBroadcast(getChannel()));
				if (received!= null)
					rallyPt = received;
				//receive mining command
				int ir = rc.readBroadcast(getChannel()+1);
				if (ir!=0&&ir<=2)
					status = ir;

				Robot[] allies = rc.senseNearbyGameObjects(Robot.class,14,rc.getTeam());
				Robot[] enemies = rc.senseNearbyGameObjects(Robot.class,10000000,rc.getTeam().opponent());
				Robot[] nearbyEnemies = rc.senseNearbyGameObjects(Robot.class,14,rc.getTeam().opponent());
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
		}else if (allies.length>=nearbyEnemies.length+3){
			if(allies.length>0){
				MapLocation closestAlly = findClosest(allies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestAlly),5);
			}
			if(nearbyEnemies.length>0){
				MapLocation closestEnemy = findClosest(nearbyEnemies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestEnemy),10);
			}else{// no nearby enemies; go toward far enemy
				MapLocation closestEnemy = findClosest(enemies);
				goalLoc = goalLoc.add(myLoc.directionTo(closestEnemy),10);
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
	private static void simpleMove(Direction dir, MapLocation myLoc, boolean defuseMines) throws GameActionException {
		//first try to capture an encampment
		if (defuseMines&&rc.getTeamPower()>rc.senseCaptureCost()&&rc.senseEncampmentSquare(myLoc)){//leisure indicator
			if(Math.random()<.7){
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
					// Spawn a soldier
					//			Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
					if(rc.getTeamPower()-40>10){
						lookAround: for (Direction d:Direction.values()){
							if (rc.canMove(d)){
								rc.spawn(d);
								break lookAround;
							}
						}
					}else if (!rc.hasUpgrade(Upgrade.PICKAXE)){
						rc.researchUpgrade(Upgrade.PICKAXE);
					}
				}
				
				//move the rally point if it is a captured encampment
				MapLocation[] alliedEncampments = rc.senseAlliedEncampmentSquares();
				if (alliedEncampments.length>0&&among(alliedEncampments,rallyPt)){
					MapLocation closestEncampment = captureEncampments(alliedEncampments);
					if (closestEncampment!= null){
						rallyPt = closestEncampment;
					}
				}
				
				if(rc.getEnergon()<300||Clock.getRoundNum()>2000||rc.senseEnemyNukeHalfDone()){//kill enemy if nearing round limit or injured
					rallyPt = enemyLoc;
				}
				
				//message allies about where to go
				int channel = getChannel();
				int msg = MapLocationToInt(rallyPt);
				rc.broadcast(channel, msg);
				rc.setIndicatorString(0,"Posted "+msg+" to "+channel);
				
				//message allies about whether to mine
				if (/*rc.hasUpgrade(Upgrade.PICKAXE)*/rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent()).length<3){
					rc.broadcast(getChannel()+1, 2);
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
		int channel = (Clock.getRoundNum()*mult)%GameConstants.BROADCAST_MAX_CHANNELS;
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
}