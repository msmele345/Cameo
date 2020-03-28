package com.mitchmele.cameo

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mitchmele.cameo.ui.CameoFragment

class CameoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cameo)

        val isFragmentContainerEmpty: Boolean = savedInstanceState == null
        if(isFragmentContainerEmpty) { //need to check if a fragment is already hosted by the main activity
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, CameoFragment.newInstance())
                .commit()
        }

    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, CameoActivity::class.java)
        }
    }
}


//extend app to check for version and create channel
//create companion in base activity to create new new intent with activity
//when there are new photos, use NotificationCompat builder to build the notification in app drawer