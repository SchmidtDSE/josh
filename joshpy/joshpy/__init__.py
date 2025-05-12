"""Entry point into the Python library for interacting with Josh simulations.

License: BSD-3-Clause
"""

import typing

import joshpy.definitions
import joshpy.embed
import joshpy.geocode
import joshpy.metadata
import joshpy.remote
import joshpy.strategy
import joshpy.virtual_file


class Josh:
  """Entrypoint into the Python library for interacting with Josh simulations.
  
  Client for interacting with Josh simulations through Python, either working with a remote server
  or with a embedded WebAssembly Josh version.
  """

  def __init__(self, server: typing.Optional[str] = None, api_key: typing.Optional[str] = None):
    """Create a new Josh client.

    Create a new Josh client, using an embedded Josh backend if neither API key or server endpoint
    are provided. If one or both are provided, will use a remote backend for eactual execution.
    
    Args:
      server: The endpoint at which the Josh server will be found if in use.
      api_key: The API key to use in communication with the Josh server if in use.
    """
    self._server = server
    self._api_key = api_key

    has_server = self._server is not None
    has_api_key = self._api_key is not None

    self._backend: joshpy.strategy.JoshBackend = joshpy.embed.EmbeddedJoshServer()
    
    if has_server or has_api_key:
      self._backend = joshpy.remote.RemoteJoshDecorator(
        server=self._server,
        api_key=self._api_key,
        inner=self._backend
      )

  def get_error(self, code: str) -> typing.Optional[str]:
    """Get the error in the given Josh code if one is present.
    
    Use the current backend to attempt to interpret the given Josh code and the first error found if
    one is present. Otherwise, return None.

    Returns:
      typing.Optional[str]: The first error found described as a string or None if no errors found.
    """
    return self._backend.get_error(code)

  def get_simulations(self, code: str) -> typing.List[str]:
    """Get the list of simulations found within code if it interprets correctly.

    Get the list of simulations found within code if it interprets correctly using the current Josh
    backend.
    
    Args:
      code: The code to be parsed and from which list of simulation names is to be returned.

    Returns:
      typing.List[str]: List of simulation names found within the code.

    Raises:
      RuntimeError: Raised if the code provided has an error in it. The exception will have the
        string description of the first error.
    """
    return self._backend.get_simulations(code)

  def get_metadata(self, code: str, name: str) -> joshpy.metadata.SimulationMetadata:
    """Get the metadata for a specific simulation.
    
    Use the current Josh backend to parse the given code and return the metadata parsed from that
    simulation including dimensions of the simulation grid.

    Args:
      code: The code from which metadata should be parsed.
      name: The name of the simulation for which metadata should be parsed.

    Returns:
      joshpy.metadata.SimulationMetadata: Metadata object parsed for the requested simulation.

    Raises:
      RuntimeError: Raised if the code provided has an error in it. The exception will have the
        string description of the first error.
      ValueError: Raised if the simulation of the given name cannot be found.
    """
    return self._backend.get_metadata(code, name)

  def run_simulation(self, code: str, name: str, virtual_files: joshpy.definitions.FlatFiles,
        replicates: int = 1, geocode: bool = False) -> joshpy.definitions.SimulationResults:
    """Run a simulation using the current Josh backend.

    Run a simulation through the current Josh backend, printing to the console when each replicate
    is completed. The full result set will be returned with position.x and position.y as well as a
    step attribute along with exported fields. If geocode is enabled, position.longitude and
    position.latitude will be added. Currently only sandboxed execution returning patch export
    values is supported in the Python client.

    Args:
      code: The code to execute.
      name: The name of the simulation from the provided code to execute.
      virtual_files: List of virutal files to provide to the simulation within its sandbox.
      replicates: The number of replicates for which the simulation should run.
      geocode: Flag indicating if geocoding should be provided where true will add
        position.longitude and position.latitude. These will not be added if false.

    Returns:
      joshpy.definitions.SimulationResults: Outer list where each element is a replicate and each
        replicate is a list containing each data point.

    Raises:
      RuntimeError: Raised if the code provided has an error in it. The exception will have the
        string description of the first error.
      ValueError: Raised if the simulation of the given name cannot be found.
    """
    results = self._backend.run_simulation(code, name, virtual_files, replicates)

    if geocode:
      metadata = self._backend.get_metadata(code, name)
      return joshpy.geocode.add_positions(results, metadata)
    else:
      return results
