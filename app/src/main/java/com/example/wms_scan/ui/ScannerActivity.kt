package com.example.wms_scan.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.scanmate.data.callback.Status
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.Toast.noRecordFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isLogin
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityScannerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlin.system.exitProcess


class ScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannerBinding
    private var cameraRequestCode = 100
    private lateinit var viewModel:MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private var analOrMatInput =  ""
    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),cameraRequestCode)
        }

        setupUi()
        initListeners()
        initObserver()
//        getLocation()
        getCurrentLocation()

    }

    private fun setupUi(){

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)
        viewModel = obtainViewModel(MainViewModel::class.java)
        dialog = CustomProgressDialog(this)

    }

    private fun initObserver()
    {
        viewModel.getCartonQnWise.observe(this){
            when(it.status)
            {
                Status.LOADING ->
                {
                    dialog.show()
                }
                Status.SUCCESS ->
                {
                    if (isNetworkConnected(this))
                    {
                        it.let {
                            try
                            {
                                if (it.data?.get(0)?.status == true)
                                {
                                    dialog.hide()
                                    val intent = Intent(this, ShowAllHierarchy::class.java)
                                    intent.putExtra("manualMatName",analOrMatInput )
                                    intent.putExtra("manualAnalyticalKey",true)
                                    startActivity(intent)
                                }
                                else
                                {
                                    toast(noRecordFound)
                                }
                            }
                            catch (e:Exception)
                            {
                                Log.i("inputManual","${e.message}")
                                Log.i("inputManual","${e.stackTrace}")
                            }
                        }
                    }
                }
                Status.ERROR ->
                {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun initListeners(){

        binding.backBtn.click {
            gotoActivity(MenuActivity::class.java)
            finish()
        }

        binding.scanBtn.click {
            if (isNetworkConnected(this))
            {
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
                {
                    toast("Open camera permission manually")
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
               else
                {
                    gotoActivity(ScannerCameraActivity::class.java)
                }
            }
            else
            {
                toast("No internet")
            }
        }

        binding.scanCont.click {
            binding.themeChangeCont.gone()
        }

        binding.scannerView.click {
            binding.themeChangeCont.gone()
        }


        binding.theme1.click {

            binding.scanCont.visible()
            binding.videoSplash.gone()
            binding.scanTV.setTextColor(Color.parseColor("#000000"))
            binding.manualSearchTV.setTextColor(Color.parseColor("#000000"))
            binding.themeChangeCont.gone()

        }

        binding.theme2.click {

            binding.videoSplash.visible()
            binding.videoSplash.setDataSource(this, Uri.parse("android.resource://" + packageName + "/" + R.raw.warehouse22))
            binding.videoSplash.setLooping(true)
            binding.scanCont.gone()
            binding.scanTV.setTextColor(Color.parseColor("#FFFFFF"))
            binding.manualSearchTV.setTextColor(Color.parseColor("#FFFFFF"))
            binding.themeChangeCont.gone()
//            binding.themeChangeCont.gone()
            binding.videoSplash.play()
        }

        binding.loginIV.click {
            gotoActivity(LoginActivity::class.java)
        }


        binding.changeThemeIV.setOnClickListener {
            binding.themeChangeCont.visible()
        }

        binding.searchManualTV.click {

            binding.scanOptionCont.gone()
            binding.searchManualTV.gone()
            binding.searchScanTV.visible()
            binding.manualOptionCont.visible()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            binding.loginCont.gone()
//            binding.videoSplash.gone()
//            binding.themeChangeScannerCont.gone()
        }

        binding.searchScanTV.click {

            binding.scanOptionCont.visible()
            binding.searchManualTV.visible()
            binding.searchScanTV.gone()
            binding.manualOptionCont.gone()
            binding.loginCont.visible()

        }

        binding.searchBtn.click {

            if (isNetworkConnected(this))
            {
                ////    input of analytical num or material name

                if (binding.numOrMatNameTV.text.toString().isNullOrEmpty())
                {
                    var analOrMatInput = binding.numOrMatNameTV.text.toString()
                    toast("Field must not be empty")

                    Log.i("matInput",analOrMatInput)
                }
                else if(binding.numOrMatNameTV.text.toString().startsWith(" "))
                {
                    toast("Please do not enter whitespaces")
                }
                else if(binding.numOrMatNameTV.text.toString().startsWith("0") )
                {
                    toast("Please enter correct value")
                }
                else
                {
                    analOrMatInput = binding.numOrMatNameTV.text.toString()
                    viewModel.getCartonQnWise(analOrMatInput)
                }
            }

            else
            {
                toast("No internet found")
            }
        }





//        binding.toolbar.menu.findItem(R.id.loginItem).setOnMenuItemClickListener {
//            gotoActivity(LoginActivity::class.java)
//            true
//        }

//        binding.toolbar.menu.findItem(R.id.themeItem).setOnMenuItemClickListener {
////            binding.themeChangeCont.visible()
////            binding.videoSplash.visible()
////            binding.themeChangeScannerCont.visible()
//            true
//
//        }

//        binding.toolbar.menu.findItem(R.id.settings).setOnMenuItemClickListener {
//            true
//        }

//        binding.themeChangeScanner.click {
//            gotoActivity(ScannerCameraActivity::class.java)
//        }
    }

    override fun onResume() {
        super.onResume()

        when
        {
            LocalPreferences.getBoolean(this, isLogin)->{

                binding.scanToolbarTV.gone()
                binding.loginIV.gone()
                binding.changeThemeIV.gone()
                binding.backBtn.visible()
                binding.backBtn.click {
                    gotoActivity(MenuActivity::class.java)
                    finish()
                }
            }
            else->
            {
                binding.backBtn.gone()
            }
        }
    }

    override fun onBackPressed() {

        when
        {
            LocalPreferences.getBoolean(this, isLogin)->
            {
                toast("Please press back button")
            }
            else ->
            {
                exitProcess(0)
            }
        }

    }

//    private fun getLocation()
//    {
//        try {
//            if (ContextCompat.checkSelfPermission(
//                    applicationContext,
//                    Manifest.permission.ACCESS_FINE_LOCATION
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                    101
//                )
//            }
//        }
//        catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
//    }

    private fun isLocationEnabled():Boolean
    {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun getCurrentLocation()
    {
        if (checkPermission())
        {
            if (isLocationEnabled())
            {
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task->
                    val location = task.result
                    if (location == null)
                    {
                        toast("Null Received")
                    }
                    else
                    {
                        Log.i("latLngValues", "${location.latitude} ${location.longitude}")
                    }

                }
            }
            else
            {
                toast("Turn on Location")
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else
        {
            requestPermission()
        }
    }

    companion object
    {
        private const val PERMISSION_REQUEST_CURRENT_LOCATION = 100
    }

    private fun checkPermission():Boolean
    {

        if (
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION )== PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
        {
            return true
        }
        return false
    }

    private fun requestPermission()
    {
        ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ), PERMISSION_REQUEST_CURRENT_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CURRENT_LOCATION)
        {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                toast("Granted")
                getCurrentLocation()
            }
            else
            {
                toast("Denied")
            }
        }
    }
}