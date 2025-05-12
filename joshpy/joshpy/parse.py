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


class ResponseReader:
    """Reads and parses responses from the engine."""
    
    def __init__(self, on_replicate_external):
        """Create a new response reader.
        
        Args:
            on_replicate_external: Callback to invoke when replicates are ready.
        """
        self._replicate_reducer = {}  # Map equivalent in Python
        self._complete_replicates = []
        self._on_replicate_external = on_replicate_external
        self._buffer = ""
        self._completed_replicates = 0

    def process_response(self, text: str) -> None:
        """Parse a response into OutputDatum and SimulationResult objects.
        
        Args:
            text: The text returned by the engine where the simulation is executing.
        """
        self._buffer += text
        lines = self._buffer.split("\n")
        self._buffer = lines.pop()

        for line in (x.strip() for x in lines):
            if not line:
                continue
                
            intermediate = parse_engine_response(line)
            
            if intermediate["type"] == "datum":
                replicate = intermediate["replicate"]
                if replicate not in self._replicate_reducer:
                    self._replicate_reducer[replicate] = SimulationResultBuilder()
                
                raw_input = intermediate["datum"]
                parsed = OutputDatum(raw_input["target"], raw_input["attributes"])
                self._replicate_reducer[replicate].add(parsed)
                
            elif intermediate["type"] == "end":
                self._completed_replicates += 1
                replicate = intermediate["replicate"]
                self._complete_replicates.append(
                    self._replicate_reducer[replicate].build()
                )
                self._on_replicate_external(self._completed_replicates)

    def get_buffer(self) -> str:
        """Get the buffer of response data not yet processed.
        
        Returns:
            Data waiting to be processed.
        """
        return self._buffer

    def get_complete_replicates(self) -> list:
        """Get a listing of all completed replicates.
        
        Returns:
            Result from each replicate as an individual element in the resulting array.
        """
        return self._complete_replicates


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
