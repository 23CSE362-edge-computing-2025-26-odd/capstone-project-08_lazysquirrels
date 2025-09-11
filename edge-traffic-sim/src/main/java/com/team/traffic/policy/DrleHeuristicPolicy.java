package com.team.traffic.policy;

/**
 * DRLE-style heuristic: serve the heavier approach (queue proxy),
 * with a min-green hold to prevent flapping.
 */
public class DrleHeuristicPolicy {
  public static final double MIN_GREEN_S = 3.0;

  /** @return "NS" or "EW" */
  public static String decide(double qNS, double qEW, String current, double sinceSwitch) {
    if (sinceSwitch < MIN_GREEN_S) return current;
    return (qNS >= qEW) ? "NS" : "EW";
  }
}
