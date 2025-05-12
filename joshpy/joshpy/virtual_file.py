"""Structures to support virutal file systems for the Josh sandbox.

License: BSD-3-Clause
"""

import typing


class VirtualFile:
  """Definition of a file occupying a virutal file system.
  
  Definition of a file occupying a virutal file system which can be used in the sandbox Josh
  environment where access to the underlying file system is restricted.
  """

  def __init__(self, name: str, content: str, is_binary: bool):
    """Create a new record of a virtual file.
    
    Args:
      name: The name or path of the file which is in the virutal file system.
      content: The content of the file which is base64 encoded if the file is binary or plain text
        otherwise.
      is_binary: Flag indicating if the file is binary or plain text. If true, then the file is
        binary and its content is a string holding the base64 encoded version of its contents. If
        false, the file is plain text.
    """
    self._name = name
    self._content = content
    self._is_binary = is_binary
  
  def get_name(self) -> str:
    """Get the name or location of this file in the virtual file system.
    
    Returns:
      str: The name or path of the file which is in the virutal file system.
    """
    return self._name

  def get_content(self) -> str:
    """Get the contents of this file as a string.
    
    Returns:
      str: The content of the file which is base64 encoded if the file is binary or plain text
        otherwise.
    """
    return self._content

  def is_binary(self) -> bool:
    """Determine if this is a binary file.
    
    Returns:
      bool: Flag indicating if the file is binary or plain text. If true, then the file is binary
        and its content is a string holding the base64 encoded version of its contents. If false,
        the file is plain text.
    """
    return self._is_binary


def serialize_files(files: typing.List[VirtualFile]) -> str:
  """Serialize a virutal file system to a string representation.
  
  Args:
    files: The list of files in the virtual file system to be serialized.

  Returns:
    str: The string serialization of the given virtual file system in the format expected by the
      Josh server.
  """
  raise NotImplementedError('Not yet implemented.')