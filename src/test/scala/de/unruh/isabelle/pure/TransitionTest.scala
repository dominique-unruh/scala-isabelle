package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleMLException
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.Version
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration.{Duration, MILLISECONDS}


class TransitionTest extends AnyFunSuite {
  test ("basic transition parsing") {
    val theory = Theory("Main")
    val source = "lemma foo: True by simp"
    val transitions = Transition.parseOuterSyntax(theory, source).filter(!_._1.isIgnored)

    assert(transitions.length == 2)
    assert(transitions(0)._1.name == "lemma")
    assert(transitions(0)._2 == "lemma foo: True")
    assert(transitions(1)._1.name == "by")
    assert(transitions(1)._2 == "by simp")
  }

  test ("basic transition execution") {
    val source = "lemma foo: True by simp"
    val theory = Theory.mergeTheories("Foo", endTheory=false, List(Theory("Main")))
    val transitions = Transition.parseOuterSyntax(theory, source)
    var state = ToplevelState(theory)
    for ((tr, s) <- transitions)
        state = tr.execute(state)
  }

  test ("theory transition execution") {
    val thy_header = "theory Foo imports Main begin"
    val source = "lemma foo: True by simp"
    val theory = Theory.mergeTheories("Foo", endTheory=false, List(Theory("Main")))
    val transitions = Transition.parseOuterSyntax(theory, s"$thy_header $source end")
    var state = ToplevelState()
    for ((tr, s) <- transitions)
      state = tr.execute(state)
    assert(state.isEndTheory)
  }

  test ("basic transition states") {
    val thy_header = "theory Foo imports Main begin"
    val source = "lemma foo: True by simp"
    val theory = Theory.mergeTheories("Foo", endTheory=false, List(Theory("Main")))
    val transitions = Transition.parseOuterSyntax(theory, s"$thy_header $source end").filter(!_._1.isIgnored)

    var state = ToplevelState()
    assert(state.mode == ToplevelState.Modes.Toplevel)
    assert(!state.isEndTheory)

    assert(transitions(0)._2 == "theory Foo imports Main begin")
    assert(transitions(0)._1.name == "theory")
    assert(transitions(0)._1.isInit)
    state = transitions(0)._1.execute(state)

    assert(state.mode == ToplevelState.Modes.Theory)
    assert(state.proofLevel == 0)
    assert(state.localTheoryDescription == "theory Foo")
    assert(state.proofStateDescription == "")

    assert(transitions(1)._2 == "lemma foo: True")
    assert(transitions(1)._1.name == "lemma")
    assert(!transitions(1)._1.isInit)
    state = transitions(1)._1.execute(state)

    assert(state.mode == ToplevelState.Modes.Proof)
    assert(state.proofLevel == 1)
    assert(state.localTheoryDescription == "theory Foo")
    assert(state.proofStateDescription.replaceAll("\\s+", " ") == "proof (prove) goal (1 subgoal): 1. True")

    assert(transitions(2)._2 == "by simp")
    assert(transitions(2)._1.name == "by")
    state = transitions(2)._1.execute(state)

    assert(state.mode == ToplevelState.Modes.Theory)
    assert(state.proofLevel == 0)
    assert(state.localTheoryDescription == "theory Foo")
    assert(state.proofStateDescription == "")
    assert(!state.isEndTheory)

    assert(transitions(3)._2 == "end")
    assert(transitions(3)._1.name == "end")
    state = transitions(3)._1.execute(state)

    assert(state.mode == ToplevelState.Modes.Toplevel)
    assert(state.localTheoryDescription == "")
    assert(state.isEndTheory)
  }


  test("malformed transition") {
    val theory = Theory("Main")
    val source = "foo"
    val transitions = Transition.parseOuterSyntax(theory, source)

    assert(transitions.length == 1)
    val tr = transitions(0)._1
    assert(tr.name == "<malformed>")
    if (Version.from2021_1)
      assert(tr.isMalformed) // Not supported before 2020

    val state = ToplevelState(theory)
    val thrown = intercept[IsabelleMLException] { tr.execute(state) }
    val msg = thrown.getMessage
    assert(msg.contains("Outer syntax error"))
    assert(msg.contains("command expected"))
    assert(msg.contains("but identifier foo (line 1) was found"))
  }

