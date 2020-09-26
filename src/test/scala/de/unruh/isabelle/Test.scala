package de.unruh.isabelle

import java.nio.file.{Files, Path}

import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLValue}
import de.unruh.isabelle.pure.{Abs, App, Const, Context, PathConverter, TFree, TVar, Term, Theory, Typ}
import org.scalatest.funsuite.AnyFunSuite

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import MLValue.{compileFunction, compileValue}

import scala.collection.JavaConverters.asScalaIteratorConverter

// Implicits
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.control.IsabelleTest.isabelle
import pure.Implicits._
import mlvalue.Implicits._

class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }


  test("temporary experiments") {
    MLValue.init()
    Theory.init()
    PathConverter.init()

    val initBase: MLFunction3[List[(String,String)], List[String], List[(String,String)], Unit] =
      MLValue.compileFunction("fn (gl,lo,kn) => Resources.init_session_base {sessions=[], docs=[], global_theories=gl, loaded_theories=lo, known_theories=kn}")

    val importsOf: MLFunction[Theory, List[String]] =
      MLValue.compileFunction("Resources.imports_of #> map fst")
    val lookupTheory: MLFunction[String, Option[Theory]] =
      MLValue.compileFunction("Thy_Info.lookup_theory")
    val useTheory: MLFunction3[Path, String, String, Unit] =
      MLValue.compileFunction[Path, String, String, Unit](
        """fn (dir,qual,name) => Thy_Info.use_theories {options = Options.default (), symbols = HTML.no_symbols, bibtex_entries = [], last_timing = K Time.zeroTime}
                                 qual dir [(name, Position.none)]""")

    val theoryName: MLFunction[Theory, String] =
      MLValue.compileFunction("Context.theory_id #> Context.theory_id_long_name")
    val loadedTheories: MLFunction[Unit, List[String]] =
      MLValue.compileFunction("Resources.global_session_base_value #> #loaded_theories #> Symtab.keys")
    val theoryImports: MLFunction[Path, List[String]] =
      MLValue.compileFunction("File.read #> Thy_Header.read Position.none #> #imports #> map fst")
    val parentNamesOf = compileFunction[Theory,List[String]]("Theory.parents_of #> map Context.theory_id #> map Context.theory_id_long_name")

    def thyName(thy: Theory) = theoryName(thy).retrieveNow

    def printThyName(thy: Theory): Unit = println(thyName(thy))

/*    def theoryFinder(name: String): Path = {
      val Array(qual,shortName) = name.split('.')
      val src = isabelle.setup.isabelleHome.resolve("src")
      val sessionDir = qual match {
        case "HOL-Library" => src.resolve("HOL/Library")
        case "Program-Conflict-Analysis" => Path.of("/opt/afp-2019/thys/Program-Conflict-Analysis")
      }
      sessionDir.resolve(shortName+".thy")
    }*/

/*    @tailrec
    def loadTheory(name: String, relativeTo: String = ""): Theory = lookupTheory(name).retrieveNow match {
        // global theory names (like Main) that are not in the session image are not supported
      case Some(thy) => thy
      case None =>
        if (name.contains('.')) {
          val path = theoryFinder(name)
          println(s"$name -> $path")
          loadTheoryFrom(name, path)
        } else {
          loadTheory(s"$relativeTo.$name")
        }
    }*/

/*    def loadTheoryFrom(name: String, path: Path): Theory = {
      val (qualifier, shortName) = name.split('.') match {
        case Array(qualifier, shortName) => (qualifier, shortName)
        case Array(_) => ???
        case _ => fail()
      }
      val file = path.getFileName
      val dir = path.getParent
      assert(file.toString == s"$shortName.thy")
      val imports = theoryImports(path).retrieveNow
      println(s"$name imports ${imports.mkString(" ")}")
      for (imprt <- imports)
        loadTheory(imprt, relativeTo = qualifier)
      println(s"Use: $qualifier $shortName @ $dir")
      useTheory(dir, qualifier, shortName).retrieveNow
      lookupTheory(name).retrieveNow match {
        case None => fail()
        case Some(thy) =>
          println(s"Got: ${thyName(thy)}")
          thy
      }
    }*/

//    val loaded = new mutable.HashSet[String]()

    val updateKnownTheories = compileFunction[List[(String,String)], Unit]("""fn known => let
val names = Thy_Info.get_names ()
val global = names |> List.mapPartial (fn n => case Resources.global_theory n of SOME session => SOME (n,session) | NONE => NONE)
val loaded = names |> filter Resources.loaded_theory

in
  Resources.init_session_base {sessions=[], docs=[], global_theories=global, loaded_theories=[], known_theories=known}
end
        """)


    val theoryNames = compileValue[List[String]]("Thy_Info.get_names ()").retrieveNow
    val loadedTheory = compileFunction[String, Boolean]("Resources.loaded_theory")


/*    for (thy <- theoryNames)
      if (loadedTheory(thy).retrieveNow)
        loaded += thy

*/

    val known = ListBuffer[(String,String)]()

/*    val theoryQualifier = MLValue.compileFunction[String,String]("Resources.theory_qualifier")
    val globalTheory = compileFunction[String,Option[String]]("Resources.global_theory")*/

/*    def addToLoaded(name: String) : Unit =
      if (!loaded.contains(name)) {
        println(s"Checking ${name}")
        val thy = Theory(name)
//        val imports = importsOf(thy).retrieveNow
//        println(s"Imports: ${imports.mkString(" ")}")

        val parents = parentNamesOf(thy).retrieveNow
        println(s"Imports: ${parents.mkString(" ")}")
        for (parent <- parents)
          addToLoaded(parent)

/*        for (imprt <- imports) {
          globalTheory(imprt).retrieveNow match {
            case Some(_) => addToLoaded(imprt)
            case None =>
              if (imprt.contains('.'))
                addToLoaded(imprt)
              else {
                val qualifier = theoryQualifier(name).retrieveNow
                println(s"Qualifier for $name: $qualifier")
                addToLoaded(s"$qualifier.$imprt")
              }
          }
        }*/

        println(s"Adding $name")
        loaded += name
      }
    addToLoaded("Main")
    addToLoaded("Complex_Main")*/

    def addFromSession(session: String, path: Path): Unit =
      for (file <- Files.list(path).iterator().asScala;
           fileName = file.getFileName.toString;
//           _ = println("XXX"+fileName);
           if fileName.endsWith(".thy");
           thyName = fileName.stripSuffix(".thy"))
        known += session + "." + thyName -> path.resolve(file).toString

    addFromSession("HOL-Library", isabelle.setup.isabelleHome.resolve("src/HOL/Library"))

    addFromSession("Program-Conflict-Analysis", Path.of("/opt/afp-2019/thys/Program-Conflict-Analysis")


//    val glob = loaded.toList.collect(Function.unlift { thy:String => globalTheory(thy).retrieveNow match {
//      case Some(session) => Some(thy -> session)
//      case None => None
//    }})

//    initBase(glob,loaded.toList,known.toList).retrieveNow

    updateKnownTheories(known.toList).retrieveNow

    val thyMain = Theory("Main")
    printThyName(thyMain)

    val thyHolSet = Theory("HOL.Set")
    printThyName(thyHolSet)

    val thyAList = Theory("HOL-Library.AList")
    printThyName(thyAList)

    val thyBigO = Theory("HOL-Library.BigO")
    printThyName(thyBigO)

    val thyFlowgraph = Theory("Program-Conflict-Analysis.MainResult")
    printThyName(thyFlowgraph)

    //    println(loadedTheories(()).retrieveNow)
  }
}
