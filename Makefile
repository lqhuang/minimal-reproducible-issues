MILL := ./mill
TASK_TARGET := demo[]

.PHONY: bloop
bloop:
	${MILL} bloop.install
