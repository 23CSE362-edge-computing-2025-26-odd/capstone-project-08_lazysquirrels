package com.team.traffic;

/**
 * No-op App graph for TIMER/ACTUATED modes.
 * We drive the sim via ControlLoop (features → policy → actuators),
 * so we don't construct an iFogSim Application DAG here.
 */
public class AppGraphBuilder {
  public static Object build(String name, int userId) {
    // Return null; Runner/placements shouldn't depend on an Application object.
    return null;
  }
}
