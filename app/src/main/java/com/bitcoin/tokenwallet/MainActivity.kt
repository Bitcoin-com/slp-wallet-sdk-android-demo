package com.bitcoin.tokenwallet

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import com.bitcoin.tokenwallet.receive.ReceiveFragment
import com.bitcoin.tokenwallet.send.PermissionsReceiver
import com.bitcoin.tokenwallet.send.SendFragment
import com.bitcoin.slpwallet.SLPWalletConfig
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : AppCompatActivity() {

    val tabWalletWrapper: FrameLayout by lazy { tab_wallet_wrapper }
    val fragmentContainer: FrameLayout by lazy { fragment_container }
    val tabSendWrapper: FrameLayout by lazy { tab_send_wrapper }

    val navSendController: NavController by lazy { findNavController(this, R.id.tab_send) }
    val navSendFragment: Fragment by lazy { tab_send }

    val navWalletController: NavController by lazy { findNavController(this, R.id.tab_wallet) }
    val navWalletFragment: Fragment by lazy { tab_wallet }

    var currentNavController: NavController? = null

    var receiveFragment: ReceiveFragment? = null

    private val scheduledPool: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    private var scheduledFuture: ScheduledFuture<*>? = null
    private var tabWasSelectedSinceResuming: AtomicBoolean = AtomicBoolean(false)

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

        var fragment: Fragment? = null
        tabWasSelectedSinceResuming.set(true)


        when (item.itemId) {
            R.id.balancesFragment -> {

                currentNavController = navWalletController

                fragmentContainer.visibility = View.GONE

                tabSendWrapper.visibility = View.GONE
                hideNavFragment(navSendFragment)

                tabWalletWrapper.visibility = View.VISIBLE
                showNavFragment(navWalletFragment)

                setUpMainActionBar()

                return@OnNavigationItemSelectedListener true
            }
            R.id.receiveFragment -> {

                currentNavController = null

                tabWalletWrapper.visibility = View.GONE
                hideNavFragment(navWalletFragment)

                tabSendWrapper.visibility = View.GONE
                hideNavFragment(navWalletFragment)

                fragmentContainer.visibility = View.VISIBLE


                val receiveFragment: ReceiveFragment = ReceiveFragment.newInstance()
                this.receiveFragment = receiveFragment
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, receiveFragment).commit()

                return@OnNavigationItemSelectedListener true
            }
            R.id.sendFragment -> {
                //navigateToSendTab()
                currentNavController = navSendController

                tabWalletWrapper.visibility = View.GONE
                hideNavFragment(navWalletFragment)

                fragmentContainer.visibility = View.GONE

                tabSendWrapper.visibility = View.VISIBLE
                showNavFragment(navSendFragment)

                return@OnNavigationItemSelectedListener true
            }
        }

        false
    }

    private val onNavigationItemReselectedListener = BottomNavigationView.OnNavigationItemReselectedListener { _ ->
        // Just to prevent mOnNavigationItemSelectedListener getting called when reselected
        Timber.d("OnNavigationItemReselected()")

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        //val binding = MainActivity.inflate
        setContentView(R.layout.activity_main)

        Timber.d("onCreate()")


        val toolbar: Toolbar = findViewById<Toolbar>(R.id.my_toolbar)
        setSupportActionBar(toolbar)

        setUpMainActionBar()

        bottom_navigation.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)
        bottom_navigation.setOnNavigationItemReselectedListener(onNavigationItemReselectedListener)

        SLPWalletConfig.restAPIKey = "slpsdkH6aIcXEApC4wXQfqqPH"

        currentNavController = navWalletController

        tabWalletWrapper.visibility = View.VISIBLE
        showNavFragment(navWalletFragment)
    }

    override fun onStart() {
        super.onStart()
        hideNavFragment(navSendFragment)
    }


    override fun onResume() {
        super.onResume()
        Timber.d("onResume()")
        // Terrible hack because fragments for all tabs are active.
        scheduledFuture = scheduledPool.schedule({

            runOnUiThread {
                if (!tabWasSelectedSinceResuming.get()) {
                    Timber.d("Scheduled action bar setup.")
                    setUpActionBar()
                }
            }

        }, 100, TimeUnit.MILLISECONDS)
        tabWasSelectedSinceResuming.set(false)
    }

    override fun onPause() {
        scheduledFuture?.let {
            it.cancel(true)
        }
        super.onPause()
    }


    override fun onBackPressed() {
        Timber.d("onBackPressed()")
        val localNavController: NavController? = currentNavController

        if (localNavController != null) {
            Timber.d("onBackPressed() found navController")
            val userWasNavigatedToAnotherDestination: Boolean = localNavController.popBackStack()
            Timber.d("onBackPressed() userWasNavigatedToAnotherDestination: $userWasNavigatedToAnotherDestination")
            if (!userWasNavigatedToAnotherDestination) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }

    }


    override fun onSupportNavigateUp(): Boolean {
        Timber.d("onSupportNavigateUp()")

        val localNavController: NavController? = currentNavController

        if (localNavController != null) {
            Timber.d("onSupportNavigateUp() found navController")
            val userWasNavigatedToAnotherDestination: Boolean = localNavController.popBackStack()
            Timber.d("onSupportNavigateUp() userWasNavigatedToAnotherDestination: $userWasNavigatedToAnotherDestination")
            return userWasNavigatedToAnotherDestination
        } else {
            return false
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Timber.d("onRequestPermissionsResult()")
        if (requestCode == PermissionsReceiver.REQ_CAMERA) {
            val cameraPermissionIndex: Int = permissions.indexOf(Manifest.permission.CAMERA)

            if (cameraPermissionIndex >= 0) {
                val grantResult: Int = grantResults[cameraPermissionIndex]

                val fragments = supportFragmentManager.fragments
                Timber.d("Fragment count: ${fragments.size}")

                var receiver: PermissionsReceiver? = null
                for (fragment: Fragment in fragments) {
                    if (fragment is PermissionsReceiver) {
                        receiver = fragment
                    } else {

                        val childFragments = fragment.childFragmentManager.fragments
                        for (childFragment: Fragment in childFragments) {
                            if (childFragment is PermissionsReceiver) {
                                receiver = childFragment
                            }
                        }
                    }
                }

                if (receiver != null) {
                    Timber.d("Fragment permissions receiver found.")
                    receiver.onPermissionResult(grantResult)
                } else {
                    Timber.d("Fragment permissions receiver missing.")
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    private fun hideNavFragment(navFragment: Fragment) {
        val childCount: Int = navFragment.childFragmentManager.fragments.count()
        Timber.d("hideNavFragment() child count: ${childCount}")

        val fragmentOnTop: Fragment? = navFragment.childFragmentManager.fragments.lastOrNull()
        if (fragmentOnTop != null) {
            if (fragmentOnTop is MyNavSubGraphFragment) {
                Timber.d("hideNavFragment() IS a nav fragment.")
                fragmentOnTop.onNavigationHide()
            } else {
                Timber.d("hideNavFragment() NOT a nav fragment.")
            }
        }
        navFragment.view?.visibility = View.GONE
    }

    private fun showNavFragment(navFragment: Fragment) {
        val childCount: Int = navFragment.childFragmentManager.fragments.count()
        Timber.d("showNavFragment() child count: ${childCount}")

        val fragmentOnTop: Fragment? = navFragment.childFragmentManager.fragments.lastOrNull()
        if (fragmentOnTop != null) {
            if (fragmentOnTop is MyNavSubGraphFragment) {
                Timber.d("showNavFragment() IS a nav fragment.")
                fragmentOnTop.onNavigationShow()
            } else {
                Timber.d("showNavFragment() NOT a nav fragment.")
            }
        }
        navFragment.view?.visibility = View.VISIBLE
    }

    private fun setUpActionBar() {
        Timber.d("setUpActionBar()")
        val currentNav = currentNavController
        if (currentNav != null) {
            (receiveFragment as? MyNavSubGraphFragment)?.onNavigationHide()
            if (currentNav == navWalletController) {
                hideNavFragment(navSendFragment)
                showNavFragment(navWalletFragment)
            } else {
                hideNavFragment(navWalletFragment)
                showNavFragment(navSendFragment)
            }
        } else {
            hideNavFragment(navWalletFragment)
            hideNavFragment(navSendFragment)
            (receiveFragment as? MyNavSubGraphFragment)?.onNavigationShow()
        }
    }

    private fun setUpMainActionBar() {
        supportActionBar?.let {
            Timber.d("setUpActionBar() found ActionBar.")
            it.setTitle(R.string.title_wallet)

            // To see logo, need to display both home and logo
            it.setDisplayOptions(
                ActionBar.DISPLAY_SHOW_TITLE,
                ActionBar.DISPLAY_SHOW_TITLE or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_HOME_AS_UP
            )
        }
    }

    fun navigateToSendTab(tokenId: String) {
        bottom_navigation.selectedItemId = R.id.sendFragment

        navSendController.popBackStack(R.id.sendFragment2, false)

        val sendFragment: SendFragment? = navSendFragment.childFragmentManager.fragments[0] as? SendFragment
        if (sendFragment != null) {
            Timber.d("SendFragment found.")
            sendFragment.setSelectedToken(tokenId)
        } else {
            Timber.d("SendFragment missing.")
        }
    }
}