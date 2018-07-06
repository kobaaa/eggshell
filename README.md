# eggshell

A Clojure-driven spreadsheet

## Shortcut keys

* `F2` to edit cell content in the code editor (at the top of the grid)
* `Esc` while in cell editor to go back to grid
* `Enter` while in cell editor to apply your change
* `Ctrl+Enter` while in cell editor to enter a newline
* `Cmd+E` to show cell value/cell error area (at the bottom of the frame)

## TODO (essential)

- [x] Saving
- [x] Loading
- [x] Cell slices in formulas
- [ ] main function and package as a jar
- [x] Code editor: multiline support
- [ ] (WIP) Code editor: parinfer
- [x] Cell editing: design interaction with code editor, F2
- [ ] Cell editing: sync editor with cell display, maybe overwrite when in cell
- [x] Grid: proper row header
- [x] Errors: error handling in cells (and GUI to diplay stacktraces)
- [x] Errors: handle compile errors in cells
- [ ] autofill: fill cells by increasing numbers in cell references in code
- [ ] copy/paste cell value, copy/paste cell formula
- [ ] "Import" or "layer" CSV files
- [ ] export CSV
- [ ] Keyboard shortcuts
- [x] Aliases for namespaces + GUI + saving as part of files - WIP
- [x] Aliases: maybe require namespaces?
- [ ] Aliases: user input validation with meaningful errors
- [x] deps: all-lib for adding dependencies on the fly + GUI + saving as part of files
- [ ] named cells?

## TODO (polish)

- [ ] user-visible logging
- [x] deps: re-used editor for deps should be wired for newlines etc
- [ ] deps: disable all while working
- [x] show pretty-printed value in error area when there is no error
- [x] show cell id on the left of code editor
- [ ] Row header: resize row height uniformly and individually
- [ ] provide a way to make a cell value `nil` and a way to make it `""`
- [x] Aliases: prevent opening more than one frame
- [ ] Aliases: autocomplete??
- [ ] Aliases/text editors: Ctrl+A, Ctrl+E, Ctrl+Z
- [ ] save/load column widths
- [ ] trace errors with arrows
- [ ] maybe merge interface for deps and aliases?
- [ ] inputs with data structure literals should maintain their formatting for better editing

## TODO (advanced)

- [ ] lazy graph
- [ ] whole-column functions
- [ ] non-expanded range dependencies
- [ ] export spreadsheet as some sort of Clojure code
- [ ] some meta madness like spreadsheet-in-a-cell

## Bugs

- [x] Having lots of rows make startup slow because row header doesn't seem to be lazy
- [ ] Cells without deps (such as `(+ 1 2)`) don't get calculated
- [x] Fix selection after cell value has been updated
- [ ] Fix calculations happening on the swing thread
- [ ] Fix save/load after state refactoring
- [x] Once you've entered a function in a cell, you can't replace it with a scalar value

## License

Copyright © 2018 Stathis Sideris

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
