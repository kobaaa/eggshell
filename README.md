# eggshell

A Clojure-driven spreadsheet

Maturity level: early development, works OK for simple cases, no
releases yet.

## Shortcut keys

* `F2` to edit cell content in the code editor (at the top of the grid)
* `Esc` while in cell editor to go back to grid
* `Enter` while in cell editor to apply your change
* `Ctrl+Enter` while in cell editor to enter a newline
* `Ctrl+E` (Windows/Linux) or `Cmd+E` (Mac) to show cell value/cell
  error area (at the bottom of the frame)

## TODO (essential)

- [x] Saving
- [x] Loading
- [ ] main function and package as a jar (WIP test on Windows)
- [ ] (WIP) Code editor: parinfer
- [ ] UI for layers: disable/enable, re-order, change options
- [ ] autofill: fill cells by increasing numbers in cell references in code
- [ ] "Import" or "layer" CSV files, pandas dataframe
- [ ] Cell editing: sync editor with cell display, maybe overwrite when in cell
- [ ] copy/paste cell value, copy/paste cell formula
- [ ] export CSV
- [ ] Insert row
- [ ] Insert column
- [x] Keyboard shortcuts (for windows too)
- [ ] Aliases: user input validation with meaningful errors
- [ ] named cells?
- [ ] multiple selections rendered ok, but confusing behaviour when starting to edit
- [ ] multiple sheets
- [ ] return formatted cell
- [ ] return clickable cell

## TODO (polish)

- [ ] user-visible logging
- [x] remember cursor position in code editor after hitting `enter`
- [ ] freeze rows and show as header
- [ ] select whole column/row
- [ ] select multiple columns/rows by clicking and dragging in the headers
- [ ] deps: disable all while working
- [x] Row/col resize: double click to change size to content
- [ ] provide a way to make a cell value `nil` and a way to make it `""`
- [ ] Aliases: autocomplete??
- [ ] Aliases/text editors: Ctrl+A, Ctrl+E, Ctrl+Z
- [x] save/load column/row widths
- [ ] trace errors with arrows
- [ ] maybe merge interface for deps and aliases?
- [ ] inputs with data structure literals should maintain their formatting for better editing
- [x] highlight focused column (as with focused row)
- [ ] a bit more work on row resize mouse behaviour
- [ ] re-order columns
- [ ] render vector and set values with monospaced font in grid
- [x] split result (down/right)
- [ ] split arrows (down/right)
- [ ] split let?

## TODO (advanced)

- [ ] lazy graph
- [ ] whole-column functions
- [ ] relative references in formulas
- [ ] show CSVs of any size based on random access
- [ ] non-expanded range dependencies
- [ ] export spreadsheet as some sort of Clojure code
- [ ] some meta madness like spreadsheet-in-a-cell
- [ ] defer: run when I click
- [ ] defer: run every x seconds

## Bugs

- [x] Bug: Cells without deps (such as `(+ 1 2)`) don't get calculated
- [ ] Bug: Fix calculations happening on the swing thread
- [ ] Bug: Fix save/load after state refactoring
- [ ] Bug: Try editing some multi-line code and hitting `enter`. The whole
  grid flickers. If you hold `enter` you hit a concurrency exception
  that reads `java.lang.IllegalArgumentException: Column index out of
  range`.
- [ ] Bug: Impossible to switch cells out of errored state
- [ ] Fit column (double click) does not work for strings

## Done

- [x] Cell slices in formulas
- [x] Code editor: multiline support
- [x] Cell editing: design interaction with code editor, F2
- [x] Grid: proper row header
- [x] Errors: error handling in cells (and GUI to diplay stacktraces)
- [x] Errors: handle compile errors in cells
- [x] Aliases for namespaces + GUI + saving as part of files - WIP
- [x] Aliases: maybe require namespaces?
- [x] deps: all-lib for adding dependencies on the fly + GUI + saving as part of files
- [x] fix glass pane clipping
- [x] Bug: Having lots of rows make startup slow because row header doesn't seem to be lazy
- [x] Bug: Fix selection after cell value has been updated
- [x] Bug: Once you've entered a function in a cell, you can't replace it with a scalar value
- [x] deps: re-used editor for deps should be wired for newlines etc
- [x] show pretty-printed value in error area when there is no error
- [x] show cell id on the left of code editor
- [x] Row header: resize row height individually
- [x] Row header: resize row height uniformly
- [x] Aliases: prevent opening more than one frame

## License

Copyright © 2018 Stathis Sideris

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
