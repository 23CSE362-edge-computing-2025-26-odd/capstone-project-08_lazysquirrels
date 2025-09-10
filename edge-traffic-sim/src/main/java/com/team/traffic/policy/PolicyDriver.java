package com.team.traffic.policy;

/**
 * Thin wrapper that chooses which policy logic to use at runtime.
 * It also derives a crude "gap" proxy from speeds for the Actuated policy.
 */
public class PolicyDriver {
  public enum Mode { TIMER, ACTUATED, DRLE }

  private final Mode mode;

  public PolicyDriver(String policy) {
    String p = (policy == null ? "DRLE" : policy.trim().toUpperCase());
    this.mode = Mode.valueOf(p);
  }

  /**
   * @param i,j          intersection coordinates
   * @param t            current sim time (s)
   * @param qNS,qEW      queue proxies
   * @param vNS,vEW      average speed proxies (0..10, higher = faster)
   * @param current      current green axis ("NS"|"EW")
   * @param sinceSwitch  seconds since last switch
   * @return             "NS" or "EW"
   */
  public String decide(int i, int j, double t,
                       double qNS, double qEW,
                       double vNS, double vEW,
                       String current, double sinceSwitch) {
    switch (mode) {
      case TIMER:
        return TimerPolicy.decide(i, j, t, current, sinceSwitch);
      case ACTUATED:
        // gap proxy: faster speed ⇒ larger gap (totally coarse, good enough for baseline)
        double gapNS = vNS * 0.2;
        double gapEW = vEW * 0.2;
        return ActuatedPolicy.decide(qNS, qEW, gapNS, gapEW, current, sinceSwitch);
      default: // DRLE
        return DrleHeuristicPolicy.decide(qNS, qEW, current, sinceSwitch);
    }
  }

  public Mode mode() { return mode; }
}