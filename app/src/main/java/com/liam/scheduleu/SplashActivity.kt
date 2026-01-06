package com.liam.scheduleu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var tvLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Map kode negara -> sapaan
    private val greetingMap = mapOf(
        "ID" to "Halo",
        "MY" to "Halo",
        "SG" to "Hello",
        "US" to "Hello",
        "GB" to "Hello",
        "CA" to "Hello",
        "AU" to "Hello",
        "NZ" to "Hello",
        "FR" to "Bonjour",
        "BE" to "Bonjour",
        "CH" to "Bonjour",
        "ES" to "Hola",
        "MX" to "Hola",
        "AR" to "Hola",
        "CO" to "Hola",
        "PE" to "Hola",
        "CL" to "Hola",
        "DE" to "Hallo",
        "AT" to "Hallo",
        "CH" to "Hallo",
        "PT" to "Olá",
        "BR" to "Olá",
        "IT" to "Ciao",
        "JP" to "こんにちは",
        "KR" to "안녕하세요",
        "CN" to "你好",
        "TW" to "你好"
    )

    private var detectedCountryCode: String? = null
    private var detectedGreeting: String = "Hello"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        tvLocation = findViewById(R.id.tvLocation)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // coba pakai lokasi GPS/network dulu; kalau gagal, pakai Locale default
        requestLocationThenShowCountry()

        // tetap pindah ke Home setelah 2 detik, bawa greeting + countryCode
        Handler(Looper.getMainLooper()).postDelayed({
            goToHome()
        }, 2000L)
    }

    private fun getGreetingForCountry(countryCode: String?): String {
        val code = countryCode?.uppercase(Locale.ROOT) ?: ""
        return greetingMap[code] ?: "Hello"
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("EXTRA_GREETING", detectedGreeting)
            putExtra("EXTRA_COUNTRY_CODE", detectedCountryCode ?: "")
        }
        startActivity(intent)
        finish()
    }

    private fun requestLocationThenShowCountry() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            getLastLocationAndSetCountry()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                getLastLocationAndSetCountry()
            } else {
                setCountryFromLocale()
            }
        }

    @Suppress("MissingPermission")
    private fun getLastLocationAndSetCountry() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val code = getCountryCodeFromLocation(location.latitude, location.longitude)
                    if (code != null) {
                        val flag = countryCodeToFlagEmoji(code)
                        detectedCountryCode = code
                        detectedGreeting = getGreetingForCountry(code)
                        tvLocation.text = "$code $flag"
                    } else {
                        setCountryFromLocale()
                    }
                } else {
                    setCountryFromLocale()
                }
            }
            .addOnFailureListener {
                setCountryFromLocale()
            }
    }

    @Suppress("DEPRECATION")
    private fun getCountryCodeFromLocation(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].countryCode
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun setCountryFromLocale() {
        val locale = Locale.getDefault()
        val code = locale.country.uppercase(Locale.ROOT)
        val flag = countryCodeToFlagEmoji(code)
        detectedCountryCode = code
        detectedGreeting = getGreetingForCountry(code)
        tvLocation.text = "$code $flag"
    }

    private fun countryCodeToFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return ""
        val upper = countryCode.uppercase(Locale.ROOT)
        val firstLetter = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) +
                String(Character.toChars(secondLetter))
    }
}
