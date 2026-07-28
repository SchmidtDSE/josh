"""Tests for the seam between the builder and the engine."""

import pytest
from joshpy.cli import CLIResult

from joshdocs.joshjar import JarUnavailable, JoshJar, first_error_line, may_read_externals


def result(exit_code, stdout="", stderr=""):
    return CLIResult(exit_code=exit_code, stdout=stdout, stderr=stderr, command=["java"])


def test_a_missing_jar_names_the_build_command(tmp_path):
    with pytest.raises(JarUnavailable, match="gradlew fatJar"):
        JoshJar(tmp_path / "absent.jar")


def test_first_error_line_skips_blanks():
    assert first_error_line(result(1, "\n\n  line 1:0 mismatched input  \nmore\n")) == (
        "line 1:0 mismatched input"
    )


def test_first_error_line_reads_stderr_when_stdout_is_empty():
    assert first_error_line(result(1, "", "Found errors in Josh code")) == (
        "Found errors in Josh code"
    )


def test_first_error_line_without_output():
    assert first_error_line(result(1)) == "(no output)"


def test_a_failed_invocation_is_not_reported_as_an_invalid_model(tmp_path):
    # joshpy turns a timeout or a missing `java` into a negative exit code rather than an
    # exception. Blaming the author's model for that would be a lie.
    jar = tmp_path / "present.jar"
    jar.write_bytes(b"")
    wrapper = JoshJar(jar)
    with pytest.raises(JarUnavailable, match="could not run the jar"):
        wrapper._check_invocation(result(-1, "", "Command timed out after 120 seconds"), jar)


def test_a_model_mentioning_external_goes_to_the_parser():
    # Commented out, so the parser will report nothing -- but only the parser can know that.
    assert may_read_externals("start patch Default\n  # value.init = sample external Data\n")


def test_a_model_mentioning_import_goes_to_the_parser():
    # The read may live in the imported file, which the command flattens in.
    assert may_read_externals('import "shared.josh"\n')


def test_a_model_mentioning_neither_is_provably_empty():
    assert not may_read_externals("start simulation Main\n  grid.size = 10 m\nend simulation\n")
