package com.carplayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import kotlinx.coroutines.*
import java.text.DateFormat
import java.text.Normalizer
import java.util.Date

var Accent by mutableStateOf(Color(0xFF8A6BFF))
var Bg by mutableStateOf<Brush>(THEMES[0].brush)
val NAMES=listOf("CANCIONES","ÁLBUMES","ARTISTAS","CARPETAS","FAVORITOS","LISTAS")
enum class Sort(val n:String){TITLE("Título"),ARTIST("Artista"),FILE("Archivo"),YEAR("Año"),MOD("Modif."),ADDED("Añadida")}
fun cmp(s:Sort,asc:Boolean):Comparator<Song>{
 val c:Comparator<Song> =when(s){
  Sort.TITLE->compareBy(String.CASE_INSENSITIVE_ORDER){it.title}
  Sort.ARTIST->compareBy(String.CASE_INSENSITIVE_ORDER){it.artist}
  Sort.FILE->compareBy(String.CASE_INSENSITIVE_ORDER){it.path.substringAfterLast('/')}
  Sort.YEAR->compareBy{it.year};Sort.MOD->compareBy{it.modified};Sort.ADDED->compareBy{it.added}}
 return if(asc)c else c.reversed()}
fun letter(s:Song,o:Sort):Char{
 val t=when(o){Sort.ARTIST->s.artist;Sort.FILE->s.path.substringAfterLast('/');else->s.title}.trim()
 val c=Normalizer.normalize(t.take(1),Normalizer.Form.NFD).firstOrNull()?.uppercaseChar()?:'#'
 return if(c in 'A'..'Z')c else '#'}
fun fm(ms:Long)="%d:%02d".format(ms/60000,ms/1000%60)
@Composable fun Ic(t:String,on:Boolean=false,size:TextUnit=26.sp,f:()->Unit){
 val col=if(on)Accent else Color.White;val k=GL.keys.firstOrNull{t.startsWith(it)}
 Row(Modifier.clip(CircleShape).clickable(onClick=f).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
  if(k!=null)Ico(GL[k]!!,(size.value*0.95f).dp,col)
  val rest=if(k!=null)t.removePrefix(k).trim() else t
  if(rest.isNotEmpty())Text(rest,fontSize=if(k!=null)size*0.55f else size,color=col,fontWeight=FontWeight.Bold,modifier=Modifier.padding(start=if(k!=null)6.dp else 0.dp))}}
@Composable fun H(t:String)=Text(t,color=Accent,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=18.dp,bottom=6.dp))

