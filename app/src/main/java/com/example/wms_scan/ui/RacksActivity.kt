package com.example.wms_scan.ui

import android.Manifest
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanmate.data.callback.Status
import com.example.scanmate.data.response.GetRackResponse
import com.example.scanmate.data.response.GetWarehouseResponse
import com.example.scanmate.data.response.UserLocationResponse
import com.example.scanmate.extensions.*
import com.example.scanmate.util.Constants.Toast.NoInternetFound
import com.example.scanmate.util.CustomProgressDialog
import com.example.scanmate.util.LocalPreferences
import com.example.scanmate.util.LocalPreferences.AppLoginPreferences.isRefreshRequired
import com.example.scanmate.util.Utils
import com.example.scanmate.util.Utils.isNetworkConnected
import com.example.scanmate.viewModel.MainViewModel
import com.example.wms_scan.R
import com.example.wms_scan.adapter.racks.RackAdapter
import com.example.wms_scan.databinding.ActivityRacksBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RacksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRacksBinding
    private lateinit var racksAdapter: RackAdapter
    private lateinit var viewModel: MainViewModel
    private lateinit var dialog: CustomProgressDialog
    private var selectedBusLocNo = ""
    private var selectedWareHouseNo = ""
    private var selectedRackNo = ""
    private var selectedRackName = ""
    private var businessLocName = ""
    private var warehouseName = ""

    private var rackNo = ""
    private var rackName = ""
    private var rackCode = ""
    private lateinit var bmp:Bitmap
    private val bmpList = mutableListOf<Bitmap>()
    private var STORAGE_CODE = 1001
    private val textList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRacksBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = obtainViewModel(MainViewModel::class.java)
        setupUi()
        initListeners()
        initObserver()


    }

    private fun setupUi(){

        dialog = CustomProgressDialog(this)
        supportActionBar?.hide()
        setTransparentStatusBarColor(R.color.transparent)

        binding.userNameTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userName
        )
        binding.userDesignTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.userDesignation
        )
        binding.loginTimeTV.text = LocalPreferences.getString(this,
            LocalPreferences.AppLoginPreferences.loginTime
        )

    }

    private fun initListeners(){

        binding.toolbar.menu.findItem(R.id.logout).setOnMenuItemClickListener {
            clearPreferences(this)
            true
        }
        binding.rackAddBTN.click {
            if (isNetworkConnected(this))
            {
                val intent = Intent(this, AddUpdateRackDetails::class.java)
                intent.putExtra("addBusNo",selectedBusLocNo)
                intent.putExtra("addBusName",businessLocName)
                intent.putExtra("addWHNo",selectedWareHouseNo)
                intent.putExtra("addWHName",warehouseName)
                intent.putExtra("addRackNo",selectedRackNo)
                intent.putExtra("addRackName",selectedRackName)
                intent.putExtra("AddRackKey",true)
                startActivity(intent)
            }
            else
            {
                toast(NoInternetFound)
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            if (isNetworkConnected(this))
            {
                viewModel.userLocation(
                    Utils.getSimpleTextBody(
                        LocalPreferences.getInt(this, LocalPreferences.AppLoginPreferences.userNo).toString()
                    )
                )
                viewModel.getWarehouse("", selectedBusLocNo)

                viewModel.getRack(
                    Utils.getSimpleTextBody(""),
                    Utils.getSimpleTextBody(selectedWareHouseNo),
                    Utils.getSimpleTextBody(selectedBusLocNo)
                )

            }
            else
            {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.printIV.click {
            generatePDF()
        }

    }

    fun openActivity(rackName: String?, rackNo: String){

        if (isNetworkConnected(this))
        {
            Log.i("Warehouse","$rackName $rackNo")
            val intent = Intent(this, AddUpdateRackDetails::class.java)
            intent.putExtra("updateBusNo",selectedBusLocNo)
            intent.putExtra("updateBusName",businessLocName)
            intent.putExtra("updateWHNo",selectedWareHouseNo)
            intent.putExtra("updateWhName",warehouseName)
            intent.putExtra("updateRackName",rackName)
            intent.putExtra("updateRackNo",rackNo)
            intent.putExtra("updateRackKey",true)
            startActivity(intent)
        }
        else
        {
            toast(NoInternetFound)
        }
    }

    private fun initObserver(){

        /**
         *      USER LOCATION OBSERVER
         */

        viewModel.userLocation(
            Utils.getSimpleTextBody(
                LocalPreferences.getInt(this,
                    LocalPreferences.AppLoginPreferences.userNo
                ).toString()
            ))
        viewModel.userLoc.observe(this, Observer {
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status){
                        Status.LOADING->{
                            dialog.show()
                            dialog.setCanceledOnTouchOutside(true);
                        }
                        Status.SUCCESS ->{
                            if(it.data?.get(0)?.status == true)
                            {
                                dialog.dismiss()
                                showBusLocSpinner(it.data)
                            }
                            else
                            {

                                binding.racksRV.adapter = null
                            }
                        }
                        Status.ERROR ->{
                            dialog.dismiss()
                        }
                    }
                }
                else
                {

                }

            }
        })

        /**
         *      GET WAREHOUSE OBSERVER
         */

        viewModel.getWarehouse.observe(this, Observer{
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status){
                        Status.LOADING->{
                        }
                        Status.SUCCESS ->{

                            try {
                                if(it.data?.get(0)?.status == true)
                                {
                                    it.data[0].wHName?.let { it1 -> Log.i("warehouseResponse", it1) }
                                    showWarehouseSpinner(it.data)
                                }
                                else
                                {
                                    toast("No record found")
                                    binding.racksRV.adapter = null
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
                        }
                    }
                }

            }
        })

        /**
         *      GET WAREHOUSE OBSERVER
         */

        viewModel.getWarehouse.observe(this, Observer{
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status){
                        Status.LOADING->{
                        }
                        Status.SUCCESS ->{
                            binding.swipeRefresh.isRefreshing = false

                            try {
                                LocalPreferences.put(this, isRefreshRequired, false)

                                if(it.data?.get(0)?.status == true)
                                {
                                    it.data[0].wHName?.let { it1 -> Log.i("warehouseResponse", it1) }
                                    showWarehouseSpinner(it.data)

                                }
                                else
                                {
                                    binding.racksRV.adapter = null
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
                        }
                    }
                }

            }
        })

        /**
         *      GET RACK OBSERVER
         */

        viewModel.getRack.observe(this, Observer{
            it.let {
                if(isNetworkConnected(this))
                {
                    when(it.status){
                        Status.LOADING ->{
                        }
                        Status.SUCCESS ->{
                            // Log.i("getRack",it.data?.get(0)?.rackNo.toString())
                            try
                            {
                                if(it.data?.get(0)?.status == true)
                                {
                                    rackName = it.data[0].rackName.toString()
                                    rackNo = it.data[0].rackNo.toString()
                                    rackCode = it.data[0].rackCode.toString()

                                    showRackSpinner(it.data)
                                    racksAdapter = RackAdapter(this,it.data as ArrayList<GetRackResponse>)

                                    bmpList.clear()
                                    textList.clear()

                                    for (i in it.data)
                                    {
                                        generateQRCode("${i.rackCode}-${i.rackNo}")
                                        textList.add(i.rackCode!!)
                                        Log.i("rackList","${i.rackCode}-${i.rackNo}")
                                    }

                                    binding.racksRV.apply {

                                        layoutManager = LinearLayoutManager(this@RacksActivity)
                                        adapter = racksAdapter
                                    }
                                }else{
                                    binding.racksRV.adapter = null
                                }
                            }
                            catch (e: Exception)
                            {
                                Log.i("RACK_OBSERVER","${e.message}")
                                Log.i("RACK_OBSERVER","${e.stackTrace}")
                            }
                        }
                        Status.ERROR ->{
                            dialog.dismiss()
                        }
                    }
                }

            }
        })

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

                if (isNetworkConnected(this@RacksActivity)){
                    Log.i("LocBus","business Location no ${data[position].orgBusLocNo}")
                    // binding.rackSpinnerCont.visible()
                    businessLocName = data[position].busLocationName.toString()
                    selectedBusLocNo = data[position].orgBusLocNo.toString()
                    viewModel.getWarehouse("", selectedBusLocNo)
                }
                else
                {
                    binding.racksRV.adapter = null

                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun showWarehouseSpinner(data:List<GetWarehouseResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val warehouseSpinner = binding.warehouseSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].wHName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        warehouseSpinner.adapter = adapter
        warehouseSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{

            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {

                if (isNetworkConnected(this@RacksActivity)) {
                    selectedWareHouseNo = data[position].wHNo.toString()
                    viewModel.getRack(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(selectedWareHouseNo),
                        Utils.getSimpleTextBody(selectedBusLocNo)
                    )
                    warehouseName = data[position].wHName.toString()
                    Log.i("LocBus","This is warehouse name is ${adapter?.getItemAtPosition(position)}")
                    Log.i("LocBus","This is warehouse pos is ${data[position].wHNo}")
                }
                else
                {
                    binding.racksRV.adapter = null
                    toast(NoInternetFound)
                }

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun showRackSpinner(data:List<GetRackResponse>) {
        //String array to store all the book names
        val items = arrayOfNulls<String>(data.size)
        val rackSpinner = binding.rackSpinner

        //Traversing through the whole list to get all the names
        for (i in data.indices) {
            //Storing names to string array
            items[i] = data[i].rackName
        }
        val adapter: ArrayAdapter<String?> =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        //setting adapter to spinner
        rackSpinner.adapter = adapter

        rackSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, long: Long) {
                if (Utils.isNetworkConnected(this@RacksActivity)) {
                    selectedRackNo = data[position].rackNo.toString()
                    selectedRackName = data[position].rackName.toString()
                    viewModel.getShelf(
                        Utils.getSimpleTextBody(""),
                        Utils.getSimpleTextBody(selectedRackNo),
                        Utils.getSimpleTextBody(selectedBusLocNo)
                    )
                }
                else
                {
                    binding.racksRV.adapter = null
                    toast(NoInternetFound)
                }

                Log.i("LocBus","This is rack pos ${adapter?.getItemAtPosition(position)}")
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()

        if (LocalPreferences.getBoolean(this, isRefreshRequired))
        {
            viewModel.getRack(
                Utils.getSimpleTextBody(""),
                Utils.getSimpleTextBody(selectedWareHouseNo),
                Utils.getSimpleTextBody(selectedBusLocNo)
            )
        }
    }

    fun showQrCode(rackCode:String, rackName:String, rackNo:String){
        val intent = Intent(this, QrCodeDetailActivity::class.java)
        intent.putExtra("rackKey",true)
        intent.putExtra("rackQrCode",rackCode)
        intent.putExtra("rackQrName",rackName)
        intent.putExtra("rackQrNo",rackNo)
        startActivity(intent)
    }

    private fun clearPreferences(context: Context){
        val settings: SharedPreferences =
            context.getSharedPreferences(LocalPreferences.AppLoginPreferences.PREF, Context.MODE_PRIVATE)
        settings.edit().clear().apply()
        gotoActivity(LoginActivity::class.java)
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
        val mFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        //pdf file path
        val mFilePath = Environment.getExternalStorageDirectory().toString() + "/" + "QrGeneratedFile" +".pdf"
        try {

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

                val headingPara = Paragraph(Chunk("Racks"))
                headingPara.alignment = Element.ALIGN_CENTER

                val rackCode = Paragraph(Chunk("1010-2"))
                rackCode.alignment = Element.ALIGN_CENTER

                val pdfcell = PdfPCell()
                with(pdfcell)
                {
                    rowspan = 2
                    addElement(headingPara)
                    addElement(myImg)
                    addElement(rackCode)
                    paddingBottom = 10f
                }

                pdfTable.addCell(pdfcell)
            }

            mDoc.add(pdfTable)
            mDoc.close()

            //show file saved message with file name and path
            Toast.makeText(this, "$mFileName.pdf\nis saved to\n$mFilePath", Toast.LENGTH_SHORT).show()
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
}