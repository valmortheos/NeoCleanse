package com.neocleanse.ui.storage

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.neocleanse.databinding.FragmentStorageBinding
import com.neocleanse.ui.common.PermissionManager
import com.neocleanse.ui.home.UiState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StorageFragment : Fragment() {

    private var _binding: FragmentStorageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StorageViewModel by viewModels()
    private lateinit var categoryAdapter: StorageCategoryAdapter
    private lateinit var permissionManager: PermissionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInsets()
        setupRecyclerView()
        setupPermissions()
    }

    private fun setupPermissions() {
        permissionManager = PermissionManager(this) { granted ->
            if (granted) {
                observeData()
            }
        }

        if (permissionManager.hasStoragePermission()) {
            observeData()
        } else {
            permissionManager.requestStoragePermission()
        }
    }

    private fun setupRecyclerView() {
        categoryAdapter = StorageCategoryAdapter()
        binding.rvCategories.apply {
            adapter = categoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
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
                launch {
                    viewModel.storageStats.collect { state ->
                        if (state is UiState.Success) {
                            categoryAdapter.setTotalUsedBytes(state.data.usedBytes)
                        }
                    }
                }

                launch {
                    viewModel.categoryStats.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.loadingState.root.visibility = View.VISIBLE
                                binding.rvCategories.visibility = View.GONE
                            }
                            is UiState.Success -> {
                                binding.loadingState.root.visibility = View.GONE
                                binding.rvCategories.visibility = View.VISIBLE
                                categoryAdapter.submitList(state.data)
                            }
                            is UiState.Error -> {
                                binding.loadingState.root.visibility = View.GONE
                                // Handle error
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
