package com.gojol.notto.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gojol.notto.common.AdapterViewType
import com.gojol.notto.databinding.ItemMonthlyCalendarBinding
import com.gojol.notto.ui.home.HomeViewModel

class CalendarAdapter(val viewModel: HomeViewModel) :
    RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        return CalendarViewHolder(
            ItemMonthlyCalendarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(viewModel)
    }

    override fun getItemCount(): Int = 1

    override fun getItemViewType(position: Int): Int {
        return AdapterViewType.CALENDAR.viewType
    }

    class CalendarViewHolder(private val binding: ItemMonthlyCalendarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cvHomeMonth.setOnDateChangeListener { _, y, m, d ->
                val adapter = bindingAdapter as CalendarAdapter
                adapter.viewModel.updateDate(y, m, d)
                // T0D0: 추후에 BindingAdapter 사용
                adapter.notifyDataSetChanged()

                binding.executePendingBindings()
            }
        }

        fun bind(viewModel: HomeViewModel) {
            binding.viewmodel = viewModel
            binding.executePendingBindings()
        }
    }
}
