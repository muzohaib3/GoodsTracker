package com.example.wms_scan.ui

import android.accessibilityservice.AccessibilityService
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.scanmate.data.callback.Status
import com.example.scanmate.extensions.*
import com.example.scanmate.util.BiometricPromptUtils
import com.example.scanmate.util.Constants.LogMessages.success
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isLogin
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.loginTime
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userDesignation
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userName
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.Utils
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.databinding.ActivityLoginBinding
import com.example.wms_scan.utils.TextureVideoView

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initObservers()

    }

    private fun setupUi(){
//
        supportActionBar?.hide()
        dialog = CustomProgressDialog(this)
        setTransparentStatusBarColor(R.color.transparent)
        initListeners()

    }


    private fun initObservers(){

        viewModel.data.observe(this) {
            it.let {
                when (it.status) {

                    Status.LOADING -> {
                        dialog.show()
                        dialog.setCanceledOnTouchOutside(true)
                    }
                    Status.SUCCESS -> {
                        dialog.dismiss()

                        it.let {

                            Log.i(success, "${it.data?.get(0)?.emailID}")

                            // check status of user

                            if (it.data?.get(0)?.status == true) {
                                it.data[0].error?.let { it1 -> toast(it1) }

                                // check whether user is active or not

                                if (it.data[0].active == true) {
                                    gotoActivity(MenuActivity::class.java)

                                    // userNo and isLogin sent in preferences

                                    it.data[0].userNo?.let { it1 ->
                                        LocalPreferences.put(this, userNo, it1)
                                    }
                                    LocalPreferences.put(this, isLogin, true)
                                    LocalPreferences.put(
                                        this,
                                        userDesignation,
                                        it.data[0].desigName.toString()
                                    )
                                    LocalPreferences.put(
                                        this,
                                        userName,
                                        it.data[0].userName.toString()
                                    )
                                    LocalPreferences.put(
                                        this,
                                        loginTime,
                                        it.data[0].loginDT.toString()
                                    )

                                } else {
                                }
                            } else {
                                it.data?.get(0)?.error?.let { it1 -> toast(it1) }
                            }
                        }
                    }
                    Status.ERROR -> {
                        toast("Something went wrong")
                    }
                }
            }
        }
    }

    private fun initListeners()
    {
//
        binding.loginBtn.click {
            validations()
        }
        binding.fingerPrintIV.click {
            showBiometricPrompt()
        }

    }

    private fun showBiometricPrompt()
    {
        val biometricPromptUtils = BiometricPromptUtils(
            this,
            object : BiometricPromptUtils.BiometricListener
            {

                override fun onAuthenticationLockoutError() {}

                override fun onAuthenticationPermanentLockoutError() {}

                override fun onAuthenticationSuccess()
                {
                    gotoActivity(MenuActivity::class.java)
                }

                override fun onAuthenticationFailed() {}

                override fun onAuthenticationError() {}

            })

        biometricPromptUtils.showBiometricPrompt(
            resources.getString(R.string.confirmYourBiometricsKey),
            resources.getString(R.string.cancelKey),
            confirmationRequired = false
        )
    }

    private fun validations()
    {

        val userID = binding.userIdET.text.toString()
        val password = binding.passwordET.text.toString()

        //validations for fields and send parameters in viewModel

        if (userID.isNullOrEmpty() or password.isNullOrEmpty())
        {
            toast("Field must not be empty")
        }
        else
        {
            viewModel.loginUser(
                Utils.getSimpleTextBody(userID),
                Utils.getSimpleTextBody(password)
            )
        }
    }

    override fun onBackPressed()
    {
        finish()
    }

}