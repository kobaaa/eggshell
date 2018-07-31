repl:
	clj -R:local:repl \
  -Sdeps '{:deps {rakk {:local/root "/Users/sideris/devel/rakk"}}}' \
  -J'-XX:-OmitStackTraceInFastThrow' bin/repl.clj

uberjar:
	rm -f eggshell.jar
	clj -A:pack mach.pack.alpha.capsule eggshell.jar -e target --application-id eggshell --application-version "666" -m eggshell.main
