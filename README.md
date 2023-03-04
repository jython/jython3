## This is not where Jython 3 is developing.

The latest released version of Jython remains 2.7.x
(see [Jython](https://github.com/jython/jython) for that).

This doesn't mean that there isn't any progress on a Jython 3.
A lot of exploration has taken place away from the official repository,
in order not to pollute it with false starts and dead-ends.

Since the beginning of 2021, Jython 3 exploratory work reached a form
that merited porting from the experiment to the `main` of the official repo.
Risky design concepts still begin life in the exploratory environment,
then arrive as a PR to the official `main` branch.
Please watch the jython-dev mailing list for developements,
or the [`main` branch](https://github.com/jython/jython/tree/main)
(not `master`) of the official Jython repo.

## If this is not the real thing, what is it?

This repo represents an attempt made in 2016 to implement a Jython 3.5,
that is, with language and runtime compatibility with
CPython 3.5, along with continued substantial support of the Python
ecosystem.
It is a sandbox for that venture.
This code having branched from an early 2.7,
the architecture is that of Jython 2,
without taking advantage of the (then fairly new) dynamic language support.
It has not been updated with subsequent bug-fixes made on Jython 2.7,
and there is no plan to to do so.

Please see ACKNOWLEDGMENTS for details about Jython's copyright,
license, contributors, and mailing lists; and NEWS for detailed
release notes, including bugs fixed, backwards breaking changes, and
new features.
