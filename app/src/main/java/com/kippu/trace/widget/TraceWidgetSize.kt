package com.kippu.trace.widget

import com.kippu.trace.R

// 小组件尺寸定义
enum class TraceWidgetSize(
    val layoutRes: Int,
    val backgroundWidthPx: Int,
    val backgroundHeightPx: Int,
) {
    TWO_BY_TWO(
        layoutRes = R.layout.widget_trace_2x2,
        backgroundWidthPx = 280,
        backgroundHeightPx = 280,
    ),
    THREE_BY_TWO(
        layoutRes = R.layout.widget_trace_3x2,
        backgroundWidthPx = 420,
        backgroundHeightPx = 280,
    ),
    FOUR_BY_TWO(
        layoutRes = R.layout.widget_trace_4x2,
        backgroundWidthPx = 640,
        backgroundHeightPx = 320,
    ),
}
