"""Retargeting a model's exports without editing what the author wrote.

An authored model that writes to ``memory://editor/patches`` runs in the browser but throws
``Unsupported protocol: memory`` on the JVM, and one that writes to ``file:///tmp/...`` throws
``Only in-memory targets supported on WASM``. That conflict is why ``examples/guide`` carries four
``_cli`` twins of otherwise identical models.

Rather than rewrite the authored source, the harvester appends an ``update simulation <Name>``
stanza that redeclares only the ``exportFiles`` handlers. ``update`` merges onto the entity already
declared, replacing the handlers it names and leaving everything else intact, so appending is enough
and the view-source box on a rendered page still shows the authored file byte for byte. The pattern
is already in production in ``paper/management/management.josh``.

``update`` is not limited to exports, and neither is this module: a unit may name an ``overlay:``
file holding stanzas the author wrote, which is how a difference the generator cannot express --
a shortened ``steps.high`` for CI, a ``debugFiles`` target -- is declared without a second copy of
the model. Those stanzas are read from a ``.josh`` file rather than built from strings here, so
Josh syntax stays in Josh files and this module only concatenates.

Emitted models carry no comments: Josh sources in this repo are comment-free by convention, and the
provenance of a generated model lives in the manifest instead.
"""

from __future__ import annotations

from pathlib import Path

from .schema import ExportSlot

#: Exports are written as CSV, the format the run command produces for a `.csv` target.
EXPORT_SUFFIX = "csv"


def export_target(export_dir: Path, unit_id: str, slot: ExportSlot) -> str:
    """Build the ``file://`` target for one export slot.

    Args:
        export_dir: Directory the exports are written into.
        unit_id: Id of the unit being retargeted.
        slot: Which ``exportFiles`` attribute is being retargeted.

    Returns:
        A ``file://`` URL with an absolute path.
    """
    destination = export_dir.resolve() / f"{unit_id}_{slot.value}.{EXPORT_SUFFIX}"
    return f"file://{destination}"


def render_overlay(simulation: str, targets: dict[ExportSlot, str]) -> str:
    """Render an ``update simulation`` stanza that retargets export files.

    Args:
        simulation: Name of the simulation stanza to update.
        targets: Export slot to target URL.

    Returns:
        The stanza, newline-terminated. Empty when there is nothing to retarget.
    """
    if not targets:
        return ""

    lines = [f"update simulation {simulation}", ""]
    for slot in ExportSlot:
        if slot in targets:
            lines.append(f'  exportFiles.{slot.value} = "{targets[slot]}"')
    lines.extend(["", "end simulation", ""])
    return "\n".join(lines)


def compose(source_text: str, *overlays: str) -> str:
    """Append overlays to an authored model, in order.

    An overlay has to follow the stanza it updates, which appending guarantees. Several `update`
    stanzas for the same entity compose, each replacing the handlers it names, so an authored
    fragment and a generated export block can both apply to one model.

    Args:
        source_text: The authored model, verbatim.
        *overlays: Stanzas to append. Empty ones are skipped.

    Returns:
        The composed model.
    """
    composed = source_text
    for overlay in overlays:
        if not overlay:
            continue
        if composed.endswith("\n\n"):
            separator = ""
        elif composed.endswith("\n"):
            separator = "\n"
        else:
            separator = "\n\n"
        composed = f"{composed}{separator}{overlay}"
    return composed


def emit_runnable(
    source: Path,
    destination: Path,
    simulation: str,
    slots: list[ExportSlot],
    export_dir: Path,
    unit_id: str,
    overlay_file: Path | None = None,
) -> Path:
    """Write a runnable copy of an authored model with its overlays applied.

    The authored fragment goes on first and the generated export block last, so the export target
    the build controls always wins: an author who also names ``exportFiles`` in their fragment gets
    the build's path, not a hardcoded one that would write outside the export directory.

    Args:
        source: The authored ``.josh`` file.
        destination: Directory to write the runnable model into.
        simulation: Name of the simulation stanza to update.
        slots: Export slots to retarget.
        export_dir: Directory the exports should be written into.
        unit_id: Id of the unit, used for both filenames.
        overlay_file: An authored ``.josh`` fragment to append, or None.

    Returns:
        The path of the written model.
    """
    targets = {slot: export_target(export_dir, unit_id, slot) for slot in slots}
    authored_overlay = "" if overlay_file is None else overlay_file.read_text(encoding="utf-8")
    composed = compose(
        source.read_text(encoding="utf-8"),
        authored_overlay,
        render_overlay(simulation, targets),
    )
    if targets:
        # The jar opens an export target without creating its parents, so a model emitted against a
        # directory that does not exist yet fails at run time with a bare FileNotFoundException.
        # The overlay names that directory, so this is the place that owes it.
        export_dir.mkdir(parents=True, exist_ok=True)
    destination.mkdir(parents=True, exist_ok=True)
    written = destination / f"{unit_id}.josh"
    written.write_text(composed, encoding="utf-8")
    return written
