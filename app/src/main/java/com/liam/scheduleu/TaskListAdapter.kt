package com.liam.scheduleu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.liam.scheduleu.data.local.TaskEntity
import java.util.Calendar

class TaskListAdapter(
    private var items: List<TaskEntity>,
    private val onCheckedChange: (TaskEntity, Boolean) -> Unit,
    private val onDeleteClick: (TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskListAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvTaskDesc)
        val tvDeadline: TextView = view.findViewById(R.id.tvTaskDeadline)
        val cbSelesai: CheckBox = view.findViewById(R.id.cbTaskSelesai)
        val imgAttachment: ImageView = view.findViewById(R.id.imgTaskAttachment)
        val imgDelete: ImageView = view.findViewById(R.id.imgDeleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.judul
        holder.cbSelesai.setOnCheckedChangeListener(null)
        holder.cbSelesai.isChecked = item.selesai

        if (item.deskripsi.isNullOrBlank()) {
            holder.tvDesc.visibility = View.GONE
        } else {
            holder.tvDesc.visibility = View.VISIBLE
            holder.tvDesc.text = item.deskripsi
        }

        if (item.deadlineDate != null && item.deadlineTime != null) {
            holder.tvDeadline.visibility = View.VISIBLE
            holder.tvDeadline.text = "${item.deadlineDate} • ${item.deadlineTime}"
        } else {
            holder.tvDeadline.visibility = View.GONE
            holder.tvDeadline.text = ""
        }

        val now = System.currentTimeMillis()
        val isOverdue = !item.selesai && isTaskOverdue(item, now)

        val context = holder.itemView.context
        if (isOverdue) {
            holder.tvDeadline.setTextColor(
                ContextCompat.getColor(context, R.color.red_overdue)
            )
        } else {
            holder.tvDeadline.setTextColor(
                ContextCompat.getColor(context, R.color.gray_deadline_normal)
            )
        }

        if (item.imageUri.isNullOrBlank()) {
            holder.imgAttachment.visibility = View.GONE
        } else {
            holder.imgAttachment.visibility = View.VISIBLE
        }

        holder.cbSelesai.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(item, isChecked)
        }

        holder.imgDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    fun submitList(newItems: List<TaskEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun isTaskOverdue(task: TaskEntity, nowMillis: Long): Boolean {
        val date = task.deadlineDate ?: return false
        val time = task.deadlineTime ?: return false

        val dateParts = date.split("-")
        val timeParts = time.split(".")

        if (dateParts.size != 3 || timeParts.size != 2) return false

        val year = dateParts[0].toIntOrNull() ?: return false
        val month = dateParts[1].toIntOrNull()?.minus(1) ?: return false
        val day = dateParts[2].toIntOrNull() ?: return false
        val hour = timeParts[0].toIntOrNull() ?: return false
        val minute = timeParts[1].toIntOrNull() ?: return false

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis < nowMillis
    }
}
