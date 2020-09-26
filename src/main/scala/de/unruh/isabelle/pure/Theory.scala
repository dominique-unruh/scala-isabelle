package de.unruh.isabelle.pure

import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap

import de.unruh.isabelle.control.{Isabelle, OperationCollection}
import de.unruh.isabelle.mlvalue.MLValue.Converter
import de.unruh.isabelle.mlvalue.{FutureValue, MLFunction, MLFunction3, MLValue, Version}
import de.unruh.isabelle.pure.Theory.Ops
import org.log4s

import scala.collection.JavaConverters.{asScalaIteratorConverter, mapAsScalaMapConverter}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._


/** Represents a theory (ML type `theory`) in the Isabelle process.
 *
 * An instance of this class is merely a thin wrapper around an [[mlvalue.MLValue MLValue]],
 * all explanations and examples given for [[Context]] also apply here.
 *
 * The name of the theory can be retrieved via the member [[name]] if the theory was created
 * by [[Theory.apply]]`(ctxt, name)`. Otherwise, [[name]] returns a placeholder.
 */
final class Theory private [Theory](val name: String, val mlValue : MLValue[Theory]) extends FutureValue {
  override def toString: String = s"theory $name${mlValue.stateString}"

  /** Imports an ML structure from a theory into the global ML namespace.
   *
   * WARNING: This has a global effect on the Isabelle process because it modifies the ML name space.
   *
   * In an Isabelle theory `T`, it is possible to include ML source code using the `ML_file` command and related commands.
   * In that ML source code, new symbols (values, types, structures) can be declared. These will be visible
   * to further ML code in the same theory `T` and in theories that import `T`. However, it is not directly possible
   * to use those symbols in ML code on the ML toplevel (i.e., in commands such as [[control.Isabelle.executeMLCode Isabelle.executeMLCode]]
   * or [[mlvalue.MLValue.compileValue MLValue.compileValue]] and friends). Instead, the symbols must be imported using this method. (Only supported
   * for ML structures, not for values or types that are declared outside a structure.) [[importMLStructure]]`(name,newName)`
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
  // TODO: Check if the above example works. (test case)
  // TODO: Is there an alternative for this that does not affect the global namespace? (And then deprecate.)
  def importMLStructure(name: String, newName: String)
                       (implicit isabelle: Isabelle, executionContext: ExecutionContext): Unit =
    Ops.importMLStructure(this, name, newName).retrieveNow

  override def await: Unit = mlValue.await
  override def someFuture: Future[Any] = mlValue.someFuture
}

object Theory extends OperationCollection {
  // DOCUMENT
  def registerSessionDirectoriesNow(paths: (String,Path)*)(implicit isabelle: Isabelle, ec: ExecutionContext): Unit =
    Await.result(registerSessionDirectories(paths : _*), Duration.Inf)
  def registerSessionDirectories(paths: (String,Path)*)(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Unit] = {
    var changed = false
    for ((session, path) <- paths) {
      val absPath = path.toAbsolutePath
      if (!Files.isDirectory(absPath))
        throw new IllegalArgumentException(s"Session directory for session $session is not a directory ($path)")
      logger.debug(s"registerTheoryPaths: Session $session -> $absPath")
      val previous = Ops.sessionPaths.put(session, absPath)
      if (previous != absPath)
        changed = true
    }

    if (!changed) return Future.successful(())

    logger.debug(Ops.sessionPaths.toString)

    if (Version.from2020) {
      Ops.updateKnownTheories(Ops.sessionPaths.asScala.toList.map { case (n,p) => (p.toString,n) }).retrieve
    } else {
      val thyPaths = ListBuffer[(String,String)]()
      for ((session,path) <- Ops.sessionPaths.asScala;
           file <- Files.list(path).iterator().asScala;
           fileName = file.getFileName.toString;
           if fileName.endsWith(".thy");
           thyName = fileName.stripSuffix(".thy"))
        thyPaths += s"$session.$thyName" -> path.resolve(file.toString).toString
      Ops.updateKnownTheories(thyPaths.toList).retrieve
    }
  }

  override protected def newOps(implicit isabelle: Isabelle, ec: ExecutionContext): Ops = new Ops()
  //noinspection TypeAnnotation
  protected[isabelle] class Ops(implicit val isabelle: Isabelle, ec: ExecutionContext) {

    import MLValue.compileFunction

    MLValue.init()
    isabelle.executeMLCodeNow("exception E_Theory of theory")

    val sessionPaths = new ConcurrentHashMap[String, Path]()

    /** Before Isabelle2020: Expects (theory-name, theory-file) pairs. From 2020: (session-name, directory) */
    val updateKnownTheories = {
      val initSession = if (Version.from2020)
        "{session_directories=known, session_positions=[], docs=[], global_theories=global, loaded_theories=[]}"
      else
        "{sessions=[], docs=[], global_theories=global, loaded_theories=[], known_theories=known}"

      compileFunction[List[(String, String)], Unit](
        s"""fn known => let
        val names = Thy_Info.get_names ()
        val global = names |> List.mapPartial (fn n => case Resources.global_theory n of SOME session => SOME (n,session) | NONE => NONE)
        val loaded = names |> filter Resources.loaded_theory
        in
          Resources.init_session_base $initSession
        end
        """)
    }

    val loadTheory =
      MLValue.compileFunction[String, String, Theory]("fn (name1,name2) => (Thy_Info.use_thy name1; Thy_Info.get_theory name2)")
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

  /** Retrieves a theory by its name. E.g., `Theory("HOL-Analysis.Inner_Product")`. **/
  // DOCUMENT caveats: full name, only names in heap (unless registerTheoryPaths), ignores ROOT/ROOTS
  // TODO find out (& document) what happens when we register a session path for an already loaded session (with correct/different path)
  def apply(name: String)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory =
    Ops.loadTheory(name, name).retrieveNow

  // DOCUMENT (mention: path relative to isabelle wd, where are imports searched (find ou)
  def apply(path: Path)(implicit isabelle: Isabelle, ec: ExecutionContext): Theory = {
    val filename = path.getFileName.toString
    if (!filename.endsWith(".thy"))
      throw new IllegalArgumentException("Theory file must end in .thy")
    val thyName = filename.stripSuffix(".thy")
    val thyPath = path.toString.stripSuffix(".thy")
    Ops.loadTheory(thyPath, s"Draft.$thyName").retrieveNow
  }

  /** Representation of theories in ML. (See the general discussion of [[Context]], the same things apply to [[Theory]].)
   *
   *  - ML type: `theory`
   *  - Representation of theory `thy` as an exception: `E_Theory thy`
   *
   * (`E_Theory` is automatically declared when needed by the ML code in this package.
   * If you need to ensure that it is defined for compiling own ML code, invoke [[Theory.init]].)
   *
   * Available as an implicit value by importing [[de.unruh.isabelle.pure.Implicits]]`._`
   **/
  object TheoryConverter extends Converter[Theory] {
    override def retrieve(value: MLValue[Theory])(implicit isabelle: Isabelle, ec: ExecutionContext): Future[Theory] =
      for (_ <- value.id)
        yield new Theory(mlValue = value, name="‹theory›")
    override def store(value: Theory)(implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[Theory] =
      value.mlValue
    override lazy val exnToValue: String = "fn E_Theory thy => thy"
    override lazy val valueToExn: String = "E_Theory"

    override def mlType: String = "theory"
  }

  private val logger = log4s.getLogger
}
