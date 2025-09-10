package com.team.traffic.scheduler;

import com.team.traffic.TopologyBuilder;
import com.team.traffic.features.FeatureExtractor;
import com.team.traffic.features.FeatureExtractor.Feat;
import com.team.traffic.metrics.MetricsSink;
import com.team.traffic.metrics.EnergyNetAccounter;
import com.team.traffic.policy.PolicyDriver;
import com.team.traffic.util.Config;

import java.util.Map;

public class ControlLoop {
  private final Config cfg;
  private final TopologyBuilder.Topo topo;
  private final FeatureExtractor fx;
  private final PolicyDriver driver;
  private final MetricsSink sink;
  private final EnergyNetAccounter power;

  // tuple sizes (must match AppGraphBuilder)
  private static final int RAW_BYTES  = 1500;
  private static final int FEAT_BYTES = 400;
  private static final int CMD_BYTES  = 120;
  private static final int ACT_BYTES  = 80;

  public ControlLoop(Config cfg, TopologyBuilder.Topo topo, String runName) throws Exception {
    this.cfg = cfg;
    this.topo = topo;
    this.fx = new FeatureExtractor(cfg.grid, /*seed*/ 42);
    this.driver = new PolicyDriver(cfg.policy);
    this.sink = new MetricsSink(cfg.results.dir, "run-" + runName + "-" + cfg.policy.toLowerCase() + "-" + cfg.grid + "x" + cfg.grid + ".csv");
    this.power = new EnergyNetAccounter();
  }

  public void run() {
    final double dt = 1.0; // 1 second per tick
    double t = 0.0;

    // CSV header
    sink.header();

    while (t < cfg.duration_s) {
      // compute features for all intersections
      Map<String, Feat> feats = fx.compute(t, dt);

      // estimate ES1 power (coarse)
      double es1PowerW = power.estimateEs1Watts(feats.size());

      // loop intersections
      for (Feat f : feats.values()) {
        String current = f.light;
        String desired = driver.decide(f.i, f.j, t, f.q_ns, f.q_ew, f.vavg_ns, f.vavg_ew, current, f.last_switch_s);

        // min-green already inside policies; apply immediately
        String applied = desired;
        if (!applied.equals(current)) {
          fx.apply(applied, f.i, f.j);
        }

        // simplistic latency model: uplink + compute + downlink
        double computeMs = 10.0; // placeholder (you can tune per-policy)
        double e2eLatencyMs = cfg.network.uplink_ms + computeMs + cfg.network.downlink_ms;
        int slaMiss = (e2eLatencyMs >= 1000.0) ? 1 : 0;

        // bytes accounting per intersection per tick
        long up = RAW_BYTES + FEAT_BYTES; // SENSOR + features
        long down = CMD_BYTES + ACT_BYTES;

        // toy halting proxy: if red on NS, halting proportional to q_ns (and vice versa)
        double haltPct = "NS".equals(applied)
            ? clamp01(f.q_ew / (f.q_ew + f.q_ns + 1e-6))
            : clamp01(f.q_ns / (f.q_ew + f.q_ns + 1e-6));

        sink.log(t, cfg.policy, f.i, f.j, desired, applied, e2eLatencyMs, slaMiss, up, down,
                 es1PowerW, f.q_ns, f.q_ew, haltPct, driver.mode().name().toLowerCase());
      }

      // advance clocks
      fx.tickIncrement(dt);
      t += dt;
    }

    sink.close();
  }

  private static double clamp01(double x){ return Math.max(0.0, Math.min(1.0, x)); }
}