package com.example.wms_scan.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetWarehouseResponse
import com.example.scanmate.data.response.UserLocationResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.Toast.NoInternetFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isRefreshRequired
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isSpinnerSelected
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.userNo
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.warehouse.WarehouseAdapter
import com.example.wms_scan.databinding.ActivityWarehouseBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws


class WarehouseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWarehouseBinding
    private lateinit var warehouseAdapter: WarehouseAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private var selectedBusLocNo = ""
    private var whcode = ""
    private var businessLocName = ""
    private lateinit var bmp:Bitmap
    private val bmpList = mutableListOf<Bitmap>()
    private val textList = mutableListOf<String>()
    private var STORAGE_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWarehouseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initObserver()
        initListeners()
    }

    @SuppressLint("ResourceType")
    private fun setupUi(){

        dialog = CustomProgressDialog(this)
        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)

        val businessSpinner = binding.businessLocationSpinner

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

        if ((businessSpinner.adapter == null))
        {
            viewModel.userLocation(
                Utils.getSimpleTextBody(
                    LocalPreferences.getInt(this, userNo).toString()
                ))
        }

    }

    private fun initObserver() {

        /**
         *      USER LOCATION OBSERVER
         */

        viewModel.userLocation(
            Utils.getSimpleTextBody(
                LocalPreferences.getInt(this, userNo).toString()
            ))
        viewModel.userLoc.observe(this)
        {
            when(it.status){
                Status.LOADING->{
                    binding.swipeRefresh.isRefreshing = false
                    dialog.setCanceledOnTouchOutside(true);
                }
                Status.SUCCESS ->{
                    binding.swipeRefresh.isRefreshing = false
                    binding.businessLocationSpinner.visible()
                    binding.businessCont.visible()
                    binding.availableWHTV.visible()

                    if (isNetworkConnected(this))
                    {
                        binding.swipeRefresh.isRefreshing = false

                        if(it.data?.get(0)?.status == true)
                        {
                            dialog.dismiss()
                            showBusLocSpinner(it.data)
                        }
                        else
                        { }
                    }
                    else { }
                }
                Status.ERROR ->{
                    dialog.dismiss()
                    binding.warehouseRV.adapter = null
                    binding.businessLocationSpinner.gone()
                    binding.swipeRefresh.isRefreshing = false
                    binding.warehouseCont.gone()
                }
            }
        }

        /**
         *      GET WAREHOUSE OBSERVER
         */

        viewModel.getWarehouse.observe(this, Observer
        {
            when(it.status){
                Status.LOADING->{
                    binding.swipeRefresh.isRefreshing = isNetworkConnected(this)
                }
                Status.SUCCESS ->{
                    try {
                        binding.swipeRefresh.isRefreshing = false
                        LocalPreferences.put(this, isRefreshRequired, true)
                        if (isNetworkConnected(this))
                        {
                            Log.i("Data",it.data?.get(0).toString())
                            if(it.data?.get(0)?.status == true)
                            {
                                it.data[0].wHName?.let { it1 -> Log.i("warehouseResponse", it1) }
                                whcode = it.data[0].wHCode.toString()
                                binding.whAddBTN.click{
                                    if (isNetworkConnected(this)){
                                        val intent = Intent(this, WarehouseDetailsActivity::class.java)
                                        intent.putExtra("addBusName",businessLocName)
                                        intent.putExtra("addBusLocNo",selectedBusLocNo)
                                        intent.putExtra("addWhCode",whcode)
                                        intent.putExtra("AddWHKey",true)
                                        startActivity(intent)
                                    }
                                    else
                                    {
                                        toast(NoInternetFound)
                                    }
                                }

                                // DATA FOR QR CODE /////////////////

                                bmpList.clear()
                                textList.clear()

                                binding.printIV.click { btn ->
                                    if (isNetworkConnected(this)){
                                        try
                                        {
                                            for (i in it.data)
                                            {
                                                generateQRCode("${i.wHCode}")
                                                textList.add("${i.wHName}")
                                            }
                                            generatePDF()
                                        }
                                        catch (e:Exception)
                                        {
                                            Log.i("exception","${e.message}")
                                        }
                                    }
                                    else
                                    {
                                    }
                                }

                                warehouseAdapter = WarehouseAdapter(this,
                                    it.data as ArrayList<GetWarehouseResponse>
                                )
                                binding.warehouseRV.apply {
                                    layoutManager = LinearLayoutManager(this@WarehouseActivity)
                                    adapter = warehouseAdapter
                                }
                            }
                            else{

                                binding.warehouseRV.adapter = null
                                binding.availableWHTV.gone()
                                binding.printIV.click { btn ->
                                    toast("Nothing to print!")
                                }
                            }
                        }
                        else
                        {
                            binding.swipeRefresh.isRefreshing = false
                            binding.warehouseRV.adapter = null
                        }
                    }

                    catch(e:Exception){
                        Log.i("rackAdapter","${e.message}")
                        Log.i("rackAdapter","${e.stackTrace}")
                    }
                    //warehouseAdapter.addItems(list)
                }
                Status.ERROR ->{
                    dialog.dismiss()
                    binding.warehouseRV.adapter = null
                    binding.businessLocationSpinner.gone()
                    binding.swipeRefresh.isRefreshing = false
                    binding.warehouseCont.gone()
                    binding.connectionTimeout.visible()
                }
            }
        })
    }

    private fun initListeners() {

        binding.backBtn.click {
            onBackPressed()
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (isNetworkConnected(this@WarehouseActivity))
            {
                viewModel.userLocation(
                    Utils.getSimpleTextBody(
                        LocalPreferences.getInt(this, userNo).toString()
                    ))
                viewModel.getWarehouse("", LocalPreferences.getString(this, isSpinnerSelected).toString())
                binding.warehouseCont.visible()
                binding.connectionTimeout.gone()
            }
            else{
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.whAddBTN.click{

            if (isNetworkConnected(this)){

                    val intent = Intent(this, WarehouseDetailsActivity::class.java)
                    intent.putExtra("addBusName",businessLocName)
                    intent.putExtra("addBusLocNo",selectedBusLocNo)
                    intent.putExtra("AddWHKey",true)
                    startActivity(intent)
            }
        }
    }

    fun performAction(whName: String?, whNo: String, whCode:String) {
        val intent = Intent(this, WarehouseDetailsActivity::class.java)
        intent.putExtra("updateBusName",businessLocName)
        intent.putExtra("updateBusLocNo",selectedBusLocNo)
        intent.putExtra("updateWhName",whName)
        intent.putExtra("updateWhNo",whNo)
        intent.putExtra("updateWhCode",whcode)
        intent.putExtra("UpdateWHKey",true)
        startActivity(intent)
    }

    fun showQrCode(warehouseCode:String, whName:String, whNo:String) {
       val intent = Intent(this, QrCodeDetailActivity::class.java)
        intent.putExtra("warehouseKey",true)
        intent.putExtra("whQrCode",warehouseCode)
        intent.putExtra("whNo",whNo)
        intent.putExtra("whName",whName)
        intent.putExtra("busLoc","${selectedBusLocNo}L")
        startActivity(intent)
    }

    private fun showBusLocSpinner(data:List<UserLocationResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val businessLocSpinner = binding.businessLocationSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].busLocationName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        businessLocSpinner.adapter = adapter

        businessLocSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {
                businessLocName = data[position].busLocationName.toString()
                selectedBusLocNo = data[position].orgBusLocNo.toString()
                binding.warehouseRV.adapter = null
                if (isNetworkConnected(this@WarehouseActivity))
                {
                    Log.i("LocBus","business Location no ${data[position].orgBusLocNo}")
                    // binding.rackSpinnerCont.visible()
                    viewModel.getWarehouse("", selectedBusLocNo)

                }
                else{
                    val selectedLocation = data[position].orgBusLocNo.toString()
                    LocalPreferences.put(this@WarehouseActivity, isSpinnerSelected,"$selectedLocation")
                    binding.warehouseRV.adapter = null
                }

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                toast("Please select any value")
                binding.whAddBTN.isEnabled = false
            }
        }
    }

    private fun clearPreferences(context: Context) {
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        gotoActivity(LoginActivity::class.java)
    }

    override fun onResume() {
        super.onResume()
        if (LocalPreferences.getBoolean(this, isRefreshRequired ))
        {
            viewModel.getWarehouse("", selectedBusLocNo)
        }
    }

    private fun generateQRCode(text:String) {
        val qrWriter = QRCodeWriter()
        try
        {
                val bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, 512,512)
                val width = bitMatrix.width
                val height = bitMatrix.height
                bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                bmpList.add(bmp)
                for (x in 0 until width)
                {
                    for(y in 0 until height)
                    {
                        bmp.setPixel(x,y, if (bitMatrix[x,y]) Color.BLACK else Color.WHITE)
                    }
                }

        }
        catch (e:Exception) { }
    }

    private fun generatePDF(){
        //handle button click
        //we need to handle runtime permission for devices with marshmallow and above
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M){
            //system OS >= Marshmallow(6.0), check permission is enabled or not
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED){
                //permission was not granted, request it
                val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permissions, STORAGE_CODE)
            }
            else{
                //permission already granted, call savePdf() method
                savePdf()
            }
        }
        else{
            //system OS < marshmallow, call savePdf() method
            savePdf()
        }
    }

    private fun savePdf() {
        //create object of Document class


        //pdf file name
        //val mFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        //pdf file path
        val mFilePath = Environment.getExternalStorageDirectory().toString() + "/" + "QrGeneratedFile" +".pdf"
        try {
            val file = File(mFilePath)
            val mDoc = Document()
            PdfWriter.getInstance(mDoc, FileOutputStream(mFilePath))
            mDoc.open()

            val pdfTable = PdfPTable(2)

            for (i in bmpList.indices)
            {
                val stream = ByteArrayOutputStream()
                bmpList[i].compress(Bitmap.CompressFormat.JPEG, 100, stream)
                val myImg: Image = Image.getInstance(stream.toByteArray())
                myImg.scaleAbsolute(100f,100f)
                myImg.setAbsolutePosition(100f,100f)
                myImg.alignment = Element.ALIGN_CENTER

                val headingPara = Paragraph(Chunk("Warehouse"))
                headingPara.alignment = Element.ALIGN_CENTER

                val paragraph = Paragraph(Chunk(textList[i]))
                paragraph.alignment = Element.ALIGN_CENTER

                val pdfcell = PdfPCell()
                with(pdfcell)
                {
                    rowspan = 2
                    addElement(headingPara)
                    addElement(myImg)
                    addElement(paragraph)
                    paddingBottom = 10f
                }
                //pdfcell.horizontalAlignment = Element.ALIGN_CENTER;
                //pdfcell.verticalAlignment = Element.ALIGN_CENTER;
                //pdfcell.isNoWrap = false
                pdfTable.addCell(pdfcell)
            }

            pdfTable.completeRow()

            mDoc.add(pdfTable)
            mDoc.close()

            //show file saved message with file name and path
            openPDF(file, "QrGeneratedFile.pdf\nis saved to\n$mFilePath")
        }
        catch (e: Exception)
        {
            Log.i("pdfException","${e.message}")
            //if anything goes wrong causing exception, get and show exception message
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            STORAGE_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted, call savePdf() method
                    savePdf()
                }
                else{
                    //permission from popup was denied, show error message
                    Toast.makeText(this, "Permission denied...!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openPDF(file: File, text: String)
    {
        val path = FileProvider.getUriForFile(
            this,
            this.applicationContext.packageName.toString() + ".provider",
            file
        )
        val pdfOpenIntent = Intent(Intent.ACTION_VIEW)
        pdfOpenIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        pdfOpenIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        pdfOpenIntent.setDataAndType(path, "application/pdf")
        try
        {
            startActivity(pdfOpenIntent)
        }
        catch (e: ActivityNotFoundException)
        {
            Log.i("openPDFException","${e.message}")
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        finish()

    }

}