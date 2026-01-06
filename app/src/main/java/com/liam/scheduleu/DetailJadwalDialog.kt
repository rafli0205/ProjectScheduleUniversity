package com.liam.scheduleu

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class DetailJadwalDialog : DialogFragment() {

    companion object {
        private const val ARG_MATKUL = "arg_matkul"
        private const val ARG_DOSEN = "arg_dosen"
        private const val ARG_JAM = "arg_jam"
        private const val ARG_RUANG = "arg_ruang"
        private const val ARG_MOTIVASI = "arg_motivasi"

        fun newInstance(
            namaMatkul: String,
            namaDosen: String,
            jam: String,
            ruang: String,
            motivasi: String = "SEMANGAT FOR TODAY !!!"
        ): DetailJadwalDialog {
            val args = Bundle().apply {
                putString(ARG_MATKUL, namaMatkul)
                putString(ARG_DOSEN, namaDosen)
                putString(ARG_JAM, jam)
                putString(ARG_RUANG, ruang)
                putString(ARG_MOTIVASI, motivasi)
            }
            return DetailJadwalDialog().apply {
                arguments = args
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_detail_jadwal)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true)

        // Ambil data dari arguments
        val tvNamaMatkul = dialog.findViewById<TextView>(R.id.tvNamaMatkul)
        val tvNamaDosen = dialog.findViewById<TextView>(R.id.tvNamaDosen)
        val tvJam = dialog.findViewById<TextView>(R.id.tvJam)
        val tvRuang = dialog.findViewById<TextView>(R.id.tvRuang)
        val tvMotivasi = dialog.findViewById<TextView>(R.id.tvMotivasi)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        val args = requireArguments()
        tvNamaMatkul.text = args.getString(ARG_MATKUL)
        tvNamaDosen.text = args.getString(ARG_DOSEN)
        tvJam.text = args.getString(ARG_JAM)
        tvRuang.text = args.getString(ARG_RUANG)
        tvMotivasi.text = args.getString(ARG_MOTIVASI)

        btnClose.setOnClickListener { dismiss() }

        return dialog
    }
}
