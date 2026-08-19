package com.example.filesystemv2

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filesystemv2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val LOGTAG = "filesystemv2.log"

    private lateinit var binding : ActivityMainBinding

    private lateinit var storageActivityResultLauncher : ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(LOGTAG, "MainActivity onCreate")

        //enableEdgeToEdge() // ХЗ что это такое и зачем

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // без этой штуки одни элементы разметки (Title Bar и Action Bar) перекрываются с другими (кнопки и др. элементы активности)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.getStoragePermissionsBtn.setOnClickListener {
            checkStoragePermission()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            Log.d(LOGTAG,"Build.VERSION_CODES.R: "+Build.VERSION_CODES.R+", новая ОС (Android 11+)")
        }else{
            Log.d(LOGTAG,"Build.VERSION_CODES.R: "+Build.VERSION_CODES.R+", старая ОС (Android 10 и ниже)")
        }

        // взял отсюда https://medium.com/@kezzieleo/manage-external-storage-permission-android-studio-java-9c3554cf79a7
        storageActivityResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {

                val intent = result.data
                // Здесь логика: что делать, если разрешение получено
                // или как запросить его у пользователя

                Log.d(LOGTAG, "storageActivityResultLauncher result.resultCode: " + RESULT_OK)

            } else if (result.resultCode == RESULT_CANCELED) {
                // Пользователь отменил задачу
                Log.d(LOGTAG, "storageActivityResultLauncher result.resultCode: " + RESULT_CANCELED)
            }
        }
    }

    fun checkStoragePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var isExternalStorageManagerFlag = Environment.isExternalStorageManager()

            Log.d(LOGTAG, "Android 11+ Environment.isExternalStorageManager(): " + isExternalStorageManagerFlag)

            if (isExternalStorageManagerFlag) {

                Log.d(LOGTAG, "Права на Storage есть, ничего запрашивать не надо")
            } else {
                Log.d(LOGTAG, "Запрашиваем эти долбанные права")

                val intent = Intent().apply {
                    action = ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    // Другие параметры для настройки доступа
                }
                intent.addCategory("android.intent.category.DEFAULT")
                intent.setData(Uri.parse(String.format("package:%s",getPackageName())))
                storageActivityResultLauncher.launch(intent)
            }

        } else {
            Log.d(LOGTAG, "Android 10-")
        }
    }
}