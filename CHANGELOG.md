# Changelog

## [0.4.2] - ???

The main change is support for Isabelle2023.

### Added

* Support for [Isabelle2023](https://isabelle.in.tum.de/website-Isabelle2023/).
* [`Position`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html):
  New methods [`line`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#line(implicitisabelle:de.unruh.isabelle.control.Isabelle):Option[Int]),
  [`offset`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#offset(implicitisabelle:de.unruh.isabelle.control.Isabelle):Option[Int]),
  [`endOffset`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#endOffset(implicitisabelle:de.unruh.isabelle.control.Isabelle):Option[Int]),
  [`file`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#file(implicitisabelle:de.unruh.isabelle.control.Isabelle):Option[String]),
  [`id`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#id(implicitisabelle:de.unruh.isabelle.control.Isabelle):Option[String])
  to access the corresponding fields in the ML-type.
  [`extract`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#extract(text:String)(implicitisabelle:de.unruh.isabelle.control.Isabelle):String),
  [`extractUntil`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Position.html#extractUntil(end:de.unruh.isabelle.pure.Position,text:String)(implicitisabelle:de.unruh.isabelle.control.Isabelle):String)
  to extract text from a string given position(s).
* [`TheoryHeader`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/TheoryHeader.html):
  New method [`name`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/TheoryHeader.html#name(implicitisabelle:de.unruh.isabelle.control.Isabelle):String)
  to get the name of the theory. [`read`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/TheoryHeader$.html#read(text:String)(implicitisabelle:de.unruh.isabelle.control.Isabelle):de.unruh.isabelle.pure.TheoryHeader)
  to parse a theory from a string.
* [`ToplevelState`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState.html):
  New methods [`proofLevel`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState.html#proofLevel(implicitisabelle:de.unruh.isabelle.control.Isabelle):Int),
  [`mode`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState.html#mode(implicitisabelle:de.unruh.isabelle.control.Isabelle):de.unruh.isabelle.pure.ToplevelState.Modes.Mode),
  [`isProofMode`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState.html#isProofMode(implicitisabelle:de.unruh.isabelle.control.Isabelle):Boolean)
  etc., [`proofStateDescription`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState.html#proofStateDescription(implicitisabelle:de.unruh.isabelle.control.Isabelle):String),
  [`localTheoryDescription`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState.html#localTheoryDescription(implicitisabelle:de.unruh.isabelle.control.Isabelle):String).
  [`ToplevelState()`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/ToplevelState$.html#apply()(implicitisabelle:de.unruh.isabelle.control.Isabelle):de.unruh.isabelle.pure.ToplevelState) creates a toplevel based on `Pure`.
* [`Transition`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/pure/Transition.html):
  New Scala class. Represents A toplevel transition (roughly speaking one command in a theory file).
* [`JIsabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/java/JIsabelle$.html):
  New methods [`setupSetVerbose`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/java/JIsabelle$.html#setupSetVerbose(verbose:Boolean,setup:de.unruh.isabelle.control.Isabelle.Setup):de.unruh.isabelle.control.Isabelle.Setup),
  [`setupSetUserDir`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/java/JIsabelle$.html#setupSetUserDir(userDir:java.nio.file.Path,setup:de.unruh.isabelle.control.Isabelle.Setup):de.unruh.isabelle.control.Isabelle.Setup),
  [`setupSetWorkingDirectory`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/java/JIsabelle$.html#setupSetWorkingDirectory(workingDirectory:java.nio.file.Path,setup:de.unruh.isabelle.control.Isabelle.Setup):de.unruh.isabelle.control.Isabelle.Setup),
  [`setupSetSessionRoots`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/java/JIsabelle$.html#setupSetSessionRoots(sessionRoots:Iterable[java.nio.file.Path],setup:de.unruh.isabelle.control.Isabelle.Setup):de.unruh.isabelle.control.Isabelle.Setup),
  [`setupSetLogic`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/java/JIsabelle$.html#setupSetLogic(verbose:String,setup:de.unruh.isabelle.control.Isabelle.Setup):de.unruh.isabelle.control.Isabelle.Setup)
  to set the corresponding fields in a [`Setup`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/control/Isabelle$$Setup.html)
  object from Java.

### Changed

* [`Isabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/control/Isabelle.html): Execution context is stored inside `Isabelle` instance, and configured via `[Setup](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/latest/de/unruh/isabelle/control/Isabelle$$Setup.html).executionContext`.
  Most `scala-isabelle` methods now use that execution context.

### Removed

None.

## [0.4.1] – 2022-11-27

Main changes are support for Isabelle2022 and improved support for handling of ML exceptions.

### Added

* Support for Isabelle2022.
* [`Isabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/Isabelle.html):
  Can configure a different [`ExceptionManager`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/ExceptionManager.html)
  for handling exceptions in ML code (instead of raising [`IsabelleMLException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/IsabelleMLException.html)s).
  See [`MLException.ExceptionManager`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/pure/exceptions/MLException$.html#ExceptionManagerextendsExceptionManager)
  for an alternative to the default.
* Added an ML value converter for [`IsabelleMLException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/IsabelleMLException.html)s,
  see [`MLException.simpleIsabelleMLExceptionConverter`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/pure/exceptions/MLException$.html#simpleIsabelleMLExceptionConverter)
  and [`MLException.distinguishingIsabelleMLExceptionConverter`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/pure/exceptions/MLException$$distinguishingIsabelleMLExceptionConverter$.html).
* [`Symbols`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/misc/Symbols.html):
  Supporting Unicode subscript symbols when converting between Isabelle symbols and Unicode.
  (See [constructor](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/misc/Symbols.html#%3Cinit%3E(symbolsFile:java.net.URL,extraSymbols:Iterable[(String,Int)]):de.unruh.isabelle.misc.Symbols)
  argument `processSubSuper`.)
* [`Symbols`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/misc/Symbols.html):
  Specify fallback translations for symbols (only used if the symbols table has no translation).
  (See [constructor](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/misc/Symbols.html#%3Cinit%3E(symbolsFile:java.net.URL,extraSymbols:Iterable[(String,Int)]):de.unruh.isabelle.misc.Symbols)
  argument `extraSymbolsLowPri`.)
* [`Version.versionFromIsabelleDirectory`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/mlvalue/Version$.html#versionFromIsabelleDirectory(directory:java.nio.file.Path):String):
  Guesses the version of an Isabelle distribution by looking at the Isabelle distribution directory (without initializing the Isabelle process).

### Changed

* `IsabelleException` was renamed to [`IsabelleMLException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/IsabelleMLException.html).
  It is only used raised when exceptions occur in ML code.
  Use [`IsabelleMiscException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/IsabelleMiscException.html)
  for other purposes.
* [`IsabelleMLException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/IsabelleMLException.html)
  contains the actual ML exception object
* [`IsabelleMLException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.1/de/unruh/isabelle/control/IsabelleMLException.html):
  The exception message contains the associated data of the exception (e.g., a `TERM` exception will contain the terms in the message).
  This message is lazily computed when needed.
* Isabelle is initialized with `quick_and_dirty` flag `true`, this means imported theories can use `sorry`.

### Removed

None


## [0.4.0] – 2021-12-27

The main changes are support up to Isabelle2021-1, and better invocation of the Isabelle process (takes into account `ROOT` files etc.).

### Added

* String interpolators for creating Isabelle terms/types (see [`StringInterpolators`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/StringInterpolators$.html))
* Support for up to Isabelle2021-1
* Added methods:
  * [`Term.fastType`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Term.html#fastType(implicitexecutionContext:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Typ) returns the type of a term (without type checking).
  * [`Context.setMode`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Context.html#setMode(mode:de.unruh.isabelle.pure.Context.Mode)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitexecutionContext:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Context) sets the syntax mode (pattern/schematic/abbrev) for parsing terms/types.
  * [`Theory.context`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Theory.html#context(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitexecutionContext:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Context) returns a context for this theory.
  * [`Term.concreteRecursive`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Term.html#concreteRecursive(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.ConcreteTerm) / [`Typ.concreteRecursive`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Typ.html#concreteRecursive(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.ConcreteTyp) converts a term/type recursively into a concrete term/type (i.e., represented fully inside the Scala process).
  * [`ToplevelState.apply`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/ToplevelState$.html#apply(theory:de.unruh.isabelle.pure.Theory)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.ToplevelState) initializes a new toplevel state based on a theory.
  * [`ToplevelState.context`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/ToplevelState.html#context(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Context) returns the proof context corresponding to a toplevel state.
* Debugging support:
  * Environment variable `SCALA_ISABELLE_NO_CLEANUP`: If set (values `true` or `1`), the temporary directory created by scala-isabelle are not removed on exit.
  * Environment variable `SCALA_ISABELLE_LOG_QUERIES`: If set, all queries from scala-isabelle to the Isabelle are logged (see header comment in [`control_isabelle_logged.ml`](https://github.com/dominique-unruh/scala-isabelle/blob/v0.4.0/src/main/resources/de/unruh/isabelle/control/control_isabelle_logged.ml)).
* In ML, exception `Control_Isabelle.E_Data` added to wrap ML type `Control_Isabelle.data` as an exception (for storing in the [object store](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/control/Isabelle.html)).

### Changed

* The Isabelle process is invoked with full PIDE context. In particular, this means that sessions are properly initialized the same way as when running Isabelle directly, and theory files can be found without needing to register directories using [`Theory.registerSessionDirectories`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Theory$.html#registerSessionDirectories(paths:(String,java.nio.file.Path)*)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):scala.concurrent.Future[Unit]).
* Declarations in ML (e.g., via [`Isabelle.executeMLCode`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/control/Isabelle.html#executeMLCode(ml:String):scala.concurrent.Future[Unit])) do not affect the global Isabelle/ML namespace anymore but a separate namespace.
* [`Cterm.apply`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Cterm$.html#apply(ctxt:de.unruh.isabelle.pure.Context,term:de.unruh.isabelle.pure.Term)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Cterm) / [`Ctyp.apply`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Ctyp$.html#apply(ctxt:de.unruh.isabelle.pure.Context,typ:de.unruh.isabelle.pure.Typ)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Ctyp) avoid re-typechecking a term/typ that is already a cterm/ctyp when converting it to a cterm, when possible (optimization).
* [`IsabelleException`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/control/IsabelleException.html) decodes Isabelle symbols in error messages to Unicode for better printing.
* [`Term.apply`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Term$.html#apply(context:de.unruh.isabelle.pure.Context,string:String,symbols:de.unruh.isabelle.misc.Symbols)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.MLValueTerm) / [`Typ.apply`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/pure/Typ$.html#apply(context:de.unruh.isabelle.pure.Context,string:String,symbols:de.unruh.isabelle.misc.Symbols)(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.MLValueTyp) now support Unicode strings when parsing terms/types from strings.

### Removed

* [`Setup.build`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.4.0/de/unruh/isabelle/control/Isabelle$$Setup.html#build:Boolean) is currently ignored. (Isabelle will always check all theory files whether a build is needed.)


## [0.3.0] – 2020-11-05

The biggest changes include support for Windows, multi-threaded execution in the Isabelle process,
as well as support for more ML types.

### Added

* Support for Windows (now runs on Linux, OS/X, Windows)
* [`MLValueWrapper`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/MLValueWrapper.html): 
  Utility class for adding support for new ML types (with corresponding Scala classes that simply reference them).
* [`AdHocConverter`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/AdHocConverter.html): 
  Utility class for adding support for new ML types very quickly (like `MLValueWrapper` but with less boilerplate but
  also less customizability).
* Support for further ML types:
  * `Position.T` (class [`Position`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Position.html))
  * `Thy_Header.header` (class [`TheoryHeader`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/TheoryHeader.html))
  * `Thy_Header.keywords` (class [`Keywords`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Keywords.html))
  * `Toplevel.state` (class [`ToplevelState`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/ToplevelState.html))
  * `Path.T` (Java's [`Path`](https://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html) via [`PathConverter`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/PathConverter$.html))
  * `Mutex.mutex` (class [`Mutex`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Mutex.html))
  * `Proofterm.proof` (experimental support, class [`Proofterm`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Proofterm.html))
* Support for commands sent from Isabelle to Scala (via `Control_Isabelle.sendToScala`, handled by custom handler
  [`isabelleCommandHandler`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle/SetupGeneral.html#isabelleCommandHandler)).  
* Support for connecting to an already running Isabelle instance (experimental, no library support for establishin that connection, 
  see [`SetupRunning`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle$$SetupRunning.html)).
* Class [`Isabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle.html) 
  supports to check/wait for successful initialization (by inheriting from [`FutureValue`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/misc/FutureValue.html)). 
* Class [`Isabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle.html)
  cleans resources (Isabelle process) after garbage collection
  (calling [`.destroy`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle.html#destroy():Unit) is optional).
* Java support:
  * Class [`JPatterns`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/java/JPatterns$.html)
    to allow pattern matching of terms/types in Java (based on [java-patterns](https://github.com/dominique-unruh/java-patterns) library).
  * [`JIsabelle.setupSetBuild`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/java/JIsabelle$.html#setupSetBuild(build:Boolean,setup:de.unruh.isabelle.control.Isabelle.Setup):de.unruh.isabelle.control.Isabelle.Setup)
    to toggle the `build` flag of an Isabelle
    [`Setup`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle$$Setup.html).
* Added methods:
  * [`Utils.freshName`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/misc/Utils$.html#freshName(name:String):String) for generating fresh (randomized) names. 
  * [`Theory.mutex`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Theory$.html#mutex(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitexecutionContext:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Mutex)
    returns a mutex for synchronizing theory operations.
  * [`Thm.theoryOf`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Thm.html#theoryOf:de.unruh.isabelle.pure.Theory) and
    [`Context.theoryOf`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Context.html#theoryOf(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitexecutionContext:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Theory)
    return theory behind a theorem/context.
  * [`Isabelle.checkDestroyed`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle.html#checkDestroyed():Unit)
    to assert that the Isabelle process is still available.
  * [`Theory.mergeTheories`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Theory$.html#mergeTheories(mergedName:String,endTheory:Boolean,theories:Seq[de.unruh.isabelle.pure.Theory])(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitexecutionContext:scala.concurrent.ExecutionContext):de.unruh.isabelle.pure.Theory)
    merges several theories into one.
* Supertrait [`PrettyPrintable`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/PrettyPrintable.html)
  for all classes that can invoke Isabelle for prettyprinting themselves.

### Changed

* Execution of ML code in the Isabelle process is now multi-threaded.
  (Several operations triggered from the Scala side are automatically executed concurrently.
  Use [`Mutex`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Mutex.html) if
  locking is needed.)
* Method `pretty` in classes
  [`Term`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Term.html#pretty(ctxt:de.unruh.isabelle.pure.Context,symbols:de.unruh.isabelle.misc.Symbols)(implicitec:scala.concurrent.ExecutionContext):String),
  [`Typ`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Typ.html#pretty(ctxt:de.unruh.isabelle.pure.Context,symbols:de.unruh.isabelle.misc.Symbols)(implicitec:scala.concurrent.ExecutionContext):String),
  [`Thm`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/pure/Thm.html#pretty(ctxt:de.unruh.isabelle.pure.Context,symbols:de.unruh.isabelle.misc.Symbols)(implicitec:scala.concurrent.ExecutionContext):String)
  return Unicode (instead of Isabelle's internal encoding with `\<...>` sequences). Use method `prettyRaw`
  if the old behavior is required.
* Class [`FutureValue`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/misc/FutureValue.html)
  was moved from package `de.unruh.isabelle.mlvalue` to `de.unruh.isabelle.misc`.
* Class [`Isabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle.html)
  does not take constructor parameter `build` any more. Set this flag in
  [`Setup`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle$$Setup.html) instead.
* Methods [`MLValue.Converter.exnToValue`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/MLValue$$Converter.html#exnToValue(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):String),
  [`.valueToExn`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/MLValue$$Converter.html#valueToExn(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):String),
  [`.mlType`](https://javadoc.io/static/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/MLValue$$Converter.html#mlType(implicitisabelle:de.unruh.isabelle.control.Isabelle,implicitec:scala.concurrent.ExecutionContext):String),
  [`MLStoreFunction`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/MLStoreFunction.html),
  [`MLRetrieveFunction`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/mlvalue/MLRetrieveFunction.html)
  take additional implicit arguments of types [`Isabelle`](https://javadoc.io/doc/de.unruh/scala-isabelle_2.13/0.3.0/de/unruh/isabelle/control/Isabelle.html)
  and [`ExecutionContext`](https://www.scala-lang.org/api/2.13.3/scala/concurrent/ExecutionContext.html).

### Removed

None


## [0.2.0] – 2020-10-01

The biggest changes include completed API documentation as well as improved support
for loading theory files that are not in the session image. 

### Added

* API documentation completed.
* `BigIntConverter` and `DataConverter`: support for `MLValue[BigInt]`, `MLValue[Isabelle.Data]`
* `Theory.apply(Path)`: Allows to load Isabelle theories from a file.
* `Theory.registerSessionDirectories`, `Theory.registerSessionDirectoriesNow`: 
  Register directories containing theory files. Required to find imports of theories.
* `mlvalue.Version`: Provides information about current Isabelle version.
* `MLvalue.function0`, `MLValue.compileFunction0`, `MLFunction0`: Support for `MLFunction`s
  with `unit` input.
* `Term`, `Typ` have method `concreteComputed`: Check whether `concrete` has already
  been computed (before, only `MLValueTerm`, `MLValueTyp` has this).
* `MLFunction0` ... `MLFunction7` now have `unsafeFromId(ID)`. Before, they only has `unsafeFromId(Future[ID])`.
* `MLValue.removeFuture`: Converts `Future[MLValue[A]]` into `MLValue[A]`
  by moving the future inside the `MLValue`.
* Object `JIsabelle` with helper functions for accessing `scala-isabelle` from Java.
  (Not much in there yet.)

### Changed

* `Symbols.symbolsToUnicode`, `Symbols.unicodeToSymbols` take additional optional
  argument `failUnknown`: Controls whether to silently ignore conversion errors.
* `MLStoreFunction.apply`: Removed implicit `MLValue.Converter` argument.
* `MLValue.compileFunction`: Implicit arguments renamed.
* `Thm.cterm` renamed to `Thm.proposition`: Returns the proposition of the theorem.
* `TFree.apply`, `TVar.apply`: The `sort` argument is now a `Seq[String]` instead of `String*`
* `Type.unapply` replaced by `Type.unapplySeq`: Use `case Type(name, arg1, arg2, ...)` instead of
  `case Type(name, Seq(arg1, arg2, ...)` now.
* `Theory.importMLStructure` takes only one argument now. The `newName` argument is gone, instead
  the new name of the imported structure is returned. (Two-argument version of `Theory.importMLStructure`
  is still available but deprecated.)
* Constructor argument of `Isabelle.Setup` have a different order. (Invocation using named arguments preferred.)

### Removed

* `MLRetrieveFunction.apply(ID)`, `MLRetrieveFunction.apply(Future(ID))`: Prefer the type-safe variant
  `MLRetrieveFunction.apply(MLValue[A])`. If the behavior of the removed functions is required, 
  use `MLRetrieveFunction.apply(MLValue.unsafeFromId(id))`.
* `MLValue.compileFunctionRaw`: `MLValue.compileFunction` is type-safe and preferred. 
  For low-level (unsafe) compilation, use `MLValue.compileValueRaw`instead.
* `Cterm.mlValueTerm`, `Ctyp.mlValueTyp` removed.  

[0.2.0]: https://github.com/dominique-unruh/scala-isabelle/compare/v0.1.0...v0.2.0
[0.3.0]: https://github.com/dominique-unruh/scala-isabelle/compare/v0.2.0...v0.3.0
[0.4.0]: https://github.com/dominique-unruh/scala-isabelle/compare/v0.3.0...v0.4.0
[0.4.1]: https://github.com/dominique-unruh/scala-isabelle/compare/v0.4.0...v0.4.1
[0.4.2]: https://github.com/dominique-unruh/scala-isabelle/compare/v0.4.1...v0.4.2
