package com.nowtoapps.bluevpn

import android.app.Activity
import android.app.backup.BackupManager
import android.content.ClipboardManager
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Handler
import android.os.Parcelable
import android.util.Log
import android.util.LongSparseArray
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.preference.PreferenceDataStore
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.nowtoapps.bluevpn.aidl.IShadowsocksService
import com.nowtoapps.bluevpn.aidl.ShadowsocksConnection
import com.nowtoapps.bluevpn.aidl.TrafficStats
import com.nowtoapps.bluevpn.bg.BaseService
import com.nowtoapps.bluevpn.database.Profile
import com.nowtoapps.bluevpn.database.ProfileManager
import com.nowtoapps.bluevpn.net.HttpsTest
import com.nowtoapps.bluevpn.network.ApiManager
import com.nowtoapps.bluevpn.preference.DataStore
import com.nowtoapps.bluevpn.preference.OnPreferenceDataStoreChangeListener
import com.nowtoapps.bluevpn.utils.Action
import com.nowtoapps.bluevpn.utils.Key
import com.nowtoapps.bluevpn.widget.UndoSnackbarManager
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), ShadowsocksConnection.Callback, OnPreferenceDataStoreChangeListener {

    companion object {
        private const val TAG = "ShadowsocksMainActivity"
        private const val REQUEST_CONNECT = 1
        const val imagePath = "file:///android_asset/flags/%s.png"

        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    @Parcelize
    data class ProfilesArg(val profiles: List<Profile>) : Parcelable

    private val isEnabled get() = state.let { it.canStop || it == BaseService.State.Stopped }

    var state = BaseService.State.Idle
    private lateinit var tester : HttpsTest

    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) =
            changeState(state, msg, true)

    override fun trafficUpdated(profileId: Long, stats: TrafficStats) {
//        if (profileId == 0L) this@HomeActivity.stats.updateTraffic(
//                stats.txRate, stats.rxRate, stats.txTotal, stats.rxTotal)
        if (state != BaseService.State.Stopping) {
            (supportFragmentManager.findFragmentById(R.id.fragment_holder) as? ToolbarFragment)
                    ?.onTrafficUpdated(profileId, stats)
        }
    }

    override fun trafficPersisted(profileId: Long) {
        onTrafficPersisted(profileId)
    }

    fun onTrafficPersisted(profileId: Long) {
        statsCache.remove(profileId)
    }

    private fun changeState(state: BaseService.State, msg: String? = null, animate: Boolean = false) {
//        fab.changeState(state, this.state, animate)
//        stats.changeState(state)
//        if (msg != null) snackbar(getString(R.string.vpn_error, msg)).show()
        when (state) {
            BaseService.State.Connecting -> {
                tvStatus.visibility = View.GONE
                tvNotice.text = getString(R.string.connecting)
                btnPower.setImageResource(R.drawable.btn_connect)
            }
            BaseService.State.Connected -> {
                tester.testConnection()
            }
            BaseService.State.Stopping -> {
                tvStatus.visibility = View.GONE
                tvNotice.text = getString(R.string.stopping)
                btnPower.setImageResource(R.drawable.btn_disconnect)
            }
            BaseService.State.Stopped -> {
                tvStatus.visibility = View.GONE
                tvNotice.text = getString(R.string.tab_to_connect)
                btnPower.setImageResource(R.drawable.btn_connect)
            }
            else -> {
                tvStatus.visibility = View.GONE
                tvNotice.text = msg ?: ""
            }
        }
        this.state = state
        ProfilesFragment.instance?.profilesAdapter?.notifyDataSetChanged()  // refresh button enabled state
        MainActivity.stateListener?.invoke(state)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == 123 -> {
                if (resultCode == Activity.RESULT_OK) {
                    getCurrentSelectVPN()
                    if (state.canStop) Core.reloadService()
                }
            }
            requestCode != REQUEST_CONNECT -> super.onActivityResult(requestCode, resultCode, data)
            resultCode == Activity.RESULT_OK -> Core.startService()
            else -> {
                Crashlytics.log(Log.ERROR, TAG, "Failed to start VpnService from onActivityResult: $data")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        tester = ViewModelProviders.of(this).get()
        changeState(BaseService.State.Idle) // reset everything to init state
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        ProfileManager.ensureNotEmpty()
        fab.setOnClickListener { toggle() }
        cardCountry.setOnClickListener {
            val intent = Intent(this, ListCountryActivity::class.java)
            startActivityForResult(intent, 123)
        }
        getCurrentSelectVPN()
        getNewData()
        tester.status.observe(this, Observer { status ->
            when (status) {
                is HttpsTest.Status.Success -> {
                    tvStatus.text = getString(R.string.connected)
                    tvStatus.visibility = View.VISIBLE
                    tvNotice.text = "Tap button to disconnect"
                    btnPower.setImageResource(R.drawable.btn_disconnect)
                }
                is HttpsTest.Status.Error -> {
                    Core.stopService()
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onStop() {
        connection.bandwidthTimeout = 0
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        BackupManager(this).dataChanged()
        handler.removeCallbacksAndMessages(null)
    }

    private fun toggle() = when {
        state.canStop -> Core.stopService()
        DataStore.serviceMode == Key.modeVpn -> {
            val intent = VpnService.prepare(this)
            if (intent != null) startActivityForResult(intent, REQUEST_CONNECT)
            else onActivityResult(REQUEST_CONNECT, Activity.RESULT_OK, null)
        }
        else -> Core.startService()
    }

    private val handler = Handler()
    private val connection = ShadowsocksConnection(handler, true)

    override fun onServiceConnected(service: IShadowsocksService) = changeState(try {
        BaseService.State.values()[service.state]
    } catch (_: DeadObjectException) {
        BaseService.State.Idle
    })

    override fun onServiceDisconnected() = changeState(BaseService.State.Idle)

    override fun onBinderDied() {
        connection.disconnect(this)
        connection.connect(this, this)
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String?) {
        when (key) {
            Key.serviceMode -> handler.post {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    private lateinit var undoManager: UndoSnackbarManager<Profile>
    private val statsCache = LongSparseArray<TrafficStats>()

    private val clipboard by lazy { this@HomeActivity.getSystemService<ClipboardManager>()!! }

    private fun startConfig(profile: Profile) {
        profile.serialize()
        startActivity(Intent(this@HomeActivity, ProfileConfigActivity::class.java).putExtra(Action.EXTRA_PROFILE_ID, profile.id))
    }

    private fun getCurrentSelectVPN() {
        val profile = ProfileManager.getProfile(DataStore.profileId) ?: return
        tvCountryName.text = profile.countryName
        Glide.with(this)
                .load(String.format(imagePath, profile.countryCode.toLowerCase())).into(ivCountryFlag)
    }

    private fun getNewData() {
        val version = DataStore.privateStore.getInt(Key.version) ?: 0
        ApiManager.shared().getApiVersion { newVersion ->
            if (newVersion > version) {
                DataStore.privateStore.putInt(Key.version, newVersion)
                ApiManager.shared().getVPNData { response ->
                    loading.gone()
                    val listData = response?.data ?: return@getVPNData
                    if (listData.isNotEmpty()) {
                        ProfileManager.clear()
                        ProfileManager.save(listData)
                        val idSaved = DataStore.profileId
                        if (listData.firstOrNull { it.id == idSaved } == null) {
                            Core.switchProfile(listData.first().id)
                            getCurrentSelectVPN()
                        }
                    }
                }
            } else {
                loading.gone()
            }
        }
    }
}