"""Implementation of JoshBackend for working with a remote Josh sever.

License: BSD-3-Clause
"""

import typing

import joshpy.definitions
import joshpy.metadata
import joshpy.parse
import joshpy.strategy

PUBLIC_DEFAULT_ENDPOINT = ''


class ParseResult:
  """Result of invoking the parse endpoint."""

  def __init__(self, error: typing.Optional[str], simulation_names: typing.List[str],
      metadata: typing.Optional[joshpy.metadata.SimulationMetadata]):
    """Create a new record of a parse result.

    Args:
      error: The error if one encountered in interpretation or None if no error encountered.
      simulation_names: List of simulation names found in the code or empty if error encountered.
      metadata: The metadata found for the requested simulation if simulation requested and no
        errors encountered.
    """
    self._error = error
    self._simulation_names = simulation_names
    self._metadata = metadata

  def get_error(self) -> typing.Optional[str]:
    """Get the error encountered if any.

    Returns:
      The error if one encountered in interpretation or None if no error encountered.
    """
    return self._error

  def get_simulation_names(self) -> typing.List[str]:
    """Get the simulations found in the code sent to the parse endpoint.

    Returns:
      List of simulation names found in the code or empty if error encountered.
    """
    return self._simulation_names

  def get_metadata(self) -> typing.Optional[joshpy.metadata.SimulationMetadata]:
    """Get the simulation metadata found in the code.

    Returns:
      typing.Optional[joshpy.metadata.SimulationMetadata]: The metadata found for the requested
        simulation if simulation requested and no errors encountered.
    """
    return self._metadata


class RemoteJoshDecorator(joshpy.strategy.JoshBackend):
  """Implementation of JoshBackend which uses a remote Josh to run simulations."""

  def __init__(self, server: typing.Optional[str] = None, api_key: typing.Optional[str] = None):
    """Load a new copy of the WASM Josh backend.

    Args:
      server: The endpoint at which the Josh server will be found. If not provided, will use the
        public default
      api_key: The API key to use in communication with the Josh server. If not provided, will use
        an empty string.
    """
    self._server = PUBLIC_DEFAULT_ENDPOINT if server is None else server
    self._api_key = '' if api_key is None else api_key

  def get_error(self, code: str) -> typing.Optional[str]:
    raise NotImplementedError('Not yet implemented.')

  def get_simulations(self, code: str) -> typing.List[str]:
    raise NotImplementedError('Not yet implemented.')

  def get_metadata(self, code: str, name: str) -> joshpy.metadata.SimulationMetadata:
    raise NotImplementedError('Not yet implemented.')

  def run_simulation(self, code: str, name: str,
      virtual_files: joshpy.definitions.FlatFiles,
      replicates: int) -> joshpy.definitions.SimulationResults:
    raise NotImplementedError('Not yet implemented.')

  def _parse_simulation(self, code: str, name: typing.Optional[str] = None) -> ParseResult:
    """Try parsing a simulation.

    Try parsing a simulation and return results from the remote. If simulation was not parsed
    successfully, returns a result with the error indicated and empty simulation names and empty
    metadata. If simulation parsed successfully, the error is indicated as None and a list of
    simulation names are provided.

    If a name is provided and parsing is successful, the metadata for the simulation of that name
    parsed and returned if the start / end is provided in degrees and the size of the  simulation is
    in meters (m, meter, meters). The other attributes are calculated through joshpy.geocode.

    Args:
      code: The code to be parsed.
      name: The name of the simulation for which metadata should be returned or None if no metadata
        should be returned.

    Returns:
      Result of parsing with error information or simulation information.
    """
    import requests

    # Prepare request data
    form_data = {'code': code}
    if name is not None:
      form_data['name'] = name

    # Make request to parse endpoint
    response = requests.post(
      f"{self._server}/parse",
      data=form_data,
      headers={'X-API-Key': self._api_key}
    )

    if response.status_code != 200:
      return ParseResult(f"Parse request failed with status {response.status_code}", [], None)

    # Parse response which is tab-delimited: status, simulation_names_csv, grid_info
    parts = response.text.split('\t')

    if len(parts) < 2:
      return ParseResult("Invalid response format from server", [], None)

    status = parts[0]
    if status != 'success':
      return ParseResult(status, [], None)

    simulation_names = parts[1].split(',') if parts[1] else []

    metadata = None
    if len(parts) > 2 and parts[2] and name is not None:
      try:
        # Parse grid info which is: start,end,size units
        grid_parts = parts[2].split(' ')
        if len(grid_parts) == 2:
          coords = grid_parts[0].split(',')
          if len(coords) == 2:
            start = joshpy.parse.parse_start_end_string(coords[0])
            end = joshpy.parse.parse_start_end_string(coords[1])
            size = joshpy.parse.parse_engine_value_string(grid_parts[1])

            if size.get_units() in ['m', 'meter', 'meters']:
              metadata = joshpy.metadata.SimulationMetadata(
                start_x=0, start_y=0,
                end_x=abs(end.get_longitude().get_value() - start.get_longitude().get_value()),
                end_y=abs(end.get_latitude().get_value() - start.get_latitude().get_value()),
                patch_size=size.get_value(),
                min_longitude=min(start.get_longitude().get_value(), end.get_longitude().get_value()),
                max_longitude=max(start.get_longitude().get_value(), end.get_longitude().get_value()),
                min_latitude=min(start.get_latitude().get_value(), end.get_latitude().get_value()),
                max_latitude=max(start.get_latitude().get_value(), end.get_latitude().get_value())
              )
      except Exception as e:
        return ParseResult(str(e), simulation_names, None)

    return ParseResult(None, simulation_names, metadata)