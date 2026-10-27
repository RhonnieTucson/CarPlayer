package com.carplayer
import androidx.compose.ui.graphics.*
class Th(val name:String,val accent:Color,val brush:Brush)
private fun s(n:String,bg:Long,ac:Long)=Th(n,Color(ac),Brush.verticalGradient(listOf(Color(bg),Color(bg))))
private fun g(n:String,ac:Long,vararg c:Long)=Th(n,Color(ac),Brush.linearGradient(c.map{Color(it)}))
val THEMES=listOf(
 g("Neón CarPlayer",0xFF8A6BFF,0xFF1A0F3A,0xFF0A1A3A,0xFF2A0F4A),
 s("Medianoche",0xFF0B0F1A,0xFF4F8CFF),s("Rojo carbón",0xFF0E0E10,0xFFFF3B4E),s("Esmeralda",0xFF07130F,0xFF2EE59D),
 s("Ámbar",0xFF120D05,0xFFFFB020),s("Violeta",0xFF0F0A1A,0xFFA77BFF),s("Océano",0xFF04121A,0xFF25C7E8),
 s("Rosa",0xFF17090F,0xFFFF5FA2),s("Grafito",0xFF121316,0xFFB8C0D0),s("Atardecer",0xFF1A0A07,0xFFFF7A45),s("Bosque",0xFF0A1209,0xFF8BD450),
 g("Aurora",0xFF3DE0C3,0xFF0F2027,0xFF203A43,0xFF2C5364),g("Ardiente",0xFFFFB347,0xFF2B0A3D,0xFF7A1F4B,0xFFFF6A3D),
 g("Océano profundo",0xFF5EF2FF,0xFF021B3A,0xFF0A4A7A,0xFF0FB5C8),g("Bosque místico",0xFF9CF26B,0xFF06150F,0xFF12392B,0xFF2D6A3E),
 g("Rubí",0xFFFF6B81,0xFF1C0509,0xFF5A0F1F,0xFFB3203A),g("Oro y noche",0xFFFFCC4D,0xFF0B0B0F,0xFF2A2210,0xFF8A6A1F),
 g("Algodón oscuro",0xFFFF8FD0,0xFF1B1038,0xFF4A1F6E,0xFFD6489C),g("Azul noche",0xFF6EA0FF,0xFF050A24,0xFF0F1F5C,0xFF2B44A8),
 g("Cromo",0xFFE0E6FF,0xFF101114,0xFF2A2D35,0xFF4A4E59),g("Lava",0xFFFF5A36,0xFF140505,0xFF4A0F0A,0xFF9A2A10))
