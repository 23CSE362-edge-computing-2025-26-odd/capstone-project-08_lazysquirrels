package com.team.traffic.placement;

import com.team.traffic.TopologyBuilder;
import com.team.traffic.util.Config;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;

import java.util.List;

public class ModulePlacementDrle extends ModulePlacementMapping {
  private final Config cfg;

  public ModulePlacementDrle(List<FogDevice> fog, Application app, TopologyBuilder.Topo topo, Config cfg) {
    super(fog, app, mapping(topo));
    this.cfg = cfg;
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

  public Config getCfg(){ return cfg; }
}