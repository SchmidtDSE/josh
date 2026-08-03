"""The one seam between the docs builder and the engine.

Every question about Josh semantics is answered by the jar, never by a regular expression here: a
naive scan for ``external`` in ``test_external_netcdf_temperature.josh`` reports two data sets that
do not exist, because that file's external block is entirely commented out. The parser reports none.

The subprocess plumbing itself belongs to :mod:`joshpy`, this engine's Python interface, so the docs
builder and joshpy cannot drift in how they spell a command line. What stays here is the part that
joshpy should not own: the harvest's provably-empty shortcut, one timeout and one jar for the whole
run, and error text aimed at someone writing documentation rather than someone writing Python.

Keeping this in one module makes the boundary greppable. If a caller elsewhere in the package needs
to know something about a model, it belongs behind a method here and, when the jar cannot answer it
yet, behind an ``inspect-*`` command added to the jar and wrapped in joshpy.
"""

from __future__ import annotations

from pathlib import Path

from joshpy.cli import CLIResult, InspectExternalsConfig, JoshCLI, ValidateConfig

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


def first_error_line(result: CLIResult) -> str:
    """Return the most useful single line of a result for an author-facing message.

    A parse failure is reported over two lines -- a ``Found errors in Josh code at <path>:`` header
    and then ``- <path>, line N: <what went wrong>`` -- so the header alone names the file the
    author already knows about and none of the problem. Where a detail line follows, that is the
    line worth reporting.

    Args:
        result: The outcome of one jar invocation.

    Returns:
        The most informative non-blank line of output, or a placeholder when there was none.
    """
    lines = [line.strip() for line in (result.stdout + result.stderr).splitlines() if line.strip()]
    if not lines:
        return "(no output)"
    if lines[0].endswith(":") and len(lines) > 1:
        return lines[1].lstrip("- ").strip()
    return lines[0]


class JoshJar:
    """Asks the engine the questions a harvest needs answered.

    A thin adapter over :class:`joshpy.cli.JoshCLI`, which owns the command lines. This fixes the
    jar and the timeout for a whole run so callers do not repeat them, and turns joshpy's failures
    into the one exception this package reports.

    Instances are safe to share across threads: the underlying client holds no state once
    constructed, and each call is its own subprocess.

    Attributes:
        jar: Path to the fat jar.
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
        try:
            # auto_download=False is not a detail: joshpy defaults to fetching a published jar, and
            # a docs build must check the jar it was handed rather than one that may disagree with
            # the working tree.
            self._cli = JoshCLI(josh_jar=jar, java_path=java, auto_download=False)
        except FileNotFoundError as exc:
            raise JarUnavailable(
                f"{jar} not found: build it with './gradlew fatJar', or pass --skip-jar to harvest "
                "without validating models"
            ) from exc
        self.jar = jar
        self.timeout = timeout

    def validate(self, path: Path) -> CLIResult:
        """Check that a model parses and interprets.

        Args:
            path: The ``.josh`` file to validate.

        Returns:
            The result; a nonzero exit code means the model was rejected.

        Raises:
            JarUnavailable: If the invocation itself failed rather than the model.
        """
        result = self._cli.validate(ValidateConfig(script=path), timeout=self.timeout)
        self._check_invocation(result, path)
        return result

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

        try:
            return self._cli.inspect_externals(
                InspectExternalsConfig(entry=path), timeout=self.timeout
            )
        # JSONDecodeError is a ValueError, raised when the command's output is not the JSON it
        # documents -- a jar older than the command, or a jar that printed something else.
        except ValueError as exc:
            raise JarUnavailable(
                f"inspect-externals produced unreadable output for {path}: {exc}"
            ) from exc
        except RuntimeError as exc:
            raise JarUnavailable(f"inspect-externals failed for {path}: {exc}") from exc

    def _check_invocation(self, result: CLIResult, path: Path) -> None:
        """Fail loudly when the jar could not be run at all.

        joshpy reports a timeout, a missing ``java``, or any other OS error as a result with a
        negative exit code rather than as an exception. Left alone that would reach an author as
        "is not valid Josh: [Errno 2] No such file or directory: 'java'", blaming their model for
        the builder's environment.

        Args:
            result: The outcome to check.
            path: The file the invocation was about, for the message.

        Raises:
            JarUnavailable: If the exit code says the process never ran to completion.
        """
        if result.exit_code < 0:
            raise JarUnavailable(
                f"could not run the jar for {path}: {first_error_line(result)}"
            )