@Composable fun App(vm:Vm){
 var scr by remember{mutableIntStateOf(0)};var vo by remember{mutableStateOf(false)}
 BackHandler(scr!=0){scr=0}
 Box(Modifier.fillMaxSize().background(Bg)){
  Library(vm,{scr=1},{scr=2},{vo=true})
  if(scr==1&&vm.cur!=null)PlayerScreen(vm,false){scr=0}
  if(scr==2)SettingsScreen(vm){scr=0}
  if(vm.lockMode&&vm.locked&&vm.cur!=null)PlayerScreen(vm,true){}
  if(vo)VoiceDialog(vm,{vo=false}){vo=false;scr=1}}}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun Library(vm:Vm,onOpen:()->Unit,onSet:()->Unit,onVoice:()->Unit){
 var tab by remember{mutableIntStateOf(vm.tabOrder[0])};var q by remember{mutableStateOf("")}
 var sort by remember{mutableStateOf(Sort.TITLE)};var asc by remember{mutableStateOf(true)}
 var group by remember{mutableStateOf<String?>(null)};var dlg by remember{mutableStateOf(false)};var nm by remember{mutableStateOf("")}
 val gr=group
 BackHandler(gr!=null){group=null}
 val base=remember(vm.songs,q,sort,asc){vm.songs.filter{q.isBlank()||"${it.title} ${it.artist} ${it.album}".contains(q,true)}.sortedWith(cmp(sort,asc))}
 val key:(Song)->String={when(tab){1->it.album;2->it.artist;else->it.folder}}
 val songsView=tab==0||tab==4||gr!=null
 val list:List<Song> =when{tab==4->base.filter{it.id in vm.fav};tab==0->base
  gr==null->emptyList<Song>();tab==5->base.filter{it.id in (vm.pls[gr.orEmpty()]?:emptySet<Long>())};else->base.filter{key(it)==gr}}
 Column(Modifier.fillMaxSize().statusBarsPadding()){
  ScrollableTabRow(vm.tabOrder.indexOf(tab).coerceAtLeast(0),containerColor=Color.Transparent,edgePadding=8.dp){
   vm.tabOrder.forEach{id->Tab(tab==id,{tab=id;group=null},text={Text(NAMES[id],fontWeight=FontWeight.ExtraBold,fontSize=13.sp)})}}
  Row(Modifier.padding(horizontal=12.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically){
   Image(painterResource(R.drawable.logo),null,Modifier.padding(end=8.dp).size(44.dp).clip(CircleShape).combinedClickable(onClick=onVoice,onLongClick=onSet,onDoubleClick=onSet))
   OutlinedTextField(q,{q=it},Modifier.weight(1f),placeholder={Text("Buscar")},singleLine=true,shape=CircleShape)
   Ic(if(asc)"↑" else "↓",size=20.sp){asc=!asc}
   TextButton({sort=Sort.values()[(sort.ordinal+1)%Sort.values().size]}){Text(sort.n)}
   Ic(listOf("☰","▦","≣")[vm.view],size=20.sp){vm.setView((vm.view+1)%3)}
   Ic("⚙",size=20.sp,f=onSet)}
  Box(Modifier.weight(1f)){
   if(songsView)Column(Modifier.fillMaxSize()){
    if(gr!=null)Row(verticalAlignment=Alignment.CenterVertically){
     Ic("←"){group=null};Text(gr,Modifier.weight(1f),fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis)
     Ic("▶"){vm.play(list,0);onOpen()};if(tab==5)Ic("🗑"){vm.delPl(gr);group=null}}
    Box(Modifier.weight(1f)){
     val st=rememberLazyListState();val gs=rememberLazyGridState();val sc=rememberCoroutineScope()
     if(vm.view==1)LazyVerticalGrid(GridCells.Adaptive(150.dp),Modifier.fillMaxSize(),gs,contentPadding=PaddingValues(12.dp)){
      itemsIndexed(list,key={_,s->s.id}){i,s->
       Column(Modifier.padding(6.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(.08f)).clickable{vm.play(list,i);onOpen()}.padding(8.dp)){
        AsyncImage(s.art,null,Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)
        Text(s.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis,color=if(vm.cur?.id==s.id)Accent else Color.White)
        Text(s.artist,fontSize=12.sp,color=Color.White.copy(.6f),maxLines=1)}}}
     else{
      LazyColumn(Modifier.fillMaxSize(),st,contentPadding=PaddingValues(end=30.dp)){
       itemsIndexed(list,key={_,s->s.id}){i,s->SongRow(s,vm.cur?.id==s.id,vm.view==2){vm.play(list,i);onOpen()}}}
      if(sort in listOf(Sort.TITLE,Sort.ARTIST,Sort.FILE))AZ(list,st,sc){letter(it,sort)}}}}
   else{
    val g:List<Pair<String,List<Song>>> =if(tab==5)vm.pls.map{(n,ids)->n to base.filter{it.id in ids}} else base.groupBy(key).toList()
    LazyColumn(Modifier.fillMaxSize()){
     if(tab==5)item{TextButton({dlg=true}){Text("＋ Nueva lista")}}
     items(g,key={it.first}){(n,l)->
      Row(Modifier.fillMaxWidth().clickable{group=n}.padding(16.dp),verticalAlignment=Alignment.CenterVertically){
       if(l.isNotEmpty())AsyncImage(l[0].art,null,Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)
       else Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.1f)))
       Column(Modifier.padding(start=14.dp)){Text(n.substringAfterLast('/'),fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${l.size} canciones",color=Color.White.copy(.6f),fontSize=13.sp)}}}}}}
  Mini(vm,onOpen)}
 if(dlg)AlertDialog({dlg=false},{TextButton({vm.newPl(nm);nm="";dlg=false}){Text("Crear")}},title={Text("Nueva lista")},text={OutlinedTextField(nm,{nm=it},singleLine=true)})}

@Composable fun SongRow(s:Song,on:Boolean,detail:Boolean,f:()->Unit){
 Row(Modifier.fillMaxWidth().clickable(onClick=f).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically){
  AsyncImage(s.art,null,Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)),contentScale=ContentScale.Crop)
  Column(Modifier.padding(start=14.dp)){
   Text(s.title,fontWeight=FontWeight.Bold,fontSize=16.sp,color=if(on)Accent else Color.White,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text("${fm(s.durMs)} · ${s.artist}",color=Color.White.copy(.6f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
   if(detail)Text("${s.album} · ${if(s.year>0)s.year else "—"} · ${DateFormat.getDateInstance().format(Date(s.modified*1000))}\n${s.path.substringAfterLast('/')}\n${s.folder}",color=Color.White.copy(.45f),fontSize=11.sp)}}}

@Composable fun BoxScope.AZ(list:List<Song>,st:LazyListState,sc:CoroutineScope,lt:(Song)->Char){
 val L="#ABCDEFGHIJKLMNOPQRSTUVWXYZ";var h by remember{mutableIntStateOf(1)}
 fun jump(y:Float){val i=(y/h*L.length).toInt().coerceIn(0,L.length-1)
  val idx=(i until L.length).firstNotNullOfOrNull{k->list.indexOfFirst{lt(it)==L[k]}.takeIf{it>=0}}
  if(idx!=null)sc.launch{st.scrollToItem(idx)}}
 Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(26.dp).onSizeChanged{h=it.height}
  .pointerInput(list){detectTapGestures{jump(it.y)}}.pointerInput(list){detectVerticalDragGestures{c,_->jump(c.position.y)}},
  verticalArrangement=Arrangement.SpaceEvenly){L.forEach{Text(it.toString(),fontSize=11.sp,fontWeight=FontWeight.Bold,color=Accent,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())}}}

@Composable fun Mini(vm:Vm,onOpen:()->Unit){
 val s=vm.cur?:return
 Row(Modifier.padding(8.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(.1f)).clickable(onClick=onOpen).padding(8.dp).navigationBarsPadding(),verticalAlignment=Alignment.CenterVertically){
  AsyncImage(s.art,null,Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),contentScale=ContentScale.Crop)
  Column(Modifier.weight(1f).padding(horizontal=12.dp)){Text(s.title,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(s.artist,color=Color.White.copy(.6f),fontSize=12.sp,maxLines=1)}
  Ic("⏮",f=vm::prev);Ic(if(vm.playing)"⏸" else "▶",true,f=vm::toggle);Ic("⏭",f=vm::next)}}

@Composable fun PlayerScreen(vm:Vm,locked:Boolean,onClose:()->Unit){
 val s=vm.cur?:return
 var pos by remember{mutableLongStateOf(0L)};var dur by remember{mutableLongStateOf(1L)}
 LaunchedEffect(s){while(true){pos=vm.pos();dur=vm.dur().coerceAtLeast(1L);delay(500)}}
 val cover:@Composable (Modifier)->Unit={m->
  Box(m.background(Brush.linearGradient(listOf(Accent,Color(0xFF0A1A3A)))).pointerInput(s){
   var dx=0f;detectHorizontalDragGestures(onDragEnd={if(dx<-80)vm.next() else if(dx>80)vm.prev();dx=0f}){_,d->dx+=d}}){
   AsyncImage(s.art,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)}}
 BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF0A0A14)).background(Bg)){
  if(maxWidth>maxHeight)Row(Modifier.fillMaxSize()){
   cover(Modifier.weight(1f).fillMaxHeight())
   Controls(vm,s,pos,dur,locked,onClose,Modifier.weight(1f).fillMaxHeight().padding(20.dp))}
  else Column(Modifier.fillMaxSize().statusBarsPadding(),horizontalAlignment=Alignment.CenterHorizontally){
   cover(Modifier.padding(24.dp).fillMaxWidth(.78f).aspectRatio(1f).clip(CircleShape))
   Controls(vm,s,pos,dur,locked,onClose,Modifier.weight(1f).padding(horizontal=24.dp,vertical=8.dp).navigationBarsPadding())}}}

@Composable fun Controls(vm:Vm,s:Song,pos:Long,dur:Long,locked:Boolean,onClose:()->Unit,m:Modifier){
 var menu by remember{mutableIntStateOf(0)};var nn by remember{mutableStateOf<String?>(null)}
 val idx=vm.queue.indexOfFirst{it.id==s.id}+1
 Column(m,verticalArrangement=Arrangement.SpaceBetween){
  if(locked)Spacer(Modifier.height(8.dp)) else Row(verticalAlignment=Alignment.CenterVertically){Ic("⌄",f=onClose);Ic("☰ #$idx/${vm.queue.size}",size=14.sp){menu=3}}
  Column{Text(s.title,fontSize=26.sp,fontWeight=FontWeight.ExtraBold,maxLines=1,overflow=TextOverflow.Ellipsis)
   Text("${s.artist} · ${s.album}".uppercase(),color=Color.White.copy(.6f),fontSize=12.sp,letterSpacing=2.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
  if(!locked)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Ic("${vm.speed}x",size=14.sp){vm.cycleSpeed()};Ic(if(s.id in vm.fav)"♥" else "♡",s.id in vm.fav){vm.toggleFav(s)}
   Ic("＋"){menu=2};Ic("⏲"){menu=1};Ic(if(vm.volume==0f)"🔇" else "🔊"){vm.toggleMute()}}
  Column{Slider(pos.toFloat().coerceAtMost(dur.toFloat()),{vm.seek(it.toLong())},valueRange=0f..dur.toFloat())
   Row{Text(fm(pos),fontSize=12.sp);Spacer(Modifier.weight(1f));Text(fm(dur),fontSize=12.sp)}}
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Ic("🔀",vm.shuffle,f=vm::toggleShuffle);Ic("⏮",size=34.sp,f=vm::prev)
   Box(Modifier.size(88.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent,Color(0xFF4F3AB8)))).clickable(onClick=vm::toggle),contentAlignment=Alignment.Center){Ico(if(vm.playing)"pause" else "play",44.dp,Color.White)}
   Ic("⏭",size=34.sp,f=vm::next);Ic(if(vm.repeat==1)"🔂" else "🔁",vm.repeat>0,f=vm::cycleRepeat)}}
 if(menu==1)AlertDialog({menu=0},{},title={Text("Temporizador")},text={Column{listOf(15,30,60,0).forEach{x->TextButton({vm.sleep(x);menu=0}){Text(if(x==0)"Desactivar" else "$x min")}}}})
 if(menu==2)AlertDialog({menu=0},{},title={Text("Añadir a lista")},text={Column{vm.pls.keys.forEach{n->TextButton({vm.addTo(n,s);menu=0}){Text(n)}};TextButton({nn=""}){Text("＋ Nueva lista")}}})
 if(menu==3)AlertDialog({menu=0},{},title={Text("Cola de reproducción")},text={LazyColumn(Modifier.heightIn(max=420.dp)){itemsIndexed(vm.queue){i,x->
  Text(x.title,color=if(x.id==s.id)Accent else Color.White,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.fillMaxWidth().clickable{vm.playIndex(i);menu=0}.padding(10.dp))}}})
 val nv=nn
 if(nv!=null)AlertDialog({nn=null},{TextButton({vm.newPl(nv);vm.addTo(nv,s);nn=null;menu=0}){Text("Crear")}},title={Text("Nueva lista")},text={OutlinedTextField(nv,{nn=it},singleLine=true)})}

