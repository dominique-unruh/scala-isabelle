package de.unruh.isabelle.pure

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.ConcurrentHashMap

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue.{Converter, compileFunction}
import de.unruh.isabelle.mlvalue.{FutureValue, MLFunction, MLFunction3, MLValue, Version}
import de.unruh.isabelle.pure.Theory.Ops
import org.log4s

import scala.collection.JavaConverters.{asScalaIteratorConverter, mapAsScalaMapConverter}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Random

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._


/** Represents a theory (ML type `theory`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * The name of the theory can be retrieved via the member [[name]] if the theory was created
 * by [[Theory.apply(name:* Theory]]`(name)`. Otherwise, [[name]] returns a placeholder.
 *
 * An implict [[mlvalue.MLValue.Converter MLValue.Converter]] can be imported from [[Implicits]]`._`. The representation
 * of a theory `thy` as an ML exception is `E_Theory thy`.
 */
final class Theory private [Theory](val name: String, val mlValue : MLValue[Theory]) extends FutureValue {
  override def toString: String = s"theory $name${mlValue.stateString}"

  /** Imports an ML structure from a theory into the global ML namespace.
   *
   * WARNING: This has a global effect on the Isabelle process because it modifies the ML name space.
   * Use [[importMLStructure(name:String)* importMLStructure(String)]] instead.
   *
   * In an Isabelle theory `T`, it is possible to include ML source code using the `ML_file` command and related commands.
   * In that ML source code, new symbols (values, types, structures) can be declared. These will be visible
   * to further ML code in the same theory `T` and in theories that import `T`. However, it is not directly possible
   * to use those symbols in ML code on the ML toplevel (i.e., in commands such as [[control.Isabelle.executeMLCode Isabelle.executeMLCode]]
   * or [[mlvalue.MLValue.compileValue MLValue.compileValue]] and friends). Instead, the symbols must be imported using this method. (Only supported
   * for ML structures, not for values or types that are declared outside a structure.) `importMLStructure(name,newName)`
   * will import the structure called `name` under the new name `newName` into the toplevel.
   *
   * We give an example.
   *
   * File `test.ML`:
   * {{{
   *   structure Test = struct
   *   val num = 123
   *   end
   * }}}
   * This declares a structure `Test` with a value member `num`.
   *
   * File `TestThy.thy`:
   * {{{
   *   theory TestThy imports Main begin
   *
   *   ML_file "test.ML"
   *
   *   end
   * }}}
   * This declares a theory which loads the ML code from "test.ML" (and thus all theories importing `TestThy`
   * have access to the ML structure `Test`).
   *
   * In Scala:
   * {{{
   *   implicit val isabelle = new Isabelle(... suitable setup ...)
   *   val thy : Theory = Theory("Draft.TestThy")                  // load the theory TestThy
   *   val num1 : MLValue[Int] = MLValue.compileValue("Test.num")  // fails
   *   thy.importMLStructure("Test", "Test")                       // import the structure Test into the ML toplevel
   *   val num2 : MLValue[Int] = MLValue.compileValue("Test.num")  // access Test in compiled ML code
   *   println(num2.retrieveNow)                                   // ==> 123
   * }}}
   */
  @deprecated("Use importMLStructure(String) instead","0.1.1")
  def importMLStructure(name: String, newName: String)
                       (implicit isabelle: Isabelle, executionContext: ExecutionContext): Unit =
    Ops.importMLStructure(this, name, newName).retrieveNow

