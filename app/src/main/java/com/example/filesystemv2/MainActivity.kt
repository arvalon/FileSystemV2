package com.example.filesystemv2

import android.Manifest.permission.READ_PHONE_STATE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filesystemv2.databinding.ActivityMainBinding
import java.io.File
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class MainActivity : AppCompatActivity() {

    private val LOGTAG = "filesystemv2.log"

    private val permission_need_read_phone_state = "Приложению необходимо разрешение на чтение состояние телефона для получения серийного номера"

    private val permission_need_write_external_storage = "Приложению необходимо разрешение на запись внешнего хранилища для ведения файла лога"

    private lateinit var binding : ActivityMainBinding

    /** это для Android 11+ */
    private lateinit var storageActivityResultLauncher : ActivityResultLauncher<Intent>

    private lateinit var requestOtherPermissionsContract: ActivityResultContracts.RequestMultiplePermissions

    private lateinit var otherPermissionActivityResultLauncher: ActivityResultLauncher<Array<String>>

    /** Пакет прав для Android10- */
    private val PERMISSIONS_A10 = arrayOf(READ_PHONE_STATE, WRITE_EXTERNAL_STORAGE)

    /** Права для Android 11+. В случае этой группы версий ОС право на файловую систему
     * ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION запрашивается ранее через механихм Intent() */
    private val PERMISSIONS_A11 = arrayOf(READ_PHONE_STATE)

    private var isAskingPermissions = false

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
            checkStoragePermission(false)
        }

        binding.buttonCreateDir.setOnClickListener {

            val dir = File(Environment.getExternalStorageDirectory(), "MyDir")

            Log.d(LOGTAG,"Создание папки " + dir.getAbsolutePath() + ": " + dir.mkdir())
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
                askPermissions()
            }
        }

        requestOtherPermissionsContract = ActivityResultContracts.RequestMultiplePermissions()

        var isAlertIsShowing = false

        val permissionAndroid10 = mapOf(
            READ_PHONE_STATE to permission_need_read_phone_state,
            WRITE_EXTERNAL_STORAGE to permission_need_write_external_storage
        )

        val permissionAndroid11 = mapOf(READ_PHONE_STATE to permission_need_read_phone_state)

        val explainPermission = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) permissionAndroid11 else permissionAndroid10

        otherPermissionActivityResultLauncher = registerForActivityResult(requestOtherPermissionsContract)
        { isGranted ->

            Log.d(LOGTAG, "Результат multiplePermissionActivityResultLauncher: $isGranted")

            if (isGranted.containsValue(false)) {

                Log.d(LOGTAG, "По крайней мере одно из разрешений не предоставлено...")

                for ((name, granted) in isGranted) {

                    if (!granted && !isAlertIsShowing) {

                        Log.d(LOGTAG, "Диалог с пользователем на предоставление разрешения: $name")

                        AlertDialog.Builder(this)
                            .setMessage(explainPermission[name])
                            .setPositiveButton(android.R.string.ok) { _, _ ->

                                // Если пользователь нажал "Больше не спрашивать" или дважды
                                // отклонил запрос на Android 11 (что равноценно)
                                // идем в настройки

                                if (!shouldShowRequestPermissionRationale(name)) {
                                    val goToSettingsIntent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID))
                                    startActivity(goToSettingsIntent)
                                } else {
                                    Log.d(LOGTAG, "По крайней мере одно из разрешений не предоставлено...")
                                    askPermissions()
                                }
                                Log.d(LOGTAG, "MainFragment MainFragment registerForActivityResult Callback 2 askPermissions()")
                                askPermissions()
                            }
                            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                                isAlertIsShowing = false
                            }
                            .show()
                        isAlertIsShowing = true
                    }
                }
            }
            isAskingPermissions = false
        }

        checkStoragePermission(true)
    }


    /** @param add так же запросить и остальные права (пока только одно, PHONE_STATE, для Android 10-) */
    fun checkStoragePermission(add : Boolean) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var isExternalStorageManagerFlag = Environment.isExternalStorageManager()

            Log.d(LOGTAG, "Android 11+ Environment.isExternalStorageManager(): " + isExternalStorageManagerFlag)

            if (isExternalStorageManagerFlag) {

                Log.d(LOGTAG, "Права на Storage есть, ничего запрашивать не надо")
                if (add) askPermissions()
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
            if (add) askPermissions()
        }
    }

    private fun askPermissions() {

        Log.d(LOGTAG,"MainActivity askPermissions")

        // На Android 10- подсовываем всё, на Android 11 только READ_PHONE_STATE
        val TAIL_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) PERMISSIONS_A11 else PERMISSIONS_A10

        if (!hasPermissions(TAIL_PERMISSIONS)) {

            Log.d(LOGTAG,"MainActivity askPermissions Запрос оставшихся разрешений через ActivityResultContracts.RequestMultiplePermissions")
            isAskingPermissions = true
            otherPermissionActivityResultLauncher.launch(TAIL_PERMISSIONS)
        } else {
            Log.d(LOGTAG,"Все разрешения уже предоставлены")
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {

        Log.d(LOGTAG, "MainActivity hasPermissions")

        permissions.forEach { permission ->
            if (ActivityCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(LOGTAG, "Не предоставлено разрешение: $permission")
                return false
            }
            Log.d(LOGTAG, "Разрешение уже предоставлено: $permission")
        }
        return true
    }
}