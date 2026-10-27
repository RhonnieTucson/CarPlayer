package com.carplayer
import android.Manifest
import android.app.KeyguardManager
import android.content.*
import android.os.*
import androidx.activity.*
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat

class MainActivity:ComponentActivity(){
 private val vm:Vm by viewModels()
 private fun chk(){vm.locked=(getSystemService(KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked}
 private val rc=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){chk()}}
 override fun onCreate(b:Bundle?){super.onCreate(b);enableEdgeToEdge()
  val perms=if(Build.VERSION.SDK_INT>=33)arrayOf(Manifest.permission.READ_MEDIA_AUDIO,Manifest.permission.POST_NOTIFICATIONS) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
  registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){vm.scan()}.launch(perms)
  val f=IntentFilter().apply{addAction(Intent.ACTION_USER_PRESENT);addAction(Intent.ACTION_SCREEN_OFF);addAction(Intent.ACTION_SCREEN_ON)}
  ContextCompat.registerReceiver(this,rc,f,ContextCompat.RECEIVER_NOT_EXPORTED)
  setContent{val lm=vm.lockMode
   SideEffect{if(Build.VERSION.SDK_INT>=27)setShowWhenLocked(lm)}
   MaterialTheme(darkColorScheme(primary=Accent)){ProvideTextStyle(TextStyle(fontFamily=AppFont,fontWeight=FontWeight.Medium,letterSpacing=.2.sp)){App(vm)}}}}
 override fun onResume(){super.onResume();chk()}
 override fun onDestroy(){unregisterReceiver(rc);super.onDestroy()}
}