  /** Imports an ML structure from a theory into the global ML namespace.
   *
   * In an Isabelle theory `T`, it is possible to include ML source code using the `ML_file` command and related commands.
   * In that ML source code, new symbols (values, types, structures) can be declared. These will be visible
   * to further ML code in the same theory `T` and in theories that import `T`. However, it is not directly possible
   * to use those symbols in ML code on the ML toplevel (i.e., in commands such as [[control.Isabelle.executeMLCode Isabelle.executeMLCode]]
   * or [[mlvalue.MLValue.compileValue MLValue.compileValue]] and friends). Instead, the symbols must be imported using this method. (Only supported
   * for ML structures, not for values or types that are declared outside a structure.) [[importMLStructureNow]]`(name)`
   * (or [[importMLStructure(name:String)* importMLStructure]]`(name)` for asynchronous execution)
   * imports the structure called `name` under a new (unique) name into the toplevel, and returns the name of the
   * structure.
   *
   * We give an example.
   *
   * File `importMe.ML`:
   * {{{
   *   structure ImportMe = struct
   *   val num = 123
   *   end
   * }}}
   * This declares a structure `ImportMe` with a value member `num`.
   *
   * File `ImportMeThy.thy`:
   * {{{
   *   theory ImportMeThy imports Main begin
   *
   *   ML_file "importMe.ML"
   *
   *   end
   * }}}
   * This declares a theory which loads the ML code from "importMe.ML" (and thus all theories importing `ImportMeThy`
   * have access to the ML structure `ImportMe`).
   *
   * In Scala:
   * {{{
   *   implicit val isabelle = new Isabelle(... suitable setup ...)
   *   val thy : Theory = Theory(Path.of("ImportMeThy.thy"))              // load the theory TestThy
   *   val num1 : MLValue[Int] = MLValue.compileValue("ImportMe.num")     // fails
   *   val importMe : String = thy.importMLStructureNow("ImportMe")       // import the structure Test into the ML toplevel
   *   val num2 : MLValue[Int] = MLValue.compileValue(s"\${importMe}.num") // access Test (under new name) in compiled ML code
   *   println(num2.retrieveNow)                                          // ==> 123
   * }}}
   */
  // The example is tested in TheoryTest
  def importMLStructureNow(name: String)(implicit isabelle: Isabelle, executionContext: ExecutionContext) : String =
    Await.result(importMLStructure(name), Duration.Inf)

  /** Like [[importMLStructureNow]] but returns a future containing the name of the imported structure without delay. */
  def importMLStructure(name: String)(implicit isabelle: Isabelle, executionContext: ExecutionContext) : Future[String] = {
    import scalaz.syntax.id._
    val newName = Utils.freshName(name).capitalize
    for (_ <- Ops.importMLStructure(this, name, newName).retrieve)
      yield newName
  }

  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
}

object Theory extends OperationCollection {
  /** Creates a new theory that imports several other theories.
   * (Equivalent to `theory MergedTheoryName import Theory1 Theory2 Theory3 ... begin` in Isabelle/Isar.)
   *
   * A typical use case would be to create a context that inherits from several theories at once,
   * E.g., [[Context]]`(Theory.mergeTheories(theories=List(theory1, theory2, theory3)))`.
   * (Or shorter: [[Context]]`(`[[mergeTheories(theories:* mergeTheories]]`(theory1, theory2, theory3))`.)
   *
   * @param mergedName The unqualified name of the new theory. The default is an autogenerated unique (randomized) name.
   * @param endTheory Whether to the returned theory should be "finished" (analogue to the "end" command in Isabelle/Isar).
   * @param theories The theories to import
   * @return a theories inheriting from all theories `theories`
   **/
  def mergeTheories(mergedName: String = null,
                    endTheory: Boolean = true,
                    theories: Seq[Theory])
                   (implicit isabelle: Isabelle, executionContext: ExecutionContext): Theory = {
    val mergedName2 =
      if (mergedName==null) Utils.freshName("Merged_Theory")
      else mergedName

    Ops.mergeTheories(mergedName2, endTheory, theories.toList).retrieveNow
  }

  /** Same as [[mergeTheories(me* mergeTheories]](theories = theories). */
  def mergeTheories(theories: Theory*)(implicit isabelle: Isabelle, executionContext: ExecutionContext): Theory =
    mergeTheories(theories = theories)

  /** Registers session directories.
   *
   * Each Isabelle session (such as `HOL`, `HOL-Library`, `FOL`, ...) has one (or several) associated session directories
   * that contain the theory files. (E.g., the session directory of `HOL` is `.../src/HOL`, and thus the theory
   * `HOL.List` is found in `.../src/HOL/List.thy`.) The session directories are configured in Isabelle via the
   * `ROOT` and `ROOTS` files. Currently, however, scala-isabelle cannot read the `ROOT(S)` files. Instead,
   * session directories need to be registered with this function.
   *
   * More specifically:
   *  - When Isabelle loads a theory `X.Y`, and `X.Y` is in contained the session image (configured with
   *    [[control.Isabelle.Setup.logic Isabelle.Setup.logic]]), then the theory contained in the session image is used.
   *    (Conflicting session directory specifications will be ignored.)
   *  - Otherwise, the session directory `D` for `X` is looked up, and the file `D/Y.thy` is read.
   *  - Otherwise `Y.thy` in the current directory is opened.
   *  - When a theory file is read, resolution of the imports of that theory is done the same way.
   *  - One session can have several session directories, but one session directory must not be shared by
   *    several sessions
   *
   * @param paths Pairs `(session, dir)`. Meaning that `dir` is a session directory for `session`.
   * @see [[Theory.apply(name:* Theory.apply(String)]]
   */
  def registerSessionDirectoriesNow(paths: (String,Path)*)(implicit isabelle: Isabelle, ec: ExecutionContext): Unit =
    Await.result(registerSessionDirectories(paths : _*), Duration.Inf)

