package io.kotest.plugin.intellij.styles

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

object StringSpecStyle : SpecStyle {

   override fun fqn() = FqName("io.kotest.core.spec.style.StringSpec")

   override fun specStyleName(): String = "StringSpec"

   override fun generateTest(specName: String, name: String): String {
      return "\"$name\" { }"
   }

   override fun isTestElement(element: PsiElement): Boolean = test(element) != null

   /**
    * A test of the form:
    *
    *   "test name"{ }
    *
    */
   private fun KtCallExpression.tryTest(): Test? {
      val name = extractStringFromStringInvokeWithLambda() ?: return null
      return Test(name, name)
   }

   /**
    * Matches tests of the form:
    *
    *   "some test".config(...) {}
    */
   private fun KtDotQualifiedExpression.tryTestWithConfig(): Test? {
      val name = extractStringForStringExtensionFunctonWithRhsFinalLambda("config") ?: return null
      return Test(name, name)
   }

   /**
    * For a StringSpec we consider the following scenarios:
    *
    * "test name" { }
    * "test name".config(...) {}
    */
   override fun test(element: PsiElement): Test? {
      if (!element.isContainedInSpec()) return null

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
      if (!element.isContainedInSpec()) return null

      val ktcall = element.ifCallExpressionLhsStringOpenQuote()
      if (ktcall != null) return test(ktcall)

      val ktdot = element.ifDotExpressionSeparator()
      if (ktdot != null) return test(ktdot)

      return null
   }
}
