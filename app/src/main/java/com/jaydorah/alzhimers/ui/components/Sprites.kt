package com.jaydorah.alzhimers.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val SPRITE_NAMES: List<String> = listOf(
    "abacus",
    "apple-pie",
    "avocado",
    "baguette",
    "ball",
    "ballons",
    "banana",
    "barbell",
    "barley",
    "barricade",
    "baseball",
    "baseball-glove",
    "basketball",
    "beach-ball",
    "bicycle",
    "billiard-ball",
    "blaster",
    "blocks",
    "bottle",
    "bowling",
    "broccoli",
    "brush",
    "bucket",
    "burger",
    "cake",
    "candy-cane",
    "capsicum",
    "car",
    "carrot",
    "cauliflower",
    "cheese-slice",
    "chess-piece",
    "chicken-leg",
    "chilli",
    "chocolate",
    "cleaning-spray",
    "coffee",
    "coffee-bean",
    "commode",
    "corn-dog",
    "crayons",
    "cricket",
    "croissant",
    "cutlery",
    "dart",
    "dart-board",
    "detergent",
    "dishwashing",
    "disposable",
    "donut",
    "drum",
    "duck",
    "dumbbell",
    "duster",
    "dusting",
    "dustpan",
    "eggplant",
    "faucet",
    "fencing",
    "first-prize",
    "fitness-watch",
    "food-package",
    "football-pitch",
    "football-trophy",
    "fried-fish",
    "frying-pan",
    "game-console",
    "game-controller",
    "garlic",
    "glass-cleaner",
    "golf-flag",
    "grocery",
    "ham",
    "handwash",
    "heart",
    "helmet",
    "honey-dipper",
    "hot-bath",
    "hotdog",
    "house-cleaning",
    "ice-cream",
    "ice-skate",
    "ironed",
    "ironing",
    "italian-pizza",
    "ketchup-bottle",
    "kettlebell",
    "keyboard",
    "lawnmower",
    "loaf",
    "lobster",
    "lollipop",
    "lunch-box",
    "maki",
    "maracas",
    "matryoshka",
    "monkey",
    "mushroom",
    "nutcracker",
    "olympic-flame",
    "open-book",
    "parachute",
    "pasta",
    "pinwheel",
    "pistachio",
    "plane",
    "plunger",
    "puzzle",
    "pyramid",
    "rabbit",
    "rattle",
    "robot",
    "rocket",
    "rocking-horse",
    "roller-skate",
    "rubik",
    "rugby",
    "sand-bucket",
    "sandwich",
    "sausage",
    "ship",
    "shuttlecock",
    "skate-shoes",
    "skateboard",
    "sled",
    "slingshot",
    "snow-globe",
    "snowman",
    "soap",
    "soup-ladle",
    "spinning-top",
    "sports",
    "star-medal",
    "steak",
    "steering",
    "stopwatch",
    "surfing-board",
    "swimming-glasses",
    "swiss-roll",
    "t-shirt",
    "table-tennis",
    "taco",
    "tea-pot",
    "teddy-bear",
    "tennis",
    "toilet-paper",
    "toothbrush",
    "towel",
    "train",
    "turkey",
    "ukelele",
    "uniform",
    "waffle",
    "washing-clothes",
    "washing-machine",
    "water-bottle",
    "water-gun",
    "weight",
    "whistle",
    "wiper",
    "xylophone",
)

fun spriteLabel(name: String): String =
    name.split('-').joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercaseChar() }
    }

private val spriteCache = mutableMapOf<String, ImageBitmap?>()

@Composable
fun SpriteImage(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp? = null,
) {
    val context = LocalContext.current
    val bitmap = remember(name) {
        spriteCache.getOrPut(name) {
            runCatching {
                context.assets.open("sprites/$name.png").use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.asImageBitmap()
        }
    }
    if (bitmap != null) {
        val m = if (size != null) modifier.size(size) else modifier
        Image(
            bitmap = bitmap,
            contentDescription = spriteLabel(name),
            contentScale = ContentScale.Fit,
            modifier = m,
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            androidx.compose.material3.Text(
                text = spriteLabel(name),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            )
        }
    }
}
