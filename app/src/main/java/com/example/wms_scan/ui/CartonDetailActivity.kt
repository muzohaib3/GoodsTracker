package com.example.wms_scan.ui

import android.content.Context
import android.content.Intent

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetShelfResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.pallets
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.rack
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.shelf
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.warehouse
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.pallets.PalletsAdapter
import com.example.wms_scan.data.response.GetPalletResponse
import com.example.wms_scan.databinding.ActivityCartonDetailBinding
import com.example.wms_scan.utils.PermissionDialog
import java.lang.Exception
import java.util.*


class CartonDetailActivity : AppCompatActivity() {
    private lateinit var binding:ActivityCartonDetailBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var permissionDialog: PermissionDialog
    private var analyticalNo = ""
    private var totCarton = ""
    private var pilotNo = ""
    private var cartonSNo = ""
    private var cartonNo = ""
    private var itemCode = ""
    private var stock = ""
    private var hierarchyLoc = ""
    private var deviceId = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartonDetailBinding.inflate(layoutInflater)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setContentView(binding.root)
        permissionDialog = PermissionDialog(this)
        initListener()
        setupUi()

        analyticalNo =  intent.extras?.getString("scanAnalyticalNum").toString()
        Log.i("analyticalNum", analyticalNo)

        val palletCode =  intent.extras?.getString("palletCode").toString()
        viewModel.palletHierarchy(Utils.getSimpleTextBody(palletCode), Utils.getSimpleTextBody("0"))
        Log.i("palletCode1", palletCode)

        /**
         *  PALLET HIERARCHY API
         */

        viewModel.palletHierarchy.observe(this, Observer {

            when(it.status){

                Status.LOADING ->
                {

                }

                Status.SUCCESS ->
                {
                    Log.i("palletCode","${it.data?.get(0)?.pilotCode}")

                    val warehouse = it.data?.get(0)?.wHName.toString()
                    val racks = it.data?.get(0)?.rackName.toString()
                    val shelf = it.data?.get(0)?.shelfName.toString()
                    val shelfNo = it.data?.get(0)?.shelfNo.toString()
                    val pallet = it.data?.get(0)?.pilotName.toString()
                    pilotNo = it.data?.get(0)?.pilotNo.toString()
                    val busLoc = it.data?.get(0)?.locationNo.toString()
                    hierarchyLoc = it.data?.get(0)?.locationNo.toString()
                    Log.i("shelfNoCarton",shelfNo)

                    binding.WHTV.text = warehouse
                    binding.rackTV.text = racks
                    binding.shelfTV.text = shelf
                    binding.palletTV.text = pallet

                    val hierarchyPilotNo = it.data?.get(0)?.pilotNo.toString()
                    Log.i("hierarchyPilot","$hierarchyPilotNo $busLoc")

                    Log.i("pallethierarchy","1.$warehouse\n2.$racks\n3.$shelf\n4.$pallet\n")

                    viewModel.getPallet(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(shelfNo),
                        Utils.getSimpleTextBody(busLoc),
                    )
                    Log.i("hierarchyPilotNo","$shelfNo $busLoc")
                }

                Status.ERROR -> { }
            }

        })

        /**
         *  CARTON DETAIL API
         */

        viewModel.getCartonDetails("$analyticalNo")

        viewModel.getCartonDetails.observe(this)
        {

            when(it.status){

                Status.LOADING ->{ }

                Status.SUCCESS ->
                {
                    it.let {
                        if(isNetworkConnected(this))
                        {

                            binding.materialNameTV.text = it.data?.get(0)?.materialName
                            binding.analyticalNumTV.text = it.data?.get(0)?.analyticalNo
                            binding.cartonNumTV.text = it.data?.get(0)?.cartonSNo.toString()
                            binding.totCartonTV.text = it.data?.get(0)?.totCarton.toString()
                            binding.stockTV.text = it.data?.get(0)?.matStock.toString()
                            binding.palletName.text = it.data?.get(0)?.pilotName.toString()
                            binding.remainingCartons.text = it.data?.get(0)?.remCarton.toString()
                            itemCode = it.data?.get(0)?.materialId.toString()
                            cartonSNo = it.data?.get(0)?.cartonSNo.toString()
                            totCarton = it.data?.get(0)?.totCarton.toString()

                            if (analyticalNo.contains("PK"))
                            {
                                binding.cartonNumber.text = "Total Carton"
                                binding.cartonNumTV.gone()
                                binding.slash.gone()
                            }
                            else if (analyticalNo.contains("RM"))
                            {
                                binding.cartonNumber.text = "Carton Number"
                            }

                            Log.i(
                                "getCartonDetail","1.0\n2.0\n3.$itemCode\n4.$pilotNo\n5.$analyticalNo\n6.0\n7.0\n8.$hierarchyLoc\n9.$deviceId"
                            )
                        }
                    }
                }
                Status.ERROR -> { }
            }
        }

        /**
         *  ADD CARTON API
         */

