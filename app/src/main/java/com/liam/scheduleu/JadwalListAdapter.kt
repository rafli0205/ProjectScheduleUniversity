package com.liam.scheduleu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.liam.scheduleu.data.local.ScheduleEntity

data class ScheduleWithCount(
    val schedule: ScheduleEntity,
    val taskCount: Int
)

class JadwalListAdapter(
    private var items: List<ScheduleWithCount>,
    private val onEditClick: (ScheduleEntity) -> Unit
) : RecyclerView.Adapter<JadwalListAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHari: TextView = itemView.findViewById(R.id.tvHari)
        val tvRingkasan: TextView = itemView.findViewById(R.id.tvRingkasan)
        val tvTaskCount: TextView = itemView.findViewById(R.id.tvTaskCount)
        val btnEdit: TextView = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_jadwal, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val schedule = item.schedule

        holder.tvHari.text = schedule.hari
        holder.tvRingkasan.text =
            listOf(schedule.namaMatkul, schedule.namaDosen, schedule.ruang)
                .filter { it.isNotBlank() }
                .joinToString(", ")

        if (item.taskCount > 0) {
            holder.tvTaskCount.visibility = View.VISIBLE
            holder.tvTaskCount.text = item.taskCount.toString()
        } else {
            holder.tvTaskCount.visibility = View.GONE
        }

        holder.btnEdit.setOnClickListener { onEditClick(schedule) }
    }

    fun submitList(newItems: List<ScheduleWithCount>) {
        items = newItems
        notifyDataSetChanged()
    }
}
