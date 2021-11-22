package com.gojol.notto.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gojol.notto.common.SuccessLevel
import com.gojol.notto.common.TodoState
import com.gojol.notto.model.data.DayWithSuccessLevelAndSelect
import com.gojol.notto.model.data.MonthlyCalendar
import com.gojol.notto.model.database.todo.DailyTodo
import com.gojol.notto.model.datasource.todo.TodoLabelRepository
import com.gojol.notto.ui.home.CalendarFragment.Companion.ITEM_ID_ARGUMENT
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: TodoLabelRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _monthlyCalendar = MutableLiveData<MonthlyCalendar>()
    val monthlyCalendar: LiveData<MonthlyCalendar> = _monthlyCalendar

    private val _monthlyAchievement = MutableLiveData<List<DayWithSuccessLevelAndSelect>>()
    val monthlyAchievement: LiveData<List<DayWithSuccessLevelAndSelect>> = _monthlyAchievement

    fun initData() {
        savedStateHandle.get<Long>(ITEM_ID_ARGUMENT)?.let {
            val year = it.div(100).toInt()
            val month = it.rem(100).toInt()
            val day = if (LocalDate.now().year == year && LocalDate.now().monthValue == month) {
                LocalDate.now().dayOfMonth
            } else {
                1
            }

            _monthlyCalendar.value = MonthlyCalendar(year, month, day)
        }
    }

    fun updateSelectedDay(date: Int) {
        _monthlyCalendar.value = _monthlyCalendar.value?.copy(selectedDay = date)

        setMonthlyDailyTodos()
    }

    fun setMonthlyDailyTodos() {
        val calendar = _monthlyCalendar.value ?: return

        viewModelScope.launch {
            // TODO DailyTodo를 가져오는 코드인데 없으면 setting을 해줌 -> 분리 필요
            repository.getTodosWithTodayDailyTodos(
                LocalDate.of(calendar.year, calendar.month, calendar.selectedDay)
            )

            val monthlyDailyTodos = repository.getAllDailyTodos().filter {
                it.date in calendar.startDate..calendar.endDate
            }

            setMonthlyAchievement(monthlyDailyTodos)
        }
    }

    private fun setMonthlyAchievement(monthlyDailyTodos: List<DailyTodo>) {
        val calendar = _monthlyCalendar.value ?: return

        _monthlyAchievement.value = calendar.getMonthlyDateList().map { date ->
            val todayDailyTodos = monthlyDailyTodos
                .filter { it.date.dayOfMonth == date }

            DayWithSuccessLevelAndSelect(date, getSuccessLevel(todayDailyTodos), isSelected(date))
        }
    }

    private fun getSuccessLevel(todayDailyTodos: List<DailyTodo>): SuccessLevel {
        val successCount = todayDailyTodos.count { it.todoState == TodoState.SUCCESS }
        val totalCount = todayDailyTodos.size

        val successRate = if (totalCount == 0) {
            SuccessLevel.ZERO.maxValue
        } else {
            successCount.toFloat() / totalCount.toFloat()
        }

        val successLevel = when {
            totalCount == SuccessLevel.ZERO.value || successCount == SuccessLevel.ZERO.value -> SuccessLevel.ZERO
            successRate <= SuccessLevel.ONE.maxValue -> SuccessLevel.ONE
            successRate <= SuccessLevel.TWO.maxValue -> SuccessLevel.TWO
            successRate <= SuccessLevel.THREE.maxValue -> SuccessLevel.THREE
            successRate < SuccessLevel.FOUR.maxValue -> SuccessLevel.FOUR
            else -> SuccessLevel.FIVE
        }

        return successLevel
    }

    private fun isSelected(date: Int): Boolean {
        return date == _monthlyCalendar.value?.selectedDay ?: false
    }
}
