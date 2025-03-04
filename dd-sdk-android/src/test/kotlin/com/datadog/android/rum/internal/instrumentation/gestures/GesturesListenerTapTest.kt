/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.instrumentation.gestures

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.RumAttributes
import com.datadog.android.rum.tracking.InteractionPredicate
import com.datadog.android.rum.tracking.ViewAttributesProvider
import com.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.lang.ref.WeakReference
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@ForgeConfiguration(Configurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class GesturesListenerTapTest : AbstractGesturesListenerTest() {

    @Test
    fun `onTap sends the right target when the ViewGroup and its child are both clickable`(
        forge: Forge
    ) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val container1: ViewGroup = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        )
        val target: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        val notClickableInvalidTarget: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        )
        val notVisibleInvalidTarget: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            visible = false,
            forge = forge
        )
        val container2: ViewGroup = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        ) {
            whenever(it.childCount).thenReturn(3)
            whenever(it.getChildAt(0)).thenReturn(notClickableInvalidTarget)
            whenever(it.getChildAt(1)).thenReturn(notVisibleInvalidTarget)
            whenever(it.getChildAt(2)).thenReturn(target)
        }
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(2)
            whenever(it.getChildAt(0)).thenReturn(container1)
            whenever(it.getChildAt(1)).thenReturn(container2)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(target, expectedResourceName)
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(target, "", expectedResourceName)
    }

    @Test
    fun `onTap dispatches an UserAction if target is ViewGroup and clickable`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val target: ViewGroup = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        ) {
            whenever(it.childCount).thenReturn(2)
            whenever(it.getChildAt(0)).thenReturn(mock())
            whenever(it.getChildAt(1)).thenReturn(mock())
        }
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(target)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(target, expectedResourceName)
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(target, "", expectedResourceName)
    }

    @Test
    fun `onTap ignores invisible or gone views`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val invalidTarget: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            visible = false,
            clickable = true,
            forge = forge
        )
        val validTarget: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(2)
            whenever(it.getChildAt(0)).thenReturn(invalidTarget)
            whenever(it.getChildAt(1)).thenReturn(validTarget)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(validTarget, expectedResourceName)
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(validTarget, "", expectedResourceName)
    }

    @Test
    fun `onTap ignores not clickable targets`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val invalidTarget: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            clickable = false,
            forge = forge
        )
        val validTarget: View = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(2)
            whenever(it.getChildAt(0)).thenReturn(invalidTarget)
            whenever(it.getChildAt(1)).thenReturn(validTarget)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(validTarget, expectedResourceName)
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(validTarget, "", expectedResourceName)
    }

    @Test
    fun `onTap does nothing if no children present and decor view not clickable`(
        forge: Forge
    ) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(0)
        }
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verify(logger.mockDevLogHandler)
            .handleLog(
                Log.INFO,
                GesturesListener.MSG_NO_TARGET_TAP
            )
        verifyZeroInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `onTap does nothing and no log triggered if no target found { target inside ComposeView } `(
        forge: Forge
    ) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val composeView: ComposeView = mockView(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(composeView)
        }
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyZeroInteractions(logger.mockDevLogHandler)
        verifyZeroInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `onTap keeps decorView as target if visible and clickable`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        ) {
            whenever(it.childCount).thenReturn(0)
        }
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(mockDecorView, expectedResourceName)

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(mockDecorView, "", expectedResourceName)
    }

    @Test
    fun `onTap adds the target id hexa if NFE while requesting resource id`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val targetId = forge.anInt()
        val validTarget: View = mockView(
            id = targetId,
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(validTarget)
        }
        whenever(mockResources.getResourceEntryName(validTarget.id)).thenThrow(
            Resources.NotFoundException(
                forge.anAlphabeticalString()
            )
        )
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(
            validTarget,
            "",
            "0x${targetId.toString(16)}"
        )
    }

    @Test
    fun `onTap adds the target id hexa when getResourceEntryName returns null`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        val targetId = forge.anInt()
        val validTarget: View = mockView(
            id = targetId,
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(validTarget)
        }
        whenever(mockResources.getResourceEntryName(validTarget.id)).thenReturn(null)
        testedListener = GesturesListener(
            WeakReference(mockWindow)
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyMonitorCalledWithUserAction(
            validTarget,
            "",
            "0x${targetId.toString(16)}"
        )
    }

    @Test
    fun `will not send any span if decor view view reference is null`(forge: Forge) {
        // Given
        val mockEvent: MotionEvent = forge.getForgery()
        testedListener = GesturesListener(WeakReference<Window>(null))

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `applies the extra attributes from the attributes providers`(forge: Forge) {
        val mockEvent: MotionEvent = forge.getForgery()
        val targetId = forge.anInt()
        val validTarget: View = mockView(
            id = targetId,
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(validTarget)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(validTarget, expectedResourceName)
        var expectedAttributes: MutableMap<String, Any?> = mutableMapOf(
            RumAttributes.ACTION_TARGET_CLASS_NAME to validTarget.javaClass.canonicalName,
            RumAttributes.ACTION_TARGET_RESOURCE_ID to expectedResourceName
        )
        val providers = Array<ViewAttributesProvider>(forge.anInt(min = 0, max = 10)) {
            mock {
                whenever(it.extractAttributes(eq(validTarget), any())).thenAnswer {
                    @Suppress("UNCHECKED_CAST")
                    val map = it.arguments[1] as MutableMap<String, Any?>
                    map[forge.aString()] = forge.aString()
                    expectedAttributes = map
                    null
                }
            }
        }

        testedListener = GesturesListener(
            WeakReference(mockWindow),
            providers
        )
        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verify(rumMonitor.mockInstance).addUserAction(
            RumActionType.TAP,
            "",
            expectedAttributes
        )
    }

    @Test
    fun `M use class simple name as target class name W tapIntercepted { cannonicalName is null }`(
        forge: Forge
    ) {
        val mockEvent: MotionEvent = forge.getForgery()
        val targetId = forge.anInt()

        // we will use a LocalViewClass to reproduce the behaviour when getCanonicalName function
        // can return a null object.
        class LocalViewClass(context: Context) : View(context)

        val validTarget: LocalViewClass = mockView(
            id = targetId,
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(validTarget)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(validTarget, expectedResourceName)
        var expectedAttributes: MutableMap<String, Any?> = mutableMapOf(
            RumAttributes.ACTION_TARGET_CLASS_NAME to validTarget.javaClass.simpleName,
            RumAttributes.ACTION_TARGET_RESOURCE_ID to expectedResourceName
        )

        val providers = Array<ViewAttributesProvider>(forge.anInt(min = 0, max = 10)) {
            mock {
                whenever(it.extractAttributes(eq(validTarget), any())).thenAnswer {
                    @Suppress("UNCHECKED_CAST")
                    val map = it.arguments[1] as MutableMap<String, Any?>
                    map[forge.aString()] = forge.aString()
                    expectedAttributes = map
                    null
                }
            }
        }

        testedListener = GesturesListener(
            WeakReference(mockWindow),
            providers
        )
        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verify(rumMonitor.mockInstance).addUserAction(
            RumActionType.TAP,
            "",
            expectedAttributes
        )
    }

    @Test
    fun `M use the custom target name W tapIntercepted { custom target name provided }`(
        forge: Forge
    ) {
        val mockEvent: MotionEvent = forge.getForgery()
        val targetId = forge.anInt()
        val fakeCustomTargetName = forge.anAlphabeticalString()
        val validTarget: View = mockView(
            id = targetId,
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        val mockInteractionPredicate: InteractionPredicate = mock {
            whenever(it.getTargetName(validTarget)).thenReturn(fakeCustomTargetName)
        }
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(validTarget)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(validTarget, expectedResourceName)
        val expectedAttributes: MutableMap<String, Any?> = mutableMapOf(
            RumAttributes.ACTION_TARGET_CLASS_NAME to validTarget.javaClass.canonicalName,
            RumAttributes.ACTION_TARGET_RESOURCE_ID to expectedResourceName
        )

        testedListener = GesturesListener(
            WeakReference(mockWindow),
            interactionPredicate = mockInteractionPredicate
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verify(rumMonitor.mockInstance).addUserAction(
            RumActionType.TAP,
            fakeCustomTargetName,
            expectedAttributes
        )
    }

    @Test
    fun `M use empty string as target name W tapIntercepted { custom target name empty }`(
        forge: Forge
    ) {
        val mockEvent: MotionEvent = forge.getForgery()
        val targetId = forge.anInt()
        val validTarget: View = mockView(
            id = targetId,
            forEvent = mockEvent,
            hitTest = true,
            forge = forge,
            clickable = true
        )
        val mockInteractionPredicate: InteractionPredicate = mock {
            whenever(it.getTargetName(validTarget)).thenReturn("")
        }
        mockDecorView = mockDecorView<ViewGroup>(
            id = forge.anInt(),
            forEvent = mockEvent,
            hitTest = false,
            forge = forge
        ) {
            whenever(it.childCount).thenReturn(1)
            whenever(it.getChildAt(0)).thenReturn(validTarget)
        }
        val expectedResourceName = forge.anAlphabeticalString()
        mockResourcesForTarget(validTarget, expectedResourceName)
        val expectedAttributes: MutableMap<String, Any?> = mutableMapOf(
            RumAttributes.ACTION_TARGET_CLASS_NAME to validTarget.javaClass.canonicalName,
            RumAttributes.ACTION_TARGET_RESOURCE_ID to expectedResourceName
        )

        testedListener = GesturesListener(
            WeakReference(mockWindow),
            interactionPredicate = mockInteractionPredicate
        )

        // When
        testedListener.onSingleTapUp(mockEvent)

        // Then
        verify(rumMonitor.mockInstance).addUserAction(
            RumActionType.TAP,
            "",
            expectedAttributes
        )
    }

    // region Internal

    private fun verifyMonitorCalledWithUserAction(
        target: View,
        expectedTargetName: String,
        expectedResourceName: String
    ) {
        verify(rumMonitor.mockInstance).addUserAction(
            eq(RumActionType.TAP),
            eq(expectedTargetName),
            argThat {
                val targetClassName = target.javaClass.canonicalName
                this[RumAttributes.ACTION_TARGET_CLASS_NAME] == targetClassName &&
                    this[RumAttributes.ACTION_TARGET_RESOURCE_ID] == expectedResourceName
            }
        )
    }

    // endregion
}
