package scala.scalanative
package nscplugin

import scala.tools.nsc._
import scalanative.nir.Shows.{showType => _, showGlobal => _, _}
import scalanative.util.{sh, Show}, Show.{Repeat => r}

trait NirNameEncoding { self: NirCodeGen =>
  import global.{Name => _, _}, definitions._

  def genClassName(sym: Symbol): nir.Global = {
    val id = sym.fullName.toString

    sym match {
      case ObjectClass                               => nir.Nrt.Object.name
      case _ if sym.isModule                         => genClassName(sym.moduleClass)
      case _ if sym.isModuleClass || sym.isImplClass => nir.Global.Val(id)
      case _ if sym.isInterface                      => nir.Global.Type(id)
      case _                                         => nir.Global.Type(id)
    }
  }

  def genFieldName(sym: Symbol) = {
    val owner = genClassName(sym.owner)
    val id0 = sym.name.toString
    val id =
      if (id0.charAt(id0.length()-1) != ' ') id0
      else id0.substring(0, id0.length()-1)

    owner + id
  }

  def genDefName(sym: Symbol) = {
    val owner         = genClassName(sym.owner)
    val id            = sym.name.toString
    val tpe           = sym.tpe.widen
    val mangledParams = tpe.params.toSeq.map(mangledType)

    if (sym.name == nme.CONSTRUCTOR) {
      owner + ("init" +: mangledParams).mkString("_")
    } else {
      owner + (id +: (mangledParams :+ mangledType(tpe.resultType))).mkString("_")
    }
  }

  private def mangledType(sym: Symbol): String =
    mangledType(sym.info)

  private def mangledType(tpe: Type): String =
    mangledType(genType(tpe))

  private def mangledType(ty: nir.Type): String = {
    implicit lazy val showMangledType: Show[nir.Type] = Show {
      case nir.Type.None                => ""
      case nir.Type.Void                => "void"
      case nir.Type.Label               => "label"
      case nir.Type.Vararg              => "..."
      case nir.Type.Bool                => "bool"
      case nir.Type.I8                  => "i8"
      case nir.Type.I16                 => "i16"
      case nir.Type.I32                 => "i32"
      case nir.Type.I64                 => "i64"
      case nir.Type.F32                 => "f32"
      case nir.Type.F64                 => "f64"
      case nir.Type.Array(ty, n)        => sh"arr.$ty.$n"
      case nir.Type.Ptr(ty)             => sh"ptr.$ty"
      case nir.Type.Function(args, ret) => sh"fun.${r(args :+ ret, sep = ".")}"
      case nir.Type.Struct(name)        => sh"struct.$name"
      case nir.Type.AnonStruct(tys)     => sh"anon-struct.${r(tys, sep = ".")}"

      case nir.Type.Size             => "size"
      case nir.Type.Unit             => "unit"
      case nir.Type.Nothing          => "nothing"
      case nir.Type.Null             => "null"
      case nir.Type.Class(name)      => sh"class.$name"
      case nir.Type.ClassValue(name) => sh"class-value.$name"
      case nir.Type.Trait(name)      => sh"trait.$name"
      case nir.Type.Module(name)     => sh"module.$name"
    }

    implicit lazy val showMangledGlobal: Show[nir.Global] = Show { g =>
      val head +: tail = g.parts
      val parts = head.replace("scala.scalanative.runtime", "nrt") +: tail
      sh"${r(parts, sep = "_")}"
    }

    sh"$ty".toString
  }
}