@Composable fun SettingsScreen(vm:Vm,onClose:()->Unit){
 var sec by remember{mutableStateOf<String?>(null)}
 BackHandler(sec!=null){sec=null}
 LaunchedEffect(Unit){while(true){vm.refreshEq();delay(1500)}}
 val folders=remember(vm.all){vm.all.map{it.folder}.distinct().sorted()}
 val r=vm.eqRange()
 val secs=listOf(Triple("lib","scan","Biblioteca"),Triple("fol","folder","Carpetas excluidas"),Triple("tem","palette","Temas"),Triple("tab","swap","Pestañas"),Triple("eq","eq","Ecualizador"),Triple("blo","lock","Pantalla de bloqueo"),Triple("voz","mic","Voz"))
 val s=sec
 Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()){
  Row(verticalAlignment=Alignment.CenterVertically){Ic("←"){if(s==null)onClose() else sec=null};Text(secs.firstOrNull{it.first==s}?.third?:"Ajustes",fontSize=22.sp,fontWeight=FontWeight.ExtraBold)}
  if(s==null)LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(12.dp)){
   items(secs.chunked(2)){row->Row(Modifier.fillMaxWidth()){
    row.forEach{(k,i,n)->Column(Modifier.weight(1f).padding(6.dp).clip(RoundedCornerShape(22.dp)).background(Color.White.copy(.09f)).clickable{sec=k}.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Ico(i,34.dp,Accent);Spacer(Modifier.height(10.dp));Text(n,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)}}
    if(row.size<2)Spacer(Modifier.weight(1f))}}}
  else LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(16.dp)){
   when(s){
    "lib"->item{Text("${vm.all.size} canciones en la biblioteca",color=Color.White.copy(.6f));Button({vm.scan()}){Text("Escanear canciones")}}
    "fol"->items(folders){f->Row(Modifier.fillMaxWidth().clickable{vm.exclude(f)},verticalAlignment=Alignment.CenterVertically){
      Text(f,Modifier.weight(1f),fontSize=13.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Switch(f in vm.excluded,{vm.exclude(f)})}}
    "tem"->items(THEMES.chunked(3).withIndex().toList()){(ri,row)->Row(Modifier.fillMaxWidth()){
      row.forEachIndexed{ci,t->val i=ri*3+ci;val sh=RoundedCornerShape(16.dp)
       Box(Modifier.weight(1f).padding(4.dp).height(64.dp).clip(sh).background(t.brush).then(if(i==vm.themeIdx)Modifier.border(2.dp,t.accent,sh) else Modifier).clickable{vm.setTheme(i)},contentAlignment=Alignment.Center){Text(t.name,fontSize=12.sp,fontWeight=FontWeight.Bold)}}
      repeat(3-row.size){Spacer(Modifier.weight(1f))}}}
    "tab"->items(vm.tabOrder.size){p->Row(verticalAlignment=Alignment.CenterVertically){
      Text(NAMES[vm.tabOrder[p]],Modifier.weight(1f),fontWeight=FontWeight.Bold);Ic("↑"){vm.moveTab(p,-1)};Ic("↓"){vm.moveTab(p,1)}}}
    "eq"->{
     if(vm.bands.isEmpty())item{Text("Se activa al iniciar la reproducción.",color=Color.White.copy(.6f))}
     items(vm.bands.size){b->Row(verticalAlignment=Alignment.CenterVertically){
      Text(vm.bandLabel(b),Modifier.width(72.dp),fontSize=12.sp)
      Slider(vm.bands[b].toFloat(),{vm.setBand(b,it.toInt())},Modifier.weight(1f),valueRange=r.first.toFloat()..r.second.toFloat())}}
     item{LazyRow{items(vm.presetCount()){p->TextButton({vm.preset(p)}){Text(vm.presetName(p))}}}}}
    "blo"->item{Row(verticalAlignment=Alignment.CenterVertically){Text("Modo bloqueo especial",Modifier.weight(1f),fontWeight=FontWeight.Bold);Switch(vm.lockMode,{vm.setLock(it)})}
     Text("Al bloquear, la canción se muestra en grande con los controles sobre la pantalla de bloqueo.",color=Color.White.copy(.6f),fontSize=12.sp)}
    else->item{Text("Toca el logo de la app para dar órdenes por voz: “pon Zion”, “siguiente”, “pausa”, “aleatorio”. Doble toque o toque largo en el logo abre Ajustes.",color=Color.White.copy(.7f))}}}}}

@Composable fun VoiceDialog(vm:Vm,onClose:()->Unit,onPlay:()->Unit){
 var msg by remember{mutableStateOf("Toca el micrófono y di: “pon Zion”, “siguiente”, “pausa”")};var txt by remember{mutableStateOf("")}
 fun run(t:String){msg=vm.voice(t);if(msg.startsWith("Reproduciendo "))onPlay()}
 val l=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){r->r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let{run(it)}}
 AlertDialog(onClose,{TextButton({run(txt)}){Text("Enviar")}},dismissButton={TextButton(onClose){Text("Cerrar")}},title={Text("Control por voz")},text={
  Column(horizontalAlignment=Alignment.CenterHorizontally){
   Box(Modifier.size(88.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Accent,Color(0xFF4F3AB8)))).clickable{
    try{l.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-ES"))}
    catch(e:Exception){msg="Este dispositivo no tiene reconocimiento de voz; escribe el comando."}},contentAlignment=Alignment.Center){Ico("mic",40.dp,Color.White)}
   Text(msg,Modifier.padding(top=12.dp),fontSize=13.sp)
   OutlinedTextField(txt,{txt=it},Modifier.padding(top=8.dp),singleLine=true,placeholder={Text("o escribe el comando")})}})}
