package oc004;

import java.util.*;

import battlecode.common.*;

public class RobotPlayer {

	private static RobotController rc;
	private static Team myTeam;
	private static Team opposingTeam;
	private static int mapWidth;
	private static int mapHeight;

	// DEBUG
	private static IntStack bytecodeStack;
	private static int[] gaeHistogram;

	public static void run(RobotController rc) {
		initialise(rc);

		while(true) {

			try {
				decideMove();
			} catch (Exception e) {
				debug_catch(e);
			}

			rc.yield();
		}
	}

	private static void initialise(RobotController rc) {
		debug_initialise();

		debug_startMethod();

		RobotPlayer.rc = rc;
		RobotPlayer.myTeam = rc.getTeam();
		RobotPlayer.opposingTeam = myTeam.opponent();
		RobotPlayer.mapHeight = rc.getMapHeight();
		RobotPlayer.mapWidth = rc.getMapWidth();

		debug_endMethod();
	}

	private static void debug_initialise() {
		bytecodeStack = new IntStack(10);
		gaeHistogram = new int[GameActionExceptionType.values().length];
	}

	// use bc.conf to turn debug mode on
	private static void debug_printf(String format, Object ... objects) {
		System.out.printf(format, objects);
	}

	private static void debug_catch(Exception e) {
		debug_printf("%s: %s\n", e.getStackTrace()[0].getMethodName(), e.getMessage());		
		if (e instanceof GameActionException) {
			GameActionException gae = (GameActionException) e;
			debug_printf("GameActionExceptionType: %s", gae.getType());
			gaeHistogram[gae.getType().ordinal()]++;

			for (int i=0; i < GameActionExceptionType.values().length; i++) {
				debug_printf("%04d: %s\n", gaeHistogram[i], GameActionExceptionType.values()[i]);
			}
		}
	}

	private static void debug_startMethod() {
		bytecodeStack.push(Clock.getBytecodeNum());
	}

	private static void debug_endMethod() {
		int bytecodesNow = Clock.getBytecodeNum();
		int bytecodesBefore = bytecodeStack.pop();
		int bytecodesUsed = bytecodesNow - bytecodesBefore;
		String methodName = new Throwable().getStackTrace()[1].getMethodName();
		if (bytecodesUsed > 0) {
			debug_printf("%d bytecodes were used by %s\n", bytecodesUsed, methodName);
		}
	}

	private static void decideMove() {
		debug_startMethod();

		// todo read game specs
		// todo read javadoc
		// todo read lecture notes

		debug_endMethod();
	}
}
