package com.example.outfitoftheday

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter

class HomeFragment : Fragment() {

    private lateinit var pieChart: PieChart

    // Inflate the layout for this fragment and setup the pie chart
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        pieChart = view.findViewById(R.id.pieChart)
        setupPieChart()
        loadPieChartData()
        return view
    }

    // Configures settings for the pie chart
    private fun setupPieChart() {
        pieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            legend.isEnabled = false
        }
    }

    // Loads data into the pie chart
    private fun loadPieChartData() {
        val entries = ArrayList<PieEntry>().apply {
            add(PieEntry(40f, "Casual"))
            add(PieEntry(30f, "Formal"))
            add(PieEntry(15f, "Sport"))
            add(PieEntry(15f, "Others"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FFD700"),
                Color.parseColor("#C5B358"),
                Color.parseColor("#FFDF00"),
                Color.parseColor("#D4AF37")
            )
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            setDrawValues(true)
        }

        pieChart.data = PieData(dataSet).apply {
            setValueFormatter(PercentFormatter(pieChart))
        }
        pieChart.invalidate()  // Refresh the pie chart
    }
}
