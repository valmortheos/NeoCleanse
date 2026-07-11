package com.neocleanse.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neocleanse.data.local.datastore.SettingsManager
import com.neocleanse.databinding.FragmentSettingsBinding
import com.neocleanse.ui.common.HapticHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var settingsManager: SettingsManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        observeSettings()
        setupListeners()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            settingsManager.dynamicColorFlow.collectLatest { enabled ->
                binding.switchDynamicColor.isChecked = enabled
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            settingsManager.compressionPresetFlow.collectLatest { quality ->
                binding.tvCompressionPresetDesc.text = "$quality%"
            }
        }
    }

    private fun setupListeners() {
        binding.switchDynamicColor.setOnCheckedChangeListener { _, isChecked ->
            HapticHelper.performClick(binding.switchDynamicColor)
            viewLifecycleOwner.lifecycleScope.launch {
                settingsManager.setDynamicColor(isChecked)
            }
        }

        binding.btnCompressionPreset.setOnClickListener {
            HapticHelper.performClick(it)
            showPresetDialog()
        }
    }

    private fun showPresetDialog() {
        val options = arrayOf("100%", "75%", "50%", "25%")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Compression Preset")
            .setItems(options) { dialog, which ->
                val preset = when (which) {
                    0 -> 100
                    1 -> 75
                    2 -> 50
                    3 -> 25
                    else -> 75
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    settingsManager.setCompressionPreset(preset)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
