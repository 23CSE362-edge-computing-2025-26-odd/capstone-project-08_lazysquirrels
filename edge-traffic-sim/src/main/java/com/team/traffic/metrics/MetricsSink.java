package com.team.traffic.metrics;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MetricsSink implements Closeable {
  private final PrintWriter out;

  public MetricsSink(String dir, String filename) throws IOException {
    Path d = Paths.get(dir);
    if (!Files.exists(d)) Files.createDirectories(d);
    this.out = new PrintWriter(Files.newBufferedWriter(d.resolve(filename)));
  }

  public void header() {
    out.println("t_s,policy,i,j,desired_axis,applied_axis,e2e_latency_ms,sla_miss,bytes_up,bytes_down,es1_power_w,q_ns,q_ew,halting_pct,mode");
  }

  public synchronized void log(double t, String policy, int i, int j,
                               String desired, String applied,
                               double latencyMs, int slaMiss, long up, long down,
                               double es1PowerW, double qns, double qew, double haltPct,
                               String mode) {
    out.printf("%.3f,%s,%d,%d,%s,%s,%.1f,%d,%d,%d,%.2f,%.3f,%.3f,%.3f,%s%n",
      t, policy, i, j, desired, applied, latencyMs, slaMiss, up, down, es1PowerW, qns, qew, haltPct, mode);
  }

  @Override public void close() {
    out.flush();
    out.close();
  }
}