package com.team.traffic;

import com.team.traffic.util.Config;
import org.fog.application.Application;
import org.fog.entities.Controller;
import org.cloudbus.cloudsim.core.CloudSim;

import java.nio.file.Path;
import java.util.Calendar;

public class Runner {
    public static void main(String[] args) throws Exception {
        // 1) Load config (defaults to configs/base-5x5.yaml)
        Path cfgPath = Config.resolveFromArgs(args);
        Config cfg = Config.load(cfgPath);

        // 2) Init CloudSim/iFogSim2
        CloudSim.init(1, Calendar.getInstance(), false);

        // 3) Build topology + app graph
        var topo = TopologyBuilder.build(cfg);
        Application app = AppGraphBuilder.build("drleApp", 1);

        // 4) Submit app with the selected policy (Timer | Actuated | DRLE)
        Controller controller = new Controller("master", topo.fogDevices, topo.sensors, topo.actuators);

        String p = cfg.policy.trim().toUpperCase();
        switch (p) {
            case "TIMER" -> controller.submitApplication(
                    app, 0,
                    new com.team.traffic.placement.ModulePlacementTimer(topo.fogDevices, app, topo)
            );
            case "ACTUATED" -> controller.submitApplication(
                    app, 0,
                    new com.team.traffic.placement.ModulePlacementActuated(topo.fogDevices, app, topo)
            );
            case "DRLE" -> controller.submitApplication(
                    app, 0,
                    new com.team.traffic.placement.ModulePlacementDrle(topo.fogDevices, app, topo, cfg)
            );
            default -> {
                throw new IllegalArgumentException("Unknown policy: " + cfg.policy +
                        " (use TIMER | ACTUATED | DRLE in your YAML)");
            }
        }

        // 5) Run the sim
        CloudSim.startSimulation();
        CloudSim.stopSimulation();

        System.out.println("[Runner] Simulation done for policy=" + p +
                " grid=" + cfg.grid + " run=" + cfg.results.run_name);
    }
}
