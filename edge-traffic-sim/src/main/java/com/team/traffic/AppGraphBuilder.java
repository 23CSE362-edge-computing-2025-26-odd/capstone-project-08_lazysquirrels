package com.team.traffic;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.application.Tuple;
import org.fog.utils.distribution.FractionalSelectivity;

public class AppGraphBuilder {
  public static Application build(String name, int userId) {
    Application app = Application.createApplication(name, userId);

    // Modules that will be placed by your placement classes:
    // Preprocess & RL_Agent @ ES1, LightCtrl @ each controller
    app.addAppModule("Preprocess", 1000);   // ~MI per tuple (coarse)
    app.addAppModule("RL_Agent",  1200);
    app.addAppModule("LightCtrl", 200);

    // Tuple sizes (bytes) + CPU MI (very coarse placeholders; tune later)
    final int RAW_BYTES  = 1500;   // SENSOR -> Preprocess
    final int FEAT_BYTES = 400;    // Preprocess -> RL_Agent
    final int CMD_BYTES  = 120;    // RL_Agent -> LightCtrl
    final int ACT_BYTES  = 80;     // LightCtrl -> ACTUATOR

    app.addAppEdge("SENSOR",     "Preprocess", RAW_BYTES, 300_000, "VEH_RAW",     Tuple.UP,   AppEdge.SENSOR);
    app.addAppEdge("Preprocess", "RL_Agent",   FEAT_BYTES,200_000, "LANE_FEAT",   Tuple.UP,   AppEdge.MODULE);
    app.addAppEdge("RL_Agent",   "LightCtrl",  CMD_BYTES, 50_000,  "SIGNAL_CMD",  Tuple.DOWN, AppEdge.MODULE);
    app.addAppEdge("LightCtrl",  "ACTUATOR",   ACT_BYTES, 10_000,  "LIGHT_APPLY", Tuple.DOWN, AppEdge.ACTUATOR);

    // 1:1 flow through the pipeline
    app.addTupleMapping("Preprocess", "VEH_RAW",    "LANE_FEAT",   new FractionalSelectivity(1.0));
    app.addTupleMapping("RL_Agent",   "LANE_FEAT",  "SIGNAL_CMD",  new FractionalSelectivity(1.0));
    app.addTupleMapping("LightCtrl",  "SIGNAL_CMD", "LIGHT_APPLY", new FractionalSelectivity(1.0));

    return app;
  }
}