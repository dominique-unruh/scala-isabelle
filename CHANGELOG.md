# Changelog

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
