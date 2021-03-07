import de.unruh.isabelle.control.PIDEWrapper

import java.nio.file.Path
import java.io.{BufferedReader, File}
import isabelle.{Bash, Isabelle_System, ML_Process, Options, Sessions, System_Channel}

import java.util.concurrent.Callable
import java.util.function.Consumer

class PIDEWrapperImpl(val isabelleRoot: Path) extends PIDEWrapper {
  override type Process = Bash.Process

  override def killProcess(process: Bash.Process): Unit =
    process.terminate()

//  override def stdout(process: Process): BufferedReader = process.stdout
//  override def stderr(process: Process): BufferedReader = process.stdout

  override def startIsabelleProcess(cwd: File, mlCode: String, logic: String): Process = {
    val channel = System_Channel()
    Isabelle_System.init(isabelle_root = isabelleRoot.toString)
    val options = Options.init()
    val channel_options = options.string.update("system_channel_address", channel.address).
      string.update("system_channel_password", channel.password)

    val sessions_structure = Sessions.load_structure(options)
    val store = Sessions.store(options)
    ML_Process(channel_options, sessions_structure, store, eval_main = mlCode, logic = logic,
      cwd = cwd)
  }

  override def waitForProcess(process: Process, progress_stdout: Consumer[String], progress_stderr: Consumer[String]): Unit = {
    process.result(progress_stderr = progress_stderr.accept, progress_stdout = progress_stdout.accept)
  }
}
