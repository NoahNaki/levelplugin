package me.nakilex.levelplugin.waypoints.engine.result;

import me.nakilex.levelplugin.waypoints.api.pathing.result.Path;
import me.nakilex.levelplugin.waypoints.api.pathing.result.PathState;
import me.nakilex.levelplugin.waypoints.api.pathing.result.PathfinderResult;

public class PathfinderResultImpl implements PathfinderResult {

  private final PathState pathState;
  private final Path path;

  public PathfinderResultImpl(PathState pathState, Path path) {
    this.pathState = pathState;
    this.path = path;
  }

  @Override
  public boolean successful() {
    return pathState == PathState.FOUND;
  }

  @Override
  public boolean hasFailed() {
    return pathState == PathState.FAILED
        || pathState == PathState.LENGTH_LIMITED
        || pathState == PathState.MAX_ITERATIONS_REACHED;
  }

  @Override
  public boolean hasFallenBack() {
    return pathState == PathState.FALLBACK;
  }

  @Override
  public PathState getPathState() {
    return this.pathState;
  }

  @Override
  public Path getPath() {
    return this.path;
  }
}
