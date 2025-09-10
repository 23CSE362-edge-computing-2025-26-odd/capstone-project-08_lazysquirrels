package com.team.traffic.policy;

/**
 * Fixed-time controller: alternates NS/EW every CYCLE_S, with a checkerboard
 * offset so neighbors aren't in the same phase. Respects MIN_GREEN_S.
 */
public class TimerPolicy {
  public static final double CYCLE_S = 30.0;
  public static final double MIN_GREEN_S = 3.0;

  /** @return "NS" or "EW" */
  public static String decide(int i, int j, double t_s, String current, double sinceSwitch) {
    if (sinceSwitch < MIN_GREEN_S) return current;
    int phase = (int) Math.floor((t_s + ((i + j) & 1) * CYCLE_S) / CYCLE_S) % 2;
    return phase == 0 ? "NS" : "EW";
  }
}