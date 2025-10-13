package com.team.traffic.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FeatureExtractor {

    // Data object returned per intersection each tick
    public static class Feat {
        public final String id;            // "i-<i>-<j>"
        public final int i, j;
        public final double q_ns, q_ew;    // queue proxies
        public final double vavg_ns, vavg_ew; // avg speed proxies
        public final String light;         // "NS" | "EW"
        public final double last_switch_ns; // time since last switch for NS
        public final double last_switch_ew; // time since last switch for EW

        public Feat(String id, int i, int j, double qns, double qew, double vns, double vew, String light, double lastSwitchNS, double lastSwitchEW) {
            this.id = id;
            this.i = i;
            this.j = j;
            this.q_ns = qns;
            this.q_ew = qew;
            this.vavg_ns = vns;
            this.vavg_ew = vew;
            this.light = light;
            this.last_switch_ns = lastSwitchNS;
            this.last_switch_ew = lastSwitchEW;
        }
    }

    // Internal per-intersection state
    private static class State {
        double q_ns = 0, q_ew = 0;    // queues (toy)
        String light = "NS";           // current green
        double lastSwitchNS = 0;       // timer for NS
        double lastSwitchEW = 0;       // timer for EW
    }

    private final int N;
    private final Map<String, State> state = new HashMap<>();
    private final Random rng;

    public FeatureExtractor(int gridN, long seed) {
        this.N = gridN;
        this.rng = new Random(seed);
        for (int i = 0; i < N; i++) 
            for (int j = 0; j < N; j++) 
                state.put(key(i, j), new State());
    }

    private static String key(int i, int j) { return i + "_" + j; }
    private static String id(int i, int j) { return "i-" + i + "-" + j; }

    /**
     * Compute features for all intersections.
     * Call this once per tick.
     */
    public Map<String, Feat> compute(double t_s, double dt_s) {
        Map<String, Feat> feats = new HashMap<>();
        for (int i = 0; i < N; i++) 
            for (int j = 0; j < N; j++) {
                State st = state.get(key(i, j));

                // Toy inflow pattern with spatial bias
                double bias = ((i + j) % 3 == 0) ? 0.7 : 0.3;
                double inflowNS = clamp(0.0, 2.0, 0.6 + 0.6 * bias + 0.20 * rng.nextGaussian());
                double inflowEW = clamp(0.0, 2.0, 0.6 + 0.6 * (1.0 - bias) + 0.20 * rng.nextGaussian());

                // Service based on current green
                double serviceNS = st.light.equals("NS") ? 0.9 : 0.2;
                double serviceEW = st.light.equals("EW") ? 0.9 : 0.2;

                st.q_ns = Math.max(0.0, st.q_ns + (inflowNS - serviceNS) * dt_s);
                st.q_ew = Math.max(0.0, st.q_ew + (inflowEW - serviceEW) * dt_s);

                // Speeds inversely correlated with queues
                double vns = Math.max(0, 10.0 - st.q_ns);
                double vew = Math.max(0, 10.0 - st.q_ew);

                feats.put(key(i, j), new Feat(
                        id(i, j), i, j, st.q_ns, st.q_ew, vns, vew, st.light, st.lastSwitchNS, st.lastSwitchEW
                ));
            }
        return feats;
    }

    // Increment timers for each intersection
    public void tickIncrement(double dt_s) {
        for (State st : state.values()) {
            st.lastSwitchNS += dt_s;
            st.lastSwitchEW += dt_s;
        }
    }

    // Flip light and reset the appropriate timer
    public void apply(String axis, int i, int j) {
        State st = state.get(key(i, j));
        if (st == null) return;
        if (!st.light.equals(axis)) {
            st.light = axis;
            if ("NS".equals(axis)) st.lastSwitchNS = 0.0;
            else if ("EW".equals(axis)) st.lastSwitchEW = 0.0;
        }
    }

    public String currentLight(int i, int j) { return state.get(key(i, j)).light; }
    public double sinceSwitchNS(int i, int j) { return state.get(key(i, j)).lastSwitchNS; }
    public double sinceSwitchEW(int i, int j) { return state.get(key(i, j)).lastSwitchEW; }

    private static double clamp(double lo, double hi, double x) { return Math.max(lo, Math.min(hi, x)); }

    // Helper: create observation JSON for CI server (DRLE)
    public String createObservationJson(Feat f) {
        double phaseElapsed = "NS".equals(f.light) ? f.last_switch_ns : f.last_switch_ew;

        String json = "{"
                + "\"sim_tick\": 0,"  // placeholder tick
                + "\"intersection_id\": \"" + f.id + "\","
                + "\"min_phase_secs\": 3,"
                + "\"phase\": \"" + f.light + "\","
                + "\"phase_elapsed\": " + phaseElapsed + ","
                + "\"obs\": {"
                + "\"halting_counts\": {\"NS\": " + f.q_ns + ", \"EW\": " + f.q_ew + "},"
                + "\"speed_lag\": {\"NS\": " + (10 - f.vavg_ns) + ", \"EW\": " + (10 - f.vavg_ew) + "}"
                + "}}";
        return json;
    }
}
