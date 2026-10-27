package com.carplayer
import android.content.*
import android.net.Uri
import android.provider.MediaStore.Audio.Media as M
import androidx.media3.common.*
import java.io.File

data class Song(val id:Long,val title:String,val artist:String,val album:String,val year:Int,val folder:String,val path:String,val added:Long,val modified:Long,val durMs:Long,val albumId:Long){
 val uri:Uri get()=ContentUris.withAppendedId(M.EXTERNAL_CONTENT_URI,id)
 val art:Uri get()=ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId)
}
fun Song.item()=MediaItem.Builder().setMediaId(id.toString()).setUri(uri)
 .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(art).build()).build()

fun queryAudio(c:Context,ex:Set<String>):List<Song>{
 val out=mutableListOf<Song>()
 val p=arrayOf(M._ID,M.TITLE,M.ARTIST,M.ALBUM,M.YEAR,M.DATA,M.DATE_ADDED,M.DATE_MODIFIED,M.DURATION,M.ALBUM_ID)
 c.contentResolver.query(M.EXTERNAL_CONTENT_URI,p,"${M.IS_MUSIC}!=0 AND ${M.DURATION}>10000",null,null)?.use{k->
  while(k.moveToNext()){
   val path=k.getString(5)?:continue;val f=File(path).parent?:"";if(f in ex)continue
   val ar=k.getString(2).let{if(it==null||it=="<unknown>")"Desconocido" else it}
   out+=Song(k.getLong(0),k.getString(1)?:File(path).nameWithoutExtension,ar,k.getString(3)?:"Sin álbum",k.getInt(4),f,path,k.getLong(6),k.getLong(7),k.getLong(8),k.getLong(9))
  }}
 return out
}
