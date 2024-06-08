package com.example.musicrecommend

import android.os.Bundle
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.musicrecommend.ui.theme.MusicRecommendTheme
import com.skydoves.landscapist.coil.CoilImage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.ProtoBuf
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val player = ExoPlayer.Builder(this).build()
        setContent {
            MusicRecommendTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        player = player,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun Greeting(player: ExoPlayer, modifier: Modifier = Modifier) {
    var coroutineScope = rememberCoroutineScope()
    val httpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            register(
                ContentType.Text.Plain,
                KotlinxSerializationConverter(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    },
                ),
            )
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                    explicitNulls = false
                    encodeDefaults = true
                },
            )
        }
    }
    val context = LocalContext.current
    var trackId by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var trackPlayingId by rememberSaveable {
        mutableStateOf("")
    }
    var recommendState by rememberSaveable {
        mutableStateOf<RecommendModel?>(null)
    }
    LaunchedEffect(key1 = recommendState) {
        Log.d("Recommend", recommendState.toString())
    }
    LaunchedEffect(key1 = loading) {
        if (loading) {
            kotlin.runCatching {
                httpClient.get("http://10.0.3.2:5000/recommend/${trackId}") {
                    contentType(ContentType.Application.Json)
                }.body<RecommendModel>()
            }.onSuccess {
                Log.d("Recommend", it.toString())
                recommendState = it
                loading = false
            }
                .onFailure {
                    loading = false
                    Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }
    Column(modifier = modifier.then(
        Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth()
    ), horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(value = trackId, onValueChange = { trackId = it }, label = {
            Text("Insert Spotify Track ID")
        })
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = {
            loading = true
        }) {
            Text(text = "Recommend")
        }
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)) {
            item {
                AnimatedVisibility(visible = loading) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            items(recommendState?.tracks ?: emptyList()) {
                SongItem(track = it, isPlaying = it.id == trackPlayingId) {uri->
                    Log.w("URI", uri.toString())
                    if (uri != null && it.id != trackPlayingId) {
                        trackPlayingId = it.id
                        Log.w("SongItem", "play")
                        player.stop()
                        player.setMediaItem(
                            MediaItem.fromUri(uri)
                        )
                        player.prepare()
                        player.play()
                    }
                    else if (it.id == trackPlayingId) {
                        trackPlayingId = ""
                        player.stop()
                    }
                    else {
                        player.stop()
                        Toast.makeText(
                            context, "This track don't have preview", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

}

@Composable
fun SongItem(track: RecommendModel.Track, isPlaying: Boolean = false,  onPlay: (String?) -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(
                RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            CoilImage(imageModel = {
                track.album?.images?.firstOrNull()?.url
            }, modifier = Modifier
                .size(40.dp)
                .clip(
                    RoundedCornerShape(8.dp)
                ))
            Spacer(modifier = Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.name, maxLines = 1, style = TextStyle(fontWeight = FontWeight.Bold),
                    fontSize = 15.sp
                )
                Text(text = track.artists?.joinToString { it.name } ?: "", maxLines = 1, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = {
            onPlay(track.previewUrl)
        }) {
            if (!isPlaying) {
                Icon(painter = painterResource(id = R.drawable.baseline_play_arrow_24), contentDescription = "", tint = Color.White)
            }
            else {
                Icon(painter = painterResource(id = R.drawable.baseline_pause_24), contentDescription = "")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    MusicRecommendTheme {

    }
}