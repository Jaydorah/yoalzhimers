package com.jaydorah.alzhimers.games.shortterm

// Word banks.

val ADJECTIVES: List<String> = listOf(
    "adorable", "adventurous", "aggressive", "agreeable", "alert", "alive", "amused", "angry", "annoyed",
    "annoying", "anxious", "arrogant", "ashamed", "attractive", "average", "awful", "bad", "beautiful",
    "better", "bewildered", "black", "bloody", "blue", "blue-eyed", "blushing", "bored", "brainy", "brave",
    "breakable", "bright", "busy", "calm", "careful", "cautious", "charming", "cheerful", "clean", "clear",
    "clever", "cloudy", "clumsy", "colorful", "combative", "comfortable", "concerned", "condemned",
    "confused", "cooperative", "courageous", "crazy", "creepy", "crowded", "cruel", "curious", "cute",
    "dangerous", "dark", "dead", "defeated", "defiant", "delightful", "depressed", "determined", "different",
    "difficult", "disgusted", "distinct", "disturbed", "dizzy", "doubtful", "drab", "dull", "eager", "easy",
    "elated", "elegant", "embarrassed", "enchanting", "encouraging", "energetic", "enthusiastic", "envious",
    "evil", "excited", "expensive", "exuberant", "fair", "faithful", "famous", "fancy", "fantastic",
    "fierce", "filthy", "fine", "foolish", "fragile", "frail", "frantic", "friendly", "frightened", "funny",
    "gentle", "gifted", "glamorous", "gleaming", "glorious", "good", "gorgeous", "graceful", "grieving",
    "grotesque", "grumpy", "handsome", "happy", "healthy", "helpful", "helpless", "hilarious", "homeless",
    "homely", "horrible", "hungry", "hurt", "ill", "important", "impossible", "inexpensive", "innocent",
    "inquisitive", "itchy", "jealous", "jittery", "jolly", "joyous", "kind", "lazy", "light", "lively",
    "lonely", "long", "lovely", "lucky", "magnificent", "misty", "modern", "motionless", "muddy", "mushy",
    "mysterious", "nasty", "naughty", "nervous", "nice", "nutty", "obedient", "obnoxious", "odd",
    "old-fashioned", "open", "outrageous", "outstanding", "panicky", "perfect", "plain", "pleasant",
    "poised", "poor", "powerful", "precious", "prickly", "proud", "putrid", "puzzled", "quaint", "real",
    "relieved", "repulsive", "rich", "scary", "selfish", "shiny", "shy", "silly", "sleepy", "smiling",
    "smoggy", "sore", "sparkling", "splendid", "spotless", "stormy", "strange", "stupid", "successful",
    "super", "talented", "tame", "tasty", "tender", "tense", "terrible", "thankful", "thoughtful",
    "thoughtless", "tired", "tough", "troubled", "ugliest", "ugly", "uninterested", "unsightly", "unusual",
    "upset", "uptight", "vast", "victorious", "vivacious", "wandering", "weary", "wicked", "wide-eyed",
    "wild", "witty", "worried", "worrisome", "wrong", "zany", "zealous"
)

val NOUNS: List<String> = listOf(
    "time", "year", "people", "way", "day", "man", "thing", "woman", "life", "child", "world", "school",
    "state", "family", "student", "group", "country", "problem", "hand", "part", "place", "case", "week",
    "company", "system", "program", "question", "work", "government", "number", "night", "point", "home",
    "water", "room", "mother", "area", "money", "story", "fact", "month", "lot", "right", "study", "book",
    "eye", "job", "word", "business", "issue", "side", "kind", "head", "house", "service", "friend",
    "father", "power", "hour", "game", "line", "end", "member", "law", "car", "city", "community", "name",
    "president", "team", "minute", "idea", "kid", "body", "information", "back", "parent", "face", "others",
    "level", "office", "door", "health", "person", "art", "war", "history", "party", "result", "change",
    "morning", "reason", "research", "girl", "guy", "moment", "air", "teacher", "force", "education"
)

