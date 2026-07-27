"""The one seam between the docs builder and the engine.

Every question about Josh semantics is answered by the jar, never by a regular expression here: a
naive scan for ``external`` in ``test_external_netcdf_temperature.josh`` reports two data sets that
do not exist, because that file's external block is entirely commented out. The parser reports none.

Keeping this in one module makes the boundary greppable. If a caller elsewhere in the package needs
to know something about a model, it belongs behind a method here and, when the jar cannot answer it
yet, behind an ``inspect-*`` command added to the jar.
"""

from __future__ import annotations

import json
import subprocess
from pathlib import Path

#: The fat jar the CLI commands live in, relative to the repo root.
DEFAULT_JAR = Path("build/libs/joshsim-fat.jar")

#: Generous enough for a cold JVM on a loaded CI runner, short enough to fail rather than hang.
DEFAULT_TIMEOUT_SECONDS = 120

#: Tokens that must appear in a model's text before it can possibly read an external data set:
#: `external` for a read in this file, `import` because reads in an imported file count too.
_EXTERNAL_TOKENS = ("external", "import")


class JarUnavailable(Exception):
    """Raised when the jar the builder was pointed at cannot be used."""


def may_read_externals(text: str) -> bool:
    """Return whether a model could possibly read an external data set.

    This is the one text check in the package, and it is sound in the direction that matters. A
    model whose source contains neither ``external`` nor ``import`` cannot name an external resource
    after imports are flattened, so asking the parser is guaranteed to return nothing. Anything that
    mentions either token is sent to the parser, which is the only thing that can tell a live read
    from one inside a comment -- 146 of the 155 conformance tests mention neither, and skipping
    their JVM starts turns a two-minute harvest into a few seconds.

    Args:
        text: The model's source.

    Returns:
        False only when the answer is provably empty.
    """
    return any(token in text for token in _EXTERNAL_TOKENS)


class CommandResult:
    """The outcome of one jar invocation.

    Attributes:
        args: The command line that was run.
        returncode: Exit status, or None when the command timed out.
        output: Combined stdout and stderr.
    """

    __slots__ = ("args", "output", "returncode")

    def __init__(self, args: list[str], returncode: int | None, output: str) -> None:
        """Initialize a result.

        Args:
            args: The command line that was run.
            returncode: Exit status, or None on timeout.
            output: Combined stdout and stderr.
        """
        self.args = args
        self.returncode = returncode
        self.output = output

    @property
    def ok(self) -> bool:
        """Return True when the command exited zero."""
        return self.returncode == 0

    def first_error_line(self) -> str:
        """Return the most useful single line of output for an error message.

        Returns:
            The first non-blank output line, or a placeholder when there was no output.
        """
        for line in self.output.splitlines():
            if line.strip():
                return line.strip()
        return "(no output)"


class JoshJar:
    """Runs the Josh CLI as a subprocess.

    Attributes:
        jar: Path to the fat jar.
        java: The java executable to invoke.
        timeout: Seconds to allow one invocation.
    """

    def __init__(
        self,
        jar: Path = DEFAULT_JAR,
        java: str = "java",
        timeout: int = DEFAULT_TIMEOUT_SECONDS,
    ) -> None:
        """Initialize the wrapper.

        Args:
            jar: Path to the fat jar.
            java: The java executable to invoke.
            timeout: Seconds to allow one invocation.

        Raises:
            JarUnavailable: If the jar does not exist.
        """
        if not jar.is_file():
            raise JarUnavailable(
                f"{jar} not found: build it with './gradlew fatJar', or pass --skip-jar to harvest "
                "without validating models"
            )
        self.jar = jar
        self.java = java
        self.timeout = timeout

    def run(self, *args: str) -> CommandResult:
        """Invoke a Josh CLI subcommand.

        Args:
            *args: The subcommand and its arguments.

        Returns:
            The result, with a None return code when the invocation timed out.
        """
        command = [self.java, "-jar", str(self.jar), *args]
        try:
            completed = subprocess.run(
                command,
                capture_output=True,
                text=True,
                timeout=self.timeout,
                check=False,
            )
        except subprocess.TimeoutExpired:
            return CommandResult(command, None, f"timed out after {self.timeout}s")
        return CommandResult(command, completed.returncode, completed.stdout + completed.stderr)

    def validate(self, path: Path) -> CommandResult:
        """Check that a model parses and interprets.

        Args:
            path: The ``.josh`` file to validate.

        Returns:
            The result; a nonzero return code means the model was rejected.
        """
        return self.run("validate", str(path))

    def inspect_externals(self, path: Path) -> list[str]:
        """List the external data sets a model reads.

        Imports are flattened by the command, so a name read only from an imported file is included.

        Most models read nothing, and a JVM start costs about a second each. Where the answer is
        provably empty the start is skipped: see :func:`may_read_externals`. Every model that could
        read something still goes to the parser, which is the only thing that can tell a real read
        from one inside a comment.

        Args:
            path: The entry ``.josh`` file.

        Returns:
            The external names, sorted, as the command reports them.

        Raises:
            JarUnavailable: If the command failed, or produced output that is not the JSON the
                command documents.
        """
        try:
            if not may_read_externals(path.read_text(encoding="utf-8")):
                return []
        except OSError:
            # Let the command report an unreadable file, so there is one error path rather than two.
            pass

        # `--json` is the default; it is passed explicitly because it now means what it says on all
        # three inspect commands (it used to select plain text on two of them).
        result = self.run("inspect-externals", str(path), "--json")
        if not result.ok:
            raise JarUnavailable(
                f"inspect-externals failed for {path}: {result.first_error_line()}"
            )
        try:
            payload = json.loads(result.output)
        except json.JSONDecodeError as exc:
            raise JarUnavailable(
                f"inspect-externals produced unreadable output for {path}: {exc}"
            ) from exc
        externals = payload.get("externals", [])
        return [str(name) for name in externals]
