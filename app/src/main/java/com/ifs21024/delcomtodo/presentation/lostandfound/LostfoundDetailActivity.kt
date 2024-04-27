package com.ifs21024.lostandfound.presentation.lostfound

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ifs21024.delcomtodo.R
import com.ifs21024.delcomtodo.data.local.entity.DelcomLostFoundEntity
import com.ifs21024.delcomtodo.data.remote.MyResult
import com.ifs21024.delcomtodo.presentation.ViewModelFactory
import com.ifs21024.delcomtodo.data.remote.response.LostFoundResponse
import com.ifs21024.delcomtodo.databinding.ActivityLostfoundDetailBinding
import com.ifs21024.delcomtodo.helper.Utils.Companion.observeOnce
import com.ifs21024.delcomtodo.presentation.lostandfound.LostfoundManageActivity
import com.ifs21024.delcomtodo.presentation.lostandfound.LostfoundViewModel
import java.io.File

class LostFoundDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLostfoundDetailBinding
    private val viewModel by viewModels<LostfoundViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private var isChanged: Boolean = false
    private var isFavorite: Boolean = false
    private var delcomLostFound: DelcomLostFoundEntity? = null

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == LostfoundManageActivity.RESULT_CODE) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLostfoundDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupAction()
    }

    private fun setupView() {
        showComponent(false)
        showLoading(false)
    }

    private fun setupAction() {
        val lostfoundId = intent.getIntExtra(KEY_LOST_FOUND_ID, 0)
        if (lostfoundId == 0) {
            finish()
            return
        }

        observeGetLostFound(lostfoundId)

        binding.appbarTodoDetail.setNavigationOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra(KEY_IS_CHANGED, true)
            setResult(RESULT_CODE, resultIntent)
            finishAfterTransition()
        }
    }

    private fun observeGetLostFound(lostfoundId: Int) {
        viewModel.getLostfound(lostfoundId).observeOnce { result ->
            when (result) {
                is MyResult.Loading -> {
                    showLoading(true)
                }

                is MyResult.Success -> {
                    showLoading(false)
                    loadLostFound(result.data.data.lostFound)
                }

                is MyResult.Error -> {
                    Toast.makeText(
                        this@LostFoundDetailActivity,
                        result.error,
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                    finishAfterTransition()
                }
            }
        }
    }

    private fun loadLostFound(lostfound: LostFoundResponse) {
        if (lostfound != null) {
            showComponent(true)

            binding.apply {
                tvLFDetailTitle.text = lostfound.title
                tvLFDetailDate.text = "Dibuat pada: ${lostfound.createdAt}"
                tvLFDetailDesc.text = lostfound.description
//            tvLostFoundDetailStatus.text = lostfound.status

                viewModel.getLocalLostFound(lostfound.id).observeOnce {
                    if(it != null){
                        delcomLostFound = it
                        setFavorite(true)
                    }else{
                        setFavorite(false)
                    }
                }

                cbLFDetailIsCompleted.isChecked = lostfound.isCompleted == 1

                val statusText = if (lostfound.status.equals("found", ignoreCase = true)) {
                    highlightText("Found", Color.GREEN)
                } else {
                    highlightText("Lost", Color.RED)
                }
                // Menetapkan teks status yang sudah disorot ke TextView
                tvLFDetailStatus.text = statusText

                cbLFDetailIsCompleted.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.putLostfound(
                        lostfound.id,
                        lostfound.title,
                        lostfound.description,
                        lostfound.status,
                        isChecked
                    ).observeOnce {
                        when (it) {
                            is MyResult.Error -> {
                                if (isChecked) {
                                    Toast.makeText(
                                        this@LostFoundDetailActivity,
                                        "Gagal menyelesaikan data lost and found:  + ${lostfound.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@LostFoundDetailActivity,
                                        "Gagal batal menyelesaikan data lost and found: " + lostfound.title,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            is MyResult.Success -> {
                                if (isChecked) {
                                    Toast.makeText(
                                        this@LostFoundDetailActivity,
                                        "Berhasil menyelesaikan data lost and found: " + lostfound.title,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@LostFoundDetailActivity,
                                        "Berhasil batal menyelesaikan data lost and found: " + lostfound.title,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                if ((lostfound.isCompleted == 1) != isChecked) {
                                    isChanged = true
                                }
                            }

                            else -> {}
                        }
                    }
                }

                ivLostFoundDetailActionFavorite.setOnClickListener {
                    if(isFavorite){
                        setFavorite(false)
                        if(delcomLostFound != null){
                            viewModel.deleteLocalLostFound(delcomLostFound!!)
                        }
                        Toast.makeText(
                            this@LostFoundDetailActivity,
                            "LostFound berhasil dihapus dari daftar favorite",
                            Toast.LENGTH_SHORT
                        ).show()
                    }else{
                        delcomLostFound = DelcomLostFoundEntity(
                            id = lostfound.id,
                            title = lostfound.title,
                            description = lostfound.description,
                            isCompleted = lostfound.isCompleted,
                            cover = lostfound.cover,
                            createdAt = lostfound.createdAt,
                            updatedAt = lostfound.updatedAt,
                            status = "", // Anda perlu memberikan nilai default untuk status
                            userId = 0 // Anda perlu memberikan nilai default untuk userId
                        )

                        setFavorite(true)
                        viewModel.insertLocalLostFound(delcomLostFound!!)
                        Toast.makeText(
                            this@LostFoundDetailActivity,
                            "LostFound berhasil ditambahkan ke daftar favorite",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                ivLFDetailActionDelete.setOnClickListener {
                    val builder = AlertDialog.Builder(this@LostFoundDetailActivity)

                    builder.setTitle("Konfirmasi Hapus Barang")
                        .setMessage("Anda yakin ingin menghapus barang ini?")

                    builder.setPositiveButton("Ya") { _, _ ->
                        observeDeleteLostFound(lostfound.id)
                    }

                    builder.setNegativeButton("Tidak") { dialog, _ ->
                        dialog.dismiss()
                    }

                    val dialog = builder.create()
                    dialog.show()
                }

                ivLFDetailActionEdit.setOnClickListener {
                    val delcomLostFound = DelcomLostFound(
                        lostfound.id,
                        lostfound.title,
                        lostfound.description,
                        lostfound.status,
                        lostfound.isCompleted == 1,
                        lostfound.cover
                    )

                    val intent = Intent(
                        this@LostFoundDetailActivity,
                        LostfoundManageActivity::class.java
                    )
                    intent.putExtra(LostfoundManageActivity.KEY_IS_ADD, false)
                    intent.putExtra(LostfoundManageActivity.KEY_LOST, delcomLostFound)
                    launcher.launch(intent)
                }
            }
        } else {
            Toast.makeText(
                this@LostFoundDetailActivity,
                "Tidak ditemukan item yang dicari",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setFavorite(status: Boolean){
        isFavorite = status
        if(status){
            binding.ivLostFoundDetailActionFavorite
                .setImageResource(R.drawable.ic_favorite_24)
        }else{
            binding.ivLostFoundDetailActionFavorite
                .setImageResource(R.drawable.ic_favorite_border_24)
        }
    }

    private fun highlightText(text: String, color: Int): SpannableString {
        val spannableString = SpannableString(text)
        spannableString.setSpan(ForegroundColorSpan(color), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannableString
    }
    private fun observeDeleteLostFound(lostfoundId: Int) {
        showComponent(false)
        showLoading(true)
        viewModel.deleteLostFound(lostfoundId).observeOnce { result ->
            result?.let {
                when (it) {
                    is MyResult.Error -> {
                        showComponent(true)
                        showLoading(false)
                        Toast.makeText(
                            this@LostFoundDetailActivity,
                            "Gagal menghapus barang: ${it.error}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is MyResult.Success -> {
                        showLoading(false)

                        Toast.makeText(
                            this@LostFoundDetailActivity,
                            "Berhasil menghapus barang",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.getLocalLostFound(lostfoundId).observeOnce {
                            if(it != null){
                                viewModel.deleteLocalLostFound(it)
                            }
                        }
                        val resultIntent = Intent()
                        resultIntent.putExtra(KEY_IS_CHANGED, true)
                        setResult(RESULT_CODE, resultIntent)
                        finishAfterTransition()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.pbLostFoundDetail.visibility =
            if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showComponent(status: Boolean) {
        binding.llLostFoundDetail.visibility =
            if (status) View.VISIBLE else View.GONE
    }

    companion object {
        const val KEY_LOST_FOUND_ID = "data_lost_found_id"
        const val KEY_IS_CHANGED = "is_changed"
        const val RESULT_CODE = 1001
    }
}
