package com.team.traffic.util;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public String policy = "TIMER";
    public int grid = 5;
    public int duration_s = 3600;

    public App app = new App();
    public Network network = new Network();
    public Tiers tiers = new Tiers();
    public Devices devices = new Devices();
    public Drle drle = new Drle();
    public Results results = new Results();

    public static class App {
        public int sensor_period_ms = 1000;
        public double min_green_s = 3.0;
        public double yellow_s = 2.0;
    }

    public static class Network {
        public int uplink_ms = 110;
        public int downlink_ms = 106;
    }

    public static class Tiers {
        public boolean include_es2 = true;
        public int es2_to_es1_latency_ms = 8;
        public int es2_to_cloud_latency_ms = 30;
    }

    public static class DeviceSpec {
        public long mips;
        public int ram;
        public long upBw;
        public long downBw;
        public double ratePerMips;
    }

    public static class Devices {
        public DeviceSpec cloud = defCloud();
        public DeviceSpec es2   = defEs2();
        public DeviceSpec es1   = defEs1();
        public DeviceSpec controller = defCtrl();

        private static DeviceSpec defCloud(){
            DeviceSpec d = new DeviceSpec();
            d.mips = 44800; d.ram = 40000; d.upBw = 10000; d.downBw = 10000; d.ratePerMips = 0.01;
            return d;
        }
        private static DeviceSpec defEs2(){
            DeviceSpec d = new DeviceSpec();
            d.mips = 12000; d.ram = 16000; d.upBw = 10000; d.downBw = 10000; d.ratePerMips = 0.0;
            return d;
        }
        private static DeviceSpec defEs1(){
            DeviceSpec d = new DeviceSpec();
            d.mips = 8000; d.ram = 8192; d.upBw = 10000; d.downBw = 10000; d.ratePerMips = 0.0;
            return d;
        }
        private static DeviceSpec defCtrl(){
            DeviceSpec d = new DeviceSpec();
            d.mips = 1000; d.ram = 1024; d.upBw = 1000; d.downBw = 1000; d.ratePerMips = 0.0;
            return d;
        }
    }

    public static class Drle {
        public String mode = "heuristic"; // heuristic | http
        public String mlEndpoint = "http://localhost:8000";
        public int http_timeout_ms = 150;
    }

    public static class Results {
        public String dir = "results";
        public String run_name = "base-5x5";
    }

    // ---------- Loader ----------
    public static Config load(Path yamlPath) throws Exception {
        if (!Files.exists(yamlPath)) {
            throw new IllegalArgumentException("Config not found: " + yamlPath);
        }
        try (InputStream in = Files.newInputStream(yamlPath)) {
            LoaderOptions lo = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(Config.class, lo));
            Config cfg = yaml.load(in);
            if (cfg == null) throw new IllegalStateException("Empty config file: " + yamlPath);
            return cfg;
        }
    }

    public static Path resolveFromArgs(String[] args) {
        // Look for: --cfg path/to.yaml
        if (args != null) {
            for (int i = 0; i < args.length - 1; i++) {
                if ("--cfg".equals(args[i])) return Path.of(args[i + 1]);
            }
        }
        return Path.of("configs", "base-5x5.yaml");
    }
}
