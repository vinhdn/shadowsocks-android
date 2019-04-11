package com.nowtoapps.bluevpn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nowtoapps.bluevpn.HomeActivity.Companion.imagePath
import com.nowtoapps.bluevpn.database.Profile
import com.nowtoapps.bluevpn.database.ProfileManager
import com.nowtoapps.bluevpn.preference.DataStore
import kotlinx.android.synthetic.main.activity_list_country.*
import kotlinx.android.synthetic.main.item_country.view.*
import java.lang.ref.WeakReference

class ListCountryActivity : AppCompatActivity() {

    var self = WeakReference(this)
    var listCountry = listOf<Profile>()
    private var failedLoadAdCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_country)
        title = "List Country"
        toolbar.title = "List Country"
        toolbar.setTitleTextColor(resources.getColor(R.color.colorPrimary))
        toolbar.setNavigationIcon(R.drawable.ic_navigation_close)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        listCountry = ProfileManager.getAllProfiles() ?: listOf()
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = Adapter()
    }

    override fun onDestroy() {
        self.clear()
        super.onDestroy()
    }

    inner class Adapter : RecyclerView.Adapter<Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(this@ListCountryActivity).inflate(R.layout.item_country, parent, false)
            return Holder(view)
        }

        override fun getItemCount(): Int {
            if (listCountry.isEmpty()) return 0
            return listCountry.size
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            if (listCountry.isEmpty() || holder.itemView.iv_country == null) {

            } else {
                holder.itemView.apply {
                    setOnClickListener {
                        Core.switchProfile(listCountry[position].id)
                        val intent = Intent()
                        setResult(Activity.RESULT_OK, intent)
                        finish()
                    }
                    iv_country?.setImageResource(0)
                    tv_country?.setAllCaps(true)
                    Glide.with(this)
                            .load(String.format(imagePath,  listCountry[position].countryCode.toLowerCase())).into(iv_country)
                    tv_country?.text = listCountry[position].countryName
                }
            }
        }

    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
