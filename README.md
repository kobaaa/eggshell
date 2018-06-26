# eggshell

A Clojure-driven spreadsheet

## TODO

- [x] Saving
- [x] Loading
- [x] Cell slices in formulas
- [ ] main function and package as a jar
- [x] Code editor: multiline support
- [ ] Code editor: parinfer - WIP
- [ ] Cell editing: design interaction with code editor, F2, overwrite etc
- [ ] Grid: proper row header
- [x] error handling in cells (and GUI to diplay stacktraces)
- [ ] hande compile errors in cells
- [ ] fill cells by increasing numbers in cell references in code
- [ ] "Import" or "layer" CSV files
- [ ] export CSV
- [ ] Keyboard shortcuts
- [ ] Aliases for namespaces + GUI + saving as part of files - WIP
- [ ] all-lib for adding dependencies on the fly + GUI + saving as part of files
- [ ] some meta madness like spreadsheet-in-a-cell
- [ ] export spreadsheet as some sort of Clojure code
- [ ] named cells?
- [ ] save/load column widths
- [ ] trace errors with arrows

## Bugs

- [ ] Cells without deps (such as `(+ 1 2)`) don't get calculated
- [ ] Fix selection after cell value has been updated
- [ ] Fix calculations happening on the swing thread
- [ ] Fix save/load after state refactoring
- [x] Once you've entered a function in a cell, you can't replace it with a scalar value

## License

Copyright © 2018 Stathis Sideris

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
