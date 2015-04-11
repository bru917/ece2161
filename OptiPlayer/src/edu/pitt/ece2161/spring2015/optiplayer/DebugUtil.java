package edu.pitt.ece2161.spring2015.optiplayer;

import java.text.NumberFormat;

public class DebugUtil {
	
	/**
	 * Constructs the debug text shown at the top of the screen.
	 * @param analysisMode True for analysis mode, false for playback mode.
	 * @param level The selected level of the backlight.
	 * @return The debug text.
	 */
	public static String printDebug(boolean analysisMode, int level) {
		String mode = analysisMode ? "Analysis" : "Playback";
		int brightness = FrameAnalyzer.getBrightness(level);
		String power =  NumberFormat.getInstance().format(cal_power(brightness));
		
		return "Mode: " + mode + "   Level: " + level
				+ "  BL: " + brightness + "  Est Power: " + power;
	}

	/**
	 * Calculates an estimated backlight power value.
	 * @param b The backlight level setting (0-255)
	 * @return A string formatted floating point representation of the power
	 */
	public static double cal_power(int b) {
		double bf = (double) b / 255.0;
		
		double power = 0.0;
		double PLin = 0.4991;
		double Psat = 0.1489;
		double Clin = 0.1113;
		double Csat = 0.6119;
		double Bs = 0.8666;

		if (bf < Math.round(255 * Bs)) {
			power = bf * PLin + Clin;
		} else {
			power = bf * Psat + Csat;
		}

		return power * 1000;
	}
}
