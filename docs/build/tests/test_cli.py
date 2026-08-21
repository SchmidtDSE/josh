"""Tests for the command line entry point, which is what CI calls."""

import http.server
import json

import pytest

from joshdocs.__main__ import EXIT_OK, EXIT_PROBLEMS, EXIT_UNUSABLE, _build_parser, main
from joshdocs.render import DEFAULT_OUT, DEFAULT_SITE
from support import with_description

MODEL = "start simulation Main\n\n  grid.size = 10 m\n\nend simulation\n"


class StubServer:
    """Stands in for the HTTP server so a `serve` test returns instead of listening forever."""

    def __init__(self, address, handler):
        self.address = address
        self.handler = handler

    def __enter__(self):
        """Hand back the stub, as the real server's context manager hands back itself."""
        return self

    def __exit__(self, *exc):
        """Let the KeyboardInterrupt through, which is what `serve` catches to shut down."""
        return False

    def serve_forever(self):
        raise KeyboardInterrupt


@pytest.fixture
def stub_server(monkeypatch):
    """Let `serve` run its checks and print its banner without binding a port."""
    monkeypatch.setattr(http.server, "ThreadingHTTPServer", StubServer)


def make_unit(src, stem, front_matter):
    src.mkdir(parents=True, exist_ok=True)
    (src / f"{stem}.josh").write_text(MODEL, encoding="utf-8")
    (src / f"{stem}.md").write_text(
        f"---\n{with_description(front_matter)}\n---\n\nProse.\n", encoding="utf-8"
    )


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
        main(["publish"])


def render_argv(tmp_path, *extra):
    return [
        "render",
        "--root",
        str(tmp_path),
        "--manifest",
        str(tmp_path / "manifest.json"),
        "--out",
        str(tmp_path / "out"),
        *extra,
    ]


def harvest_tree_argv(tmp_path, *extra):
    """Harvest the whole authored tree, so `kind` is inferred from its top-level directory."""
    return [
        "harvest",
        "--root",
        str(tmp_path),
        "--src",
        str(tmp_path / "docs" / "src"),
        "--no-tests",
        "--skip-jar",
        "--manifest",
        str(tmp_path / "manifest.json"),
        "--emit-runnable",
        str(tmp_path / "runnable"),
        *extra,
    ]


def test_render_writes_pages_from_a_harvested_manifest(tmp_path, capsys):
    src = tmp_path / "docs" / "src" / "recipes" / "dispersal"
    make_unit(src, "wind", 'title: "Wind"')
    assert main(harvest_tree_argv(tmp_path, "--quiet")) == EXIT_OK
    assert main(render_argv(tmp_path)) == EXIT_OK
    assert (tmp_path / "out" / "recipes" / "dispersal" / "wind.html").is_file()
    assert "files ->" in capsys.readouterr().out


def test_render_without_a_manifest_says_to_harvest_first(tmp_path, capsys):
    assert main(render_argv(tmp_path)) == EXIT_UNUSABLE
    assert "joshdocs harvest" in capsys.readouterr().err


def test_render_reports_a_dead_link_and_fails(tmp_path, capsys):
    src = tmp_path / "docs" / "src" / "recipes" / "dispersal"
    make_unit(src, "wind", 'title: "Wind"')
    front = with_description('title: "Wind"')
    (src / "wind.md").write_text(
        f"---\n{front}\n---\n\nSee [gone](nowhere.md).\n", encoding="utf-8"
    )
    assert main(harvest_tree_argv(tmp_path, "--quiet")) == EXIT_OK
    assert main(render_argv(tmp_path)) == EXIT_PROBLEMS
    assert "nowhere.md" in capsys.readouterr().err


def test_serve_without_a_rendered_tree_says_to_render_first(tmp_path, capsys):
    assert main(["serve", "--out", str(tmp_path / "out")]) == EXIT_UNUSABLE
    assert "joshdocs render" in capsys.readouterr().err


def test_serve_defaults_to_the_site_root_not_the_rendered_tree():
    """Pages carry site-absolute assets, so the tree `render` writes is the wrong server root."""
    args = _build_parser().parse_args(["serve"])
    assert args.out == DEFAULT_SITE
    assert args.out != DEFAULT_OUT


def test_serve_points_at_the_library_and_stays_quiet_on_a_real_site_root(
    tmp_path, capsys, stub_server
):
    (tmp_path / "library").mkdir()
    assert main(["serve", "--out", str(tmp_path)]) == EXIT_OK
    captured = capsys.readouterr()
    assert "/library/" in captured.out
    assert "warning" not in captured.err


def test_serve_warns_when_pointed_at_the_rendered_tree(tmp_path, capsys, stub_server):
    """Serving `landing/library` 404s every stylesheet and script, as a MIME refusal.

    The browser blames the content type rather than the missing file, so nothing in the error names
    the server root as the cause. This is the only place that can.
    """
    assert main(["serve", "--out", str(tmp_path)]) == EXIT_OK
    assert "warning" in capsys.readouterr().err
