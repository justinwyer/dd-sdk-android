/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.tools.detekt.rules

import fr.xgouchet.elmyr.junit5.ForgeExtension
import io.github.detekt.test.utils.KotlinCoreEnvironmentWrapper
import io.github.detekt.test.utils.createEnvironment
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.lintWithContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ForgeExtension::class)
internal class InvalidPerfMeasureBlockTest {

    lateinit var kotlinEnv: KotlinCoreEnvironmentWrapper

    @BeforeEach
    fun setup() {
        kotlinEnv = createEnvironment()
    }

    @AfterEach
    fun tearDown() {
        kotlinEnv.dispose()
    }

    // region Tests

    @Test
    fun `M detekt invalid measure block { forged parameters }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    measure { 
                       GlobalRum.get().startView(forge.aKey(), forge.aName())
                    }
                }
            """.trimIndent()

        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `M detekt invalid measure block { mixed forged and valid parameters }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    val aName = forge.aName()                                                
                    measure { 
                       GlobalRum.get().startView(forge.aKey(), aName)
                    }
                }
            """.trimIndent()

        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `M detekt invalid measure block { parameters resolved by function calls }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun forgeAKey():String {
                   return "aKey"
                }    

                fun forgeAName():String {
                   return "aName"
                }                

                fun test() {
                    measure { 
                       GlobalRum.get().startView(forgeAKey(), forgeAName())
                    }
                }
            """.trimIndent()

        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `M detekt invalid measure block { multiple calls in measure block }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    measure { 
                       GlobalRum.get().startView("aKey", "aName")
                       GlobalRum.get().stopView(forge.aKey())
                    }
                }
            """.trimIndent()

        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `M not detekt valid measure block { parameters by reference }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    val aKey = forge.aKey()
                    val aName = forge.aName()                                              
                    measure { 
                       GlobalRum.get().startView(aKey, aName)
                    }
                }
            """.trimIndent()
        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `M not detekt invalid measure block { parameters by value }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    measure { 
                       GlobalRum.get().startView("aKey", "aName")
                    }
                }
            """.trimIndent()
        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `M not detekt invalid measure block { multiple expression, valid parameters }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    val aKey = forge.aKey()    
                    measure { 
                       GlobalRum.get().startView("aKey", "aName")
                       GlobalRum.get().stopView(aKey)
                    }
                }
            """.trimIndent()
        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `M not detekt invalid measure block { different measure function }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    val aKey = forge.aKey()    
                    measure { 
                       GlobalRum.get().startView(forge.aKey(), forge.aName())
                    }
                }
            """.trimIndent()
        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(
                kotlinEnv.env,
                code,
                DIFFERENT_MEASURE_UTILS_CODE,
                RUM_CLASS,
                FORGE_CLASS
            )
        // Then
        assertThat(findings).hasSize(0)
    }

    @Test
    fun `M not detekt invalid measure block { argument static constant from different classs }`() {
        // Given
        val code =
            """ 
                package com.datadog.android.nightly.rum

                import com.datadog.android.nightly.utils.measure
                import com.datadog.android.rum.GlobalRum
                import com.datadog.android.nightly.utils.Forge

                fun test(forge:Forge) {
                    val aKey = forge.aKey()    
                    measure { 
                       GlobalRum.get().startView(GlobalRum.DEFAULT_RUM_KEY, "aName")
                       GlobalRum.get().stopView(aKey)
                    }
                }
            """.trimIndent()
        // When
        val findings = InvalidPerfMeasureBlock()
            .lintWithContext(kotlinEnv.env, code, MEASURE_UTILS_CODE, RUM_CLASS, FORGE_CLASS)

        // Then
        assertThat(findings).hasSize(0)
    }

    // endregion

    companion object {
        val MEASURE_UTILS_CODE = """
        package com.datadog.android.nightly.utils

        internal inline fun <reified R> measure(methodName: String, codeBlock: () -> R): R {
            val result = codeBlock()
            return result
        }
        """.trimIndent()

        val DIFFERENT_MEASURE_UTILS_CODE = """
        package com.datadog.android.utils

        internal inline fun <reified R> measure(methodName: String, codeBlock: () -> R): R {
            val result = codeBlock()
            return result
        }
        """.trimIndent()

        val FORGE_CLASS = """
        package com.datadog.android.nightly.utils
        
        class Forge {
            fun aKey(): String {
                return "aKey"
            }

            fun aName(): String{
                return "aName"
            }                                                                
        }
        """.trimIndent()

        val RUM_CLASS = """
         package com.datadog.android.rum
         
         object GlobalRum {
            const DEFAULT_RUM_KEY = "defaultKey"
            
            fun get(): GlobalRum {
                return this
            }

            fun startView(key:String, name:String) { }
            
            fun stopView(key:String) { }
            
         }
        """.trimIndent()
    }
}
