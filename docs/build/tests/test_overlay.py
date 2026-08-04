"""Tests for the export overlay."""

from pathlib import Path

from joshdocs.overlay import compose, emit_runnable, export_target, render_overlay
from joshdocs.schema import ExportSlot

AUTHORED = """start simulation Main

  grid.size = 10 m

end simulation
"""


def test_renders_an_update_stanza():
    overlay = render_overlay("Main", {ExportSlot.PATCH: "file:///tmp/x_patch.csv"})
    assert overlay == (
        "update simulation Main\n"
        "\n"
        '  exportFiles.patch = "file:///tmp/x_patch.csv"\n'
        "\n"
        "end simulation\n"
    )


def test_renders_slots_in_a_stable_order():
    overlay = render_overlay(
        "Main",
        {ExportSlot.ENTITY: "file:///tmp/e.csv", ExportSlot.PATCH: "file:///tmp/p.csv"},
    )
    assert overlay.index("exportFiles.patch") < overlay.index("exportFiles.entity")


def test_no_slots_renders_nothing():
    assert render_overlay("Main", {}) == ""


def test_overlay_carries_no_comments():
    overlay = render_overlay("Main", {ExportSlot.META: "file:///tmp/m.csv"})
    assert "#" not in overlay


def test_compose_keeps_the_authored_text_verbatim():
    composed = compose(AUTHORED, render_overlay("Main", {ExportSlot.PATCH: "file:///tmp/p.csv"}))
    assert composed.startswith(AUTHORED)
    assert "update simulation Main" in composed


def test_compose_separates_the_stanzas():
    composed = compose(AUTHORED, "update simulation Main\n")
    assert "end simulation\n\nupdate simulation Main" in composed


def test_compose_without_a_trailing_newline():
    composed = compose("end simulation", "update simulation Main\n")
    assert composed == "end simulation\n\nupdate simulation Main\n"


def test_compose_without_an_overlay_is_the_source():
    assert compose(AUTHORED, "") == AUTHORED


def test_export_target_is_an_absolute_file_url(tmp_path):
    target = export_target(tmp_path, "wind-dispersal", ExportSlot.PATCH)
    assert target.startswith("file://")
    assert target.endswith("wind-dispersal_patch.csv")
    assert Path(target.removeprefix("file://")).is_absolute()


def test_emit_writes_the_composed_model(tmp_path):
    source = tmp_path / "hello.josh"
    source.write_text(AUTHORED, encoding="utf-8")
    written = emit_runnable(
        source=source,
        destination=tmp_path / "runnable",
        simulation="Main",
        slots=[ExportSlot.PATCH],
        export_dir=tmp_path / "exports",
        unit_id="hello",
    )
    text = written.read_text(encoding="utf-8")
    assert written.name == "hello.josh"
    assert text.startswith(AUTHORED)
    assert "update simulation Main" in text
    assert "hello_patch.csv" in text


def test_emit_creates_the_directory_the_overlay_points_at(tmp_path):
    # The jar does not create an export target's parents, so a model emitted against a missing
    # directory harvests cleanly and then dies on `run` with a bare FileNotFoundException.
    source = tmp_path / "hello.josh"
    source.write_text(AUTHORED, encoding="utf-8")
    exports = tmp_path / "nested" / "exports"
    emit_runnable(
        source=source,
        destination=tmp_path / "runnable",
        simulation="Main",
        slots=[ExportSlot.PATCH],
        export_dir=exports,
        unit_id="hello",
    )
    assert exports.is_dir()


def test_emit_without_exports_makes_no_export_directory(tmp_path):
    source = tmp_path / "hello.josh"
    source.write_text(AUTHORED, encoding="utf-8")
    exports = tmp_path / "exports"
    written = emit_runnable(
        source=source,
        destination=tmp_path / "runnable",
        simulation="Main",
        slots=[],
        export_dir=exports,
        unit_id="hello",
    )
    assert written.read_text(encoding="utf-8") == AUTHORED
    assert not exports.exists()


def test_emit_overrides_an_authored_memory_target(tmp_path):
    # This is why the overlay works as a pure append: `update` replaces the handler the authored
    # file already declares, so a browser-targeted model runs on the CLI unmodified.
    source = tmp_path / "hello.josh"
    source.write_text(
        'start simulation Main\n  exportFiles.patch = "memory://editor/patches"\nend simulation\n',
        encoding="utf-8",
    )
    written = emit_runnable(
        source=source,
        destination=tmp_path / "runnable",
        simulation="Main",
        slots=[ExportSlot.PATCH],
        export_dir=tmp_path / "exports",
        unit_id="hello",
    )
    text = written.read_text(encoding="utf-8")
    assert "memory://editor/patches" in text
    assert text.index("memory://") < text.index("update simulation Main")


FRAGMENT = """update simulation Main

  steps.high = 5 count

end simulation
"""


def test_compose_applies_overlays_in_order():
    composed = compose(AUTHORED, "first\n", "second\n")
    assert composed.index("first") < composed.index("second")
    assert composed.startswith(AUTHORED)


def test_compose_skips_empty_overlays():
    assert compose(AUTHORED, "", "only\n") == compose(AUTHORED, "only\n")


def test_emit_puts_the_authored_fragment_before_the_generated_block(tmp_path):
    # The generated export target has to win over anything the author wrote, so that an overlay
    # naming exportFiles cannot redirect output outside the export directory the build controls.
    source = tmp_path / "two_trees.josh"
    source.write_text(AUTHORED, encoding="utf-8")
    fragment = tmp_path / "two_trees_ci.josh"
    fragment.write_text(FRAGMENT, encoding="utf-8")

    written = emit_runnable(
        source=source,
        destination=tmp_path / "runnable",
        simulation="Main",
        slots=[ExportSlot.PATCH],
        export_dir=tmp_path / "exports",
        unit_id="two_trees",
        overlay_file=fragment,
    )

    text = written.read_text(encoding="utf-8")
    assert text.startswith(AUTHORED)
    assert text.index("steps.high = 5 count") < text.index("exportFiles.patch")


def test_emit_with_an_overlay_and_no_exports(tmp_path):
    source = tmp_path / "two_trees.josh"
    source.write_text(AUTHORED, encoding="utf-8")
    fragment = tmp_path / "two_trees_ci.josh"
    fragment.write_text(FRAGMENT, encoding="utf-8")
    exports = tmp_path / "exports"

    written = emit_runnable(
        source=source,
        destination=tmp_path / "runnable",
        simulation="Main",
        slots=[],
        export_dir=exports,
        unit_id="two_trees",
        overlay_file=fragment,
    )

    text = written.read_text(encoding="utf-8")
    assert text == AUTHORED + "\n" + FRAGMENT
    assert not exports.exists()
