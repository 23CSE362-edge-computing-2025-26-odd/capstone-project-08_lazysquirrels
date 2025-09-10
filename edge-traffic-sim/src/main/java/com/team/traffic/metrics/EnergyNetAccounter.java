package com.team.traffic.metrics;

/**
 * Extremely coarse energy/network estimator for ES1:
 * - Assume ~45W active draw baseline, tiny increase with intersection count.
 */
public class EnergyNetAccounter {
  public double estimateEs1Watts(int intersections) {
    return 45.0 + 0.01 * intersections; // toy model
  }
}