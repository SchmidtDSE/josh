"""Implementation of JoshBackend for working with a remote Josh sever.

License: BSD-3-Clause
"""

import typing

import joshpy.definitions
import joshpy.metadata
import joshpy.strategy

PUBLIC_DEFAULT_ENDPOINT = ''


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
    raise NotImplementedError('Not yet implemented.')
    
