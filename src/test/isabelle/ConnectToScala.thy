theory ConnectToScala
  imports Main  
begin

ML \<open>
fun system string = if OS.Process.system string |> OS.Process.isSuccess then () else
  error ("Command " ^ string ^ " returned non-zero error code")
val inputPipeName = "/tmp/input-pipe" ^ string_of_int (Random.random_range 0 100000000000)
val outputPipeName = "/tmp/output-pipe" ^ string_of_int (Random.random_range 0 100000000000)
val _ = system ("mkfifo " ^ inputPipeName)
val _ = system ("mkfifo " ^ outputPipeName)
val _ = system ("/home/unruh/svn/qrhl-tool/scala-isabelle/scripts/connect-to-running-isabelle.py "
          ^ inputPipeName ^ " " ^ outputPipeName)
\<close>

ML_file "../../main/resources/de/unruh/isabelle/control/control_isabelle.ml"

ML \<open>
val control_isabelle_struct =
  #allStruct ML_Env.name_space () |> find_first (fn (n,_) => n = "Control_Isabelle") |> Option.valOf |> snd
val params : Standard_Thread.params = {name="scala-isabelle protocol", stack_limit=NONE, interrupts=false}
val thread = Standard_Thread.fork params (fn () =>
  (#enterStruct ML_Env.name_space ("Control_Isabelle", control_isabelle_struct);
   Control_Isabelle.handleLines ()))
\<close>

ML \<open>
open Control_Isabelle
val scala = \<open>
import de.unruh.isabelle.pure._
import de.unruh.isabelle.mlvalue._
import de.unruh.isabelle.pure.Implicits._
import de.unruh.isabelle.control.Isabelle._

val DList(DObject(idThm),DObject(idCtxt)) = data
val thm = MLValue.unsafeFromId[Thm](idThm).retrieveNow
val ctxt = MLValue.unsafeFromId[Context](idCtxt).retrieveNow
val str = thm.pretty(ctxt)
println("Got theorem: " + str)
\<close>
val scala' = Input.source_content scala |> fst
val thm = E_Thm @{thm refl}
val ctxt = E_Context \<^context>
val _ = Control_Isabelle.sendToScala (DList [DString scala', DList [DObject thm, DObject ctxt]])
\<close>


end
