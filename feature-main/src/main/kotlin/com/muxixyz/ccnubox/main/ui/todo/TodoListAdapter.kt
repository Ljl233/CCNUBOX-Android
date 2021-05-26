package com.muxixyz.ccnubox.main.ui.todo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.muxixyz.ccnubox.home.R
import com.muxixyz.ccnubox.home.databinding.ItemTodoBinding
import com.muxixyz.ccnubox.main.data.domain.Schedule
import java.util.*

class TodoListAdapter(val mTodos: MutableList<Schedule>, val viewModel: TodoViewModel) :
    RecyclerView.Adapter<VH>() {

    val listener = object : TodoItemListener {
        override fun onItemClick(todo: Schedule) {
            viewModel.openItem(todo.id)
        }

        override fun onCheckChanged(view: View, todo: Schedule) {
            val checked = (view as CheckBox).isChecked
            viewModel.completeTodo(todo.id, checked)
        }
    }

    var editing: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return VH(binding.root).also {
            binding.vh = it
        }
    }

    override fun getItemCount(): Int {
        return mTodos.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(listener, mTodos[position], editing)
    }

    fun startEdit() {
        editing = true
        notifyDataSetChanged()
    }

    fun setTodos(todos: List<Schedule>) {
        mTodos.clear()
        mTodos.addAll(todos)
        notifyDataSetChanged()
    }
}

class VH(root: View) : RecyclerView.ViewHolder(root) {

    var listener: TodoItemListener? = null

    var schedule: Schedule? = null

    var color: Int = R.color.colorNoPriority

    private val _time = MutableLiveData<String>()
    val time: LiveData<String> get() = _time

    var editing: Boolean = false

    fun bind(listener: TodoItemListener, schedule: Schedule, editing: Boolean) {
        this.schedule = schedule
        this.listener = listener
        this.editing = editing

        _time.value = formatTimeMillis(schedule.endTime)
        color = when (schedule.priority) {
            1 -> R.color.colorLowPriority
            2 -> R.color.colorMidPriority
            3 -> R.color.colorHighPriority
            else -> R.color.colorNoPriority
        }
    }
}

//如果是今天的待办则显示时间，否则显示日期
fun formatTimeMillis(time: Calendar?): String {
    if (time == null) return ""

    val curCalendar = Calendar.getInstance()
    with(time) {
        return if (get(Calendar.YEAR) == curCalendar.get(Calendar.YEAR) &&
            get(Calendar.MONTH) == curCalendar.get(Calendar.MONTH) &&
            get(Calendar.DAY_OF_MONTH) == curCalendar.get(Calendar.DAY_OF_MONTH)
        ) {
            "${get(Calendar.HOUR)}:${get(Calendar.MINUTE)}"
        } else {
            "${get(Calendar.MONTH)}月${get(Calendar.DAY_OF_MONTH)}日"
        }
    }
}

interface TodoItemListener {
    fun onItemClick(todo: Schedule)
    fun onCheckChanged(view: View, todo: Schedule)
}