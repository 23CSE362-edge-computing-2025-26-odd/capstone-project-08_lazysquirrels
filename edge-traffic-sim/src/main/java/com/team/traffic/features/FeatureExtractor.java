package com.team.traffic.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FeatureExtractor {

  // Data object returned per intersection each tick
  public static class Feat {
    public final String id;     // "i-<i>-<j>"
    public final int i, j;
    public final double q_ns, q_ew;        // queue proxies
    public final double vavg_ns, vavg_ew;  // avg speed proxies
    public final String light;             // "NS" | "EW"
    public final double last_switch_s;     // seconds since last switch
    public Feat(String id,int i,int j,double qns,double qew,double vns,double vew,String light,double lastSw){
      this.id=id; this.i=i; this.j=j; this.q_ns=qns; this.q_ew=qew; this.vavg_ns=vns; this.vavg_ew=vew; this.light=light; this.last_switch_s=lastSw;
    }
  }

  // Internal per-intersection state
  private static class State {
    double q_ns=0, q_ew=0;   // queues (toy)
    String light="NS";
    double lastSwitch=0;
  }

  private final int N;
  private final Map<String, State> state = new HashMap<>();
  private final Random rng;

  public FeatureExtractor(int gridN, long seed){
    this.N = gridN;
    this.rng = new Random(seed);
    for(int i=0;i<N;i++) for(int j=0;j<N;j++){
      state.put(key(i,j), new State());
    }
  }

  private static String key(int i,int j){ return i+"_"+j; }
  private static String id (int i,int j){ return "i-"+i+"-"+j; }

  /**
   * Advance state by dt_s and return current features.
   * Call this once per tick (e.g., dt_s = 1.0).
   */
  public Map<String, Feat> compute(double t_s, double dt_s){
    Map<String, Feat> feats = new HashMap<>();
    for(int i=0;i<N;i++) for(int j=0;j<N;j++){
      State st = state.get(key(i,j));

      // Toy inflow pattern with spatial bias (creates asymmetry in the grid)
      double bias = ((i + j) % 3 == 0) ? 0.7 : 0.3;              // more NS when bias high
      double inflowNS = clamp(0.0, 2.0, 0.6 + 0.6*bias + 0.20*rng.nextGaussian());
      double inflowEW = clamp(0.0, 2.0, 0.6 + 0.6*(1.0-bias) + 0.20*rng.nextGaussian());

      // Service higher on the green axis
      double serviceNS = st.light.equals("NS") ? 0.9 : 0.2;
      double serviceEW = st.light.equals("EW") ? 0.9 : 0.2;

      st.q_ns = Math.max(0.0, st.q_ns + (inflowNS - serviceNS) * dt_s);
      st.q_ew = Math.max(0.0, st.q_ew + (inflowEW - serviceEW) * dt_s);

      // Speeds inversely correlated with queues (0..10)
      double vns = Math.max(0, 10.0 - st.q_ns);
      double vew = Math.max(0, 10.0 - st.q_ew);

      feats.put(key(i,j), new Feat(id(i,j), i, j, st.q_ns, st.q_ew, vns, vew, st.light, st.lastSwitch));
    }
    return feats;
  }

  // Control loop should call this once per tick to age "lastSwitch"
  public void tickIncrement(double dt_s){
    for (State st : state.values()) st.lastSwitch += dt_s;
  }

  // Called by control loop when the signal actually flips
  public void apply(String axis, int i, int j){
    State st = state.get(key(i,j));
    if (st == null) return;
    if (!st.light.equals(axis)) {
      st.light = axis;
      st.lastSwitch = 0.0;
    }
  }

  public String currentLight(int i,int j){ return state.get(key(i,j)).light; }
  public double sinceSwitch(int i,int j){ return state.get(key(i,j)).lastSwitch; }

  private static double clamp(double lo,double hi,double x){ return Math.max(lo, Math.min(hi, x)); }
}