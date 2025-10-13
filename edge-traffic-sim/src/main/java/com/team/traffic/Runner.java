package com.team.traffic;

import com.team.traffic.util.Config;
import com.team.traffic.scheduler.ControlLoop;

import org.cloudbus.cloudsim.core.CloudSim;

import java.nio.file.Path;
import java.util.Calendar;

public class Runner {
    public static void main(String[] args) throws Exception {
        // 1) Load config
        Path cfgPath = Config.resolveFromArgs(args);
        Config cfg = Config.load(cfgPath);

        // 2) CloudSim must be initialized before creating any FogDevice
        CloudSim.init(1, Calendar.getInstance(), false);

        // 3) Build topology (creates FogDevices/Sensors/Actuators)
        var topo = TopologyBuilder.build(cfg);

        // 4) (No Application DAG submission — we drive via ControlLoop)
        //    AppGraphBuilder.build("drleApp", 1); // optional no-op

        // 5) Run our 1 Hz control loop
        new ControlLoop(cfg, topo, cfg.results.run_name, "http://localhost:8000").run();

        System.out.println("[Runner] Simulation done for policy=" + cfg.policy +
                " grid=" + cfg.grid + " run=" + cfg.results.run_name);
    }
}
