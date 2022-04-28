package dk.itu.moapd.scootersharing.fragments

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dk.itu.moapd.scootersharing.R
import dk.itu.moapd.scootersharing.database.RideRepository
import dk.itu.moapd.scootersharing.database.ScooterRepository
import dk.itu.moapd.scootersharing.databinding.FragmentScooterDetailsBinding
import dk.itu.moapd.scootersharing.viewmodels.ScooterDetailsViewModel
import dk.itu.moapd.scootersharing.viewmodels.ScooterDetailsViewModelFactory

class ScooterDetailsFragment : Fragment() {

    private lateinit var binding: FragmentScooterDetailsBinding
    private lateinit var viewModel: ScooterDetailsViewModel

    private val args: ScooterDetailsFragmentArgs by navArgs()

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentScooterDetailsBinding.inflate(inflater, container, false)
        val view = binding.root

        val application = requireActivity().application
        val viewModelFactory =
            ScooterDetailsViewModelFactory(
                args.scooterId,
                ScooterRepository(application),
                RideRepository(application)
            )
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(ScooterDetailsViewModel::class.java)

        viewModel.getScooter().observe(viewLifecycleOwner) { scooter ->
            scooter?.let {
                binding.scooterNameText.text =
                    resources.getString(R.string.name) + " " + it.name
                binding.scooterLocationText.text =
                    resources.getString(R.string.location) + " " + it.location
                binding.scooterActiveText.text =
                    resources.getString(R.string.active) + " " + it.active

                if (it.imageUri.isNotEmpty()) {
                    val imageUri = Uri.parse(it.imageUri)
                    binding.scooterImage.setImageURI(imageUri)
                    binding.scooterImage.visibility = View.VISIBLE
                } else {
                    binding.scooterImage.visibility = View.GONE
                }
            }
        }
        viewModel.getCurrentRide().observe(viewLifecycleOwner) { ride ->
            if (ride != null) {
                binding.toggleActiveRideButton.text = resources.getString(R.string.end_ride)
            } else {
                binding.toggleActiveRideButton.text = resources.getString(R.string.start_ride)
            }
        }

        binding.toggleActiveRideButton.setOnClickListener {
            if (viewModel.isRideActive()) {
                dialog(R.string.end_ride_dialog, R.string.yes, R.string.cancel) {
                    viewModel.toggleActiveRide()
                }
            } else {
                dialog(R.string.start_ride_dialog, R.string.yes, R.string.cancel) {
                    viewModel.toggleActiveRide()
                }
            }
        }
        binding.editButton.setOnClickListener {
            findNavController().navigate(
                ScooterDetailsFragmentDirections.actionScooterDetailsFragmentToEditRideFragment(args.scooterId)
            )
        }
        binding.updatePictureButton.setOnClickListener {
            findNavController().navigate(
                ScooterDetailsFragmentDirections.actionScooterDetailsFragmentToCameraFragment(args.scooterId)
            )
        }
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        return view
    }

    private fun dialog(
        message: Int,
        positiveButton: Int,
        negativeButton: Int,
        positiveMethod: () -> Unit,
    ) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(message)
            .setPositiveButton(
                positiveButton
            ) { dialog, _ ->
                dialog.dismiss()
                positiveMethod()
            }
            .setNegativeButton(
                negativeButton
            ) { dialog, _ ->
                dialog.dismiss()
            }
        builder.create()
        builder.show()
    }
}
