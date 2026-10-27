package com.carplayer
import android.media.audiofx.Equalizer
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlayerService:MediaLibraryService(){
 companion object{var eq:Equalizer?=null}
 private var session:MediaLibrarySession?=null
 private fun mk(id:Int){
  try{eq?.release();val e=Equalizer(0,id);e.setEnabled(true)
   getSharedPreferences("cp",0).getString("eq",null)?.split(",")?.forEachIndexed{i,v->if(i<e.numberOfBands)e.setBandLevel(i.toShort(),v.trim().toShort())}
   eq=e}catch(x:Exception){}}
 private val cb=object:MediaLibrarySession.Callback{
  override fun onGetLibraryRoot(s:MediaLibrarySession,b:MediaSession.ControllerInfo,p:LibraryParams?):ListenableFuture<LibraryResult<MediaItem>> =
   Futures.immediateFuture(LibraryResult.ofItem(MediaItem.Builder().setMediaId("root").setMediaMetadata(MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).build()).build(),p))
  override fun onAddMediaItems(s:MediaSession,c:MediaSession.ControllerInfo,items:MutableList<MediaItem>):ListenableFuture<MutableList<MediaItem>>{
   val out=mutableListOf<MediaItem>()
   items.forEach{m->val q=m.requestMetadata.searchQuery
    if(m.localConfiguration!=null)out.add(m)
    else if(q!=null){val w=q.lowercase().split(" ").filter{it.isNotBlank()}
     queryAudio(this@PlayerService,emptySet()).filter{x->val h="${x.title} ${x.artist} ${x.album}".lowercase();w.all{it in h}}.forEach{out.add(it.item())}}}
   return Futures.immediateFuture(out)}}
 override fun onCreate(){super.onCreate()
  val p=ExoPlayer.Builder(this).setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),true).setHandleAudioBecomingNoisy(true).build()
  mk(p.audioSessionId)
  p.addListener(object:Player.Listener{override fun onAudioSessionIdChanged(audioSessionId:Int){mk(audioSessionId)}})
  session=MediaLibrarySession.Builder(this,p,cb).build()}
 override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaLibrarySession?=session
 override fun onDestroy(){eq?.release();eq=null;session?.run{player.release();release()};session=null;super.onDestroy()}
}
