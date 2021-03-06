package scala.scalanative
package compiler
package pass

import compiler.analysis.ClassHierarchy
import compiler.analysis.ClassHierarchyExtractors._
import nir._

/** Hoists external members from external modules to top-level scope. */
class ExternHoisting(implicit chg: ClassHierarchy.Graph) extends Pass {
  private def stripName(n: Global): Global = {
    val id = n.id
    assert(id.startsWith("extern."))
    Global.Val(id.substring(7))
  }

  override def preDefn = {
    case defn @ Defn.Declare(attrs, name, _) if attrs.isExtern =>
      Seq(defn.copy(name = stripName(name)))
    case defn @ Defn.Define(attrs, name, _, _) if attrs.isExtern =>
      Seq(defn.copy(name = stripName(name)))
    case defn @ Defn.Const(attrs, name, _, _) if attrs.isExtern =>
      Seq(defn.copy(name = stripName(name)))
    case defn @ Defn.Var(attrs, name, _, _) if attrs.isExtern =>
      Seq(defn.copy(name = stripName(name)))
  }

  override def preVal = {
    case Val.Global(n @ Ref(node), ty) if node.attrs.isExtern =>
      Val.Global(stripName(n), ty)
  }
}
