---
title: "What a rejected model looks like"
description: >-
  A deliberately invalid model, and the file, line, and complaint that validating it reports.
order: 20
tags: [testing]
expect: parse-error
---

A deliberately invalid model: `start test` with no name, no body, and no `end`. Validating it
reports the file, the line, and the parser's complaint —

```
Found errors in Josh code at error.josh:
 - error.josh, line 1: no viable alternative at input 'start test'
```

— and exits nonzero, which is what a build step checks. It is the truncated form of
[test.josh](test.josh), so the same missing grammar alternative explains both.

The harvest asserts this stays invalid. A model declared `expect: parse-error` that starts
validating cleanly is reported as a problem, so this cannot become a silently passing file.
