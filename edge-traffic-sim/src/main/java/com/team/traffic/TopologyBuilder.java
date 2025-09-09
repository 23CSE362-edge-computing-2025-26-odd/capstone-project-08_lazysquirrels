package com.team.traffic;

import com.team.traffic.util.Config;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.fog.application.Application;
import org.fog.entities.*;
import org.fog.power.models.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

public class TopologyBuilder {

    // Bundle we return so Runner/placements can use everything.
    public static class Topo {
        public final List<FogDevice> fogDevices = new ArrayList<>();
        public final List<Sensor> sensors = new ArrayList<>();
        public final List<Actuator> actuators = new ArrayList<>();
        public final Map<String,String> moduleHints = new HashMap<>();
        public final int gridN;
        public final Config cfg;
        public final String es1Name;
        public final String es2Name; // may be null if disabled
        public Topo(int gridN, Config cfg, String es1Name, String es2Name) {
            this.gridN = gridN; this.cfg = cfg; this.es1Name = es1Name; this.es2Name = es2Name;
        }
    }

    public static Topo build(Config cfg) throws Exception {
        int grid = cfg.grid;
        // Names
        String cloudName = "cloud";
        String es2Name = cfg.tiers.include_es2 ? "es2-0" : null;
        String es1Name = "es1-0";

        Topo topo = new Topo(grid, cfg, es1Name, es2Name);

        // Cloud
        FogDevice cloud = createFogDevice(
                cloudName, cfg.devices.cloud.mips, cfg.devices.cloud.ram,
                cfg.devices.cloud.upBw, cfg.devices.cloud.downBw,
                /*level*/0, cfg.devices.cloud.ratePerMips);
        topo.fogDevices.add(cloud);

        // ES2 (optional)
        FogDevice es2 = null;
        if (cfg.tiers.include_es2) {
            es2 = createFogDevice(
                    es2Name, cfg.devices.es2.mips, cfg.devices.es2.ram,
                    cfg.devices.es2.upBw, cfg.devices.es2.downBw,
                    /*level*/1, cfg.devices.es2.ratePerMips);
            es2.setParentId(cloud.getId());
            es2.setUplinkLatency(cfg.tiers.es2_to_cloud_latency_ms); // ms
            topo.fogDevices.add(es2);
        }

        // ES1
        FogDevice es1 = createFogDevice(
                es1Name, cfg.devices.es1.mips, cfg.devices.es1.ram,
                cfg.devices.es1.upBw, cfg.devices.es1.downBw,
                /*level*/ (cfg.tiers.include_es2 ? 2 : 1), cfg.devices.es1.ratePerMips);
        es1.setParentId((es2 != null) ? es2.getId() : cloud.getId());
        es1.setUplinkLatency((es2 != null) ? cfg.tiers.es2_to_es1_latency_ms : cfg.tiers.es2_to_cloud_latency_ms);
        topo.fogDevices.add(es1);

        // Controllers (N x N), each with a sensor and actuator
        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
                String ctrlName = "ctrl-" + i + "-" + j;
                FogDevice ctrl = createFogDevice(
                        ctrlName, cfg.devices.controller.mips, cfg.devices.controller.ram,
                        cfg.devices.controller.upBw, cfg.devices.controller.downBw,
                        /*level*/ (es2 != null ? 3 : 2), cfg.devices.controller.ratePerMips);
                ctrl.setParentId(es1.getId());
                ctrl.setUplinkLatency( (int)Math.max(1, Math.round(cfg.network.uplink_ms * 0.07)) ); // small local hop (visual)
                topo.fogDevices.add(ctrl);

                // Sensor (1 Hz)
                Sensor s = new Sensor(
                        "sens-"+i+"-"+j,
                        "VEH_RAW",
                        ctrl.getId(),
                        "drleApp",
                        new DeterministicDistribution(cfg.app.sensor_period_ms) // period in ms
                );
                topo.sensors.add(s);

                // Actuator
                Actuator a = new Actuator(
                        "act-"+i+"-"+j,
                        ctrl.getId(),
                        "drleApp",
                        "LIGHT_APPLY"
                );
                topo.actuators.add(a);
            }
        }

        topo.moduleHints.put("Preprocess", es1Name);
        topo.moduleHints.put("RL_Agent",  es1Name);
        // LightCtrl will be mapped per controller device

        return topo;
    }

    // Utility: create a FogDevice with simple single-PE host & linear power model
    private static FogDevice createFogDevice(String name, long mips, int ram, long upBw, long downBw, int level, double ratePerMips) throws Exception {
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        int hostId = FogUtils.generateEntityId();
        long storage = 10_000; // MB
        int bw = 10_000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(87.5, 82.4)
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0);

        FogDevice device = new FogDevice(
                name, characteristics, new AppModuleAllocationPolicy(hostList),
                new LinkedList<>(), upBw, downBw, 0, ratePerMips);
        device.setLevel(level);
        return device;
    }

    public static void applyNetworkDelays(Application app, Config cfg) {
        
    }
}
