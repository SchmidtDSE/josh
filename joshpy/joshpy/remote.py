"""Implementation of JoshBackend for working with a remote Josh sever.

License: BSD-3-Clause
"""

import typing

import joshpy.definitions
import joshpy.embed
import joshpy.strategy

PUBLIC_DEFAULT_ENDPOINT = ''


class EmbeddedJoshServer(joshpy.strategy.JoshBackend):
  """Implementation of JoshBackend which uses a remote Josh to run simulations.
  
  Implementation of JoshBackend which uses a remote Josh to run simulations but all other smaller
  operations are delegated to an inner backend. These small operations include checking for errors,
  listing simulations found in a Josh script, and getting simulation metadata. For larger
  operations, an API key and 
  """

  def __init__(self, server: typing.Optional[str] = None, api_key: typing.Optional[str] = None,
        inner: typing.Optional[joshpy.strategy.JoshBackend] = None):
    """Load a new copy of the WASM Josh backend.
    
    Args:
      server: The endpoint at which the Josh server will be found. If not provided, will use the
        public default
      api_key: The API key to use in communication with the Josh server. If not provided, will use
        an empty string.
      inner: The inner backend to which smaller operations will be delegated. If not provided, will
        use a default.
    """
    self._server = PUBLIC_DEFAULT_ENDPOINT if server is None else server
    self._api_key = '' if api_key is None else api_key
    self._inner = joshpy.embed.EmbeddedJoshServer() if inner is None else inner

  def get_error(self, code: str) -> typing.Optional[str]:
    return self._inner.get_error(code)

  def get_simulations(self, code: str) -> typing.List[str]:
    return self._inner.get_simulations(code)

  def get_metadata(self, code: str, name: str) -> joshpy.metadata.SimulationMetadata:
    return self._inner.get_metadata(code, name)

  def run_simulation(self, code: str, name: str,
      virtual_files: joshpy.definitions.FlatFiles) -> joshpy.definitions.SimulationResults:
    raise NotImplementedError('Not yet implemented.')

