MILL := ./mill
MODULE := demo

.PHONY: bloop
bloop:
	${MILL} bloop.install


jvm-compile:
	${MILL} -i ${MODULE}.jvm[].compile

js-compile:
	${MILL} -i ${MODULE}.js[].compile

native-compile:
	${MILL} -i ${MODULE}.native[].compile

.PHONY: compile
compile: jvm-compile js-compile native-compile
