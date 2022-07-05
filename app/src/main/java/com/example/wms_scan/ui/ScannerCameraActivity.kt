package com.example.wms_scan.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.example.scanmate.data.callback.Status
import com.example.scanmate.extensions.gotoActivity
import com.example.scanmate.extensions.obtainViewModel
import com.example.scanmate.extensions.setTransparentStatusBarColor
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityScannerCameraBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

class ScannerCameraActivity : AppCompatActivity() {
    private val requestCodeCameraPermission = 1001
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""
    private lateinit var dialog: CustomProgressDialog
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityScannerCameraBinding
//    private lateinit var busLocNo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)
        viewModel = obtainViewModel(MainViewModel::class.java)

        if (ContextCompat.checkSelfPermission(
                this , android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askForCameraPermission()
        }
        else
        {
            setupControls()
        }

        val aniSlide: Animation = AnimationUtils.loadAnimation(this, R.anim.scanner_animation)
        binding.barcodeLine.startAnimation(aniSlide)
        dialog = CustomProgressDialog(this)
        initObserver()

    }

    private fun initObserver(){


        viewModel.scanAll.observe(this, Observer {
            when(it.status){
                Status.LOADING ->{
                    dialog.show()
                }
                Status.SUCCESS ->{
                    try
                    {
                        dialog.dismiss()
                        Log.i("scanAllResponse","${it.data?.get(0)?.pilotCode}")



                    }
                    catch (e:Exception)
                    {
                        Log.i("scanAll","${e.message}")
                    }

                }
                Status.ERROR ->{
                    dialog.dismiss()
                }
            }
        })

    }


    private fun setupControls() {
        barcodeDetector =
            BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true) //you should add this feature
            .build()

        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    //Start preview after 1s delay
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            @SuppressLint("MissingPermission")
            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int,
                width: Int, height: Int
            ) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })


        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
                Toast.makeText(applicationContext, "Scanner has been closed", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() == 1) {
                    scannedValue = barcodes.valueAt(0).rawValue


                    //Don't forget to add this line printing value or finishing activity must run on main thread
                    runOnUiThread {
                        cameraSource.stop()

                        val scannedData = scannedValue
                        var location    = ""
                        var warehouse   = ""
                        var rack        = ""
                        var shelve      = ""
                        var pallete     = ""

                        if (scannedData.contains("L"))
                        {
                            location = "${scannedData.substringBefore("L-")}L"
                            Log.i("LocCode","$location")
                        }

                        if (scannedData.contains("W"))
                        {
                            warehouse = "${scannedData.substringAfter("L-").substringBefore("WH")}WH"
                            Log.i("whCode","${warehouse}")
                        }

                        if (scannedData.contains("R"))
                        {
                            rack = "${scannedData.substringAfter("S-").substringBefore("R")}RK"
                            Log.i("rackCode","$rack")
                        }

                        if (scannedData.contains("S"))
                        {
                            shelve = "${scannedData.substringAfter("H-").substringBefore("S")}SF"
                            Log.i("shelfCode",shelve)

                        }

                        if (scannedData.contains("P"))
                        {
                            pallete = "${scannedData.substringAfter("R-").substringBefore("P")}PL"
                            Log.i("palletCode",pallete)

                        }

                        val intent =  Intent(this@ScannerCameraActivity, ShowAllHierarchy::class.java)
                        intent.putExtra("l",location)
                        intent.putExtra("w",warehouse)
                        intent.putExtra("r",rack)
                        intent.putExtra("s",shelve)
                        intent.putExtra("p",pallete)
                        startActivity(intent)

                        finish()

                    }
                }
                else { }
            }
        })
    }

    private fun askForCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            requestCodeCameraPermission
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupControls()
            } else {
                Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.stop()
    }
}