        viewModel.addCarton.observe(this, Observer {
            when(it.status){

                Status.LOADING ->{ }

                Status.SUCCESS ->
                {
                    it.let {
                        if(isNetworkConnected(this))
                        {
                            if (it.data?.status == true)
                            {
                                toast("${it.data.error}")
                                Log.i("IntentSave","${it.data.error}")

                                finish()
                            }
                            else
                            {
                                toast("${it.data?.error}")
                            }
                        }
                    }
                }

                Status.ERROR -> { }

            }
        })

    }

    private fun initListener()
    {

        binding.backBtn.click {
            onBackPressed()
        }

        binding.saveBtn.click {

            viewModel.addCarton(
                Utils.getSimpleTextBody("0"),
                Utils.getSimpleTextBody("0"),
                Utils.getSimpleTextBody("$itemCode"),
                Utils.getSimpleTextBody("$pilotNo"),
                Utils.getSimpleTextBody("$analyticalNo"),
                Utils.getSimpleTextBody(cartonSNo),
                Utils.getSimpleTextBody(totCarton),
                Utils.getSimpleTextBody(hierarchyLoc),
                Utils.getSimpleTextBody(LocalPreferences.getInt(this,userNo).toString()),
                Utils.getSimpleTextBody(deviceId)
            )
            Log.i(
                "getcarton","1.0\n2.0\n3.$itemCode\n4.$pilotNo\n5.$analyticalNo\n6.$cartonSNo\n" +
                        "7.$totCarton\n8.$hierarchyLoc\n9.$deviceId"
            )
            Log.i("itemcode",itemCode)

            binding.saveBtn.gone()
            binding.changeTV.visible()

        }

        binding.changeTV.click {
            binding.changeTV.gone()
            binding.palletView.visible()
            binding.scanToUpdate.visible()
            binding.scanBtn.visible()
        }

        binding.scanBtn.click {
            gotoActivity(CreateCartonScanActivity::class.java)
            LocalPreferences.put(this, "isScannedAnalyticalNo",analyticalNo)
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    private fun setupUi(){

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)
        Log.i("pilotNo",pilotNo)

        binding.analyticalNumTV.text = analyticalNo
        binding.stockTV.text = stock
        binding.cartonNumTV.text = cartonNo

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        deviceId = UUID.randomUUID().toString()
        Log.i("deviceId",deviceId)

        Log.i("isExist",intent.extras?.getInt("isExist").toString())

        when
        {
            intent.extras?.getInt("isExist") == 1 ->
            {
                binding.changeTV.visible()
            }

            intent.extras?.getInt("isExist") == 0 ->
            {
                binding.changeTV.gone()
                binding.saveBtn.visible()
                binding.palletName.gone()
                binding.palletNameTV.gone()
            }
        }

        when
        {
            intent.extras!!.getBoolean("PKSNo") ->
            {
                binding.saveBtn.gone()
                Log.i("PHSno", "${intent.extras!!.getBoolean("PKSNo")}")
            }
        }

        var scannedValue = intent.extras?.getString("createCartonFlag")
        Log.i("scannedValue", scannedValue.toString())
    }

    override fun onResume()
    {
        super.onResume()
        when
        {
            intent.extras?.getBoolean("isScannedKey") == true ->{

                var scannedPallet = LocalPreferences.getString(this,"createCartonScanValue").toString()

                viewModel.palletHierarchy(Utils.getSimpleTextBody(scannedPallet), Utils.getSimpleTextBody("0"))
                Log.i("createCartonScannedLog",LocalPreferences.getString(this, "createCartonScanValue").toString())

                viewModel.getCartonDetails(LocalPreferences.getString(this,"isScannedAnalyticalNo").toString())

                binding.scanToUpdate.gone()
                binding.changeTV.gone()
                binding.updateBtn.visible()

                binding.updateBtn.click {

                    Log.i("isScannedAddCarton","1.$itemCode\n2.$pilotNo\n3.$analyticalNo\n4.$hierarchyLoc\n5.$deviceId")
                    viewModel.addCarton(
                        Utils.getSimpleTextBody(cartonNo),
                        Utils.getSimpleTextBody("0"),
                        Utils.getSimpleTextBody("$itemCode"),
                        Utils.getSimpleTextBody("$pilotNo"),
                        Utils.getSimpleTextBody("${LocalPreferences.getString(this,"isScannedAnalyticalNo").toString()}"),
                        Utils.getSimpleTextBody(cartonSNo),
                        Utils.getSimpleTextBody(totCarton),
                        Utils.getSimpleTextBody(hierarchyLoc),
                        Utils.getSimpleTextBody(LocalPreferences.getInt(this,userNo).toString()),
                        Utils.getSimpleTextBody(deviceId)
                    )

                    viewModel.palletHierarchy(Utils.getSimpleTextBody(scannedPallet), Utils.getSimpleTextBody("0"))

                }
            }
        }
    }
}