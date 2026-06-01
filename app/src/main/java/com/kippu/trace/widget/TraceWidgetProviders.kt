package com.kippu.trace.widget

// 各尺寸小组件实现类
class TraceWidget2x2Provider : TraceWidgetProvider() {
    override val widgetSize = TraceWidgetSize.TWO_BY_TWO
}

// 3x2小组件
class TraceWidget3x2Provider : TraceWidgetProvider() {
    override val widgetSize = TraceWidgetSize.THREE_BY_TWO
}

// 4x2小组件
class TraceWidget4x2Provider : TraceWidgetProvider() {
    override val widgetSize = TraceWidgetSize.FOUR_BY_TWO
}
