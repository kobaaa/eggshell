repl:
	clj -R:local:repl \
  -Sdeps '{:deps {rakk {:local/root "/Users/sideris/devel/rakk"}}}' \
  -J'-XX:-OmitStackTraceInFastThrow' bin/repl.clj
