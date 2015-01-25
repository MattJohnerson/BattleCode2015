package team428;

import battlecode.common.*;

import java.util.*;

//Gobbledygook

public class RobotPlayer {
	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	static int myRange;
	static Random rand;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	static int MFNum = 0;
	static int HeliNum = 0;
	static int SupplyNum = 0;
	static int MinerNum = 0;
	static int DroneNum = 0;
	
	
	public static void run(RobotController pineapplejuice) {
		rc = pineapplejuice;
		rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		MapLocation enemyHQ = rc.senseEnemyHQLocation();
		
		MapLocation myHQ = rc.senseHQLocation();
		Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		RobotInfo[] myRobots;

		while(true) {
			try {
				rc.setIndicatorString(0, "I am a " + rc.getType());
			} catch (Exception e) {
				System.out.println("Unexpected exception");
				e.printStackTrace();
			}

			if (rc.getType() == RobotType.HQ) {
				try {
					myRobots = rc.senseNearbyRobots(Integer.MAX_VALUE, myTeam);
					boolean spawnBeaver = true;
					if(myRobots.length > 0){
						for(RobotInfo r : myRobots){
							if(r.type.equals(rc.getType().BEAVER)){
								spawnBeaver = false;
							}
						}
					}
					if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady() && rc.getTeamOre() >= 100 && spawnBeaver) {
						trySpawn(myHQ.directionTo(enemyHQ).opposite(), RobotType.BEAVER);
					}
				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.TOWER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
				} catch (Exception e) {
					System.out.println("Tower Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.BEAVER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
					if (rc.isCoreReady()) {
						if(MFNum == 0){
							if(rc.getTeamOre() >= RobotType.MINERFACTORY.oreCost && rc.canBuild(directions[5], RobotType.MINERFACTORY)){
								tryBuild(directions[0], RobotType.MINERFACTORY);
								MFNum++;
							}
						}else if(MFNum > 0 && HeliNum <= 2){
							if(rc.getTeamOre() >= RobotType.HELIPAD.oreCost && rc.canBuild(directions[7], RobotType.MINERFACTORY)){
								tryBuild(directions[HeliNum*2 + 2], RobotType.HELIPAD);
								HeliNum++;
							}
						}
						rc.yield();
					}
				} catch (Exception e) {
					System.out.println("Beaver Exception");
					e.printStackTrace();
				}
			}
			
			
			if(rc.getType() == RobotType.MINER){
				try {
					MapLocation mineHere = findMost(rc.getLocation());
					if(rc.isCoreReady() && rc.canMine()){
						if(rc.senseOre(rc.getLocation()) >= rc.senseOre(mineHere) || rc.senseOre(rc.getLocation()) > 10){
							rc.mine();
						} else {
							tryAvoidMove(rc.getLocation().directionTo(mineHere));
						}
					}
				}
				catch (GameActionException e) {
					e.printStackTrace();

				}
			}
			
			if(rc.getType() == RobotType.DRONE){
				try {
					Direction towardEnemy = rc.getLocation().directionTo(enemyHQ);
					if(rc.isCoreReady()){
						if(rc.isWeaponReady()){
							attackSomething();
						}
						if(rc.canMove(towardEnemy)){
							tryAvoidMove(towardEnemy);
						}
					}
				} catch (Exception e) {
					System.out.println("Drone Exception");
					e.printStackTrace();
				}
			}
			
			if (rc.getType() == RobotType.MINERFACTORY){
				try {
					if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.MINER.oreCost) {
						trySpawn(rc.getLocation().directionTo(myHQ).opposite(), RobotType.MINER);
					}
				} catch (Exception e) {
					System.out.println("MinerFactory Exception");
					e.printStackTrace();
				}
			}
			
			if (rc.getType() == RobotType.HELIPAD) {
				try {
					
					if (rc.isCoreReady() && rc.getTeamOre() >= RobotType.DRONE.oreCost) {
						trySpawn(directions[rand.nextInt(8)],RobotType.DRONE);
					}
				} catch (Exception e) {
					System.out.println("Helipad Exception");
					e.printStackTrace();
				}
			}

			rc.yield();
		}
	}

	//This method finds the location with the most ore on it
	private static MapLocation findMost(MapLocation location) {
		MapLocation most = location;
		int mostSoFar = 0;
		for(int i = 0; i < 8; i++){
			if(rc.senseOre(rc.getLocation().add(directions[i])) > mostSoFar){
				most = rc.getLocation().add(directions[i]);
				mostSoFar =(int) rc.senseOre(rc.getLocation().add(directions[i]));
			}
		}
		return most;
	}
	
	

	// This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

	// This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}
	
	// This method will attempt to move in Direction d (or as close to it as possible) while avoiding towers and hq
		static void tryAvoidMove(Direction d) throws GameActionException {
			MapLocation tileInFront = rc.getLocation().add(d);
			MapLocation[] enemyTowers = rc.senseEnemyTowerLocations();
			RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
			MapLocation eHQ = rc.senseEnemyHQLocation();
			int offsetIndex = 0;
			int[] offsets = {0,1,-1,2,-2};
			int dirint = directionToInt(d);
			boolean tileInFrontSafe = true;
			for(MapLocation m : enemyTowers){
				if(m.distanceSquaredTo(tileInFront) <= RobotType.TOWER.attackRadiusSquared){
					tileInFrontSafe = false;
					break;
				}
			}
			if(eHQ.distanceSquaredTo(tileInFront) <= RobotType.HQ.attackRadiusSquared){
				tileInFrontSafe = false;
			}
			do{
				while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {

					tileInFront = rc.getLocation().add(directions[(dirint+offsets[offsetIndex]+8)%8]);
					for(MapLocation m : enemyTowers){
						if(m.distanceSquaredTo(tileInFront) > RobotType.TOWER.attackRadiusSquared){
							tileInFrontSafe = true;
							break;
						}
					}

					offsetIndex++;
				}
			}while(!tileInFrontSafe);
			
			if (offsetIndex < 5 && tileInFrontSafe) {
				rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
			}
		}

	// This method will attempt to spawn in the given direction (or as close to it as possible)
	static void trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint+offsets[offsetIndex]+8)%8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}

	// This method will attempt to build in the given direction (or as close to it as possible)
	static void tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}

	static int directionToInt(Direction d) {
		switch(d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}
}
