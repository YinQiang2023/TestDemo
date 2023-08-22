package com.smartwear.publicwatch.ui.debug

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.widget.Button
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.*
import com.smartwear.publicwatch.R
import com.smartwear.publicwatch.base.BaseActivity
import com.smartwear.publicwatch.databinding.ActivityDebugLogDecodingBinding
import com.smartwear.publicwatch.databinding.DialogDebugBinListBinding
import com.smartwear.publicwatch.dialog.DialogUtils
import com.smartwear.publicwatch.dialog.customdialog.CustomDialog
import com.smartwear.publicwatch.dialog.customdialog.MyDialog
import com.smartwear.publicwatch.utils.AESUtils
import com.smartwear.publicwatch.utils.AppUtils
import com.smartwear.publicwatch.utils.SaveLog
import com.smartwear.publicwatch.viewmodel.DeviceModel
import kotlinx.coroutines.*
import java.io.File
import java.io.FileFilter
import kotlin.coroutines.resume

/**
 * Created by Android on 2023/3/25.
 */
class DebugLogDecodingActivity : BaseActivity<ActivityDebugLogDecodingBinding, DeviceModel>(
    ActivityDebugLogDecodingBinding::inflate, DeviceModel::class.java
), View.OnClickListener {

    private val mFilePath = PathUtils.getExternalAppFilesPath() + "/decoding"
    private lateinit var selectFile: File
    val loadDialog: Dialog by lazy { DialogUtils.showLoad(this) }

    override fun setTitleId() = binding.title.root.id

    override fun initView() {
        super.initView()
        setTvTitle("意见反馈日志解码")
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnFile.id -> {
                if (!checkPermissions()) return
                val files = FileUtils.listFilesInDirWithFilter(mFilePath, object : FileFilter {
                    override fun accept(pathname: File?): Boolean {
                        if (pathname != null) {
                            //取文件夹内所有bin,zip
                            if (pathname.absolutePath.endsWith("bin") || pathname.absolutePath.endsWith("zip")) {
                                return true
                            }
                        }
                        return false
                    }
                })
                if (files.isNullOrEmpty()) {
                    ToastUtils.showShort("$mFilePath 目录下有效文件为空！")
                    return
                }
                showListDialog(files)
            }
            binding.btnDecoding.id -> {
                if (!::selectFile.isInitialized) {
                    ToastUtils.showShort("未选择日志文件")
                    return
                }
                decodingFile(selectFile)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        if (!PermissionUtils.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionUtils.permission(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .callback(object : PermissionUtils.FullCallback {
                    override fun onGranted(
                        granted: MutableList<String>
                    ) = LogUtils.d("申请权限同意$granted")

                    override fun onDenied(deniedForever: MutableList<String>, denied: MutableList<String>) {
                        LogUtils.d("申请权限永远拒绝 = $deniedForever,\n拒绝 = $denied")
                    }
                })
                .request()
            return false
        }
        return true
    }

    private lateinit var dialogBinding: DialogDebugBinListBinding
    private var dialog: MyDialog? = null
    private fun showListDialog(files: List<File>) {
        dialogBinding = DialogDebugBinListBinding.inflate(layoutInflater)
        dialog = CustomDialog.builder(this)
            .setContentView(dialogBinding.root)
            .setWidth(AppUtils.getScreenWidth())
            .setHeight((AppUtils.getScreenHeight() * 0.8f).toInt())
            .setGravity(Gravity.CENTER)
            .build()
        for (file in files) {
            val view = Button(this)
            view.isAllCaps = false
            view.text = file.name
            view.setTextColor(ContextCompat.getColor(this, R.color.app_index_color))

            view.setOnClickListener {
                binding.tvName.text = "文件名： ${file.name}"
                selectFile = file
                dialog?.dismiss()
            }
            dialogBinding.listLayout.addView(view)
        }
        dialog?.show()
    }


    @SuppressLint("SetTextI18n")
    private fun decodingFile(file: File) {
        viewModel.launchUI {
            loadDialog.show()
            loadDialog.setCancelable(false)
            if (file.absolutePath.endsWith("zip")) {
                val unZipDirPath = PathUtils.getExternalAppFilesPath() + "/decoding/data/" + FileUtils.getFileNameNoExtension(file)
                FileUtils.delete(unZipDirPath)
                FileUtils.createOrExistsDir(unZipDirPath)
                ZipUtils.unzipFile(file, FileUtils.getFileByPath(unZipDirPath))
                val files = FileUtils.listFilesInDir(unZipDirPath)
                val ds = mutableListOf<Deferred<String?>>()
                for (f in files) {
                    ds.add(async { decodingData(f) })
                }
                val logPath = StringBuilder()
                for (d in ds) {
                    logPath.append(d.await()).append("\n")
                }
                binding.tvResult.text = "解码日志路径：${logPath}"
            } else if (file.absolutePath.endsWith("bin")) {
                val d = async { decodingData(file) }
                binding.tvResult.text = "解码日志路径：${d.await()}"
            }
            if (loadDialog.isShowing) loadDialog.dismiss()
        }
    }

    private suspend fun decodingData(file: File): String? {
        return withContext(Dispatchers.IO) {
            return@withContext withTimeoutOrNull(300 * 1000) {
                suspendCancellableCoroutine {
                    try {
                        val newFilePath = FileUtils.getDirName(file) + "data/" + FileUtils.getFileNameNoExtension(file) + "_data" + ".txt"
                        FileUtils.createFileByDeleteOldFile(newFilePath)
                        val newFile = FileUtils.getFileByPath(newFilePath)
                        val context = FileIOUtils.readFile2String(file)
                        val cs = context.replace("\n", "").replace("\r", "").split("#").toMutableList()
                        for (log in cs) {
                            val newLog = StringBuffer(AESUtils.decrypt(log, com.smartwear.publicwatch.utils.JsonUtils.serviceKey)).append("\n")
                            SaveLog.writeFileFromString(newFile, newLog.toString(), true)
                        }
                        it.resume(newFile.absolutePath)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        it.resume("")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}