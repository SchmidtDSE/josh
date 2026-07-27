"""Tests for the command line entry point, which is what CI calls."""

import json

import pytest

from joshdocs.__main__ import EXIT_OK, EXIT_PROBLEMS, EXIT_UNUSABLE, main

MODEL = "start simulation Main\n\n  grid.size = 10 m\n\nend simulation\n"


def make_unit(src, stem, front_matter):
    src.mkdir(parents=True, exist_ok=True)
    (src / f"{stem}.josh").write_text(MODEL, encoding="utf-8")
    (src / f"{stem}.md").write_text(f"---\n{front_matter}\n---\n\nProse.\n", encoding="utf-8")


def harvest_argv(tmp_path, *extra):
    return [
        "harvest",
        "--root",
        str(tmp_path),
        "--src",
        str(tmp_path / "docs" / "src" / "recipes"),
        "--no-tests",
        "--skip-jar",
        "--manifest",
        str(tmp_path / "manifest.json"),
        "--emit-runnable",
        str(tmp_path / "runnable"),
        *extra,
    ]


def test_clean_harvest_writes_the_manifest_and_exits_zero(tmp_path, capsys):
    make_unit(tmp_path / "docs" / "src" / "recipes", "wind", 'title: "Wind"\nkind: recipe')

    assert main(harvest_argv(tmp_path)) == EXIT_OK
    payload = json.loads((tmp_path / "manifest.json").read_text(encoding="utf-8"))
    assert payload["counts"]["total"] == 1
    assert "1 units" in capsys.readouterr().out


def test_problems_go_to_stderr_and_exit_one(tmp_path, capsys):
    make_unit(tmp_path / "docs" / "src" / "recipes", "wind", "kind: recipe")

    assert main(harvest_argv(tmp_path)) == EXIT_PROBLEMS
    captured = capsys.readouterr()
    assert "title: required field is missing" in captured.err
    assert not (tmp_path / "manifest.json").exists()


def test_quiet_prints_nothing_on_success(tmp_path, capsys):
    make_unit(tmp_path / "docs" / "src" / "recipes", "wind", 'title: "Wind"\nkind: recipe')

    assert main(harvest_argv(tmp_path, "--quiet")) == EXIT_OK
    assert capsys.readouterr().out == ""


def test_a_missing_jar_is_an_environment_failure(tmp_path, capsys):
    make_unit(tmp_path / "docs" / "src" / "recipes", "wind", 'title: "Wind"\nkind: recipe')
    argv = [arg for arg in harvest_argv(tmp_path) if arg != "--skip-jar"]

    assert main([*argv, "--jar", str(tmp_path / "absent.jar")]) == EXIT_UNUSABLE
    assert "./gradlew fatJar" in capsys.readouterr().err


def test_a_named_source_tree_that_is_missing_is_a_problem(tmp_path, capsys):
    assert main(harvest_argv(tmp_path)) == EXIT_PROBLEMS
    assert "does not exist" in capsys.readouterr().err


def test_the_default_source_tree_may_not_exist_yet(tmp_path, capsys, monkeypatch):
    # The authored tree arrives with the content migration, so a default run before then harvests
    # the conformance suite and says so, rather than failing.
    monkeypatch.chdir(tmp_path)
    tests = tmp_path / "josh-tests" / "conformance" / "core"
    tests.mkdir(parents=True)
    (tests / "test_a.josh").write_text(
        "# @category: core\n# @subcategory: s\n# @priority: high\n# @description: d\n\n" + MODEL,
        encoding="utf-8",
    )

    exit_code = main(
        ["harvest", "--skip-jar", "--manifest", str(tmp_path / "manifest.json")]
    )
    captured = capsys.readouterr()
    assert exit_code == EXIT_OK
    assert "docs/src does not exist yet" in captured.err
    assert json.loads((tmp_path / "manifest.json").read_text(encoding="utf-8"))["counts"][
        "total"
    ] == 1


def test_an_unknown_subcommand_is_rejected():
    with pytest.raises(SystemExit):
        main(["render"])
