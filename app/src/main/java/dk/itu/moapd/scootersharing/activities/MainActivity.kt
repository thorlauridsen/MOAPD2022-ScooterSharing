package dk.itu.moapd.scootersharing.activities

import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dk.itu.moapd.scootersharing.R
import dk.itu.moapd.scootersharing.database.AppDatabase
import dk.itu.moapd.scootersharing.database.ScooterRepository
import dk.itu.moapd.scootersharing.database.UserRepository
import dk.itu.moapd.scootersharing.databinding.ActivityMainBinding
import dk.itu.moapd.scootersharing.models.Scooter
import dk.itu.moapd.scootersharing.models.User
import dk.itu.moapd.scootersharing.utils.AirplaneModeChangeReceiver
import dk.itu.moapd.scootersharing.utils.UserContentProvider
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var scooterRepository: ScooterRepository
    private lateinit var receiver: AirplaneModeChangeReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        receiver = AirplaneModeChangeReceiver()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomAppBar.menu.add(1, R.id.scooterListFragment, 1, "List")
            .setIcon(R.drawable.ic_baseline_list_alt_24)
        binding.bottomAppBar.menu.add(1, R.id.mapsFragment, 2, "Maps")
            .setIcon(R.drawable.ic_baseline_map_24)
        binding.bottomAppBar.menu.add(1, R.id.menuFragment, 3, "Menu")
            .setIcon(R.drawable.ic_baseline_menu_open_24)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?

        NavigationUI.setupWithNavController(
            binding.bottomAppBar,
            navHostFragment!!.navController
        )
        val scooterDao = AppDatabase.getDatabase(application).scooterDao()
        scooterRepository = ScooterRepository(scooterDao)
        seedDefaultBikes()

        IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED).also {
            registerReceiver(receiver, it)
        }
    }

    private fun seedDefaultBikes() {
        val scooterDao = AppDatabase.getDatabase(application).scooterDao()
        val scooterRepository = ScooterRepository(scooterDao)
        val scooters = scooterRepository.getAll()

        scooters.observe(this) {
            if (it.isEmpty()) {

                addScooter(
                    1,
                    "Fields",
                    55.629526888870515,
                    12.579146202544704
                )
                addScooter(
                    2,
                    "Bella Center",
                    55.63787677915786,
                    12.582772549132839
                )
                addScooter(
                    3,
                    "Ørestad Bibliotek",
                    55.63124243188086,
                    12.580912113189697
                )
                addScooter(
                    4,
                    "CF Møllers Allé",
                    55.63565136426479,
                    12.57451134536697
                )
                addScooter(
                    5,
                    "Martha Christensens vej",
                    55.639351440372124,
                    12.577665623168484
                )
                addScooter(
                    6,
                    "Royal Golf Center",
                    55.63842177800499,
                    12.570842083434597
                )
                addScooter(
                    7,
                    "Byparken",
                    55.633661865368246,
                    12.577601250152126
                )
                addScooter(
                    8,
                    "Irma",
                    55.631542125301195,
                    12.57502632949783
                )
                addScooter(
                    9,
                    "Amagerkollegiet",
                    55.63379510234193,
                    12.584403332213894
                )
                addScooter(
                    10,
                    "Amager",
                    55.634957878517426,
                    12.587171371917263
                )
            }
        }
    }

    private fun addScooter(
        id: Int,
        location: String,
        latitude: Double,
        longitude: Double,
    ) {
        lifecycleScope.launch {
            scooterRepository.insert(
                Scooter(
                    id = id,
                    name = "Scooter $id",
                    location = location,
                    timestamp = randomDate(),
                    active = false,
                    locked = true,
                    latitude = latitude,
                    longitude = longitude,
                    batteryLevel = 800.0
                )
            )
        }
    }

    private fun randomDate(): Long {
        val random = Random()
        val now = System.currentTimeMillis()
        val year = random.nextDouble() * 1000 * 60 * 60 * 24 * 365
        return (now - year).toLong()
    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser == null) {
            startLoginActivity()
        } else {
            auth.currentUser?.uid?.let { uid ->

                val userDao = AppDatabase.getDatabase(application).userDao()
                val userRepository = UserRepository(userDao)
                val user = userRepository.findByUid(uid)

                user.observe(this) {
                    if (it == null) {
                        lifecycleScope.launch {
                            val newUser = User(
                                0,
                                uid,
                                auth.currentUser?.displayName ?: "Chuck",
                                auth.currentUser?.email ?: "chuck@gmail.com"
                            )
                            userRepository.insert(newUser)
                            val url =
                                "https://scootersharing-2022-default-rtdb.europe-west1.firebasedatabase.app/"
                            val database = Firebase.database(url).reference

                            database.child("users").child(newUser.uid).child("name")
                                .setValue(newUser.name)
                            database.child("users").child(newUser.uid).child("email")
                                .setValue(newUser.email)
                            database.child("users").child(newUser.uid).child("balance")
                                .setValue(newUser.balance)

                            Log.e("Debug", "Saved user with uid: $uid")

                            val values = ContentValues()
                            values.put(UserContentProvider.name, newUser.name)
                            contentResolver.insert(UserContentProvider.CONTENT_URI, values)

                            Log.e("Debug", "Saved user name: ${newUser.name} using ContentProvider")
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
