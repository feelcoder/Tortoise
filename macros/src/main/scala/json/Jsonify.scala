// (C) Uri Wilensky. https://github.com/NetLogo/Tortoise

package org.nlogo.tortoise.json

import
  scala.reflect.macros.blackbox.{ Context => BlackBoxContext }

import
  scalaz.ValidationNel

object Jsonify {

  import scala.language.experimental.{ macros => enableMacros }

  def writer[T, S]: T => S = macro writableInternal[T, S]

  def reader[T, S]: T => ValidationNel[String, S] = macro readableInternal[T, S]

  def readableInternal[T: c.WeakTypeTag, S: c.WeakTypeTag](c: BlackBoxContext)(implicit inputType: c.WeakTypeTag[T], resultType: c.WeakTypeTag[S]): c.Tree = {

      import c.universe._

      val json = TermName(c.freshName("json"))

      val treeOpt = withConstructorProperties(c)(resultType.tpe) {
        (target, propertyName, propertyKey) =>
          val propValue = TermName(c.freshName("prop"))
          val readField = q"JsonReader.readField[$target]($json, $propertyKey)"
          (fq"$propValue <- $readField", q"$propertyName = $propValue")
      }

      treeOpt.map {
        constructorElems =>
          val name = TermName(c.freshName("reader"))
          q"""{
            import org.nlogo.tortoise.json.JsonReader
            import scalaz.Validation.FlatMap.ValidationFlatMapRequested
            implicit object $name extends JsonReader[${inputType.tpe}, ${resultType.tpe}]{
              def apply($json: ${inputType.tpe}) =
                for (..${constructorElems.map(_._1)})
                  yield new ${resultType.tpe}(..${constructorElems.map(_._2)})
            }
            $name
          }"""
      } getOrElse c.abort(c.enclosingPosition, s"Could not find constructor for ${resultType.tpe}")

  }

  def writableInternal[T: c.WeakTypeTag, S: c.WeakTypeTag](c: BlackBoxContext)(implicit toBeWritten: c.WeakTypeTag[T], res: c.WeakTypeTag[S]): c.Tree = {

    import c.universe._

    val typeToJsonify = toBeWritten.tpe
    val writerType    = c.mirror.staticClass("org.nlogo.tortoise.json.JsonWriter").toType

    if (typeToJsonify.typeSymbol.asClass.isCaseClass) {

      val writtenObject = TermName(c.freshName("target"))

      val treeOpt = withConstructorProperties(c)(typeToJsonify) {
        (from, propertyName, propertyKey) =>
          val converter = c.inferImplicitValue(appliedType(writerType, from))
          if (converter != EmptyTree)
            q"$propertyKey -> $converter.write($writtenObject.$propertyName)"
          else
            c.abort(c.enclosingPosition, s"Please supply JsonWriter[$from] or JsonWriter[Option[$from]] for field $propertyKey")
      }

      treeOpt.map {
        constructorConversions =>

          val typeElement = q"${"type"} -> Some(${downCamelCaseTypeName(c)(typeToJsonify)})"
          val allElems    = constructorConversions :+ typeElement
          val name        = TermName(c.freshName("writer"))

          q"""{
            implicit object $name extends JsonWriter[$typeToJsonify] {
            import collection.immutable.ListMap
            def apply($writtenObject: $typeToJsonify): TortoiseJson = {
              val seq = Seq[(String, Option[org.nlogo.tortoise.json.TortoiseJson])](..$allElems)
                  org.nlogo.tortoise.json.TortoiseJson.JsObject(ListMap(seq.collect {
                    case (a, Some(b)) => (a, b)
                  }: _*))
                }
            }
            $name
          }"""

      } getOrElse c.abort(c.enclosingPosition, s"unable to find case class constructor")

    } else
      c.abort(c.enclosingPosition, s"Only case classes may be jsonified")

  }

  private def withConstructorProperties[T](c: BlackBoxContext)(constructedType: c.Type)(f: (c.Type, c.TermName, String) => T): Option[Seq[T]] =
    constructedType.decls.find(_.isConstructor).map {
      constructor =>
        val typeMap = (constructedType.etaExpand.typeParams zip constructedType.typeArgs).toMap
        constructor.asMethod.paramLists.head.map {
          p =>
            val tpe               = typeMap.getOrElse(p.typeSignature.typeSymbol, p.typeSignature)
            val propertyName      = p.name.toTermName
            val propertyStringKey = propertyName.decodedName.toString
            f(tpe, propertyName, propertyStringKey)
        }
    }

  private def downCamelCaseTypeName(c: BlackBoxContext)(t: c.Type) = {
    val shortName = t.toString.split('.').last
    shortName.head.toLower + shortName.tail
  }

}
