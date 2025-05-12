"""Utilties for parsing certain strings returned from the engine.

License: BSD-3-Clause
"""

class EngineValue:
  """Value returned by the engine."""

  def __init__(self, value: float, units: str):
    """Create a new engine value record.

    Args:
      value: The numeric value.
      units: The description of the units for this value like degrees.
    """
    self._value = value
    self._units = units

  def get_value(self) -> float:
    """Get the numeric portion of this engine value.

    Returns:
      The numeric value.
    """
    return self._value

  def get_units(self) -> str:
    """Get the units portion of this engine value.

    Returns:
      The description of the units for this value like degrees.
    """
    return self._units


class StartEndString:
  """Description of a start or end string."""

  def __init__(self, longitude: EngineValue, latitude: EngineValue):
    """Create a new point parsed from a start or end string.

    Args:
      longitude: The horizontal component.
      latitude: The vertical component.
    """
    self._longitude = longitude
    self._laitutde = latitude

  def get_longitude(self) -> EngineValue:
    """Get the longitude parsed from the engine-returned string.

    Returns:
      The horizontal component.
    """
    return self._longitude

  def get_latitude(self) -> EngineValue:
    """Get the latitude parsed from the engine-returned string.

    Returns:
      The vertical component.
    """
    return self._laitutde


def parse_engine_value_string(target: str) -> EngineValue:
  """Parse an EngineValue returned from the engine.
  
  Parse an EngineValue returned from the engine which is in the string of form like follows without
  quotes: "30 m".

  Args:
    target: The string to parse as an EngineValue.

  Returns:
    Parsed EngineValue.
  """
  parts = target.strip().split(' ', 1)
  if len(parts) != 2:
    raise ValueError(f"Invalid engine value string format: {target}")
  value = float(parts[0])
  units = parts[1]
  return EngineValue(value, units)


def parse_start_end_string(target: str) -> StartEndString:
  """Parse a start or an end string.

  Parse a start or end string which may be like the following without quotes:
  "36.51947777043374 degrees latitude, -118.67203360913730 degrees longitude"

  Returns:
    Parsed version of the string.
  """
  parts = target.strip().split(',')
  if len(parts) != 2:
    raise ValueError(f"Invalid start/end string format: {target}")
    
  first_parts = parts[0].strip().split(' ')
  second_parts = parts[1].strip().split(' ')
  
  if len(first_parts) < 3 or len(second_parts) < 3:
    raise ValueError(f"Invalid coordinate format in: {target}")
    
  first_is_latitude = 'latitude' in first_parts[2]
  
  if first_is_latitude:
    latitude = EngineValue(float(first_parts[0]), first_parts[1])
    longitude = EngineValue(float(second_parts[0]), second_parts[1])
  else:
    longitude = EngineValue(float(first_parts[0]), first_parts[1])
    latitude = EngineValue(float(second_parts[0]), second_parts[1])
  
  return StartEndString(longitude, latitude)
