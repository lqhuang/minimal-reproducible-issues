MILL := ./mill
MODULE := demo

.PHONY: bloop
bloop:
	${MILL} bloop.install

resolve-all:
	${MILL} resolve __

jvm-compile:
	${MILL} ${MODULE}.jvm[].compile

js-compile:
	${MILL} ${MODULE}.js[].compile

native-compile:
	${MILL} ${MODULE}.native[].compile

.PHONY: compile
compile: jvm-compile js-compile native-compile
