package oc005;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameActionExceptionType;

public class Util {

	// DEBUG
	private static IntStack bytecodeStack;
	private static int[] gaeHistogram;

	static {
		debug_initialise();
	}

	private static void debug_initialise() {
		bytecodeStack = new IntStack(10);
		gaeHistogram = new int[GameActionExceptionType.values().length];
	}	

	// use bc.conf to turn debug mode on
	public static void debug_printf(String format, Object ... objects) {
		//System.out.printf("ROUND %d\n", Clock.getRoundNum());
		System.out.printf(format, objects);
		System.out.printf("\n");
	}	

	public static void debug_catch(Exception e) {
		debug_printf("%s: %s\n", e.getStackTrace()[0].getMethodName(), e.getMessage());		
		if (e instanceof GameActionException) {
			GameActionException gae = (GameActionException) e;
			debug_printf("GameActionExceptionType: %s", gae.getType());
			e.printStackTrace();

			gaeHistogram[gae.getType().ordinal()]++;

			for (int i=0; i < GameActionExceptionType.values().length; i++) {
				debug_printf("%04d: %s\n", gaeHistogram[i], GameActionExceptionType.values()[i]);
			}
		}
	}

	public static void debug_startMethod() {
		bytecodeStack.push(Clock.getBytecodeNum());
	}

	public static void debug_endMethod() {
		int bytecodesNow = Clock.getBytecodeNum();
		int bytecodesBefore = bytecodeStack.pop();
		int bytecodesUsed = bytecodesNow - bytecodesBefore;
		StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
		String methodName = stackTraceElements[1].getMethodName();
		if (bytecodesUsed >= 5000) {
			debug_printf("%d bytecodes were used by %s\n", bytecodesUsed, methodName);
		}
	}	

}
