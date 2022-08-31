package com.example.wms_scan.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Path
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Transition
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.animation.PathInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.scanmate.data.callback.Status
import com.example.scanmate.extensions.*
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.pallets
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.rack
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.shelf
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.warehouse
import com.example.scanmate.util.Utils
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityCreateCartonBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.itextpdf.text.pdf.ColumnText.getWidth
import java.io.IOException


class CreateCartonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCartonBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var codeScanner: CodeScanner
    private lateinit var dialog: CustomProgressDialog
    private var scannedValue = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCartonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        codeScanner = CodeScanner(this,binding.cameraSurfaceView)
        setupUi()
        initListeners()
        codeScannerCamera()

        viewModel.palletHierarchy.observe(this, Observer {

            when(it.status)
            {
                Status.LOADING ->{}

                Status.SUCCESS ->{

                    it.let {
                        if(Utils.isNetworkConnected(this))
                        {
                            try
                            {
                                if (it.data?.get(0)?.status == true)
                                {
                                    if (scannedValue.contains("PL"))
                                    {
                                        gotoActivity(ScanCartonActivity::class.java, "scannedValue",scannedValue)
                                        Log.i("palletHierarchyCode",scannedValue)
                                    }
                                    else
                                    {
                                        toast( "Please scan pallet only" )
                                    }
                                }
                                else
                                {
                                    toast( "${it.data?.get(0)?.error}" )
                                }
                            }
                            catch (e:Exception)
                            {
                                Log.i("getCartonDetails","${Exception().message}")
                            }
                        }
                        else
                        {

                        }
                    }
                }
                Status.ERROR-> {}
            }

        })
    }

    private fun setupUi(){

        dialog = CustomProgressDialog(this)
        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)

    }

    private fun initListeners(){


        binding.scanBtn.click {
            binding.scanBtn.gone()
            binding.scannerCont.visible()
            binding.closeIV.visible()
            binding.clickHereTV.gone()
            codeScanner.startPreview()
        }

        binding.closeIV.click {
            binding.scannerCont.gone()
            binding.scanBtn.visible()
            binding.clickHereTV.visible()
            binding.closeIV.gone()
            codeScanner.stopPreview()
        }

        binding.backBtn.click {
            onBackPressed()
        }

    }

    private fun codeScannerCamera()
    {
        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                scannedValue = it.text

//                Toast.makeText(this, "Scan result: ${it.text}", Toast.LENGTH_LONG).show()

                if (scannedValue.contains("PL"))
                {
                    viewModel.palletHierarchy(
                        Utils.getSimpleTextBody(scannedValue),
                        Utils.getSimpleTextBody("0")
                    )
                }
                else
                {
                    toast("Scan pallet please")
                    codeScanner.startPreview()
                }
            }
        }

        codeScanner.errorCallback = ErrorCallback{ // or ErrorCallback.SUPPRESS
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }

        binding.cameraSurfaceView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume()
    {
        super.onResume()
        codeScanner.startPreview()
    }

    private fun clearPreferences(context: Context)
    {
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        gotoActivity(LoginActivity::class.java)
    }

    override fun onPause()
    {
        codeScanner.releaseResources()
        super.onPause()
    }

}