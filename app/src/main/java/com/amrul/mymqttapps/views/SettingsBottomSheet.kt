package com.amrul.mymqttapps.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.amrul.mymqttapps.Constants
import com.amrul.mymqttapps.OnSettingsSaveListener
import com.amrul.mymqttapps.R
import com.amrul.mymqttapps.databinding.FragmentSettingsBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentSettingsBottomSheetBinding? = null
    private val binding get() = _binding!!
    var onSettingsSaveListener: OnSettingsSaveListener? = null

    private var serverUrl: String? = null
    private var clientId: String? = null
    private var publishTopic: String? = null

    companion object {
        fun newInstance(serverUrl: String, clientId: String, publishTopic: String): SettingsBottomSheet {
            val fragment = SettingsBottomSheet()
            val bundle = Bundle().apply {
                putString(Constants.SERVER_URL, serverUrl)
                putString(Constants.CLIENT_ID, clientId)
                putString(Constants.PUBLISH_TOPIC, publishTopic)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            serverUrl = it.getString(Constants.SERVER_URL)
            clientId = it.getString(Constants.CLIENT_ID)
            publishTopic = it.getString(Constants.PUBLISH_TOPIC)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            etServerUrl.setText(serverUrl)
            etClientId.setText(clientId)
            etPublishTopic.setText(publishTopic)

            btnSave.setOnClickListener {
                val serverUrl = etServerUrl.text.toString()
                val clientId = etClientId.text.toString()
                val publishTopic = etPublishTopic.text.toString()

                onSettingsSaveListener?.onSettingsSave(serverUrl, clientId, publishTopic)

                dismiss()
            }
        }

        // Dapatkan BottomSheetBehavior
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet!!)

        // Atur tinggi awal (peekHeight) dan pastikan bisa mencapai fullscreen
        behavior.peekHeight = 900 // Tinggi awal saat collapsed
        behavior.isFitToContents = true
        behavior.isDraggable = true

        // Agar memungkinkan layar penuh
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheet.requestLayout()

        // Tambahkan listener untuk memantau perubahan state
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // Fullscreen state
                        bottomSheet.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.background_flat_top
                        )
                    }

                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        // Collapsed state
                        bottomSheet.background = ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.background_rounded_top
                        )
                    }

                    else -> {
                        // Handle other states if needed
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Lakukan sesuatu saat user menarik bottom sheet
//                bottomSheet.alpha = 1f - (0.2f * (1 - slideOffset))
            }
        })
    }

    override fun getTheme(): Int {
        return R.style.DraggableFullScreenBottomSheet
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }
}