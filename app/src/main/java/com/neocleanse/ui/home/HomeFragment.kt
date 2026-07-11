package com.neocleanse.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neocleanse.R
import com.neocleanse.databinding.FragmentHomeBinding
import com.neocleanse.ui.common.HapticHelper
import com.neocleanse.ui.common.PermissionManager
import com.neocleanse.util.Formatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var permissionManager: PermissionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupListeners()
        setupPermissions()

        binding.emptyRecentActivity.tvEmptyTitle.text = "No Recent Activity"
        binding.emptyRecentActivity.tvEmptyDesc.text = "Your recent scans and compressions will appear here."
        binding.emptyRecentActivity.ivEmptyIcon.setImageResource(R.drawable.ic_storage)
    }

    private fun setupPermissions() {
        permissionManager = PermissionManager(this) { granted ->
            if (granted) {
                observeData()
            } else {
                binding.tvUsedStorage.text = "-"
                binding.tvFreeStorage.text = "-"
                binding.tvTotalStorage.text = "-"
                binding.pbStorage.progress = 0
            }
        }

        if (permissionManager.hasStoragePermission()) {
            observeData()
        } else {
            permissionManager.requestStoragePermission()
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.storageStats.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.pbStorage.isIndeterminate = true
                        }
                        is UiState.Success -> {
                            binding.pbStorage.isIndeterminate = false
                            val stats = state.data
                            binding.tvUsedStorage.text = Formatter.formatSize(stats.usedBytes)
                            binding.tvFreeStorage.text = Formatter.formatSize(stats.freeBytes)
                            binding.tvTotalStorage.text = Formatter.formatSize(stats.totalBytes)

                            val percentage = if (stats.totalBytes > 0) {
                                (stats.usedBytes.toFloat() / stats.totalBytes.toFloat() * 100).toInt()
                            } else 0

                            binding.pbStorage.progress = percentage
                        }
                        is UiState.Error -> {
                            binding.pbStorage.isIndeterminate = false
                            // Handle error nicely
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnCompressImages.setOnClickListener {
            HapticHelper.performClick(it)
        }
        binding.btnScanDuplicates.setOnClickListener {
            HapticHelper.performClick(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
