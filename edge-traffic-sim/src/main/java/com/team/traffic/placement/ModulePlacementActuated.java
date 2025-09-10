package com.team.traffic.placement;

import com.team.traffic.TopologyBuilder;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;

import java.util.List;

public class ModulePlacementActuated extends ModulePlacementMapping {
  public ModulePlacementActuated(List<FogDevice> fog, Application app, TopologyBuilder.Topo topo) {
    super(fog, app, mapping(topo));
  }

  private static ModuleMapping mapping(TopologyBuilder.Topo topo) {
    ModuleMapping m = ModuleMapping.createModuleMapping();
    m.addModuleToDevice("Preprocess", topo.moduleHints.get("Preprocess"));
    m.addModuleToDevice("RL_Agent",  topo.moduleHints.get("RL_Agent"));
    for (FogDevice d : topo.fogDevices) {
      if (d.getName().startsWith("ctrl-")) {
        m.addModuleToDevice("LightCtrl", d.getName());
      }
    }
    return m;
  }
}