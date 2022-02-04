/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.tools.detekt.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * A rule to detekt bad practice in generating the measured expression arguments in our
 * nightly tests.
 * @active
 */
class InvalidPerfMeasureBlock : Rule() {
    override val issue: Issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "This rule reports whenever a performance test generates" +
            " the measured expression arguments inside the measure block.",
        Debt.TEN_MINS
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (bindingContext == BindingContext.EMPTY) {
            return
        }

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        val callDescriptor = resolvedCall.candidateDescriptor
        val callContainingPackage = callDescriptor.containingPackage()?.toString().orEmpty()
        if (callDescriptor.fqNameSafe.asString() == MEASURE_FUNCTION_NAME &&
            callContainingPackage == MEASURE_FUNCTION_PACKAGE
        ) {
            expression.forEachDescendantOfType<KtCallExpression> {
                if (it.hasExpressionAsArguments()) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(expression),
                            "Bad practice detected in one of your performance tests." +
                                "Function arguments should not be generated inside the" +
                                "measure code block to not alter the metrics."
                        )
                    )
                }
            }
        }
    }

    private fun KtCallExpression.hasExpressionAsArguments(): Boolean {
        val resolvedCall = this.getResolvedCall(bindingContext) ?: return false
        val argumentAsExpression = resolvedCall.valueArguments
            .map { it.value }
            .flatMap { it.arguments }
            .map { it.getArgumentExpression() }
            .firstOrNull {
                it is KtCallExpression ||
                    (it is KtDotQualifiedExpression && it.isFunctionCall())
            }
        return argumentAsExpression != null
    }

    private fun KtDotQualifiedExpression.isFunctionCall(): Boolean {
        return selectorExpression is KtCallExpression
    }

    companion object {
        private const val MEASURE_FUNCTION_PACKAGE = "com.datadog.android.nightly.utils"
        private const val MEASURE_FUNCTION_NAME = "$MEASURE_FUNCTION_PACKAGE.measure"
    }
}
