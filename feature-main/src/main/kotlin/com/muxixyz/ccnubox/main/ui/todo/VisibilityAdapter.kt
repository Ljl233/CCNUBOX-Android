package com.muxixyz.ccnubox.main.ui.todo

import android.view.View
import androidx.databinding.BindingAdapter

@BindingAdapter("app:goneUnless")
fun goneUnless(view: View, visible: Boolean) {
    view.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("app:isEditing")
fun isEditing(view: View, editing: Boolean) {
    view.visibility = if (editing) View.GONE else View.VISIBLE
}