val VERBS: List<String> = listOf(
    "walk", "run", "jump", "sit", "stand", "fall", "climb", "crawl", "swim", "fly", "drive", "ride", "push",
    "pull", "lift", "carry", "throw", "catch", "kick", "hit", "touch", "grab", "hold", "drop", "open",
    "close", "turn", "bend", "stretch", "shake", "wave", "point", "cut", "tear", "break", "fix", "build",
    "draw", "write", "paint", "read", "look", "watch", "see", "listen", "hear", "smell", "taste", "eat",
    "drink", "cook", "bake", "clean", "wash", "dry", "sweep", "dig", "plant", "grow", "pick", "chase",
    "follow", "lead", "stop", "start", "wait", "sleep", "wake", "rest", "smile", "laugh", "cry", "shout",
    "talk", "whisper", "sing", "dance", "clap", "nod", "dress", "pack", "unpack", "lock", "unlock", "enter",
    "leave", "hide", "find", "search", "drag", "roll", "slide"
)

val FAMOUS_LIST: List<String> = listOf(
    "Jesus", "Muhammad", "Buddha", "Moses", "Abraham", "Stalin", "Napoleon", "Caesar", "Cleopatra",
    "Einstein", "Newton", "Darwin", "Galileo", "Tesla", "Edison", "Aristotle", "Plato", "Socrates",
    "Confucius", "Shakespeare", "Homer", "Dante", "Dickens", "Tolstoy", "Hemingway", "Orwell", "Rowling",
    "Tolkien", "Elvis", "Madonna", "Beyonce", "Drake", "Eminem", "Rihanna", "Adele", "Sinatra", "Mozart",
    "Beethoven", "Jordan", "Messi", "Ronaldo", "Pele", "Ali", "Tyson", "Serena", "Federer", "Apple",
    "Google", "Microsoft", "Amazon", "Facebook", "Instagram", "YouTube", "TikTok", "Netflix", "Disney",
    "Pixar", "Marvel", "Sony", "Samsung", "Intel", "Nvidia", "Nike", "Adidas", "Puma", "Reebok", "CocaCola",
    "Pepsi", "Starbucks", "McDonalds", "Subway", "Walmart", "Target", "Costco", "Ikea", "Uber", "Lyft",
    "Airbnb", "Tesla", "Toyota", "Ferrari", "Lamborghini", "Porsche", "Mercedes", "Earth", "Moon", "Mars",
    "Paris", "London", "Rome", "Athens", "Cairo", "Jerusalem", "Mecca", "America", "China", "India",
    "Russia", "Japan", "Germany", "France", "Italy", "Spain", "Brazil", "Africa", "Europe", "Asia",
    "Australia", "Antarctica", "Bread", "Rice", "Pizza", "Burger", "Pasta", "Steak", "Chicken", "Bacon",
    "Egg", "Cheese", "Milk", "Butter", "Sugar", "Salt", "Pepper", "Coffee", "Tea", "Water", "Wine", "Beer",
    "Coke", "Sprite", "RedBull", "Gatorade", "Phone", "iPhone", "Android", "Computer", "Laptop", "Internet",
    "Wifi", "Keyboard", "Mouse", "Screen", "Television", "Radio", "Camera", "Hammer", "Screwdriver",
    "Wrench", "Drill", "Saw", "Knife", "Fork", "Spoon", "Plate", "Cup", "Car", "Truck", "Bus", "Train",
    "Plane", "Jet", "Helicopter", "Rocket", "Ship", "Boat", "Robot", "Algorithm", "Software", "Hardware",
    "Code", "Data", "Cloud", "Bitcoin", "Blockchain", "Atom", "Molecule", "Energy", "Gravity", "Electricity",
    "Aspirin", "Tylenol", "Ibuprofen", "Advil", "Penicillin", "Insulin", "Alcohol", "Caffeine", "Hollywood",
    "Spotify", "Wikipedia", "Reddit", "Kindle", "Mario", "Zelda", "Pokemon", "Minecraft", "Fortnite",
    "Money", "Dollar", "Gold", "Silver", "Credit", "Debt", "Bank", "WallStreet", "Economy", "Government",
    "Democracy", "Capitalism", "Communism", "Socialism", "Love", "Hate", "Life", "Death", "Time", "Truth",
    "Power", "Freedom", "Justice", "Peace", "War", "Faith", "Hope", "Fear", "Destiny", "Family", "Mother",
    "Father", "Child", "Baby", "Friend", "Enemy", "Human", "Person", "People", "God", "Devil", "Angel",
    "Heaven", "Hell", "King", "Queen", "President", "Empire", "Crown", "Hero", "Legend", "Myth", "History",
    "World", "Universe", "Reality", "Nature", "Pikachu", "Sherlock"
)

