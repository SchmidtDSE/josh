"""Tests for rendering: page placement, link rewriting, and the single-source promise.

The promise these tests exist to defend is that the complete model on a page is the authored file
and not a transcription of it. `test_the_listing_is_the_authored_file_byte_for_byte` is that check
against a tree built here; `test_integration.py` makes the same assertion against the real repo.
"""

import html
import re
from pathlib import Path

import pytest

from joshdocs.harvest import HarvestOptions, harvest
from joshdocs.manifest import Manifest, ManifestUnreadable
from joshdocs.render import (
    MODEL_DIR,
    Library,
    RenderOptions,
    kind_index_path,
    page_path,
    relative_href,
    render,
)
from joshdocs.schema import Kind

MODEL = """start simulation Main

  grid.size = 10 m

end simulation
"""

LISTING = re.compile(r'<pre><code class="language-joshlang">(.*?)</code></pre>', re.S)


def write_unit(src, relative, front_matter, prose="Prose.", model=MODEL):
    """Write a `.josh` and its `.md` sidecar, returning the model's path."""
    path = src / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.with_suffix(".josh").write_text(model, encoding="utf-8")
    path.with_suffix(".md").write_text(
        f"---\n{front_matter}\n---\n\n{prose}\n", encoding="utf-8"
    )
    return path.with_suffix(".josh")


def build(tmp_path):
    """Harvest a tmp tree with no jar, returning the manifest."""
    result = harvest(
        HarvestOptions(
            root=tmp_path,
            src=tmp_path / "docs" / "src",
            tests=None,
            jar=None,
            runnable_dir=None,
        )
    )
    assert not result.log, result.log.report()
    return result.manifest


def draw(tmp_path, **overrides):
    """Harvest and render a tmp tree, returning the render result and the output root."""
    out = tmp_path / "out"
    fields = {"manifest": build(tmp_path), "root": tmp_path, "out": out}
    fields.update(overrides)
    return render(RenderOptions(**fields)), out


def listing(page: Path) -> str:
    """Return the last joshlang block on a page, unescaped."""
    blocks = LISTING.findall(page.read_text(encoding="utf-8"))
    assert blocks, f"no joshlang block on {page}"
    return html.unescape(blocks[-1])


