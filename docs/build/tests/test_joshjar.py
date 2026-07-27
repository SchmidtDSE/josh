"""Tests for the seam between the builder and the engine."""

import pytest

from joshdocs.joshjar import CommandResult, JarUnavailable, JoshJar, may_read_externals


def test_a_missing_jar_names_the_build_command(tmp_path):
    with pytest.raises(JarUnavailable, match="gradlew fatJar"):
        JoshJar(tmp_path / "absent.jar")


def test_result_ok_reflects_the_exit_status():
    assert CommandResult(["x"], 0, "").ok
    assert not CommandResult(["x"], 3, "").ok
    assert not CommandResult(["x"], None, "timed out").ok


def test_first_error_line_skips_blanks():
    result = CommandResult(["x"], 1, "\n\n  line 1:0 mismatched input  \nmore\n")
    assert result.first_error_line() == "line 1:0 mismatched input"


def test_first_error_line_without_output():
    assert CommandResult(["x"], 1, "").first_error_line() == "(no output)"


def test_a_model_mentioning_external_goes_to_the_parser():
    # Commented out, so the parser will report nothing -- but only the parser can know that.
    assert may_read_externals("start patch Default\n  # value.init = sample external Data\n")


def test_a_model_mentioning_import_goes_to_the_parser():
    # The read may live in the imported file, which the command flattens in.
    assert may_read_externals('import "shared.josh"\n')


def test_a_model_mentioning_neither_is_provably_empty():
    assert not may_read_externals("start simulation Main\n  grid.size = 10 m\nend simulation\n")
