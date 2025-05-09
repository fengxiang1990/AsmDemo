package com.example.asmdemo

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable.Orientation
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chaoxing.transformer.ThreadStackUtil
import com.example.testlibrary.LibraryTestUtil


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val rootView = LinearLayout(this)
        rootView.orientation = LinearLayout.VERTICAL
        setContentView(rootView)

        val btn1 = Button(this)
        btn1.text = "click me"
        rootView.addView(btn1)

        val text1 = TextView(this)
        text1.text = "click me text"
        text1.height = 120
        text1.setBackgroundColor(Color.parseColor("#999999"))
        val lp = LinearLayout.LayoutParams(-1,-2)
        lp.gravity = Gravity.CENTER
        rootView.addView(text1,lp)

        val image1 = ImageView(this)
        val image1_lp = LinearLayout.LayoutParams(120,120)
        image1.setImageResource(R.drawable.ic_launcher_background)
        rootView.addView(image1,image1_lp)


        var id = Settings.Secure.getString(contentResolver,"android_id")
        Log.e("fxa","android id->"+id)
        TestUtils().test1(this)
        TestUtils().test2()
        LibraryTestUtil.test(this)
        showNotificationPerm(this)

        text1.setOnClickListener {
            Log.e("fxa", "text1 clicked") }

        btn1.setOnClickListener {
            Log.e("fxa","btn1 clicked lambda")
        }

        image1.setOnClickListener {
            Log.e("fxa","image1 clicked lambda")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showNotification(this, "标题", "内容")
        }
    }


    fun showNotificationPerm(context : Context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                // 如果你在 Activity 中，可以直接请求
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            } else {
                // 权限已授权，可直接发通知
                showNotification(context, "标题", "内容")
            }
        } else {
            // Android 13 以下系统不需要请求通知权限
            showNotification(context, "标题", "内容")
        }
    }

    fun showNotification(context: Context, title: String, content: String) {
        val channelId = "my_channel_id"
        val channelName = "MyChannel"

        // Android 8.0+ 需要通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel description"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setGroup("1111")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify(1, builder.build())  // 第一个参数是通知的唯一ID
        }
    }


}