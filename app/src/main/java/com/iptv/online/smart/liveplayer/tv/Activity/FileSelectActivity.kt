package com.iptv.online.smart.liveplayer.tv.Activity
import com.iptv.online.smart.liveplayer.tv.adsutils.populateNativeAdView

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ads.module.ads.ERainAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.funtion.AdCallback
import com.iptv.online.smart.liveplayer.tv.Model.AppDatabase
import com.iptv.online.smart.liveplayer.tv.R
import com.iptv.online.smart.liveplayer.tv.ReadFile.FileReader
import com.iptv.online.smart.liveplayer.tv.Ads.AdsManager
import com.iptv.online.smart.liveplayer.tv.adsutils.NativeAdUiState
import com.iptv.online.smart.liveplayer.tv.adsutils.RemoteConfigdata
import com.iptv.online.smart.liveplayer.tv.adsutils.isInternetAvailable
import com.iptv.online.smart.liveplayer.tv.databinding.ActivityFileSelectBinding
import com.iptv.online.smart.liveplayer.tv.utils.gone
import com.iptv.online.smart.liveplayer.tv.utils.visible

class FileSelectActivity : Base__Activity<ActivityFileSelectBinding>() {

    private lateinit var selectFileLauncher: ActivityResultLauncher<String>
    private var selectedFileUri: Uri? = null
    private var configScript: RemoteConfigdata? = null

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun setViewBinding(): ActivityFileSelectBinding {
        return ActivityFileSelectBinding.inflate(layoutInflater)
    }

    override fun bindObjects() {
        setupFilePicker()
        configScript = RemoteConfigdata(this@FileSelectActivity)

        nativeAds()
        AdsManager.loadInterAddPlaylist(this)
    }

    override fun bindListener() {
        // Back બટન
        binding.back.setOnClickListener { onBackPressed() }

        binding.btnFile.setOnClickListener {

            openFilePicker()

        }

        binding.btnAddPlaylist.setOnClickListener { validateAndAddPlaylist() }
    }

    private fun nativeAds() {
        configScript?.let {
            if (it.isNeedToShowADs) {
                if (configScript!!.nativePlaylist) {

                    val handledAds = mutableSetOf<String>()
                    val tag = "native_playlist"

                    AdsManager.getAdLive(tag).observe(this@FileSelectActivity) { state ->
                            if (state is NativeAdUiState.Success && !handledAds.contains(tag)) {
                                handledAds.add(tag) // mark as handled
                                Log.d(
                                    "AdManager121113",
                                    "[$tag] Success,🚀 showing ad is [${state.adsID}]"
                                )
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.visible
                                ERainAd.getInstance().populateNativeAdView(
                                    this@FileSelectActivity,
                                    state.ad,
                                    binding?.frAds,
                                    binding?.adShimmer?.root
                                )

                                //// Height cta
                                val remoteHeightDp = state.ctaHeight
                                val adBtn = binding.frAds.findViewById<View>(R.id.ad_call_to_action)

                                if (adBtn != null) {
                                    val density = resources.displayMetrics.density
                                    val heightInPx = (remoteHeightDp * density).toInt()

                                    val params = adBtn.layoutParams
                                    params?.let {
                                        it.height = heightInPx
                                        adBtn.layoutParams = it
                                        adBtn.requestLayout()
                                        Log.d(
                                            "AdManager123",
                                            "Height set to: $heightInPx px for tag: $tag"
                                        )
                                    }
                                } else {
                                    Log.e(
                                        "AdManager123",
                                        "CTA Button not found in layout for tag: $tag"
                                    )
                                }
                                //

                            } else if (state is NativeAdUiState.Loading) {
                                binding?.adShimmer?.root?.visible
                                binding?.frAds?.visible

                            } else if (state is NativeAdUiState.Failed || state is NativeAdUiState.Empty) {
                                binding?.adShimmer?.root?.gone
                                binding?.frAds?.gone
                            }

                    }

                } else {
                    binding?.adShimmer?.root?.gone
                    binding?.frAds?.gone
                }

            } else {
                binding?.adShimmer?.root?.gone
                binding?.frAds?.gone
            }
        }
    }

