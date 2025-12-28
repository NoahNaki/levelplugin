package me.nakilex.levelplugin.waypoints.engine.pathfinder.processing;

import me.nakilex.levelplugin.waypoints.api.pathing.configuration.PathfinderConfiguration;
import me.nakilex.levelplugin.waypoints.api.pathing.context.EnvironmentContext;
import me.nakilex.levelplugin.waypoints.api.pathing.processing.context.SearchContext;
import me.nakilex.levelplugin.waypoints.api.provider.NavigationPointProvider;
import me.nakilex.levelplugin.waypoints.api.wrapper.PathPosition;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SearchContextImpl implements SearchContext {

  private final PathPosition startPathPosition;
  private final PathPosition targetPathPosition;
  private final PathfinderConfiguration pathfinderConfiguration;
  private final NavigationPointProvider navigationPointProvider;
  private final Map<String, Object> sharedData;
  private final EnvironmentContext environmentContext;

  public SearchContextImpl(
      PathPosition startPathPosition,
      PathPosition targetPathPosition,
      PathfinderConfiguration pathfinderConfiguration,
      NavigationPointProvider navigationPointProvider,
      EnvironmentContext environmentContext) {

    this.startPathPosition =
        Objects.requireNonNull(startPathPosition, "startPathPosition must not be null");
    this.targetPathPosition =
        Objects.requireNonNull(targetPathPosition, "targetPathPosition must not be null");
    this.pathfinderConfiguration =
        Objects.requireNonNull(pathfinderConfiguration, "pathfinderConfiguration must not be null");
    this.navigationPointProvider =
        Objects.requireNonNull(navigationPointProvider, "navigationPointProvider must not be null");
    this.sharedData = new HashMap<>();
    this.environmentContext = environmentContext;
  }

  @Override
  public PathPosition getStartPathPosition() {
    return startPathPosition;
  }

  @Override
  public PathPosition getTargetPathPosition() {
    return targetPathPosition;
  }

  @Override
  public PathfinderConfiguration getPathfinderConfiguration() {
    return pathfinderConfiguration;
  }

  @Override
  public NavigationPointProvider getNavigationPointProvider() {
    return navigationPointProvider;
  }

  @Override
  public Map<String, Object> getSharedData() {
    return sharedData;
  }

  @Override
  public EnvironmentContext getEnvironmentContext() {
    return environmentContext;
  }
}
