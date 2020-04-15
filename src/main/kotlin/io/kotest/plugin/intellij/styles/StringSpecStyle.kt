package io.kotest.plugin.intellij.styles

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import io.kotest.plugin.intellij.psi.extractStringForStringExtensionFunctonWithRhsFinalLambda
import io.kotest.plugin.intellij.psi.extractStringFromStringInvokeWithLambda
import io.kotest.plugin.intellij.psi.ifCallExpressionLhsStringOpenQuote
import io.kotest.plugin.intellij.psi.ifDotExpressionSeparator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

object StringSpecStyle : SpecStyle {

   override fun fqn() = FqName("io.kotest.core.spec.style.StringSpec")

   override fun specStyleName(): String = "String Spec"

   override fun generateTest(specName: String, name: String): String {
      return "\"$name\" { }"
   }

   /**
    * A test of the form:
    *
    *   "test name"{ }
    *
    */
   private fun KtCallExpression.tryTest(): Test? {
      val name = extractStringFromStringInvokeWithLambda() ?: return null
      return Test(name, name, TestType.Test)
   }

   /**
    * Matches tests of the form:
    *
    *   "some test".config(...) {}
    */
   private fun KtDotQualifiedExpression.tryTestWithConfig(): Test? {
      val name = extractStringForStringExtensionFunctonWithRhsFinalLambda("config") ?: return null
      return Test(name, name, TestType.Test)
   }

   /**
    * For a StringSpec we consider the following scenarios:
    *
    * "test name" { }
    * "test name".config(...) {}
    */
   override fun test(element: PsiElement): Test? {
      return when (element) {
         is KtCallExpression -> element.tryTest()
         is KtDotQualifiedExpression -> element.tryTestWithConfig()
         else -> null
      }
   }

   /**
    * For a StringSpec we consider the following scenarios:
    *
    * "test name" { }
    * "test name".config(...) {}
    */
   override fun test(element: LeafPsiElement): Test? {
      val ktcall = element.ifCallExpressionLhsStringOpenQuote()
      if (ktcall != null) return test(ktcall)

      val ktdot = element.ifDotExpressionSeparator()
      if (ktdot != null) return test(ktdot)

      return null
   }
}
