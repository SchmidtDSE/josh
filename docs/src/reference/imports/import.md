---
title: "Importing another model (reserved URL form)"
description: >-
  Pulling one .josh into another, kept here to record that the file:// spelling is refused
  where a bare relative path is not.
order: 20
tags: [imports, reserved]
status: reserved
reason: >-
  Written with the `file://` protocol form, which the engine rejects with "Only relative import
  paths are supported"; the supported spelling is a bare relative path.
---

`import` pulls another `.josh` file into the current one, so the units declared in
[import_operand.josh](import_operand.josh) become available to the `ForeverTree` below without being
repeated.

This file is kept as written to record the `file://` spelling and the fact that it is refused: the
engine supports only bare relative paths, and the protocol form is reserved. Nothing is validated or
run for a reserved unit, so if the protocol form is ever implemented, this status is what has to be
updated.
