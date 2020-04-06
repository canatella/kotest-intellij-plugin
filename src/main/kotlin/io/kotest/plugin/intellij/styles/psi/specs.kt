package io.kotest.plugin.intellij.styles.psi

import com.intellij.psi.PsiElement
import io.kotest.plugin.intellij.styles.SpecStyle
import io.kotest.plugin.intellij.styles.functionName
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Returns any [KtClassOrObject]s located in this [PsiElement]
 */
fun PsiElement.classes(): List<KtClassOrObject> {
   return this.getChildrenOfType<KtClassOrObject>().asList()
}

/**
 * Returns any [KtClassOrObject] children of this [PsiElement] that are specs.
 */
fun PsiElement.specs(): List<KtClassOrObject> {
   return this.classes().filter { it.isAnySpecSubclass() }
}

/**
 * Returns true if this [PsiElement] is inside a spec class.
 */
fun PsiElement.isContainedInSpec(): Boolean {
   val enclosingClass = getParentOfType<KtClassOrObject>(true) ?: return false
   return enclosingClass.isAnySpecSubclass()
}

/**
 * Returns true if this [KtClassOrObject] is a subclass of any Spec.
 * This function will recursively check all superclasses.
 */
fun KtClassOrObject.isAnySpecSubclass(): Boolean {
   val superClass = getSuperClass()
      ?: return SpecStyle.styles.any { it.fqn().shortName().asString() == getSuperClassSimpleName() }
   val fqn = superClass.getKotlinFqName()
   return if (SpecStyle.styles.any { it.fqn() == fqn }) true else superClass.isAnySpecSubclass()
}


/**
 * Returns true if this [KtClassOrObject] is a subclass of a specific Spec.
 * This function will recursively check all superclasses.
 */
fun KtClassOrObject.isSpecSubclass(style: SpecStyle) = isSpecSubclass(style.fqn())

fun KtClassOrObject.isSpecSubclass(fqn: FqName): Boolean {
   val superClass = getSuperClass() ?: return getSuperClassSimpleName() == fqn.shortName().asString()
   return if (superClass.getKotlinFqName() == fqn) true else superClass.isSpecSubclass(fqn)
}

fun KtClassOrObject.specStyle(): SpecStyle? = SpecStyle.styles.find { this.isSpecSubclass(it) }

fun KtCallExpression.isDslInvocation(): Boolean {
   return children.size == 2
      && children[0] is KtNameReferenceExpression
      && children[1] is KtLambdaArgument
}

fun KtClassOrObject.callbacks(): List<Callback> {
   val body = this.getChildrenOfType<KtClassBody>().firstOrNull()
   if (body != null) {
      val init = body.getChildrenOfType<KtClassInitializer>().firstOrNull()
      if (init != null) {
         val block = init.getChildrenOfType<KtBlockExpression>().firstOrNull()
         if (block != null) {
            val calls = block.getChildrenOfType<KtCallExpression>()
            return calls
               .filter { it.isDslInvocation() }
               .mapNotNull { call ->
                  val fname = call.functionName()
                  CallbackType.values().find { it.text == fname }?.let { Callback(it, call) }
               }
         }
      }
   }
   return emptyList()
}

data class Callback(val type: CallbackType, val psi: PsiElement)

enum class CallbackType {

   BeforeTest {
      override val text = "beforeTest"
   },
   AfterTest {
      override val text = "afterTest"
   },
   BeforeSpec {
      override val text = "beforeSpec"
   },
   AfterSpec {
      override val text = "afterSpec"
   };

   abstract val text: String
}