  /** Like [[registerSessionDirectoriesNow]] but returns a [[scala.concurrent.Future Future]]. Only once the future completes successfully,
   * the session directories are guaranteed to have been registered.
   **/
  def registerSessionDirectories(paths: (String,Path)*)(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Unit] = {
    var changed = false
    for ((session, path) <- paths) {
      val absPath = path.toAbsolutePath
      if (!Files.isDirectory(absPath))
        throw new IllegalArgumentException(s"Session directory for session $session is not a directory ($path)")
      logger.debug(s"registerSessionDirectories: Session $session -> $absPath")
      val previous = Ops.sessionPaths.put(session, absPath)
      if (previous != absPath)
        changed = true
    }

    if (!changed) return Future.successful(())

    logger.debug(Ops.sessionPaths.toString)

    if (Version.from2020) {
      Ops.updateKnownTheories2020(Ops.sessionPaths.asScala.toList.map { case (n,p) => (p,n) }).retrieve
    } else {
      val thyPaths = ListBuffer[(String,Path)]()
      for ((session,path) <- Ops.sessionPaths.asScala;
           file <- Files.list(path).iterator().asScala;
           fileName = file.getFileName.toString;
           if fileName.endsWith(".thy");
           thyName = fileName.stripSuffix(".thy"))
        thyPaths += s"$session.$thyName" -> path.resolve(file.toString)
      Ops.updateKnownTheories2019(thyPaths.toList).retrieve
    }
  }

  /** A mutex (in the Isabelle process) for synchronizing non-threadsafe theory operations (e.g., `Thy_Info.use_thy`).
   * This mutex is internally by operations in [[Theory]]. Use this mutex to avoid concurrent execution with
   * the functions in [[Theory]].
   *
   * @see [[Mutex.wrapWithMutex]] for a helper function to do locking with an Isabelle [[Mutex]]
   **/
  def mutex(implicit isabelle: Isabelle, executionContext: ExecutionContext): Mutex = Theory.Ops.theoryMutex

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  //noinspection TypeAnnotation
  protected[isabelle] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {
    import MLValue.compileFunction

//    MLValue.init()
//    isabelle.executeMLCodeNow("exception E_Theory of theory")

    val sessionPaths = new ConcurrentHashMap[String, Path]()

    /** Expects (directory, session-name) pairs. Works on Isabelle2020+ */
    lazy val updateKnownTheories2020 = compileFunction[List[(Path, String)], Unit](
      s"""fn known => let
        val known = map (apfst Path.implode) known
        val names = Thy_Info.get_names ()
        val global = names |> List.mapPartial (fn n => case Resources.global_theory n of SOME session => SOME (n,session) | NONE => NONE)
        val loaded = names |> filter Resources.loaded_theory
        in
          Resources.init_session_base {session_directories=known, session_positions=[], docs=[], global_theories=global, loaded_theories=[]}
        end
        """)

    /** Expects (theory-name, theory-file) pairs. Works on Isabelle2019 */
    lazy val updateKnownTheories2019 = compileFunction[List[(String, Path)], Unit](
      s"""fn known => let
        val known = map (apsnd Path.implode) known
        val names = Thy_Info.get_names ()
        val global = names |> List.mapPartial (fn n => case Resources.global_theory n of SOME session => SOME (n,session) | NONE => NONE)
        val loaded = names |> filter Resources.loaded_theory
        in
          Resources.init_session_base {sessions=[], docs=[], global_theories=global, loaded_theories=[], known_theories=known}
        end
        """)

    val mergeTheories =
      MLValue.compileFunction[String, Boolean, List[Theory], Theory](
        """fn (name, endThy, thys) => let
          |val thy = Theory.begin_theory (name, Position.none) thys
          |val thy = if endThy then Theory.end_theory thy else thy
          |in thy end""".stripMargin)

    val theoryMutex = Mutex()

    val loadTheoryInternal =
      MLValue.compileFunction[Mutex, String, Theory](
        s"fn (mutex,name) => (${Mutex.wrapWithMutex("mutex", "Thy_Info.use_thy name")}; Thy_Info.get_theory name)")
    val loadTheoryPath =
      MLValue.compileFunction[Mutex, Path, String, Theory](
        s"fn (mutex,path,name) => (${Mutex.wrapWithMutex("mutex", "Thy_Info.use_thy (Path.implode path)")}; Thy_Info.get_theory name)")
    val importMLStructure : MLFunction3[Theory, String, String, Unit] = compileFunction(
      """fn (thy,theirName,hereStruct) => let
                  val theirAllStruct = Context.setmp_generic_context (SOME (Context.Theory thy))
                                       (#allStruct ML_Env.name_space) ()
                  val theirStruct = case List.find (fn (n,_) => n=theirName) theirAllStruct of
                           NONE => error ("Structure " ^ theirName ^ " not declared in given context")
        | SOME (_,s) => s
                  val _ = #enterStruct ML_Env.name_space (hereStruct, theirStruct)
                  in () end""")
  }

