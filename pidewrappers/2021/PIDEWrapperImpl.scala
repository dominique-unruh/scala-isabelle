import de.unruh.isabelle.control.PIDEWrapper

import java.nio.file.Path
import java.io.{BufferedReader, File}

import isabelle.{Bash, System_Channel, Isabelle_System, Options, Sessions, ML_Process}

class PIDEWrapperImpl(val isabelleRoot: Path) extends PIDEWrapper {
  override type Process = Bash.Process

  override def killProcess(process: Bash.Process): Unit =
    process.terminate()

  override def stdout(process: Process): BufferedReader = process.stdout
  override def stderr(process: Process): BufferedReader = process.stdout

  override def startIsabelleProcess(cwd: File, mlCode: String, logic: String): Process = {
    val channel = System_Channel()
    Isabelle_System.init(isabelle_root = isabelleRoot.toString)
    val options = Options.init()
    val channel_options = options.string.update("system_channel_address", channel.address).
      string.update("system_channel_password", channel.password)

    val sessions_structure = Sessions.load_structure(options)
    val store = Sessions.store(options)
    ML_Process(channel_options, sessions_structure, store, eval_main = mlCode, logic = logic,
      cwd = cwd, cleanup = () => print("*** CLEANUP ***"))
  }

  override def result(process: Process): Unit = {
    process.result(
      progress_stderr = { s:String => println(f"*[$s]") },
      progress_stdout = { s:String => println(f" [$s]") }
    )
  }
}
