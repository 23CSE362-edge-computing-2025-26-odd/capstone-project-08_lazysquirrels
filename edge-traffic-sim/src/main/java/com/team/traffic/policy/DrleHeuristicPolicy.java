package com.team.traffic.policy;

/**
 * Simple actuated rules:
 * - If current green has a big gap and the cross axis has larger queue, switch.
 * - Otherwise hold current. Always respect MIN_GREEN_S.
 */
public class ActuatedPolicy {
  public static final double MIN_GREEN_S = 3.0;
  public static final double GAP_S = 1.4;  // crude threshold; tune later

  /** @return "NS" or "EW" */
  public static String decide(double qNS, double qEW, double gapNS, double gapEW,
                              String current, double sinceSwitch) {
    if (sinceSwitch < MIN_GREEN_S) return current;

    if ("NS".equals(current)) {
      if (gapNS > GAP_S && qEW > qNS) return "EW";
      return "NS";
    } else {
      if (gapEW > GAP_S && qNS > qEW) return "NS";
      return "EW";
    }
  }
}