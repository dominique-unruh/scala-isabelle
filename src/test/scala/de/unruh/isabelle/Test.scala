package de.unruh.isabelle

import de.unruh.isabelle.control.{IsabelleTest, PIDEWrapper}
import de.unruh.isabelle.misc.Utils
import de.unruh.isabelle.mlvalue.MLValue
import de.unruh.isabelle.pure.{Context, Cterm, Term, Thm}
import org.scalatest.funsuite.AnyFunSuite

import _root_.java.net.{URL, URLClassLoader}
import _root_.java.nio.file.{FileVisitOption, Files, Path, Paths}
import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable.ListBuffer
import scala.reflect.api.JavaUniverse
import scala.reflect.internal.{StdNames, SymbolTable}
import scala.util.Random

// Implicits
import de.unruh.isabelle.control.IsabelleTest.isabelle
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._

class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("Java example") {
    JavaExample.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("temporary experiments") {
    val isaRoot = Paths.get("/opt/Isabelle2021")


    val mlCode =
      """tracing "1";;
        |fun f x = f x;;
        |""".stripMargin

//    val jars = for (file <- Files.walk(Paths.get(isaRoot)).iterator().asScala
//         if file.getFileName.toString.endsWith(".jar")
//         if Files.isRegularFile(file))
//      yield file.toUri.toURL

//    val classloader = new _root_.java.net.URLClassLoader(
//      "Isabelle2021 PIDE", jars.toArray, getClass.getClassLoader)

//    val pideWrapper = classloader.loadClass("de.unruh.isabelle.control.PIDEWrapper2021").getDeclaredConstructor()
//      .newInstance().asInstanceOf[PIDEWrapper]

    val pideWrapper = PIDEWrapper.getPIDEWrapper(isaRoot)

    val process = pideWrapper.startIsabelleProcess(mlCode=mlCode)
//
//    import scala.reflect.runtime.universe
//    import scala.reflect.runtime.universe._
//    val mirror = runtimeMirror(classloader)
//
//    val stdNames = universe.asInstanceOf[scala.reflect.runtime.JavaUniverse] : StdNames
//    def defaultGetterName(name: String, pos: Int) =
//      name + stdNames.termNames.DEFAULT_GETTER_STRING + pos.toString
//
//    def module[A](name: String): A =
//      mirror.reflectModule(mirror.staticModule(name)).instance.asInstanceOf[A]
//
//    import scala.language.reflectiveCalls
//
////    type System_Channel_Module = { def apply(): AnyRef }
//
//    val systemChannelModule = module[{ def apply(): AnyRef }]("isabelle.System_Channel")
//    val channel = systemChannelModule.apply().asInstanceOf[System_Channel]
////    val channel = System_Channel()
//
//    type Isabelle_System_Module = { def init(isabelle_root: String, cygwin_root: String): Unit }
//
//    val isabelleSystemModule = module[Isabelle_System_Module]("isabelle.Isabelle_System")
//    isabelleSystemModule.init(isabelle_root = isaRoot, cygwin_root = "")
////    Isabelle_System.init(isabelle_root = isaRoot)
//
//    type Path_T = _root_.isabelle.Path
//
//    type Options_Module = {
//      val PREFS : Path_T
//      def read_prefs(file: Path_T): String
//      def init(prefs: String, opts: List[String]) : AnyRef
//    }
//
//    def invoke(module: ModuleMirror, name: String)(args: Any*) = {
//      val m = module.symbol.typeSignature.decl(TermName(name)).asMethod
//      val m2 = mirror.reflect(module.instance).reflectMethod(m)
//      m2.apply(args : _*)
//    }
//
////    val optionsModule = module[Options_Module]("isabelle.Options")
//    val optionsModule = mirror.reflectModule(mirror.staticModule("isabelle.Options"))
//    val options = invoke(optionsModule, "init")(invoke(optionsModule, defaultGetterName("init", 1))(), Nil)
//      .asInstanceOf[Options]
////    val options = Options.init()
//
//    val channel_options = options.string.update("system_channel_address", channel.address).
//        string.update("system_channel_password", channel.password)
//
//    val sessions_structure = Sessions.load_structure(options)
////    val sessions_structure = Sessions.Structure.empty
//    val store = Sessions.store(options)
//
//    val process = ML_Process(options, sessions_structure, store, eval_main = mlCode)

    println(1,process)

    pideWrapper.result(process)

    println(2,process)
  }
}
