package pl.com.edps.uidReader

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.hardware.Camera
import android.location.Location
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.PowerManager
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import com.android.hdhe.uhf.reader.SerialPort
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.magicrf.uhfreaderlib.reader.UhfReader
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.absoluteValue


const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
const val MY_PERMISSIONS_REQUEST_ACCESS_NFC = 2
const val MY_PERMISSIONS_REQUEST_ACCESS_CAMERA = 3
const val REQUEST_IMAGE_CAPTURE = 4

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent

    private var listEPC: ArrayList<EPC>? = null
    private var listMap: ArrayList<Map<String, Any>>? =
        null
    private val runFlag = true
    private val startFlag = false
    private var serialPortPath = "/dev/ttyS2"
    private var reader
            : UhfReader? = null
    private var readerDevice
            : UhfReaderDevice? = null
    private var mWakeLock: PowerManager.WakeLock? = null

    private var screenReceiver: ScreenStateReceiver? = null

    class EPC {

        var id = 0
        var epc: String? = null
        var count = 0

        override fun toString(): String {
            return "EPC [id=$id, epc=$epc, count=$count]"
        }
    }

    class ScreenStateReceiver : BroadcastReceiver() {
        private var reader: UhfReader? = null
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            reader = UhfReader.getInstance()

        }
    }

    class UhfReaderDevice {
        fun powerOn() {

        }

        fun powerOff() {
            if (devPower != null) {

                devPower = null
            }
        }

        companion object {
            private var readerDevice: UhfReaderDevice? = null
            private var devPower: SerialPort? = null

            fun getInstance(): UhfReaderDevice? {
                if (devPower == null) {
                    devPower = try {
                        SerialPort()
                    } catch (e: Exception) {
                        return null
                    }
                }
                if (readerDevice == null) {
                    readerDevice = UhfReaderDevice()
                }
                return readerDevice
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if(nfcAdapter == null)
            findViewById<TextView>(R.id.nfcView).text = "!! NULL !!"

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, this.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            0
        )
        findViewById<TextView>(R.id.stateView).text = "onCreate"
    }

    override fun onResume() {
        super.onResume()

        if(nfcAdapter != null && nfcAdapter.isEnabled) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onPause() {
        super.onPause()

        if(nfcAdapter != null && nfcAdapter.isEnabled) {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        setIntent(intent)
        resolveIntent(intent)
    }

    private fun resolveIntent(intent: Intent) {
        when(intent.action) {
            NfcAdapter.ACTION_TAG_DISCOVERED, NfcAdapter.ACTION_TECH_DISCOVERED, NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                if (rawMessages != null) {
                    findViewById<TextView>(R.id.nfcView).text = "Got A message!"
                } else {
                    val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    findViewById<TextView>(R.id.nfcView).text = tag.id.toHex()
                }
            }
            else -> {
                findViewById<TextView>(R.id.nfcView).text = "Not NFC"
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data.extras.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
        }
    }

    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()

    fun ByteArray.toHex() : String {
        val result = StringBuffer()

        forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS[firstIndex])
            result.append(HEX_CHARS[secondIndex])
        }

        return result.toString()
    }

    fun openUART(view: View){
        val sharedPortPath = getSharedPreferences("portPath", 0)
        serialPortPath = sharedPortPath.getString("portPath", "/dev/ttyS2")
        UhfReader.setPortPath(serialPortPath)
        reader = UhfReader.getInstance()

        readerDevice = UhfReaderDevice.getInstance()

        if (reader == null) {
            findViewById<TextView>(R.id.stateView).text = "serialport init fail"
            return
        }
        if (readerDevice == null) {
            findViewById<TextView>(R.id.stateView).text = "UHF reader power on failed"
        }
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val shared = getSharedPreferences("power", 0)
        val value = shared.getInt("value", 26)
        Log.d("", "value$value")
        reader!!.setOutputPower(value)

        //添加广播，默认屏灭时休眠，屏亮时唤醒
        screenReceiver = ScreenStateReceiver()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)


        if (readerDevice != null && reader != null) {
            findViewById<TextView>(R.id.stateView).text = "Port open: " + reader!!.portPath

        }
    }

    fun emuLegic(view: View){
        findViewById<TextView>(R.id.stateView).text = "Legic: "
    }

    fun readUID(view: View){
        Thread.sleep(1)
        val ser = reader!!.portPath
        findViewById<TextView>(R.id.stateView).text = "UID: "
    }

    fun sendGPSmessage(view: View) {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
                val loc = if (location != null) location else Location("")
                findViewById<TextView>(R.id.gpcoView).text = LocationConverter.latitudeAsDMS(loc.latitude,9).plus(" ").plus(LocationConverter.longitudeAsDMS(loc.longitude, 9))
            }
        } else {
            findViewById<TextView>(R.id.gpcoView).text = getString (R.string.pl_com_edps_basmoc_info_noGPSpriv)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
        findViewById<TextView>(R.id.stateView).text = "sendGPSmessage"
    }

    fun sendNFCmessage(view: View) {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.NFC ) == PackageManager.PERMISSION_GRANTED ) {
            findViewById<TextView>(R.id.nfcView).text = getString( R.string.pl_com_edps_basmoc_info_waitingForInput)
        } else {
            findViewById<TextView>(R.id.nfcView).text = getString (R.string.pl_com_edps_basmoc_info_noNFCpriv)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.NFC), MY_PERMISSIONS_REQUEST_ACCESS_NFC)
        }
        findViewById<TextView>(R.id.stateView).text = "sendNFCmessage"
    }

    fun sendPhotoMessage(view: View) {
        if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED ) {
            dispatchTakePictureIntent()
            findViewById<TextView>(R.id.stateView).text = "sendPhotoMessage"
        } else {
            findViewById<TextView>(R.id.stateView).text = getString (R.string.pl_com_edps_basmoc_info_noCAMpriv)
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_ACCESS_CAMERA)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED ) {
                        findViewById<TextView>(R.id.gpcoView).text = getString( R.string.pl_com_edps_basmoc_info_points)
                    }
                } else {
                    // permission denied, boo!
                    findViewById<TextView>(R.id.gpcoView).text = getString (R.string.pl_com_edps_basmoc_info_noGPSpriv)
                }
                return
            }

            MY_PERMISSIONS_REQUEST_ACCESS_NFC -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.NFC ) == PackageManager.PERMISSION_GRANTED ) {
                        findViewById<TextView>(R.id.nfcView).text = getString( R.string.pl_com_edps_basmoc_info_points)
                    }
                } else {
                    // permission denied, boo!
                    findViewById<TextView>(R.id.nfcView).text = getString (R.string.pl_com_edps_basmoc_info_noNFCpriv)
                }
                return
            }

            MY_PERMISSIONS_REQUEST_ACCESS_CAMERA -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED ) {
                        findViewById<TextView>(R.id.stateView).text = getString( R.string.pl_com_edps_basmoc_info_points)
                    }
                } else {
                    // permission denied, boo!
                    findViewById<TextView>(R.id.stateView).text = getString (R.string.pl_com_edps_basmoc_info_noCAMpriv)
                }
                return
            }
            // Add other 'when' lines to check for other permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
        findViewById<TextView>(R.id.stateView).text = "onRequestPermissionsResult"
    }

    object LocationConverter {

        fun latitudeAsDMS(latitude: Double, decimalPlace: Int): String {
            val direction = if (latitude > 0) "N" else "S"
            var strLatitude = Location.convert(latitude.absoluteValue, Location.FORMAT_SECONDS)
            strLatitude = replaceDelimiters(strLatitude, decimalPlace)
            strLatitude += " $direction"
            return strLatitude
        }

        fun longitudeAsDMS(longitude: Double, decimalPlace: Int): String {
            val direction = if (longitude > 0) "W" else "E"
            var strLongitude = Location.convert(longitude.absoluteValue, Location.FORMAT_SECONDS)
            strLongitude = replaceDelimiters(strLongitude, decimalPlace)
            strLongitude += " $direction"
            return strLongitude
        }

        private fun replaceDelimiters(str: String, decimalPlace: Int): String {
            var str = str
            str = str.replaceFirst(":".toRegex(), "°")
            str = str.replaceFirst(":".toRegex(), "'")
            val pointIndex = str.indexOf(".")
            val endIndex = pointIndex + 1 + decimalPlace
            if (endIndex < str.length) {
                str = str.substring(0, endIndex)
            }
            str += "\""
            return str
        }
    }

    fun rotateBitmap(source: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(180.0F)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    var bitmap: Bitmap? = null
    var mCamera = android.hardware.Camera.open(1)

    fun getQuickPhoto(view: View) {
        mCamera.run {
            val st = SurfaceTexture(Context.MODE_PRIVATE)
            this.setPreviewTexture(st)
            this.startPreview()
        }
        val mCall = Camera.PictureCallback { data, _ ->
            imageView?.setImageBitmap(
                BitmapFactory.decodeByteArray(data, 0, data.size)
            )
            bitmap = rotateBitmap((imageView.drawable as BitmapDrawable).bitmap)
            imageView?.setImageBitmap(
                bitmap
            )
        }
        mCamera.takePicture(null, null, mCall) // takes 1.0sec
    }
}
