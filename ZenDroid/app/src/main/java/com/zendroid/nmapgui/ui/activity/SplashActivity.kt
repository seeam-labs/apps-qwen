package com.zendroid.nmapgui.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.zendroid.nmapgui.R
import com.zendroid.nmapgui.databinding.ActivitySplashBinding
import com.zendroid.nmapgui.domain.detector.RootDetector

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashTimeOut: Long = 3000 // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAnimations()
        performChecks()
    }

    private fun setupAnimations() {
        try {
            val glitchAnim = AnimationUtils.loadAnimation(this, R.anim.glitch_anim)
            binding.splashLogo.startAnimation(glitchAnim)
            
            val fadeAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            binding.splashStatus.startAnimation(fadeAnim)
        } catch (e: Exception) {
            // Fallback if animations don't exist
        }
    }

    private fun performChecks() {
        Handler(Looper.getMainLooper()).postDelayed({
            // Check root status
            val rootDetector = RootDetector(this)
            val rootStatus = rootDetector.checkRoot()
            
            binding.splashStatus.text = if (rootStatus.isRooted) {
                "Root Access: DETECTED\n${rootStatus.rootMethod}"
            } else {
                "Root Access: NOT AVAILABLE\nStandard mode only"
            }

            binding.splashArch.text = "Architecture: ${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"}"

            // Navigate to main activity after delay
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 1500)

        }, 1500)
    }
}
