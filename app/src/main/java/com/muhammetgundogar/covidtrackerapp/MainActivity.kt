package com.muhammetgundogar.covidtrackerapp

import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


private const val BASE_URL = "https://covidtracking.com/api/v1/"
private const val TAG = "MainActivity"
private const val ALL_STATES: String = "All (Nationwide)"

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShowData: List<CovidData>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)





        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        val covidService = retrofit.create(CovidService::class.java)

        covidService.getNationalData().enqueue(object: Callback<List<CovidData>> {

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.w(TAG,"Did not receive a valid response body")
                    return
                }
                setupEventListeners()
                nationalDailyData = nationalData.reversed()
                Log.i(TAG,"Update graph with national data")
                updateDisplayWithData(nationalDailyData)


            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }

        })


       // fetch the state data

        covidService.getStatesData().enqueue(object: Callback<List<CovidData>> {

            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i(TAG,"onResponse $response")
                val statesData = response.body()
                if (statesData == null) {
                    Log.w(TAG,"Did not receive a valid response body")
                    return
                }
                perStateDailyData = statesData.reversed().groupBy { it.state }
                Log.i(TAG,"Update spinner with state names")
                // Update spinner with state names
                updateSpinnerWithStateData(perStateDailyData.keys)
            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e(TAG,"onFailure $t")
            }

        })
    }

    private fun updateSpinnerWithStateData(stateNames: Set<String>) {
        val stateAbbreviationList=  stateNames.toMutableList()
         stateAbbreviationList.sort()
        stateAbbreviationList.add(0,ALL_STATES)

        // Add state list as data source for the spinner
        spinnerSelect.attachDataSource(stateAbbreviationList)
        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedState = parent.getItemAtPosition(position) as String
            val selectedData = perStateDailyData[selectedState] ?: nationalDailyData
            updateDisplayWithData(selectedData)
        }

    }


    private fun setupEventListeners() {

        tickerView.setCharacterLists(TickerUtils.provideNumberList())
        // Add a listener for the user sctubbing on the chart
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener {  itemData ->
            if (itemData is CovidData) {
                updateInfoForDate(itemData)
            }

        }
            // respond to radio button selected events

        radioGroupTimeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }

        radioGroupMetricSelection.setOnCheckedChangeListener { _ , checkedId ->
            when (checkedId) {
                R.id.radioButtonNegative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radioButtonDeath -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }


    private fun updateDisplayMetric(metric: Metric) {
        // update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.colorNegative
            Metric.POSITIVE -> R.color.colorPositive
            Metric.DEATH -> R.color.colorDeath
        }





      @ColorInt val colorInt = ContextCompat.getColor(this,colorRes)

        sparkView.lineColor = colorInt
        tickerView.setTextColor(colorInt)

        // Update the metric on the adapter


        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // Reset number and date shown in the bottom text views
        updateInfoForDate(currentlyShowData.last())
    }

    private fun updateDisplayWithData(dailyData: List<CovidData>) {
        currentlyShowData = dailyData
        // Create a new SparkAdapter with the data
       adapter = CovidSparkAdapter(dailyData)
        sparkView.adapter = adapter
        // update radio buttons to select the positive cases and max time by default
        radioButtonPositive.isChecked = true
        radioButtonMax.isChecked = true
        // Display metric for the most recent date
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(covidData: CovidData) {
            val numCases = when (adapter.metric) {
                Metric.NEGATIVE -> covidData.negativeIncrease
                Metric.POSITIVE -> covidData.positiveIncrease
                Metric.DEATH -> covidData.deathIncrease
            }
            tickerView.text = NumberFormat.getInstance().format(numCases)
            val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
           tvDateLabel.text =  outputDateFormat.format(covidData.dateChecked)
    }




}