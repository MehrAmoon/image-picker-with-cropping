package com.example.image_picker_with_cropping

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.mehramoon.imagepickerwithcropping.crop.CropImage
import com.mehramoon.imagepickerwithcropping.crop.CropImageView
import com.mehramoon.imagepickerwithcropping.ImagePicker
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    private var imagePicker = ImagePicker()

    private fun setLayout(): Int = R.layout.main_activity

    companion object {
        const val REQUEST_CODE = 920
        const val RESULT_CODE = 1000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(setLayout())

        select_source.setOnClickListener {
            runBlocking {
                pickImage()
            }
        }

    }

    private fun pickImage() {
        imagePicker.startChooser(this, object : ImagePicker.Callback() {

            override fun onPickImage(imageUri: Uri?) {
            }

            override fun onCropImage(imageUri: Uri?) {
                super.onCropImage(imageUri)

                Glide.with(this@MainActivity)
                    .load(imageUri)
                    .into(image);
            }

            override fun cropConfig(builder: CropImage.ActivityBuilder) {
                builder.setMultiTouchEnabled(false)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .setCropShape(CropImageView.CropShape.RECTANGLE)
            }

            override fun onPermissionDenied(
                requestCode: Int,
                permissions: Array<String>?,
                grantResults: IntArray?
            ) {

            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            imagePicker.onActivityResult(this, requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_CODE)
                finish()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, state: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, state)
        imagePicker.onRequestPermissionsResult(this, requestCode, permissions, state)
    }

}