val POKEMON_LIST: List<String> = listOf(
    "Bulbasaur", "Ivysaur", "Venusaur", "Charmander", "Charmeleon", "Charizard", "Squirtle", "Wartortle",
    "Blastoise", "Caterpie", "Metapod", "Butterfree", "Weedle", "Kakuna", "Beedrill", "Pidgey", "Pidgeotto",
    "Pidgeot", "Rattata", "Raticate", "Spearow", "Fearow", "Ekans", "Arbok", "Pikachu", "Raichu",
    "Sandshrew", "Sandslash", "Nidoran F", "Nidorina", "Nidoqueen", "Nidoran M", "Nidorino", "Nidoking",
    "Clefairy", "Clefable", "Vulpix", "Ninetales", "Jigglypuff", "Wigglytuff", "Zubat", "Golbat", "Oddish",
    "Gloom", "Vileplume", "Paras", "Parasect", "Venonat", "Venomoth", "Diglett", "Dugtrio", "Meowth",
    "Persian", "Psyduck", "Golduck", "Mankey", "Primeape", "Growlithe", "Arcanine", "Poliwag", "Poliwhirl",
    "Poliwrath", "Abra", "Kadabra", "Alakazam", "Machop", "Machoke", "Machamp", "Bellsprout", "Weepinbell",
    "Victreebel", "Tentacool", "Tentacruel", "Geodude", "Graveler", "Golem", "Ponyta", "Rapidash",
    "Slowpoke", "Slowbro", "Magnemite", "Magneton", "Farfetchd", "Doduo", "Dodrio", "Seel", "Dewgong",
    "Grimer", "Muk", "Shellder", "Cloyster", "Gastly", "Haunter", "Gengar", "Onix", "Drowzee", "Hypno",
    "Krabby", "Kingler", "Voltorb", "Electrode", "Exeggcute", "Exeggutor", "Cubone", "Marowak", "Hitmonlee",
    "Hitmonchan", "Lickitung", "Koffing", "Weezing", "Rhyhorn", "Rhydon", "Chansey", "Tangela", "Kangaskhan",
    "Horsea", "Seadra", "Goldeen", "Seaking", "Staryu", "Starmie", "Mr Mime", "Scyther", "Jynx",
    "Electabuzz", "Magmar", "Pinsir", "Tauros", "Magikarp", "Gyarados", "Lapras", "Ditto", "Eevee",
    "Vaporeon", "Jolteon", "Flareon", "Porygon", "Omanyte", "Omastar", "Kabuto", "Kabutops", "Aerodactyl",
    "Snorlax", "Articuno", "Zapdos", "Moltres", "Dratini", "Dragonair", "Dragonite", "Mewtwo", "Mew"
)

val NUMBERS_LIST: List<String> = listOf("0","1","2","3","4","5","6","7","8","9")

// Note: String.split("") would add empty strings at both ends, so build the list from the char range.
val LETTERS_LIST: List<String> = ('A'..'Z').map { it.toString() }
