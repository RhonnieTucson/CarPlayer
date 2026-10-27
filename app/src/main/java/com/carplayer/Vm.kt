package com.carplayer
import android.app.Application
import android.content.ComponentName
import android.os.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.media3.common.*
import androidx.media3.session.*
import kotlinx.coroutines.*
import org.json.*
import java.text.Normalizer

class Vm(private val app:Application):AndroidViewModel(app){
 private val sp=app.getSharedPreferences("cp",0)
 var all by mutableStateOf(listOf<Song>())
 var excluded by mutableStateOf<Set<String>>(sp.getStringSet("ex",emptySet())!!.toSet())
 val songs by derivedStateOf{all.filter{it.folder !in excluded}}
 var cur by mutableStateOf<Song?>(null);var playing by mutableStateOf(false)
 var repeat by mutableIntStateOf(0);var shuffle by mutableStateOf(false);var queue by mutableStateOf(listOf<Song>())
 var fav by mutableStateOf(sp.getStringSet("fav",emptySet())!!.mapNotNull{it.toLongOrNull()}.toSet())
 var themeIdx by mutableIntStateOf(sp.getInt("th",0))
 var tabOrder by mutableStateOf(sp.getString("tabs","0,1,2,3,4,5")!!.split(",").mapNotNull{it.toIntOrNull()}.let{if(it.size==6)it else listOf(0,1,2,3,4,5)})
 var view by mutableIntStateOf(sp.getInt("view",0))
 var lockMode by mutableStateOf(sp.getBoolean("lock",false));var locked by mutableStateOf(false)
 var speed by mutableFloatStateOf(1f);var volume by mutableFloatStateOf(1f)
 var bands by mutableStateOf(listOf<Int>())
 var pls by mutableStateOf(loadPls())
 private var ctl:MediaController?=null
 private val h=Handler(Looper.getMainLooper())
 private fun loadPls():Map<String,Set<Long>>{val o=JSONObject(sp.getString("pls","{}")!!)
  return o.keys().asSequence().associateWith{k->val a=o.getJSONArray(k);(0 until a.length()).map{a.getLong(it)}.toSet()}}
 private fun savePls(){val o=JSONObject();pls.forEach{(k,v)->o.put(k,JSONArray(v.toList()))};sp.edit().putString("pls",o.toString()).apply()}
 private val ln=object:Player.Listener{
  override fun onIsPlayingChanged(isPlaying:Boolean){playing=isPlaying}
  override fun onMediaItemTransition(m:MediaItem?,r:Int){cur=queue.firstOrNull{it.id.toString()==m?.mediaId}}
  override fun onRepeatModeChanged(m:Int){repeat=when(m){Player.REPEAT_MODE_ONE->1;Player.REPEAT_MODE_ALL->2;else->0}}
  override fun onShuffleModeEnabledChanged(s:Boolean){shuffle=s}}
 init{applyTheme()
  val f=MediaController.Builder(app,SessionToken(app,ComponentName(app,PlayerService::class.java))).buildAsync()
  f.addListener({ctl=f.get().also{it.addListener(ln)}},ContextCompat.getMainExecutor(app))}
 private fun applyTheme(){val t=THEMES[themeIdx.coerceIn(0,THEMES.size-1)];Accent=t.accent;Bg=t.brush}
 fun setTheme(i:Int){themeIdx=i;applyTheme();sp.edit().putInt("th",i).apply()}
 fun setView(v:Int){view=v;sp.edit().putInt("view",v).apply()}
 fun setLock(b:Boolean){lockMode=b;sp.edit().putBoolean("lock",b).apply()}
 fun moveTab(p:Int,d:Int){val j=p+d;if(j<0||j>=tabOrder.size)return;val l=tabOrder.toMutableList();val t=l[p];l[p]=l[j];l[j]=t;tabOrder=l;sp.edit().putString("tabs",l.joinToString(",")).apply()}
 fun scan(){viewModelScope.launch(Dispatchers.IO){val l=queryAudio(app,emptySet());withContext(Dispatchers.Main){all=l}}}
 fun exclude(f:String){excluded=if(f in excluded)excluded-f else excluded+f;sp.edit().putStringSet("ex",excluded).apply()}
 fun play(l:List<Song>,i:Int){val c=ctl?:return;if(l.isEmpty())return;queue=l;c.setMediaItems(l.map{it.item()},i,0);c.prepare();c.play();cur=l[i]}
 fun playIndex(i:Int){ctl?.seekTo(i,0)}
 fun toggle(){ctl?.let{if(it.isPlaying)it.pause() else it.play()}}
 fun next(){ctl?.let{if(it.hasNextMediaItem())it.seekToNextMediaItem() else it.seekTo(0,0)}}
 fun prev(){ctl?.let{if(it.currentPosition>3000)it.seekTo(0) else it.seekToPreviousMediaItem()}}
 fun cycleRepeat(){ctl?.repeatMode=when(repeat){0->Player.REPEAT_MODE_ONE;1->Player.REPEAT_MODE_ALL;else->Player.REPEAT_MODE_OFF}}
 fun toggleShuffle(){ctl?.shuffleModeEnabled=!shuffle}
 fun cycleSpeed(){val l=listOf(.75f,1f,1.25f,1.5f,2f);speed=l[(l.indexOf(speed)+1)%l.size];ctl?.setPlaybackSpeed(speed)}
 fun toggleMute(){volume=if(volume==0f)1f else 0f;ctl?.volume=volume}
 fun seek(ms:Long){ctl?.seekTo(ms)}
 fun pos()=ctl?.currentPosition?:0L
 fun dur()=(ctl?.duration?:0L).coerceAtLeast(0L)
 fun toggleFav(s:Song){fav=if(s.id in fav)fav-s.id else fav+s.id;sp.edit().putStringSet("fav",fav.map{it.toString()}.toSet()).apply()}
 fun sleep(min:Int){h.removeCallbacksAndMessages(null);if(min>0)h.postDelayed({ctl?.pause()},min*60000L)}
 fun newPl(n:String){if(n.isNotBlank()&&n !in pls){pls=pls+(n to emptySet<Long>());savePls()}}
 fun delPl(n:String){pls=pls-n;savePls()}
 fun addTo(n:String,s:Song){pls=pls+(n to ((pls[n]?:emptySet<Long>())+s.id));savePls()}
 fun refreshEq(){val e=PlayerService.eq?:return;bands=(0 until e.numberOfBands.toInt()).map{e.getBandLevel(it.toShort()).toInt()}}
 fun eqRange():Pair<Int,Int>=PlayerService.eq?.bandLevelRange?.let{it[0].toInt() to it[1].toInt()}?:(-1500 to 1500)
 fun bandLabel(b:Int):String{val f=(PlayerService.eq?.getCenterFreq(b.toShort())?:0)/1000;return if(f>=1000)"${f/1000} kHz" else "$f Hz"}
 fun presetCount()=PlayerService.eq?.numberOfPresets?.toInt()?:0
 fun presetName(p:Int)=PlayerService.eq?.getPresetName(p.toShort())?:""
 private fun saveEq(){PlayerService.eq?.let{e->sp.edit().putString("eq",(0 until e.numberOfBands.toInt()).joinToString(","){e.getBandLevel(it.toShort()).toString()}).apply()}}
 fun setBand(b:Int,l:Int){PlayerService.eq?.setBandLevel(b.toShort(),l.toShort());val m=bands.toMutableList();if(b<m.size)m[b]=l;bands=m;saveEq()}
 fun preset(p:Int){PlayerService.eq?.usePreset(p.toShort());refreshEq();saveEq()}
 fun voice(t:String):String{
  fun n(x:String)=Normalizer.normalize(x.lowercase(),Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"")
  val s=n(t).trim()
  when{
   Regex("^(siguiente|proxima|next)").containsMatchIn(s)->{next();return "Siguiente"}
   Regex("^(anterior|atras|previous)").containsMatchIn(s)->{prev();return "Anterior"}
   Regex("^(pausa|pausar|detener|para)").containsMatchIn(s)->{ctl?.pause();return "Pausa"}
   Regex("^(continua|continuar|reanuda|play|reproduce|reproducir)$").containsMatchIn(s)->{ctl?.play();return "Reproduciendo"}
   "aleatorio" in s->{toggleShuffle();return "Aleatorio"}
   "repetir" in s->{cycleRepeat();return "Repetir"}}
  val q=s.replace(Regex("^(pon|ponme|reproduce|reproducir|toca|quiero escuchar|escuchar|play)\\s+"),"").replace(Regex("^(la cancion|el album|el disco|algo de|a|de)\\s+"),"").trim()
  val w=q.split(Regex("\\s+")).filter{it.isNotEmpty()}
  val l=songs.filter{x->val h=n("${x.title} ${x.artist} ${x.album}");w.all{it in h}}
  if(w.isEmpty()||l.isEmpty())return "No encontré \"$q\""
  play(l,0);return "Reproduciendo ${l[0].title}"}
 override fun onCleared(){ctl?.release()}
}
