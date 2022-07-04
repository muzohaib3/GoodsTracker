package com.example.wms_scan.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import com.example.scanmate.data.callback.Status
import com.example.scanmate.extensions.obtainViewModel
import com.example.scanmate.extensions.setTransparentStatusBarColor
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppConstants.orgBusLocNo
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.Utils
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityScanAllHierarchyBinding

class ScanAllHierarchy : AppCompatActivity() {
    private lateinit var dialog: CustomProgressDialog
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityScanAllHierarchyBinding
    private var scannedValue = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanAllHierarchyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUi()
        initObserver()
    }

    private fun setupUi(){

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)
        viewModel = obtainViewModel(MainViewModel::class.java)
        scannedValue = intent.extras?.getString("subStringBefore").toString()
        Log.i("Scannedvalue",scannedValue)

    }

    private fun initObserver(){

        val scannedValue = "1L01WH001RK001SF001PL"

        viewModel.scanAll("$scannedValue", "0")
        viewModel.scanAll.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{

                }
                Status.SUCCESS ->{
                    it.let {

                        val warehouse = it.data?.get(0)?.wHName.toString()
                        val rack = it.data?.get(0)?.rackName.toString()
                        val shelf = it.data?.get(0)?.shelfName.toString()
                        val pallet = it.data?.get(0)?.pilotName.toString()
                        val busLocNo = it.data?.get(0)?.locationNo.toString()
                        val whNo =  it.data?.get(0)?.wHNo.toString()
                        val rackNo =  it.data?.get(0)?.rackNo.toString()
                        val shelfNo =  it.data?.get(0)?.shelfNo.toString()
                        val palletNo =  it.data?.get(0)?.pilotNo.toString()

                        binding.WHTV.text = warehouse
                        binding.rackTV.text = rack
                        binding.shelfTV.text = shelf
                        binding.palletTV.text = pallet

                        val scannedData = scannedValue
                        if (scannedData.contains("WH"))
                        {
                            scannedData.substringAfter("L").substringBefore("")
                            viewModel.getWarehouse("", busLocNo)
                        }
                        if (scannedData.contains("RK"))
                        {
                            scannedData.substringAfter("WH")
                            viewModel.getRack(
                                Utils.getSimpleTextBody(""),
                                Utils.getSimpleTextBody(whNo),
                                Utils.getSimpleTextBody(busLocNo),
                            )
                        }
                        if (scannedData.contains("SF"))
                        {
                            scannedData.substringAfter("RK").substringBefore("")
                            viewModel.getShelf(
                                Utils.getSimpleTextBody(""),
                                Utils.getSimpleTextBody(rackNo),
                                Utils.getSimpleTextBody(busLocNo)
                            )
                        }
                        if (scannedData.contains("PL"))
                        {
                            scannedData.substringAfter("SF")
                            Log.i("palletCode",scannedData)
                            viewModel.getPallet(
                                Utils.getSimpleTextBody(""),
                                Utils.getSimpleTextBody(shelfNo),
                                Utils.getSimpleTextBody(busLocNo)
                            )
                        }
                    }
                }
                Status.ERROR ->{ }
            }
        })

        viewModel.getWarehouse.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{

                }
                Status.SUCCESS ->{

                }
                Status.ERROR ->{

                }
            }
        })

        viewModel.getRack.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{

                }
                Status.SUCCESS ->{

                }
                Status.ERROR ->{

                }
            }
        })


        viewModel.getShelf.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{

                }
                Status.SUCCESS ->{

                }
                Status.ERROR ->{

                }
            }
        })


        viewModel.getPallet.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{

                }
                Status.SUCCESS ->{

                }
                Status.ERROR ->{

                }
            }
        })


    }
}