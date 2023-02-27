package me.wxc.todolist.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.PagerSnapHelper
import kotlinx.coroutines.launch
import me.wxc.todolist.App
import me.wxc.todolist.R
import me.wxc.todolist.databinding.ActivityMainBinding
import me.wxc.widget.SchedulerConfig
import me.wxc.widget.base.ISchedulerWidget
import me.wxc.widget.calender.MonthAdapter
import me.wxc.widget.scheduler.SchedulerWidget
import me.wxc.widget.scheduler.components.CreateTaskModel
import me.wxc.widget.scheduler.components.DailyTaskModel
import me.wxc.widget.tools.*

class MainActivity : AppCompatActivity() {
    private val mainViewModel by viewModels<MainViewModel>()
    private lateinit var binding: ActivityMainBinding
    private val monthAdapter by lazy { MonthAdapter(binding.monthViewList) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeSchedulerConfig()
        val calendarWidget = SchedulerWidget(binding.schedulerView)
        binding.yyyyM.text = sdf_yyyyM.format(System.currentTimeMillis())

        binding.monthViewList.run {
            PagerSnapHelper().attachToRecyclerView(this)
            adapter = monthAdapter
        }

        binding.today.setOnClickListener {
            if (binding.schedulerView.isVisible) {
                calendarWidget.selectedDayTime = startOfDay().timeInMillis
            } else {
                monthAdapter.selectedDayTime = startOfDay().timeInMillis
            }
        }
        binding.fab.setOnClickListener {
            SchedulerConfig.onCreateTaskClickBlock(CreateTaskModel(
                startTime = SchedulerConfig.selectedDayTime + 10 * hourMillis,
                duration = quarterMillis * 2,
                onNeedScrollBlock = { _, _ -> }
            ))
        }
        binding.more.setOnClickListener {
            PopupMenu(this, it).run {
                menuInflater.inflate(R.menu.main, menu)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.threeDay -> {
                            val fromMonth = binding.monthViewList.visibility == View.VISIBLE
                            binding.schedulerView.visibility = View.VISIBLE
                            binding.monthViewList.visibility = View.GONE
                            calendarWidget.renderRange = ISchedulerWidget.RenderRange.ThreeDayRange
                            if (fromMonth) {
                                calendarWidget.selectedDayTime =
                                    startOfDay(SchedulerConfig.selectedDayTime).timeInMillis
                            }
                        }
                        R.id.singleDay -> {
                            val fromMonth = binding.monthViewList.visibility == View.VISIBLE
                            binding.schedulerView.visibility = View.VISIBLE
                            binding.monthViewList.visibility = View.GONE
                            calendarWidget.renderRange = ISchedulerWidget.RenderRange.SingleDayRange
                            if (fromMonth) {
                                calendarWidget.selectedDayTime =
                                    startOfDay(SchedulerConfig.selectedDayTime).timeInMillis
                            }
                        }
                        R.id.month -> {
                            val fromDay = binding.schedulerView.visibility == View.VISIBLE
                            binding.schedulerView.visibility = View.GONE
                            binding.monthViewList.visibility = View.VISIBLE
                            if (fromDay) {
                                monthAdapter.selectedDayTime =
                                    startOfDay(SchedulerConfig.selectedDayTime).timeInMillis
                            }
                        }
                        else -> {}
                    }
                    true
                }
                show()
            }
        }
    }

    private fun initializeSchedulerConfig() {
        SchedulerConfig.run {
            app = App.self
//        schedulerStartTime = System.currentTimeMillis() - 30 * dayMillis
//        schedulerEndTime = System.currentTimeMillis() + 30 * dayMillis
            onDateSelectedListener = {
                Log.i(TAG, "date select: ${sdf_yyyyMMddHHmmss.format(timeInMillis)}")
                binding.yyyyM.text = sdf_yyyyM.format(timeInMillis)
                selectedDayTime = timeInMillis
            }
            schedulerModelsProvider = { startTime, endTime ->
                mainViewModel.getRangeDailyTask(startTime, endTime)
            }
            lifecycleScope = this@MainActivity.lifecycleScope
            onDailyTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model
                    onSaveBlock = {
                        SchedulerConfig.lifecycleScope.launch {
                            mainViewModel.updateDailyTask(
                                it as DailyTaskModel,
                                binding.schedulerView.adapter.models
                            )
                            binding.schedulerView.adapter.notifyModelsChanged()
                            binding.schedulerView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                    onDeleteBlock = {
                        SchedulerConfig.lifecycleScope.launch {
                            mainViewModel.removeDailyTask(
                                it as DailyTaskModel,
                                binding.schedulerView.adapter.models
                            )
                            binding.schedulerView.adapter.notifyModelRemoved(model)
                            binding.schedulerView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                }.show(supportFragmentManager, "DetailsFragment")
            }
            onCreateTaskClickBlock = { model ->
                DetailsFragment().apply {
                    taskModel = model
                    onSaveBlock = {
                        SchedulerConfig.lifecycleScope.launch {
                            mainViewModel.saveCreateDailyTask(
                                it as CreateTaskModel,
                                binding.schedulerView.adapter.models
                            )
                            binding.schedulerView.adapter.notifyModelsChanged()
                            Log.i(TAG, "daily task added: ${model.title}")
                            binding.schedulerView.invalidate()
                            monthAdapter.refreshCurrentItem()
                        }
                    }
                }.show(supportFragmentManager, "DetailsFragment")

            }
        }
    }
}