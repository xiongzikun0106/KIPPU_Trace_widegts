package com.kippu.trace.widget

import com.kippu.trace.R

enum class TraceWidgetSize(
    val layoutRes: Int,
    val backgroundWidthPx: Int,
    val backgroundHeightPx: Int,
) {
    TWO_BY_TWO(
        layoutRes = R.layout.widget_trace_2x2,
        backgroundWidthPx = 320,
        backgroundHeightPx = 320,
    ),
    THREE_BY_TWO(
        layoutRes = R.layout.widget_trace_3x2,
        backgroundWidthPx = 480,
        backgroundHeightPx = 320,
    ),
    FOUR_BY_TWO(
        layoutRes = R.layout.widget_trace_4x2,
        backgroundWidthPx = 640,
        backgroundHeightPx = 320,
    ),
}
