"""Implementation of JoshBackend for working with a remote Josh sever.

License: BSD-3-Clause
"""

import typing

import requests

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
    raise NotImplementedError('Not implemented yet.')

  def _parse_metadata(self, target: str) -> joshpy.metadata.SimulationMetadata:
    """Parse the string returned from the server describing the metadata for a simulation.

    Args:
      target: The string section returned from the server, specifically the third value after the
        two tab characters.

    Returns:
      joshpy.metadata.SimulationMetadata: Parsed simulation metadata.
    """
    raise NotImplementedError('Not implemented yet.')