  test("execution timeout") {
    val source = raw"lemma foo: True by (sleep 300.0) simp"
    val theory = Theory.mergeTheories("Foo", endTheory=false, List(Theory("Main")))
    val transitions = Transition.parseOuterSyntax(theory, source)
    var state = ToplevelState(theory)
    var start = System.currentTimeMillis
    val thrown = intercept[IsabelleMLException] {
      for ((tr, _) <- transitions) {
        start = System.currentTimeMillis
        state = tr.execute(state, timeout=Duration(2, MILLISECONDS))
      }
    }
    println(thrown)
    // The timeout gets rounded up to a full second for some reason.
    // We allow it to be up to 10 seconds in order not to fail the test under high load.
    assert(System.currentTimeMillis - start < 10000)
    val msg = thrown.getMessage
    assert(msg.contains("Timeout"))
  }

  test("parsing a full theory") {
    var source = """
      (* A comment
      On multiple lines
      *)

      theory Foo
      imports
        Main
        "HOL-Library.Extended_Real"
      begin

      section \<open>Foo\<close>

      record ('a,'b) pre_digraph =
      verts :: "'a set"
      arcs :: "'b set"
      tail :: "'b \<Rightarrow> 'a"
      head :: "'b \<Rightarrow> 'a"

      definition arc_to_ends :: "('a,'b) pre_digraph \<Rightarrow> 'b \<Rightarrow> 'a \<times> 'a" where
      "arc_to_ends G e \<equiv> (tail G e, head G e)"

      definition arcs_ends :: "('a,'b) pre_digraph \<Rightarrow> ('a \<times> 'a) set" where
      "arcs_ends G \<equiv> arc_to_ends G ` arcs G"

      locale pre_digraph =
      fixes G :: "('a, 'b) pre_digraph" (structure)

      locale wf_digraph = pre_digraph +
      assumes tail_in_verts[simp]: "e \<in> arcs G \<Longrightarrow> tail G e \<in> verts G"
      assumes head_in_verts[simp]: "e \<in> arcs G \<Longrightarrow> head G e \<in> verts G"
      begin

      lemma wf_digraph: "wf_digraph G" by intro_locales

      lemmas wellformed = tail_in_verts head_in_verts

      end

      text \<open>
      Example
      \<close>

      lemma (in wf_digraph) fin_digraphI[intro]:
      assumes "finite (verts G)"
      assumes "finite (arcs G)"
      shows "fin_digraph G"
      using assms
      sorry

      subsection \<open>Reachability\<close>

      abbreviation dominates :: "('a,'b) pre_digraph \<Rightarrow> 'a \<Rightarrow> 'a \<Rightarrow> bool" ("_ \<rightarrow>\<index> _" [100,100] 40) where
      "dominates G u v \<equiv> (u,v) \<in> arcs_ends G"

      context wf_digraph begin

      lemma arcs_ends_conv: "arcs_ends G = (\<lambda>e. (tail G e, head G e)) ` arcs G"
      by (auto simp: arc_to_ends_def arcs_ends_def)

      lemma adj_in_verts:
      assumes "u \<rightarrow>\<^bsub>G\<^esub> v" shows "u \<in> verts G" "v \<in> verts G"
      using assms unfolding arcs_ends_conv by auto

      end

      end
    """
    val comment_regex = raw"(?s)\(\*.*\*\)".r
    val theory = Theory("Main")
    val transitions = Transition.parseOuterSyntax(theory, source)
    var prev_tr : Transition = null
    var prev_s : String = null
    for ((tr, s) <- transitions) {
      if (tr.isIgnored) {
        assert(tr.name == "<ignored>")
        assert(comment_regex.replaceAllIn(tr.position.extract(source), "").trim == "")
        assert(comment_regex.replaceAllIn(s, "").trim == "")
      } else {
        assert(tr.name == tr.position.extract(source))
      }

      // Check that extracting the source from positions gives the same substring as parseOuterSyntax.
      if (prev_tr != null)
        assert(prev_s == prev_tr.position.extractUntil(tr.position, source))
      prev_tr = tr
      prev_s = s
    }
  }
}
