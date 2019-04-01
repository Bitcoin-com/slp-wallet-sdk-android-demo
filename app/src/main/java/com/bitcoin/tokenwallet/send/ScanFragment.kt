package com.bitcoin.tokenwallet.send

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Build
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bitcoin.tokenwallet.MyNavSubGraphFragment

import com.bitcoin.tokenwallet.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class ScanFragment: Fragment(), BarcodeUpdateListener, MyNavSubGraphFragment, PermissionsReceiver {
    private val TAG: String = "ScanFragment"
    private lateinit var testButton: Button

    private val mBarcodes = HashMap<Int, Barcode>()
    private var mCameraSource: CameraSource? = null
    private var mCameraSourcePreview: CameraSourcePreview? = null
    private var mIsReading = AtomicBoolean(false)
    private var mIsRequestingPermission = false

    // intent request code to handle updating play services if needed.
    private val RC_HANDLE_GMS: Int = 9001

    private val singleThreadPool: ExecutorService = Executors.newSingleThreadExecutor()

    private val addressPattern = Pattern.compile("^\\w+:\\w+$")

    companion object {
        fun newInstance() = ScanFragment()
    }

    private lateinit var viewModel: ScanViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.scan_fragment, container, false)

        testButton = view.findViewById<Button>(R.id.testButton)
        testButton.setOnClickListener { v: View ->
            navigateToSendWithText(testButton, "hello")
        }

        mCameraSourcePreview = view.findViewById(R.id.preview)

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ScanViewModel::class.java)
        // TODO: Use the ViewModel

        setUpActionBar()

        (activity as? AppCompatActivity)?.let {
            if (hasCameraPermission(it)) {
                startReadingWithPermission()
            }
        }
    }

    private fun hasCameraPermission(act: AppCompatActivity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission: Int? = act.applicationContext?.checkSelfPermission(Manifest.permission.CAMERA)
            if (permission != null && permission == PackageManager.PERMISSION_GRANTED) {
                return true
            } else {
                mIsRequestingPermission = true
                act.requestPermissions(arrayOf(Manifest.permission.CAMERA),PermissionsReceiver.REQ_CAMERA)
                return false
            }
        } else {
            return true
        }
    }


    override fun onBarcodeDetected(barcode: Barcode?) {
        if (barcode != null) {
            val rawValue: String? = barcode.rawValue
            if (rawValue != null) {
                Timber.d("Detected barcode. ${rawValue}")

                if (addressPattern.matcher(rawValue).matches()) {
                    navigateToSendWithText(testButton, rawValue)
                } else {
                    Timber.d("Detected barcode was invalid (not an address).")
                }
            } else {
                Timber.e("Barcode raw value was null.")
            }
        }
    }

    override fun onPermissionResult(grantResult: Int) {
        Timber.d("onPermissionResult().")

        mIsRequestingPermission = false

        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Timber.d("Permissions newly granted.")
            startReadingWithPermission()
        }
    }

    override fun onPause() {
        Timber.d("onPause()")
        onPauseOrNavigationHide()
        super.onPause()
    }


    override fun onResume() {
        Timber.d("onResume()")
        super.onResume()
        if (isVisible) {
            onResumeOrNavigationShow()
        }
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private fun createCameraSource(context: Context): Boolean {
        Timber.d("createCameraSource()")
        val useFlash = false

        val autoFocus = true
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeMapTrackerFactory(mBarcodes, this)
        barcodeDetector.setProcessor(
            MultiProcessor.Builder(barcodeFactory).build()
        )

        if (!barcodeDetector.isOperational) {
            // Note: The first time that an app using the barcode or face API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any barcodes
            // and/or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Timber.d("Detector dependencies are not yet available.")
            //callbackContext.error(QRReaderError.ERROR_DETECTOR_DEPENDENCIES_UNAVAILABLE.name())
            return false


            /* TODO: Handle this better later?
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, "Low storage error.");
            }
            */
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        var builder: CameraSource.Builder = CameraSource.Builder(context, barcodeDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(1600, 1024)
            .setRequestedFps(15.0f)

        // Auto focus is always available since version is Ice Cream Sandwich or higher
        builder = builder.setFocusMode(
            if (autoFocus) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else null
        )

        mCameraSource = builder
            .setFlashMode(if (useFlash) Camera.Parameters.FLASH_MODE_TORCH else null)
            .build()

        return true
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    @Throws(SecurityException::class)
    private fun startCameraSource(context: Context): Boolean {


        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (code != ConnectionResult.SUCCESS) {
            Timber.d("Asking to update Google Play services")
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(activity, code, RC_HANDLE_GMS)
            dlg.show()
            //callbackContext.error(QRReaderError.ERROR_GOOGLE_PLAY_SERVICES_UNAVAILABLE.name)
            return false
        }

        // TODO: Check for valid mCameraSourcePreview
        if (mCameraSource != null) {
            try {
                mCameraSourcePreview?.start(mCameraSource)
            } catch (e: IOException) {

                Timber.e("Unable to start camera source. $e")
                mCameraSource?.release()
                mCameraSource = null
                //callbackContext.error(QRReaderError.ERROR_CAMERA_FAILED_TO_START.name + " " + e.getMessage())
                return false
            }

        } else {
            Timber.e("No camera source to start.")
            //callbackContext.error(QRReaderError.ERROR_CAMERA_UNAVAILABLE.name)
            return false
        }
        return true
    }

    private fun startReadingWithPermission() {
        Timber.d("startReadingWithPermission()")

        singleThreadPool.execute {
            startReadingSync()
        }
    }

    @Synchronized
    private fun startReadingSync() {
        activity?.let {

            if (mCameraSourcePreview != null) {

                if (mCameraSource == null) {
                    if (!createCameraSource(it)) {
                        return
                    }
                } else {
                    Timber.d("mCamera source was not null")
                    return
                }

                try {
                    startCameraSource(it)
                } catch (e: SecurityException) {
                    Timber.e("Security Exception when starting camera source. ${e.message}")
                    return
                }

                mIsReading.set(true)
            }
        }
    }

    @Synchronized
    private fun stopReading() {
        Timber.d("stopReading()")
        val cameraSource: CameraSource? = mCameraSource
        if (cameraSource != null) {
            Timber.d("stopReading() stopping the camera.")
            try {
                cameraSource.stop()
            } catch (e: RuntimeException) {
                Timber.e("stopReading() exception when stopping the camera. $e")
            }

            // Keep the camera source so we can resume
            mCameraSource = null
            Timber.d("stopReading() set mCameraSource to null.")
            //Log.d(TAG, "onPause() finished stopping the camera.");

        } else {
            Timber.d("stopReading() did nothing because mCameraSource missing.")
        }
    }

    private fun navigateToSendWithText(view: View, text: String) {

        val navController: NavController? = findNavController(view)
        //val navController = myNavController
        navController?.let {
            val currentDestinationId: Int? = navController.currentDestination?.id
            if (currentDestinationId == R.id.scanFragment2) { // Prevent fast follower triggering navigation.
                Timber.d("navigatorName: ${it.currentDestination?.navigatorName}")

                val directions: NavDirections = ScanFragmentDirections.actionScanFragmentToSendFragment(text)

                activity?.runOnUiThread {
                    it.navigate(directions)
                }

            } else {
                Timber.d( "currentDestination ID was not scanFragment, it was ${currentDestinationId}")

                activity?.runOnUiThread {
                    val clipboard: ClipboardManager? =
                        activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    if (clipboard != null) {
                        val clip: ClipData = ClipData.newPlainText("toAddress", text)
                        clipboard.primaryClip = clip
                        showSnackbar("Address has been copied to clipboard. Tap the Back button. Navigation failed.")
                    } else {
                        Timber.e("Failed to get ClipboardManager.")
                    }
                }


            }
        }
    }

    private fun showSnackbar(message: String) {
        view?.let {
            Snackbar.make(it, message,
                Snackbar.LENGTH_LONG)
                .show();
        }

    }

    override fun onNavigationHide() {
        onPauseOrNavigationHide()
    }

    private fun onPauseOrNavigationHide() {
        Timber.d("onPauseOrNavigationHide()")
        // TODO: Release the camera here
        if (!mIsRequestingPermission) {
            stopReading()
        } else {
            Timber.d("onPause() requesting permission, so not messing with camera.")
        }
    }

    override fun onNavigationShow() {
        onResumeOrNavigationShow()
    }

    private fun onResumeOrNavigationShow() {
        Timber.d("onResumeOrNavigationShow")
        setUpActionBar()
        if (mIsReading.get()) {
            Timber.d("onResumeOrNavigationShow() trying to resume reading.")

            val cameraSource: CameraSource? = mCameraSource
            if (cameraSource == null) {
                startReadingWithPermission()
                Timber.d("onResumeOrNavigationShow() started reading.")
            } else {
                Timber.d("onResumeOrNavigationShow() skipped restart of reading because camera source was already valid.")
            }
        } else {
            Timber.d("onResumeOrNavigationShow() was idle.")
        }
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")

        (activity as? AppCompatActivity)?.supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")
            it.setTitle(R.string.title_scan)
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }
        setMenuVisibility(false)
    }

}
