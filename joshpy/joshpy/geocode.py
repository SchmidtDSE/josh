"""Utilities to convert from grid-space to Earth-space results.

Utilities to convert from grid-space to Earth-space results including a Python-based implmentation
of Haversine to support these operations.

License: BSD-3-Clause
"""

import joshpy.definitions
import joshpy.metadata


def add_positions(results: joshpy.definitions.SimulationResults,
      metadata: joshpy.metadata.SimulationMetadata) -> joshpy.definitions.SimulationResults:
  """Add position.longitude and position.latitude to results.
  
  Under assumption that all replicates share the same metadata, convert from grid-space coordinates
  which report in number of patches or cells to the left and to the bottom of the origin to also
  report on position.longitude and position.latitude in degrees.

  Args:
    results: The results to be modified in place in which position.longitude and position.latitude
      will be added.
    metadata: Metadata about the simulation executed including bounds to be used in geocoding.
      The dictionaries inside will be modified in place to add a position.longitude and
      position.latitude, overwritting prior values if present.

  Returns:
    joshpy.definitions.SimulationResults: The results after modification in place.
  """
  raise NotImplementedError('Not yet implemented.')


class EarthPoint:
  """Internal reprsentation of a geographic point on Earth."""

  def __init__(self, longitude: float, latitude: float):
    """Create a new record of a position on Earth.

    Args:
      longitude: The longitude of this point in degrees.
      latitude: The latitude of this point in degrees.
    """
    self._longitude = longitude
    self._latitude = latitude
  
  def get_longitude(self) -> float:
    """Get the east / west position of this point.
    
    Returns:
      The longitude of this point in degrees where positive numbers are east and negative west.
    """
    return self._longitude
  
  def get_latitude(self) -> float:
    """Get the north / south position of this point.
    
    Returns:
      The latitude of this point in degrees where positive numbers are north and negative south.
    """
    return self._latitude


def get_distance_meters(start: EarthPoint, end: EarthPoint) -> float:
  """Get the distance in meters between two points on Earth.
  
  Args:
    start: The starting point from which distance should be measured.
    end: The ending point to which distance should be measured.

  Returns:
    Distance between start and end in meters.
  """
  raise NotImplementedError('Not yet implemented.')


def get_at_distance_from(start: EarthPoint, distance_meters: float, direction: str) -> EarthPoint:
  """Get a new point which is some distance from a starting point.
  
  Args:
    start: The starting point from which a new point should be derived.
    distance_meters: How far in a cardinal direction the new point should be from the starting
      point.
    direction: The direction as a single letter string like N, S, E, W corresponding to the
      cardinal directions.

  Returns:
    New point which is the given distance in the given direction from the starting point.
  """
  raise NotImplementedError('Not yet implemented.')
