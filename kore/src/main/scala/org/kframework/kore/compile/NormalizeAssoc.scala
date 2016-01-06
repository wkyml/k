package org.kframework.kore.compile

import org.kframework.Collections._
import org.kframework.attributes.Att
import org.kframework.definition.{Module, Rule, Sentence}
import org.kframework.kore._


/**
 * Compiler pass flattening associative collections
 */
class NormalizeAssoc(c: Constructors[K]) extends (Module => Module) {

  import c._

  override def apply(m: Module): Module = Module(m.name, m.imports, m.localSentences map {apply(_)(m)}, m.att)

  def apply(s: Sentence)(implicit m: Module): Sentence = s match {
    case r: Rule => Rule(apply(r.body), apply(r.requires), apply(r.ensures), r.att)
    case _ => s
  }

  def apply(k: K)(implicit m: Module): K = k match {
    case kApply: KApply =>
      if (m.attributesFor.getOrElse(kApply.klabel, Att()).contains(Att.assoc)) {
        val opKLabel: KLabel = kApply.klabel
        val unitKLabel: KLabel = KLabel(m.attributesFor(opKLabel).get(Att.unit).get)
        val flattenChildren = flatten(kApply, opKLabel, unitKLabel)
        if (flattenChildren exists {_.isInstanceOf[KRewrite]}) {
          KRewrite(
            KApply(opKLabel, KList(flattenChildren map RewriteToTop.toLeft flatMap {flatten(_, opKLabel, unitKLabel)} map apply: _*), kApply.att),
            KApply(opKLabel, KList(flattenChildren map RewriteToTop.toRight flatMap {flatten(_, opKLabel, unitKLabel)} map apply: _*), kApply.att),
            Att())
        } else {
          KApply(opKLabel, KList(flattenChildren map apply: _*), kApply.att)
        }
      } else {
        KApply(kApply.klabel, KList(immutable(kApply.klist.items) map apply: _*), kApply.att)
      }
    case kRewrite: KRewrite => KRewrite(apply(kRewrite.left), apply(kRewrite.right), kRewrite.att)
    case _ => k
  }

  def flatten(k: K, op: KLabel, unit: KLabel): Seq[K] = k match {
    case Unapply.KApply(`op`, children: List[K]) =>
      children flatMap {flatten(_, op, unit)}
    case Unapply.KApply(`unit`, List()) =>
      Seq()
    case kRewrite: KRewrite =>
      (flatten(kRewrite.left, op, unit) map {KRewrite(_, KApply(unit), kRewrite.att)}) :+ KRewrite(KApply(unit), kRewrite.right, kRewrite.att)
    case _ =>
      Seq(k)
  }

}
