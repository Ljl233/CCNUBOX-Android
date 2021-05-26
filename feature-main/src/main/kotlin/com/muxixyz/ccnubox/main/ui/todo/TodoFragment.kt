package com.muxixyz.ccnubox.main.ui.todo

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.muxixyz.android.iokit.Result
import com.muxixyz.android.iokit.succeeded
import com.muxixyz.ccnubox.home.R
import com.muxixyz.ccnubox.home.databinding.FragmentTodoBinding
import com.muxixyz.ccnubox.home.databinding.TodoAddPopupBinding
import com.muxixyz.ccnubox.main.data.domain.Schedule
import com.muxixyz.ccnubox.main.data.repository.ScheduleRepository
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class TodoFragment : Fragment() {

    private lateinit var listAdapter: TodoListAdapter
    lateinit var dataBinding: FragmentTodoBinding
    lateinit var mPopupWindow: PopupWindow
    lateinit var mPopupBinding: TodoAddPopupBinding
    lateinit var mDatePickerView: View
    lateinit var mTimerPickerView: View
    lateinit var mPriorityPickerView: View
    val mTodoViewModel: TodoViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        dataBinding =
            FragmentTodoBinding.inflate(
                layoutInflater, container, false
            ).apply {
                viewmodel = mTodoViewModel
            }
        Toast.makeText(context, "Todo fragment show", Toast.LENGTH_SHORT).show()

        initFabAndPopupWindow()
        return dataBinding.root
    }

    private fun initFabAndPopupWindow() {
        if (!this::mPopupBinding.isInitialized) {
            mPopupBinding = TodoAddPopupBinding.inflate(LayoutInflater.from(context)).apply {
                viewmodel = getViewModel()
            }
        }
        mPopupWindow = PopupWindow(
            mPopupBinding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            isTouchable = true
            isOutsideTouchable = true
        }

        dataBinding.todoFab.setOnClickListener { v ->
            mPopupWindow.apply {
                showAtLocation(dataBinding.root, Gravity.BOTTOM, 0, 0)
                darkenBackground(0.7f)
            }
        }

        with(mPopupBinding) {
            todoPopupFinish.apply {
                setOnClickListener {
                    viewmodel?.complete()
                    Log.e("TodoFragment", "viewmodel?.complete()")
                    mPopupWindow.dismiss()
                    darkenBackground(1.0f)
                    refreshData()
                }
            }
            todoAddDatePicker.setOnClickListener {
                showDatePicker()
            }
            todoAddTimePicker.setOnClickListener {
                showTimePicker()
            }
            todoAddPriorityPicker.setOnClickListener {
                showPriorityPicker()
            }
        }
    }

    private fun darkenBackground(alpha: Float) {
        activity?.window?.apply {
            val layoutParams = attributes
            layoutParams?.alpha = alpha
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = layoutParams
        }
    }

    private fun showDatePicker() {
        if (!this::mDatePickerView.isInitialized) {
            mDatePickerView = LayoutInflater.from(context)
                .inflate(R.layout.todo_date_picker_popup, null)
        }

        val datePicker = PopupWindow(
            mDatePickerView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            showAtLocation(dataBinding.root, Gravity.BOTTOM, 0, 0)
            darkenBackground(0.7f)
            setOnDismissListener { darkenBackground(1f) }
        }
        mDatePickerView.findViewById<CalendarView>(R.id.todo_date_picker)
            .setOnDateChangeListener { view, year, month, dayOfMonth ->
                mPopupBinding.viewmodel?.date?.value = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                datePicker.dismiss()
            }
    }

    private fun showTimePicker() {

        if (!this::mTimerPickerView.isInitialized) {
            mTimerPickerView =
                LayoutInflater.from(context).inflate(R.layout.todo_time_picker_popup, null)
        }
        val mTimePickerPopupWindow = PopupWindow(
            mTimerPickerView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            showAtLocation(dataBinding.root, Gravity.BOTTOM, 0, 0)
            darkenBackground(0.7f)
            setOnDismissListener { darkenBackground(1.0f) }
        }

        mTimerPickerView.apply {

            var h = 0
            var m = 0

            findViewById<TimePicker>(R.id.todo_time_picker).apply {
                setIs24HourView(true)
                setOnTimeChangedListener { view, hourOfDay, minute ->
                    h = hourOfDay
                    m = minute
                }
            }
            findViewById<TextView>(R.id.todo_time_dismiss).setOnClickListener {
                mTimePickerPopupWindow.dismiss()
            }
            findViewById<TextView>(R.id.todo_time_finish).setOnClickListener {
                mPopupBinding.viewmodel?.time?.value?.apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                }
                mTimePickerPopupWindow.dismiss()
            }
        }

    }

    private fun showPriorityPicker() {
        if (!this::mPriorityPickerView.isInitialized) {
            mPriorityPickerView =
                LayoutInflater.from(context).inflate(R.layout.todo_priority_popup, null)
        }
        val priorityPopup = PopupWindow(
            mPriorityPickerView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            isFocusable = true
            showAtLocation(dataBinding.root, Gravity.TOP, 0, 0)
            darkenBackground(0.7f)
            setOnDismissListener { darkenBackground(1.0f) }
        }

        mPriorityPickerView.apply {
            findViewById<TextView>(R.id.priority_low).setOnClickListener {
                mPopupBinding.viewmodel?.priority?.value = 1
                priorityPopup.dismiss()
            }
            findViewById<TextView>(R.id.priority_mid).setOnClickListener {
                mPopupBinding.viewmodel?.priority?.value = 2
                priorityPopup.dismiss()
            }
            findViewById<TextView>(R.id.priority_high).setOnClickListener {
                mPopupBinding.viewmodel?.priority?.value = 3
                priorityPopup.dismiss()
            }
            findViewById<TextView>(R.id.priority_no).setOnClickListener {
                mPopupBinding.viewmodel?.priority?.value = 0
                priorityPopup.dismiss()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dataBinding.lifecycleOwner = this.viewLifecycleOwner
        mPopupBinding.lifecycleOwner = this.viewLifecycleOwner
        setupListAdapter()
    }

    private fun setupListAdapter() {

        listAdapter = TodoListAdapter(mutableListOf(), mTodoViewModel)

        dataBinding.todoList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
    }

    private fun refreshData() {
        if (this::listAdapter.isInitialized) {
            Log.e("TodoFragment", "refresh")
            mTodoViewModel.getAllSchedules(object :
                ScheduleRepository.LoadSchedulesCallback {
                override fun onSchedulesLoaded(schedules: List<Schedule>) {
                    listAdapter.setTodos(schedules)
                }

                override fun onDataNotAvailable(message: String) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPopupWindow.dismiss()
    }
}