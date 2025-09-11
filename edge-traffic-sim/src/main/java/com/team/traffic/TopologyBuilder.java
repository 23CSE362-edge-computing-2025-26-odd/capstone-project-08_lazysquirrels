package com.team.traffic;

import com.team.traffic.util.Config;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogUtils;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

/**
 * Cloud -> (optional ES2) -> ES1 -> grid[NxN] controllers, each with a 1 Hz sensor + actuator.
 */
public class TopologyBuilder {

    /** Bundle returned to Runner / placements */
    public static class Topo {
        public final List<FogDevice> fogDevices = new ArrayList<>();
        public final List<Sensor> sensors = new ArrayList<>();
        public final List<Actuator> actuators = new ArrayList<>();
        public final Map<String, String> moduleHints = new HashMap<>();
        public final int gridN;
        public final Config cfg;
        public final String es1Name;
        public final String es2Name; // null if disabled

        public Topo(int gridN, Config cfg, String es1Name, String es2Name) {
            this.gridN = gridN; this.cfg = cfg; this.es1Name = es1Name; this.es2Name = es2Name;
        }
    }

    public static Topo build(Config cfg) throws Exception {
        final int grid = cfg.grid;
        final int userId = 1;
        final String appId = "drleApp";

        final String cloudName = "cloud";
        final String es2Name  = cfg.tiers.include_es2 ? "es2-0" : null;
        final String es1Name  = "es1-0";

        Topo topo = new Topo(grid, cfg, es1Name, es2Name);

        // --- Cloud ---
        FogDevice cloud = createFogDevice(
                cloudName, cfg.devices.cloud.mips, cfg.devices.cloud.ram,
                cfg.devices.cloud.upBw, cfg.devices.cloud.downBw, cfg.devices.cloud.ratePerMips
        );
        topo.fogDevices.add(cloud);

        // --- ES2 (optional) ---
        FogDevice es2 = null;
        if (cfg.tiers.include_es2) {
            es2 = createFogDevice(
                    es2Name, cfg.devices.es2.mips, cfg.devices.es2.ram,
                    cfg.devices.es2.upBw, cfg.devices.es2.downBw, cfg.devices.es2.ratePerMips
            );
            es2.setParentId(cloud.getId());
            es2.setUplinkLatency(cfg.tiers.es2_to_cloud_latency_ms);
            topo.fogDevices.add(es2);
        }

        // --- ES1 ---
        FogDevice es1 = createFogDevice(
                es1Name, cfg.devices.es1.mips, cfg.devices.es1.ram,
                cfg.devices.es1.upBw, cfg.devices.es1.downBw, cfg.devices.es1.ratePerMips
        );
        es1.setParentId(es2 != null ? es2.getId() : cloud.getId());
        es1.setUplinkLatency(es2 != null ? cfg.tiers.es2_to_es1_latency_ms : cfg.tiers.es2_to_cloud_latency_ms);
        topo.fogDevices.add(es1);

        // --- Controllers + I/O per intersection ---
        for (int i = 0; i < grid; i++) {
            for (int j = 0; j < grid; j++) {
                String ctrlName = "ctrl-" + i + "-" + j;

                FogDevice ctrl = createFogDevice(
                        ctrlName, cfg.devices.controller.mips, cfg.devices.controller.ram,
                        cfg.devices.controller.upBw, cfg.devices.controller.downBw, cfg.devices.controller.ratePerMips
                );
                ctrl.setParentId(es1.getId());
                ctrl.setUplinkLatency(Math.max(1, (int)Math.round(cfg.network.uplink_ms * 0.07))); // small local hop
                topo.fogDevices.add(ctrl);

                // Sensor @ 1 Hz
                Sensor s = new Sensor(
                        "sens-" + i + "-" + j,
                        "VEH_RAW",
                        userId,
                        appId,
                        new DeterministicDistribution(cfg.app.sensor_period_ms)
                );
                s.setGatewayDeviceId(ctrl.getId());
                s.setLatency(1.0);
                topo.sensors.add(s);

                // Actuator
                Actuator a = new Actuator(
                        "act-" + i + "-" + j,
                        userId,
                        appId,
                        "LIGHT_APPLY"
                );
                a.setGatewayDeviceId(ctrl.getId());
                a.setLatency(1.0);
                topo.actuators.add(a);
            }
        }

        // Placement hints
        topo.moduleHints.put("Preprocess", es1Name);
        topo.moduleHints.put("RL_Agent",  es1Name);

        return topo;
    }

    /** Creates a FogDevice using iFogSim2 "Variant B" constructor (with FogDeviceCharacteristics). */
    private static FogDevice createFogDevice(String name,
                                             long mips,
                                             int ram,
                                             long upBw,
                                             long downBw,
                                             double ratePerMips) throws Exception {

        // 1) One PE
        List<Pe> peList = new ArrayList<>();
        peList.add(new Pe(0, new PeProvisionerSimple(mips)));

        // 2) Power host with stream operator scheduler
        int hostId = FogUtils.generateEntityId();
        long storageMB = 10_000L;
        long hostBw    = 10_000L;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerSimple((int) hostBw),
                storageMB,
                peList,
                new StreamOperatorScheduler(peList),
                new PowerModelLinear(87.5, 0.70) // maxPower, static%
        );

        List<Host> hostList = new ArrayList<>();
        hostList.add(host);

        // 3) Characteristics object (required by your FogDevice ctor)
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                "x86", "Linux", "Xen", host,
                10.0, 3.0, 0.05, 0.001, 0.0
        );

        // 4) Storage + VM allocation policy
        List<Storage> storageList = new LinkedList<>();
        VmAllocationPolicy vmPolicy = new VmAllocationPolicySimple(hostList);

        // 5) Build device (note all bandwidths must be double here)
double schedulingInterval = 1.0;
double busyPower = 87.5; // keep a single power param; no idlePower in this ctor

return new FogDevice(
        name,
        characteristics,
        vmPolicy,
        storageList,
        schedulingInterval,
        (double) upBw,
        (double) downBw,
        ratePerMips,
        busyPower
);

    }

    public static void applyNetworkDelays(Object unused, Config cfg) {
        // optional: adjust per-link delays inside the app graph later
    }
}