  /** Retrieves a theory by its name. E.g., `Theory("HOL-Analysis.Inner_Product")`.
   *
   * `name` must be the fully qualified name of the theory (with exception of "global theories"
   * which have no qualifier, e.g., `Pure`, `Main`, `Complex_Main`, ...).
   *
   * For a theory to be found, it needs to be included in the current session image (specified via the
   * [[control.Isabelle.Setup.logic logic]] parameter in [[control.Isabelle.Setup Isabelle.Setup]]), or the directory containing its that theory's
   * session must be configured via [[registerSessionDirectoriesNow]]. The same requirements apply to all theories imported
   * by the theory `name`. (I.e., all theories required to execute `name` must be either in the session image or
   * registered via [[registerSessionDirectoriesNow]].) `ROOT` and `ROOTS` are not taken into account for finding the
   * theories.
   *
   * Note: This function invokes `Thy_Info.use_thy` in the Isabelle process. That function is not thread-safe.
   * Therefore separate invocations of `apply` will not be executed in parallel.
   * (Locking happens on Isabelle side, thus this function returns immediately anyway.)
   * If you want to invoke `Thy_Info.use_thy` or related functions yourself, please use [[Theory.mutex]]
   * to avoid concurrent execution with this function.
   **/
  def apply(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory =
    Ops.loadTheoryInternal(Ops.theoryMutex, name).retrieveNow

  /** Retrieves a theory located at the path `path`.
   *
   * The `path` is interpreted relative to the Isabelle working directory
   * ([[control.Isabelle.Setup.workingDirectory Isabelle.Setup.workingDirectory]]).
   *
   * If `path` ends in `X.thy`, the theory will have the fully qualified name `Draft.X`.
   *
   * Unqualified imports of the theory `X` are searched for in the same directory. Qualified imports
   * must be findable according to the rules specified in [[apply(name* apply(String)]].
   *
   * The note about thread-safety from [[apply(name* apply(String)]] applies for this function, too.
   **/
  def apply(path: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = {
    val filename = path.getFileName.toString
    if (!filename.endsWith(".thy"))
      throw new IllegalArgumentException("Theory file must end in .thy")
    val thyName = filename.stripSuffix(".thy")
    val thyPath = path.getParent match { case null => Paths.get(thyName); case p => p.resolve(thyName) }
    Ops.loadTheoryPath(Ops.theoryMutex, thyPath, s"Draft.$thyName").retrieveNow
  }

  /** Representation of theories in ML. (See the general discussion of [[Context]], the same things apply to [[Theory]].)
   *
   *  - ML type: `theory`
   *  - Representation of theory `thy` as an exception: `E_Theory thy`
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object TheoryConverter extends Converter[Theory] {
    override def retrieve(value: MLValue[Theory])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Theory] =
      Future.successful(new Theory(mlValue = value, name="‹theory›"))
    override def store(value: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Theory] =
      value.mlValue
    override def exnToValue(implicit isabelle: Isabelle, ec: ExecutionContext): String = "fn E_Theory thy => thy"
    override def valueToExn(implicit isabelle: Isabelle, ec: ExecutionContext): String = "E_Theory"

    override def mlType(implicit isabelle: Isabelle, ec: ExecutionContext): String = "theory"
  }

  private val logger = log4s.getLogger
}
