package com.team.traffic.scheduler;

import com.team.traffic.TopologyBuilder;
import com.team.traffic.features.FeatureExtractor;
import com.team.traffic.features.FeatureExtractor.Feat;
import com.team.traffic.metrics.MetricsSink;
import com.team.traffic.metrics.EnergyNetAccounter;
import com.team.traffic.policy.HttpDrlePolicy;
import com.team.traffic.policy.ActuatedPolicy;
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

  // HTTP-based DRLE policy (integration with CI server) is now only initialized for DRLE mode
  private HttpDrlePolicy drlePolicy = null;

  // tuple sizes (must match AppGraphBuilder)
  private static final int RAW_BYTES = 1500;
  private static final int FEAT_BYTES = 400;
  private static final int CMD_BYTES = 120;
  private static final int ACT_BYTES = 80;

  public ControlLoop(Config cfg, TopologyBuilder.Topo topo, String runName, String ciServerUrl) throws Exception {
    this.cfg = cfg;
    this.topo = topo;
    this.fx = new FeatureExtractor(cfg.grid, /*seed*/ 42);
    this.driver = new PolicyDriver(cfg.policy);  // Policy driver based on config policy type
    this.sink = new MetricsSink(cfg.results.dir, "run-" + runName + "-" + cfg.policy.toLowerCase() + "-" + cfg.grid + "x" + cfg.grid + ".csv");
    this.power = new EnergyNetAccounter();

    // Initialize the DRLE policy only if the mode is set to "http" (DRLE mode)
    if ("http".equals(cfg.drle.mode) && "DRLE".equals(cfg.policy)) {
      this.drlePolicy = new HttpDrlePolicy(ciServerUrl);  // Initialize HttpDrlePolicy only for DRLE mode
    }
  }

  public void run() {
    final double dt = 1.0; // 1 second per tick
    double t = 0.0;

    // CSV header
    sink.header();

    while (t < cfg.duration_s) {
      // Compute features for all intersections
      Map<String, Feat> feats = fx.compute(t, dt);

      // Estimate ES1 power (coarse)
      double es1PowerW = power.estimateEs1Watts(feats.size());

      // Loop through each intersection
      for (Feat f : feats.values()) {
        String current = f.light;

        // Default action is to keep current state
        String desired = current;

        // Handle DRLE policy - if DRLE is enabled, get action from the CI server
        if ("DRLE".equals(cfg.policy) && drlePolicy != null) {
          String jsonObservation = fx.createObservationJson(f); // Create the observation in JSON format
          desired = drlePolicy.sendToCI(jsonObservation); // Get the action (SWITCH/HOLD)
        }

        // Handle TIMER policy - Hold the current state (do nothing)
        else if ("TIMER".equals(cfg.policy)) {
            // Fixed 30-second phase duration
            double PHASE_DURATION = 30.0;

            // Determine time since last switch for current light
            double timeSinceSwitch = "NS".equals(current) ? f.last_switch_ns : f.last_switch_ew;

            // Switch light if 30 seconds have passed
            if (timeSinceSwitch >= PHASE_DURATION) {
                desired = "NS".equals(current) ? "EW" : "NS";  // switch to opposite axis
            } else {
                desired = current;
            }
        }


else if ("ACTUATED".equals(cfg.policy)) {
    // Use the correct gap times for NS and EW based on the current light
    double gapNS = f.last_switch_ns;  // Time since last switch for NS
    double gapEW = f.last_switch_ew;  // Time since last switch for EW
    
    // Use ActuatedPolicy to decide whether to switch or hold
    // The `current` direction will determine which gap is relevant
    if ("NS".equals(current)) {
        // If NS is green, we use `gapNS` and `last_switch_ns` to decide for NS
        desired = ActuatedPolicy.decide(f.q_ns, f.q_ew, gapNS, gapEW, current, f.last_switch_ns);
    } else {
        // If EW is green, we use `gapEW` and `last_switch_ew` to decide for EW
        desired = ActuatedPolicy.decide(f.q_ns, f.q_ew, gapNS, gapEW, current, f.last_switch_ew);
    }
}


        // Min-green already inside policies; apply immediately
        String applied = desired;
        if (!applied.equals(current)) {
          fx.apply(applied, f.i, f.j);  // Apply the action in the simulation
        }

        // Simulate latency model (uplink + compute + downlink)
        double computeMs = 10.0; // placeholder
        double e2eLatencyMs = cfg.network.uplink_ms + computeMs + cfg.network.downlink_ms;
        int slaMiss = (e2eLatencyMs >= 1000.0) ? 1 : 0;

        // Bytes accounting per intersection per tick
        long up = RAW_BYTES + FEAT_BYTES; // SENSOR + features
        long down = CMD_BYTES + ACT_BYTES;

        // Toy halting proxy: if red on NS, halting proportional to q_ns (and vice versa)
        double haltPct = "NS".equals(applied)
            ? clamp01(f.q_ew / (f.q_ew + f.q_ns + 1e-6))
            : clamp01(f.q_ns / (f.q_ew + f.q_ns + 1e-6));

        // Log the metrics
        sink.log(t, cfg.policy, f.i, f.j, desired, applied, e2eLatencyMs, slaMiss, up, down,
                 es1PowerW, f.q_ns, f.q_ew, haltPct, driver.mode().name().toLowerCase());
      }

      // Advance clocks
      fx.tickIncrement(dt);
      t += dt;
    }

    sink.close();
  }

  private static double clamp01(double x) {
    return Math.max(0.0, Math.min(1.0, x));
  }
}
