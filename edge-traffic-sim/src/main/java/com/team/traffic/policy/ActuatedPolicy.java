package com.team.traffic.policy;

/**
 * Actuated policy logic:
 * - If the current green has a large gap (time since last switch) and the cross axis has a larger queue, switch.
 * - Otherwise, hold current. Always respect MIN_GREEN_S (minimum green time).
 */
public class ActuatedPolicy {
    public static final double MIN_GREEN_S = 3.0; // Minimum green time in seconds
    public static final double GAP_S = 1.4;  // Gap threshold for switching

    /**
     * Decide whether to switch or hold the current light.
     *
     * @param qNS        Queue for NS direction
     * @param qEW        Queue for EW direction
     * @param gapNS      Time since last switch for NS direction
     * @param gapEW      Time since last switch for EW direction
     * @param current    Current green direction ("NS" or "EW")
     * @param sinceSwitch Time since the last switch
     * @return "NS" or "EW" - the new phase (green light direction)
     */
    public static String decide(double qNS, double qEW, double gapNS, double gapEW,
                                String current, double sinceSwitch) {
        if (sinceSwitch < MIN_GREEN_S) return current; // Don't switch if the minimum green time hasn't elapsed

        if ("NS".equals(current)) {
            // If the NS direction has been green long enough and the EW direction has a larger queue, switch to EW
            if (gapNS > GAP_S && qEW > qNS) return "EW";
            return "NS"; // Otherwise, stay on NS
        } else {
            // If the EW direction has been green long enough and the NS direction has a larger queue, switch to NS
            if (gapEW > GAP_S && qNS > qEW) return "NS";
            return "EW"; // Otherwise, stay on EW
        }
    }
}