def test_a_page_lands_under_its_destination(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    _, out = draw(tmp_path)
    assert (out / "reference/syntax/comments.html").is_file()


def test_the_listing_is_the_authored_file_byte_for_byte(tmp_path):
    src = tmp_path / "docs" / "src"
    # Characters HTML has to escape, so the test fails if escaping is not reversed exactly.
    model = 'start simulation Main\n\n  a = "x" & b < c > d\n\nend simulation\n'
    write_unit(src, "reference/syntax/comments", 'title: "Comments"', model=model)
    _, out = draw(tmp_path)
    assert listing(out / "reference/syntax/comments.html") == model


def test_the_downloadable_model_is_the_authored_file(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    _, out = draw(tmp_path)
    assert (out / MODEL_DIR / "comments.josh").read_text(encoding="utf-8") == MODEL


def test_prose_is_rendered_as_markdown(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"', prose="## A heading")
    _, out = draw(tmp_path)
    text = (out / "reference/syntax/comments.html").read_text(encoding="utf-8")
    assert '<h2 id="a-heading">A heading</h2>' in text


def test_a_link_to_another_unit_points_at_its_page(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"',
               prose="See [next](../two_trees/two_trees.md).")
    write_unit(src, "guides/two_trees/two_trees", 'title: "Two Trees"')
    result, out = draw(tmp_path)
    assert not result.log, result.log.report()
    text = (out / "guides/hello/hello.html").read_text(encoding="utf-8")
    assert 'href="../two_trees/two_trees.html"' in text


def test_a_bare_filename_resolves_to_the_unit_of_that_name(tmp_path):
    # The guides cite each other by filename alone, a spelling inherited from the flat tree they
    # were migrated out of, where every tutorial was a sibling.
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"', prose="See [next](two_trees.md).")
    write_unit(src, "guides/two_trees/two_trees", 'title: "Two Trees"')
    result, out = draw(tmp_path)
    assert not result.log, result.log.report()
    assert 'href="../two_trees/two_trees.html"' in (
        out / "guides/hello/hello.html"
    ).read_text(encoding="utf-8")


def test_a_fragment_survives_rewriting(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"',
               prose="See [it](two_trees.md#agents).")
    write_unit(src, "guides/two_trees/two_trees", 'title: "Two Trees"')
    _, out = draw(tmp_path)
    assert 'href="../two_trees/two_trees.html#agents"' in (
        out / "guides/hello/hello.html"
    ).read_text(encoding="utf-8")


def test_a_link_to_nothing_is_a_problem_with_the_line_it_is_on(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"',
               prose="Line one.\n\nSee [gone](nowhere.md).")
    result, _ = draw(tmp_path)
    assert len(result.log) == 1
    problem = result.log.problems[0]
    assert "nowhere.md" in problem.message
    # Front matter is 3 lines, blank, "Line one.", blank, then the link.
    assert problem.line == 7


def test_repeated_bad_links_report_their_own_lines(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"',
               prose="[a](gone.md)\n\n[b](gone.md)")
    result, _ = draw(tmp_path)
    assert [problem.line for problem in result.log.problems] == [5, 7]


def test_an_overlay_fragment_is_linkable_though_it_is_not_a_unit(tmp_path):
    src = tmp_path / "docs" / "src"
    (src / "guides/hello").mkdir(parents=True)
    (src / "guides/hello/hello_ci.josh").write_text(
        "update simulation Main\n\n  steps.high = 1 count\n\nend simulation\n", encoding="utf-8"
    )
    write_unit(src, "guides/hello/hello", 'title: "Hello"\noverlay: hello_ci.josh',
               prose="CI appends [the overlay](hello_ci.josh).")
    result, out = draw(tmp_path)
    assert not result.log, result.log.report()
    text = (out / "guides/hello/hello.html").read_text(encoding="utf-8")
    assert f'href="../../{MODEL_DIR}/hello_ci.josh"' in text
    assert (out / MODEL_DIR / "hello_ci.josh").is_file()


def test_absolute_and_external_links_are_left_alone(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"',
               prose="[a](/use.html) [b](https://example.org/x) [c](#here)")
    result, out = draw(tmp_path)
    assert not result.log, result.log.report()
    text = (out / "guides/hello/hello.html").read_text(encoding="utf-8")
    assert 'href="/use.html"' in text
    assert 'href="https://example.org/x"' in text
    assert 'href="#here"' in text


def test_a_url_inside_a_code_fence_is_not_a_link(tmp_path):
    # The guides fence models that set exportFiles to a URL. Rewriting rendered HTML with a regex
    # would corrupt those; rewriting the token stream cannot see them.
    src = tmp_path / "docs" / "src"
    fenced = '```joshlang\nexportFiles.patch = "memory://editor/patches"\n```'
    write_unit(src, "guides/hello/hello", 'title: "Hello"', prose=fenced)
    result, out = draw(tmp_path)
    assert not result.log, result.log.report()
    assert 'memory://editor/patches' in (out / "guides/hello/hello.html").read_text(
        encoding="utf-8"
    )


def test_conformance_tests_do_not_become_pages(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    tests = tmp_path / "conformance"
    tests.mkdir()
    (tests / "test_thing.josh").write_text(
        "# @category: core\n# @subcategory: basic\n# @priority: medium\n"
        "# @description: Proves a thing.\n" + MODEL,
        encoding="utf-8",
    )
    result = harvest(
        HarvestOptions(root=tmp_path, src=src, tests=tests, jar=None, runnable_dir=None)
    )
    assert not result.log, result.log.report()
    manifest = result.manifest
    assert any(unit.kind is Kind.TEST for unit in manifest.units)
    assert [unit.id for unit in Library(manifest).units] == ["comments"]


def test_an_index_is_written_for_each_kind(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello/hello", 'title: "Hello"')
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    _, out = draw(tmp_path)
    assert (out / "guides/index.html").is_file()
    assert (out / "reference/index.html").is_file()
    assert (out / "index.html").is_file()


def test_an_index_orders_units_by_order(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/second", 'title: "Second"\norder: 20')
    write_unit(src, "reference/syntax/first", 'title: "First"\norder: 10')
    _, out = draw(tmp_path)
    text = (out / "reference/index.html").read_text(encoding="utf-8")
    assert text.index("First") < text.index("Second")


def test_a_reserved_unit_says_it_is_not_implemented(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/soon",
               'title: "Soon"\nstatus: reserved\nreason: "the grammar has no rule for it"')
    _, out = draw(tmp_path)
    text = (out / "reference/syntax/soon.html").read_text(encoding="utf-8")
    assert "Not implemented yet" in text
    assert "the grammar has no rule for it" in text


def test_a_clean_render_removes_a_page_whose_unit_is_gone(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    _, out = draw(tmp_path)
    stale = out / "reference/syntax/gone.html"
    stale.write_text("stale", encoding="utf-8")
    draw(tmp_path)
    assert not stale.exists()


def test_keeping_the_tree_leaves_other_files_alone(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    _, out = draw(tmp_path)
    kept = out / "kept.txt"
    kept.write_text("kept", encoding="utf-8")
    draw(tmp_path, clean=False)
    assert kept.is_file()


def test_page_paths_are_relative_to_where_they_are_written():
    assert relative_href("reference/syntax/comments.html", "index.html") == "../../index.html"
    assert relative_href("index.html", "guides/index.html") == "guides/index.html"
    assert (
        relative_href("guides/hello/hello.html", "guides/two_trees/two_trees.html")
        == "../two_trees/two_trees.html"
    )


def test_a_unit_at_the_top_of_the_tree_lands_at_the_top(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "guides/hello", 'title: "Hello"')
    unit = build(tmp_path).units[0]
    assert unit.destination == "guides"
    assert page_path(unit) == "guides/hello.html"


def test_kind_index_paths_match_the_authored_directories():
    assert kind_index_path(Kind.GUIDE) == "guides/index.html"
    assert kind_index_path(Kind.REFERENCE) == "reference/index.html"


def test_a_manifest_round_trips_through_disk(tmp_path):
    src = tmp_path / "docs" / "src"
    write_unit(src, "reference/syntax/comments", 'title: "Comments"')
    manifest = build(tmp_path)
    path = tmp_path / "manifest.json"
    manifest.write(path)
    assert Manifest.read(path).to_json() == manifest.to_json()


def test_an_absent_manifest_names_the_command_that_writes_it(tmp_path):
    with pytest.raises(ManifestUnreadable, match="joshdocs harvest"):
        Manifest.read(tmp_path / "missing.json")


def test_a_manifest_from_another_schema_version_is_refused(tmp_path):
    path = tmp_path / "manifest.json"
    path.write_text('{"schemaVersion": 99, "counts": {}, "units": []}', encoding="utf-8")
    with pytest.raises(ManifestUnreadable, match="schema version"):
        Manifest.read(path)
