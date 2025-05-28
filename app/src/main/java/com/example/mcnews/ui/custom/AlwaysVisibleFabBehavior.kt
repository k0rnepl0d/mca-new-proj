package com.example.mcnews.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/**
 * Кастомное поведение для FAB, которое предотвращает её скрытие при скролле
 */
class AlwaysVisibleFabBehavior : CoordinatorLayout.Behavior<View> {

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        // Мы хотим реагировать на вертикальный скролл
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)

        // Всегда показываем FAB (не скрываем при скролле)
        when (child) {
            is FloatingActionButton -> {
                if (!child.isShown) {
                    child.show()
                }
            }
            is ExtendedFloatingActionButton -> {
                if (child.isExtended) {
                    // Если FAB расширена, сворачиваем её при скролле вниз и разворачиваем при скролле вверх
                    if (dyConsumed > 0 && child.isExtended) {
                        child.shrink()
                    } else if (dyConsumed < 0 && !child.isExtended) {
                        child.extend()
                    }
                }
                if (!child.isShown) {
                    child.show()
                }
            }
        }
    }}