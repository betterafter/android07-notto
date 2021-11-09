package com.gojol.notto.ui.home

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gojol.notto.common.TodoSuccessType
import com.gojol.notto.model.data.BindingData
import com.gojol.notto.model.data.LabelWithCheck
import com.gojol.notto.model.database.label.Label
import com.gojol.notto.model.data.RepeatType
import com.gojol.notto.model.database.todo.Todo
import com.gojol.notto.model.database.todolabel.LabelWithTodo
import com.gojol.notto.model.datasource.todo.TodoLabelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import java.util.Calendar.Builder
import java.util.Calendar.getInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: TodoLabelRepository) : ViewModel() {

    private val dummyLabels = mutableListOf(
        Label(1, "학교"),
        Label(2, "건강"),
        Label(3, "집"),
        Label(4, "과제")
    )

    private val dummyTodos = mutableListOf(
        Todo(TodoSuccessType.NOTHING, "밥 굶지 않기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
        Todo(TodoSuccessType.NOTHING, "과제 미루지 않기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
        Todo(TodoSuccessType.NOTHING, "지각하지 않기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
        Todo(TodoSuccessType.NOTHING, "밥 먹을 때 물 먹지 않기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
        Todo(TodoSuccessType.NOTHING, "회의 지각 안하기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
        Todo(TodoSuccessType.NOTHING, "핸드폰 보지 않기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
        Todo(TodoSuccessType.NOTHING, "누워있지 않기", "1", false, RepeatType.DAY, false, "1:00", "2:00", "1:00", false),
    )

    private val _date = MutableLiveData(getInstance())
    val date: LiveData<Calendar> = _date

    private val _labelList = MutableLiveData<MutableList<LabelWithCheck>>()
    val labelList: LiveData<MutableList<LabelWithCheck>> = _labelList

    private val _todoList = MutableLiveData<MutableList<Todo>>()
    val todoList: LiveData<MutableList<Todo>> = _todoList

    private val _concatList = MutableLiveData<BindingData?>()
    val concatList: LiveData<BindingData?> = _concatList

    fun setDummyData() {
        viewModelScope.launch {
            insertDummyTodoAndLabel()
        }
    }

    private suspend fun insertDummyTodoAndLabel() {
        dummyTodos.forEach {
            repository.insertTodo(it)
        }

        dummyLabels.forEach {
            repository.insertLabel(it)
        }

        insertTodoLabel()

        _todoList.value = repository.getAllTodo().toMutableList()
        val labelWithTodos = repository.getLabelWithTodo()
        val newLabelList = labelWithTodos.map { label -> LabelWithCheck(label, false) }.toMutableList()

        val totalLabel = LabelWithTodo(
            Label(0, LABEL_NAME_ALL), _todoList.value!!
        )
        newLabelList.add(0, LabelWithCheck(totalLabel, true))
        _labelList.value = newLabelList

        _concatList.value = BindingData(todoList.value, labelList.value)
    }

    private suspend fun insertTodoLabel() {
        val labels = repository.getAllLabel()
        val todos = repository.getAllTodo()

        repository.insertTodo(todos[0], labels[0])
        repository.insertTodo(todos[0], labels[1])
        repository.insertTodo(todos[0], labels[2])

        repository.insertTodo(todos[1], labels[3])

        repository.insertTodo(todos[2], labels[0])

        repository.insertTodo(todos[3], labels[1])

        repository.insertTodo(todos[4], labels[0])
        repository.insertTodo(todos[4], labels[3])

        repository.insertTodo(todos[5], labels[0])
        repository.insertTodo(todos[5], labels[1])
        repository.insertTodo(todos[5], labels[2])
        repository.insertTodo(todos[5], labels[3])

        repository.insertTodo(todos[6], labels[0])
        repository.insertTodo(todos[6], labels[2])
    }


    fun updateDate(year: Int, month: Int, day: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            _date.value = Builder().setDate(year, month, day).build()
        } else {
            val calendar: Calendar = getInstance()
            calendar.set(year, month, day)

            _date.value = calendar
        }
    }

    fun fetchTodoSuccessState(todo: Todo) {
        val newTodoList = mutableListOf<Todo>()
        viewModelScope.launch {
            repository.updateTodo(todo)
            repository.getAllTodo().forEach { databaseTodo ->
                _todoList.value?.forEach { currentTodo ->
                    if (databaseTodo.todoId == currentTodo.todoId) {
                        newTodoList.add(databaseTodo)
                    }
                }
            }
            _todoList.value = newTodoList
            _concatList.value = BindingData(todoList.value, labelList.value)
        }
    }

    suspend fun addTodoListByLabels(labels: List<LabelWithCheck>) {
        val newTodoList = mutableListOf<Todo>()

        val todoSet = mutableSetOf<Todo>()
        labels.forEach {
            todoSet.addAll(it.labelWithTodo.todo)
        }

        repository.getAllTodo().forEach {
            todoSet.forEach { setTodo ->
                if (it.todoId == setTodo.todoId) {
                    newTodoList.add(it)
                }
            }
        }

        _todoList.value = newTodoList
        _concatList.value = BindingData(todoList.value, labelList.value)
    }

    fun updateLabelList(list: MutableList<LabelWithCheck>) {
        _labelList.value = list
        _concatList.value = BindingData(todoList.value, labelList.value)
    }

    companion object {
        const val LABEL_NAME_ALL = "전체"
    }
}

