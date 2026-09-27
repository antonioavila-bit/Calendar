package org.fossify.calendar.security

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

// Adapted from the user's read/copy-only Inventory-App native EditText pattern.
// Source: 260194c0766c280f82c76a1b03f19df0770575fb, MainActivity.kt, lines 2928–3024.
// Recognition is supplied by the installed IME, not a bundled/cloud OCR model.
object HandwritingInput {
    fun attach(field: EditText, enabled: () -> Boolean) {
        field.imeOptions = field.imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        if (Build.VERSION.SDK_INT >= 34) field.setAutoHandwritingEnabled(enabled())
        field.setOnTouchListener { view,event ->
            if (Build.VERSION.SDK_INT >= 34) field.setAutoHandwritingEnabled(enabled())
            val pen = event.pointerCount > 0 && (event.getToolType(0)==MotionEvent.TOOL_TYPE_STYLUS || event.getToolType(0)==MotionEvent.TOOL_TYPE_ERASER)
            if (enabled() && pen) {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    view.requestFocus()
                    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    if (Build.VERSION.SDK_INT >= 34 && imm.isStylusHandwritingAvailable) {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        view.post { runCatching { imm.startStylusHandwriting(view) }.onFailure { imm.showSoftInput(view,InputMethodManager.SHOW_IMPLICIT) } }
                    } else {
                        imm.showSoftInput(view,InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) view.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }
}
