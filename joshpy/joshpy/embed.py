"""Implementation of JoshBackend for working with a WASM embed of Josh.

License: BSD-3-Clause
"""

import typing

import joshpy.definitions
import joshpy.strategy


class EmbeddedJoshServer(joshpy.strategy.JoshBackend):
  """Implementation of JoshBackend which uses an embedded WASM copy of Josh to operate."""

  def __init__(self):
    """Load a new copy of the WASM Josh backend."""

  def get_error(self, code: str) -> typing.Optional[str]:
    raise NotImplementedError('Not yet implemented.')

  def get_simulations(self, code: str) -> typing.List[str]:
    raise NotImplementedError('Not yet implemented.')

  def get_metadata(self, code: str, name: str) -> joshpy.metadata.SimulationMetadata:
    raise NotImplementedError('Not yet implemented.')

  def run_simulation(self, code: str, name: str,
      virtual_files: joshpy.definitions.FlatFiles) -> joshpy.definitions.SimulationResults:
    raise NotImplementedError('Not yet implemented.')

