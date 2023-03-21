## Scala 3 stageing test cases for `case class` and Circe

This is a test project for the Scala 3 compiler, also is support material for
the [circe/circe PR#2083](https://github.com/circe/circe/pull/2083). It contains
a number of test cases for the `case class` and Circe.

Appreciated if you would help me to fix these issues.

## Problems

My target is to test optional compiler options in Scala 3 for Circe libraries. I
tried to use
[Runtime Multi-Stage Programming](https://docs.scala-lang.org/scala3/reference/metaprogramming/staging.html)
in the Scala 3 via compile-time metaprogramming and macros to run code snippet
with different compiler flags.

But there are some limits in current staging API.

### Case 1

```
sbt runMain Case1
```

But seems `case class` cannot be defined in quoted scope.

    Case class definitions are not allowed in inline methods or quoted code. Use a normal class instead.

### Case 2

```
sbt runMain Case2
```

Move the `case` to outside scope and add missing `Type[T]`, but failed again

    access to object Foo from wrong staging level:
      - the definition is at level 0,
      - but the access is at level 1.

Seems because the `Foo` is defined in the top level scope (level 0), but derives
declarations are defined in the Qutoes scope (level 1). This is not allowed in
stage API, too.

### Case 3

```
sbt runMain Case3
```

In case 3, just define orginal `class` and write companion object with custom
codec manually (`derive` feature requires case class). Codes compiled
successfully, but failed in runtime. I'm not very sure whether it's caused by
importing circe packages.

```
[info] running Case3
[error] class dotty.tools.dotc.reporting.Diagnostic$Error at src/main/scala/Case3.scala:<1098..1129>: undefined: io.circe.parser.package.parse # -1: TermRef(TermRef(TermRef(ThisType(TypeRef(NoPrefix,module class circe)),object parser),package),parse) at quotedFrontend
[error] 	at dotty.tools.dotc.report$.error(report.scala:78)
[error] 	at dotty.tools.dotc.typer.ErrorReporting$.errorType(ErrorReporting.scala:36)
[error] 	at dotty.tools.dotc.typer.ErrorReporting$.errorType(ErrorReporting.scala:41)
[error] 	at dotty.tools.dotc.typer.TypeAssigner.assignType(TypeAssigner.scala:306)
[error] 	at dotty.tools.dotc.typer.TypeAssigner.assignType$(TypeAssigner.scala:16)
[error] 	at dotty.tools.dotc.typer.Typer.assignType(Typer.scala:121)
[error] 	at dotty.tools.dotc.ast.tpd$.Apply(tpd.scala:49)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1236)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1297)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1238)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1233)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1282)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler.dotty$tools$dotc$core$tasty$TreeUnpickler$TreeReader$$_$_$$anonfun$26(TreeUnpickler.scala:1249)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readIndexedStats(TreeUnpickler.scala:1095)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readStats(TreeUnpickler.scala:1099)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1249)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readLengthTerm$1(TreeUnpickler.scala:1259)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1394)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler$TreeReader.readTerm(TreeUnpickler.scala:1117)
[error] 	at dotty.tools.dotc.core.tasty.TreeUnpickler.unpickle(TreeUnpickler.scala:113)
[error] 	at dotty.tools.dotc.core.tasty.DottyUnpickler.computeRootTrees(DottyUnpickler.scala:62)
[error] 	at dotty.tools.dotc.ast.tpd$TreeProvider.rootTrees(tpd.scala:1280)
[error] 	at dotty.tools.dotc.ast.tpd$TreeProvider.rootTrees$(tpd.scala:1269)
[error] 	at dotty.tools.dotc.core.tasty.DottyUnpickler.rootTrees(DottyUnpickler.scala:44)
[error] 	at dotty.tools.dotc.ast.tpd$TreeProvider.tree(tpd.scala:1284)
[error] 	at dotty.tools.dotc.ast.tpd$TreeProvider.tree$(tpd.scala:1269)
[error] 	at dotty.tools.dotc.core.tasty.DottyUnpickler.tree(DottyUnpickler.scala:44)
[error] 	at dotty.tools.dotc.quoted.PickledQuotes$.unpickle(PickledQuotes.scala:257)
[error] 	at dotty.tools.dotc.quoted.PickledQuotes$.unpickleTerm(PickledQuotes.scala:83)
[error] 	at scala.quoted.runtime.impl.QuotesImpl.unpickleExprV2(QuotesImpl.scala:3044)
[error] 	at Case3$package$.code$1(Case3.scala:10)
[error] 	at Case3$package$.Case3$$anonfun$1(Case3.scala:47)
[error] 	at scala.quoted.staging.QuoteCompiler$QuotedFrontend.runOn$$anonfun$1(QuoteCompiler.scala:83)
[error] 	at scala.collection.immutable.List.flatMap(List.scala:293)
[error] 	at scala.quoted.staging.QuoteCompiler$QuotedFrontend.runOn(QuoteCompiler.scala:98)
[error] 	at dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:238)
[error] 	at scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
[error] 	at scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
[error] 	at scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1321)
[error] 	at dotty.tools.dotc.Run.runPhases$1(Run.scala:249)
[error] 	at dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:257)
[error] 	at dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:266)
[error] 	at dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:68)
[error] 	at dotty.tools.dotc.Run.compileUnits(Run.scala:266)
[error] 	at dotty.tools.dotc.Run.compileUnits(Run.scala:196)
[error] 	at scala.quoted.staging.QuoteCompiler$ExprRun.compileExpr(QuoteCompiler.scala:118)
[error] 	at scala.quoted.staging.QuoteDriver.run(QuoteDriver.scala:43)
[error] 	at scala.quoted.staging.Compiler$$anon$1.run(Compiler.scala:38)
[error] 	at scala.quoted.staging.package$.run(staging.scala:19)
[error] 	at Case3$package$.Case3(Case3.scala:47)
[error] 	at Case3.main(Case3.scala:3)
[error] 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
[error] 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
[error] 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
[error] 	at java.base/java.lang.reflect.Method.invoke(Method.java:568)
[error] stack trace is suppressed; run last Compile / runMain for the full output
[error] (Compile / runMain) class dotty.tools.dotc.reporting.Diagnostic$Error at src/main/scala/Case3.scala:<1098..1129>: undefined: io.circe.parser.package.parse # -1: TermRef(TermRef(TermRef(ThisType(TypeRef(NoPrefix,module class circe)),object parser),package),parse) at quotedFrontend
[error] Total time: 1 s, completed Mar 21, 2023, 8:33:41 PM
```
