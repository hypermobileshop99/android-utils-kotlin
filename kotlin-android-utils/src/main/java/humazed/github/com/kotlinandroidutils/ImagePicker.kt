package humazed.github.com.kotlinandroidutils

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.FileProvider
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import org.jetbrains.anko.longToast
import stream.customimagepicker.CustomImagePicker
import stream.jess.ui.TwoWayGridView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 *  @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
 */
fun Activity.pickImage(onItemSelected: (imageFile: File) -> Unit) {
    d { "initImagePickerDialog() called with:" }
    //Initialize Image Picker Dialogue Popup.
    val bottomSheet = layoutInflater.inflate(R.layout.bottom_sheet, null)
    val bottomSheetDialog = Dialog(this, R.style.MaterialDialogSheet)
    bottomSheetDialog.setContentView(bottomSheet)
    bottomSheetDialog.setCancelable(true)
    bottomSheetDialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    bottomSheetDialog.window?.setGravity(Gravity.BOTTOM)

    //Initialize Image Picker Menu Actions.
    val layoutCamera = bottomSheet.findViewById<LinearLayout>(R.id.btn_camera)
    val layoutGallery = bottomSheet.findViewById<LinearLayout>(R.id.btn_gallery)
    layoutCamera.setOnClickListener {
        dispatchTakePicture { data ->
            data.data?.let { imageUri ->
                getFilePath(imageUri)?.let {
                    onItemSelected(File(it))
                }
            }
        }
        bottomSheetDialog.dismiss()
    }
    layoutGallery.setOnClickListener {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent) { data ->
            data.data?.let { imageUri ->
                getFilePath(imageUri)?.let {
                    onItemSelected(File(it))
                }
            }
        }
        bottomSheetDialog.dismiss()
    }

    val imagePicker = CustomImagePicker()
    imagePicker.setHeight(100)
    imagePicker.setWidth(100)
    val adapter = imagePicker.getAdapter(this)

    val gridView = bottomSheet.findViewById<TwoWayGridView>(R.id.gridview)
    gridView.layoutParams.height = dpToPx(200)
    gridView.setNumRows(2)
    gridView.adapter = adapter

    gridView.setOnItemClickListener { _, _, _, id ->
        val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

        getFilePath(imageUri)?.let { onItemSelected(File(it)) }
        bottomSheetDialog.dismiss()
    }

    bottomSheetDialog.show()
}

fun Activity.pickImageWithPermission(onItemSelected: (imageFile: File) -> Unit): Unit {
    val permissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            pickImage(onItemSelected)
        }

        override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
            longToast("Permission Denied\n$deniedPermissions")
        }
    }
    TedPermission.with(this)
            .setPermissionListener(permissionListener)
            .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .check()
}

private fun Context.dispatchTakePicture(onSuccess: (data: Intent) -> Unit) {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
        // Ensure that there's a camera activity to handle the intent
        takePictureIntent.resolveActivity(packageManager)?.also {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                null
            }
            // Continue only if the File was successfully created
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent) { data ->
                    galleryAddPic(photoFile)
                    onSuccess(data)
                }
            }
        }
    }
}


@Throws(IOException::class)
private fun Context.createImageFile(): File {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
    return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
    )
}

private fun Context.galleryAddPic(imageFile: File) {
    Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
        mediaScanIntent.data = Uri.fromFile(imageFile)
        sendBroadcast(mediaScanIntent)
    }
}