    override fun bindMethod() {
    }

    private fun validateAndAddPlaylist() {
        val playlistName = binding.etPlaylistName.text.toString().trim()
        val playlistUrl = binding.etUrl.text.toString().trim()

        if (TextUtils.isEmpty(playlistName)) {
            Toast.makeText(this, getString(R.string.please_enter_playlist_name), Toast.LENGTH_SHORT)
                .show()
            return
        }
        Toast.makeText(this, getString(R.string.importing_playlist_please_wait), Toast.LENGTH_LONG)
            .show()
        if (TextUtils.isEmpty(playlistUrl)) {
            Toast.makeText(
                this,
                getString(R.string.please_select_a_file_or_enter_url),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (playlistUrl.startsWith("http")) {
            selectedFileUri = null
        }

        if (!playlistUrl.lowercase().endsWith(".m3u") &&
            !playlistUrl.lowercase().endsWith(".m3u8") &&
            !playlistUrl.contains("m3u")
        ) {
            Toast.makeText(
                this,
                getString(R.string.invalid_file_type_only_m3u_is_supported),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isPlaylistAlreadyExist(playlistName, playlistUrl)) {
            Toast.makeText(
                this,
                getString(R.string.this_playlist_name_or_file_is_already_added),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val reader = if (selectedFileUri != null) {
            FileReader(this, selectedFileUri!!, playlistName)
        } else {
            FileReader(this, Uri.parse(playlistUrl), playlistName)
        }

        reader.setOnFileReadListener(object : FileReader.OnFileReadListener {
            override fun onFinish(playlistName: String?, playlistUrl: String?) {
                MainActivity.triggerFromPlaylist = true
                AdsManager.showInterAddPlaylist(this@FileSelectActivity) {
                    Toast.makeText(
                        this@FileSelectActivity,
                        getString(R.string.playlist_added_successfully),
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@FileSelectActivity, MainActivity::class.java).apply {
                        putExtra("playlist_name", playlistName)
                        putExtra("playlist_url", playlistUrl)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(intent)
                    finish()
                }


            }


        })
        reader.readFile()

    }

    // Pehla aakhi channel list (hajaro rows) memory ma laavi ne loop marto hato -
    // e main thread par bov var lagadto. Have SQLite ne j puchhi laie chhie, je
    // pehli match male tya j atki jaay chhe.
    private fun isPlaylistAlreadyExist(name: String, url: String): Boolean {
        return AppDatabase.getInstance(this).historyDao().playlistExists(name, url)
    }


    private fun setupFilePicker() {
        selectFileLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val fileName = getFileNameFromUri(it)
                    val fileUriString = it.toString()

                    if (fileName != null && (fileName.lowercase()
                            .endsWith(".m3u") || fileName.lowercase().endsWith(".m3u8"))
                    ) {
                        if (isPlaylistAlreadyExist(fileName, fileUriString)) {
                            selectedFileUri = null
                            binding.etPlaylistName.setText("")
                            binding.etUrl.setText("")
                            Toast.makeText(
                                this,
                                getString(R.string.this_file_is_already_added),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            selectedFileUri = it
                            binding.etPlaylistName.setText(fileName)
                            binding.etUrl.setText(fileUriString)
                            Toast.makeText(
                                this,
                                "${getString(R.string.file_selected)}: $fileName",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.not_support_this_type_of_file_please_select_an_m3u_file),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                // Aમુક file-picker media URI (content://media/...) query karva
                // READ_EXTERNAL_STORAGE permission mange che; na hoy to SecurityException
                // aave. Etle safe try-catch — fail thay to niche URI path thi naam lai le.
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) result = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) result = result?.substring(cut + 1)
        }
        return result
    }

    private fun openFilePicker() {
        selectFileLauncher.launch("*/*")
    }


}
