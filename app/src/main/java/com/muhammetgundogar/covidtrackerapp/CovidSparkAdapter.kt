package com.muhammetgundogar.covidtrackerapp

import android.graphics.RectF
import com.muhammetgundogar.covidtrackerapp.Metric.*
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<CovidData>): SparkAdapter() {
    var metric = POSITIVE
    var daysAgo  = TimeScale.MAX

    override fun getCount() = dailyData.size


    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]
        return when (metric) {
            NEGATIVE -> chosenDayData.negativeIncrease.toFloat()
            POSITIVE -> chosenDayData.positiveIncrease.toFloat()
            DEATH ->    chosenDayData.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds  = super.getDataBounds()
        if (daysAgo != TimeScale.MAX) {
            bounds.left = count - daysAgo.numDays.toFloat()
        }

        return bounds
    }

}
