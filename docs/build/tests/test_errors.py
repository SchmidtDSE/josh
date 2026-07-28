"""Tests for problem reporting, which is the builder's whole author-facing surface."""

from pathlib import Path

import pytest

from joshdocs.errors import HarvestFailed, Problem, ProblemLog


def test_a_problem_with_a_line_reports_it():
    problem = Problem(Path("/repo/docs/src/a.md"), "order: expected an integer", 4)
    assert problem.format(Path("/repo")) == "docs/src/a.md:4\n  order: expected an integer"


def test_a_problem_without_a_line_omits_the_position():
    problem = Problem(Path("/repo/docs/src/a.josh"), "has no prose beside it")
    assert problem.format(Path("/repo")) == "docs/src/a.josh\n  has no prose beside it"


def test_a_path_outside_the_root_stays_absolute():
    problem = Problem(Path("/elsewhere/a.md"), "could not be read")
    assert problem.format(Path("/repo")).startswith("/elsewhere/a.md")


def test_the_report_is_ordered_by_file_then_line():
    log = ProblemLog(Path("/repo"))
    log.add(Path("/repo/b.md"), "second file", 1)
    log.add(Path("/repo/a.md"), "later line", 9)
    log.add(Path("/repo/a.md"), "earlier line", 2)

    assert [problem.message for problem in log] == ["earlier line", "later line", "second file"]


def test_the_report_counts_what_it_found():
    log = ProblemLog()
    log.add(Path("a.md"), "one")
    assert log.report().endswith("1 problem found.")
    log.add(Path("b.md"), "two")
    assert log.report().endswith("2 problems found.")


def test_an_empty_log_is_falsey_and_reports_nothing():
    log = ProblemLog()
    assert not log
    assert len(log) == 0
    assert log.report() == ""
    log.raise_if_any()


def test_raise_if_any_carries_the_log():
    log = ProblemLog()
    log.add(Path("a.md"), "one")
    with pytest.raises(HarvestFailed) as caught:
        log.raise_if_any()
    assert caught.value.log is log
