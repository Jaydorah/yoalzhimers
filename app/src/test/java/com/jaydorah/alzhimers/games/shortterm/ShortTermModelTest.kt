package com.jaydorah.alzhimers.games.shortterm

import com.jaydorah.alzhimers.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ShortTermModelTest {

    @Test
    fun defaultStateIsIdle() {
        val state = ShortTermState()
        assertEquals(ShortTermStage.Idle, state.stage)
        assertEquals(5, state.settings.itemCount)
        assertEquals(VocabType.FAMOUS, state.settings.vocabType)
        assertEquals(Difficulty.EASY, state.settings.difficulty)
    }

    @Test
    fun defaultSettingsClampRanges() {
        val s = ShortTermSettings(
            itemCount = 99,
            memorizeDuration = 0f,
            timePerTask = 99f,
            zenDuration = 0,
        )
        assertEquals(ShortTermSettings.MAX_ITEMS, s.effectiveItemCount)
        assertEquals(ShortTermSettings.MIN_MEMORIZE, s.effectiveMemorizeDuration, 0.001f)
        assertEquals(ShortTermSettings.MAX_TIME_PER_TASK, s.effectiveTimePerTask, 0.001f)
        assertEquals(ShortTermSettings.MIN_ZEN, s.effectiveZenDuration)
    }

    @Test
    fun impossibleForcesTenItems() {
        val s = ShortTermSettings(difficulty = Difficulty.IMPOSSIBLE, itemCount = 5)
        assertEquals(10, s.effectiveItemCount)
        assertTrue(s.isImpossible)
    }

    @Test
    fun setVocabTypeUpdatesVocab() {
        val s = ShortTermSettings().copy(vocabType = VocabType.NUMBERS)
        assertEquals(VocabType.NUMBERS, s.vocabType)
    }

    @Test
    fun runModeMapping() {
        assertEquals(RunMode.ZEN, ShortTermSettings(distractionsEnabled = false).runMode)
        assertEquals(
            RunMode.TASKS,
            ShortTermSettings(distractionsEnabled = true, distractionMode = DistractionMode.TASKS).runMode,
        )
        assertEquals(
            RunMode.TIME,
            ShortTermSettings(distractionsEnabled = true, distractionMode = DistractionMode.TIME).runMode,
        )
    }

    @Test
    fun famousPhraseHasNoDuplicates() {
        val phrase = generatePhrase(10, VocabType.FAMOUS, Random(1))
        assertEquals(10, phrase.words.size)
        assertEquals(10, phrase.words.toSet().size)
    }

    @Test
    fun randomPhraseHasNoDuplicates() {
        val phrase = generatePhrase(8, VocabType.RANDOM, Random(2))
        assertEquals(8, phrase.words.toSet().size)
    }

    @Test
    fun numbersPhraseAllowsDuplicates() {
        val phrase = generatePhrase(20, VocabType.NUMBERS, Random(3))
        assertEquals(20, phrase.words.size)
        phrase.words.forEach { assertTrue(it in NUMBERS_LIST) }
    }

    @Test
    fun lettersPhraseAllowsDuplicates() {
        val phrase = generatePhrase(20, VocabType.LETTERS, Random(4))
        assertEquals(20, phrase.words.size)
        phrase.words.forEach { assertTrue(it in LETTERS_LIST) }
    }

    @Test
    fun pokemonPhraseHasIds() {
        val phrase = generatePhrase(5, VocabType.POKEMON, Random(5))
        assertEquals(5, phrase.ids.size)
        phrase.ids.forEach { assertTrue(it in 1..151) }
        assertEquals(5, phrase.words.toSet().size)
    }

    @Test
    fun scoreVerticalPerfect() {
        val phrase = Phrase(listOf("apple", "banana"))
        assertEquals(100, scoreVertical(listOf("Apple", " banana "), phrase))
    }

    @Test
    fun scoreVerticalPartial() {
        val phrase = Phrase(listOf("apple", "banana", "cherry"))
        // 2 of 3 correct (case-insensitive) → round(66.67) = 67
        assertEquals(67, scoreVertical(listOf("apple", "wrong", "CHERRY"), phrase))
    }

    @Test
    fun scoreVerticalEmptyInputsIsZero() {
        val phrase = Phrase(listOf("apple"))
        assertEquals(0, scoreVertical(listOf(""), phrase))
    }

    @Test
    fun scorePokemonMatchById() {
        val phrase = Phrase(listOf("Pikachu", "Mew"), ids = listOf(25, 151))
        assertEquals(100, scorePokemon(listOf(25, 151), phrase))
        assertEquals(50, scorePokemon(listOf(25, 1), phrase))
        assertEquals(0, scorePokemon(listOf(null, null), phrase))
    }

    @Test
    fun resultVerdictTiers() {
        assertEquals(R.string.stm_verdict_legend, resultVerdict(100))
        assertEquals(R.string.stm_verdict_great, resultVerdict(60))
        assertEquals(R.string.stm_verdict_overflow, resultVerdict(59))
    }

    @Test
    fun difficultyTaskCounts() {
        assertEquals(10, Difficulty.EASY.tasks)
        assertEquals(20, Difficulty.MEDIUM.tasks)
        assertEquals(30, Difficulty.HARD.tasks)
        assertEquals(40, Difficulty.SUPER.tasks)
        assertEquals(50, Difficulty.ULTRA.tasks)
        assertEquals(100, Difficulty.IMPOSSIBLE.tasks)
    }

    @Test
    fun taskQueueWithinKindRange() {
        val queue = buildTaskQueue(30, Random(7))
        assertEquals(30, queue.size)
        queue.forEach { assertTrue(it in 0 until TASK_KIND_COUNT) }
    }

    @Test
    fun taskKindCountIs22() {
        assertEquals(22, TaskKind.entries.size)
        assertEquals(22, TASK_KIND_COUNT)
    }

    @Test
    fun historyCappedAtFifty() {
        val many = (1..60).map { i ->
            HistoryEntry(id = "$i", vocabType = "FAMOUS", wordCount = 5, mode = "TASKS", configValue = 10, score = i, timestamp = i.toLong())
        }
        val entry = HistoryEntry(id = "new", vocabType = "FAMOUS", wordCount = 5, mode = "TASKS", configValue = 10, score = 99, timestamp = 0L)
        val result = prependHistory(entry, many)
        assertEquals(50, result.size)
        assertEquals("new", result.first().id)
    }

    @Test
    fun historyConfigValueByMode() {
        val s = ShortTermSettings(difficulty = Difficulty.HARD, zenDuration = 45)
        assertEquals(30, historyConfigValue(RunMode.TASKS, s))
        assertEquals(45, historyConfigValue(RunMode.TIME, s))
        assertEquals(45, historyConfigValue(RunMode.ZEN, s))
    }

    @Test
    fun memorizeFractionDrainsToZero() {
        val s = ShortTermSettings(memorizeDuration = 2.0f)
        val state = ShortTermState(settings = s, memorizeElapsedMs = 1000L)
        assertEquals(0.5f, state.memorizeFraction, 0.01f)
        assertEquals(1.0f, state.memorizeSecondsLeft, 0.01f)
        val done = ShortTermState(settings = s, memorizeElapsedMs = 3000L)
        assertEquals(0f, done.memorizeFraction, 0.001f)
        assertEquals(0f, done.memorizeSecondsLeft, 0.001f)
    }

    @Test
    fun revealIndexOnlyWhenImpossibleMemorize() {
        val impossible = ShortTermState(
            stage = ShortTermStage.Memorize,
            settings = ShortTermSettings(difficulty = Difficulty.IMPOSSIBLE),
            stackStep = 3,
        )
        assertEquals(2, impossible.revealIndex)
        val normal = ShortTermState(stage = ShortTermStage.Memorize, stackStep = 3)
        assertEquals(null, normal.revealIndex)
    }
}

class ShortTermTasksTest {

    @Test
    fun digitAnswerCompletesOnExactMatch() {
        val (state, outcome) = DigitAnswerState().press("4", 4)
        assertTrue(outcome is TaskOutcome.Complete)
        assertEquals("4", state.input)
    }

    @Test
    fun digitAnswerFailsWhenLengthReached() {
        // answer 12 needs 2 digits; wrong second digit fails once length matches
        val (mid, midOut) = DigitAnswerState().press("1", 12)
        assertTrue(midOut is TaskOutcome.Incomplete)
        val (_, outcome) = mid.press("0", 12)
        assertTrue(outcome is TaskOutcome.Fail)
    }

    @Test
    fun digitAnswerIncompleteWhileTyping() {
        val (state, outcome) = DigitAnswerState().press("1", 12)
        assertTrue(outcome is TaskOutcome.Incomplete)
        assertEquals("1", state.input)
    }

    @Test
    fun mathSpecAnswerMatchesOperator() {
        val add = MathSpec(add = true, n1 = 10, n2 = 5)
        assertEquals(15, add.answer)
        val sub = MathSpec(add = false, n1 = 20, n2 = 5)
        assertEquals(15, sub.answer)
    }

    @Test
    fun generateMathInRanges() {
        repeat(50) {
            val m = generateMath(Random(it))
            assertTrue(m.n1 in 10..24)
            assertTrue(m.n2 in 1..9)
        }
    }

    @Test
    fun stroopInkIsFirstDifferentColor() {
        val spec = generateStroop(Random(1))
        assertTrue(spec.inkName != spec.word)
        // HTML: colors.find(c => c.n !== text.n) → first in [Red,Blue,Green] order
        val order = listOf("Red", "Blue", "Green")
        val expectedInk = order.first { it != spec.word }
        assertEquals(expectedInk, spec.inkName)
    }

    @Test
    fun stroopValidation() {
        val fontMode = StroopSpec(identifyFontColor = true, word = "Red", wordHex = "#ef4444", inkName = "Blue", inkHex = "#3b82f6")
        assertTrue(validateStroop(fontMode, "Blue") is TaskOutcome.Complete)
        assertTrue(validateStroop(fontMode, "Red") is TaskOutcome.Fail)
        val textMode = fontMode.copy(identifyFontColor = false)
        assertTrue(validateStroop(textMode, "Red") is TaskOutcome.Complete)
    }

    @Test
    fun letterTypeValidation() {
        val spec = LetterTypeSpec(wantVowel = true, letters = listOf("A", "B"))
        assertTrue(validateLetterType(spec, "A") is TaskOutcome.Complete)
        assertTrue(validateLetterType(spec, "B") is TaskOutcome.Fail)
    }

    @Test
    fun tapCountCompletesExactlyAtNeeded() {
        var spec = TapCountSpec(needed = 3, count = 0)
        var outcomes = mutableListOf<TaskOutcome>()
        repeat(5) {
            val (next, out) = tapCountStep(spec)
            spec = next
            outcomes += out
        }
        assertTrue(outcomes[0] is TaskOutcome.Incomplete)
        assertTrue(outcomes[1] is TaskOutcome.Incomplete)
        assertTrue(outcomes[2] is TaskOutcome.Complete)
        // overshoot stays incomplete (soft-lock, matches HTML)
        assertTrue(outcomes[3] is TaskOutcome.Incomplete)
    }

    @Test
    fun extremesValidation() {
        val spec = ExtremesSpec(numbers = listOf(5, 50), wantLargest = true)
        assertTrue(validateExtremes(spec, 50) is TaskOutcome.Complete)
        assertTrue(validateExtremes(spec, 5) is TaskOutcome.Fail)
    }

    @Test
    fun gridSearchHasOneTarget() {
        val spec = generateGridSearch(Random(9))
        assertEquals(1, spec.cells.count { it == spec.target })
        assertEquals(12, spec.cells.size)
    }

    @Test
    fun solveXProduct() {
        val spec = SolveXSpec(xVal = 5, factor = 3)
        assertEquals(15, spec.product)
        assertTrue(spec.xVal in 3..10)
        assertTrue(spec.factor in 2..5)
    }

    @Test
    fun sequenceAnswerFormula() {
        val spec = SequenceSpec(start = 4, step = 3)
        assertEquals(13, spec.answer)
        assertEquals("4, 7, 10, ?", spec.prompt)
    }

    @Test
    fun swatchTargetIsFirstAfterShuffle() {
        val spec = generateSwatch(Random(11))
        assertEquals(spec.order[0], spec.target)
        assertTrue(validateSwatch(spec, spec.target) is TaskOutcome.Complete)
    }

    @Test
    fun parityValidation() {
        val even = ParitySpec(wantEven = true, numbers = listOf(1, 3, 5, 8))
        assertTrue(validateParity(even, 8) is TaskOutcome.Complete)
        assertTrue(validateParity(even, 3) is TaskOutcome.Fail)
        val odd = ParitySpec(wantEven = false, numbers = listOf(2, 4, 6, 9))
        assertTrue(validateParity(odd, 9) is TaskOutcome.Complete)
    }

    @Test
    fun compareTiesAreNo() {
        val tie = CompareSpec(v1 = 5, v2 = 5, greater = true)
        assertFalse(tie.reality)
        assertTrue(validateCompare(tie, isYes = false) is TaskOutcome.Complete)
        assertTrue(validateCompare(tie, isYes = true) is TaskOutcome.Fail)
    }

    @Test
    fun dragThreshold() {
        val spec = DragSpec(DragCorner.TOP_LEFT)
        assertTrue(validateDrag(spec, 10f, 10f, 300f, 200f))
        assertFalse(validateDrag(spec, 100f, 10f, 300f, 200f))
        val br = DragSpec(DragCorner.BOTTOM_RIGHT)
        assertTrue(validateDrag(br, 290f, 190f, 300f, 200f))
        assertFalse(validateDrag(br, 10f, 10f, 300f, 200f))
    }

    @Test
    fun emojiValidation() {
        val fruit = EmojiSpec(wantFruit = true, choices = listOf("🍎", "🐶"))
        assertTrue(validateEmoji(fruit, "🍎") is TaskOutcome.Complete)
        assertTrue(validateEmoji(fruit, "🐶") is TaskOutcome.Fail)
        val animal = EmojiSpec(wantFruit = false, choices = listOf("🐶", "🍎"))
        assertTrue(validateEmoji(animal, "🐶") is TaskOutcome.Complete)
    }

    @Test
    fun missingDigitOptionsContainAnswer() {
        val spec = generateMissingDigit(Random(13))
        assertTrue(spec.answer in spec.options)
        assertEquals(3, spec.options.size)
        assertTrue(validateMissingDigit(spec, spec.answer) is TaskOutcome.Complete)
    }

    @Test
    fun circleSizeValidation() {
        val spec = CircleSizeSpec(CircleSizeTarget.BIGGEST, listOf(CircleId.S, CircleId.M, CircleId.L))
        assertTrue(validateCircleSize(spec, CircleId.L) is TaskOutcome.Complete)
        assertTrue(validateCircleSize(spec, CircleId.S) is TaskOutcome.Fail)
    }

    @Test
    fun orderedTapFollowsOrderThenFailsOnWrong() {
        var spec = OrderedTapSpec(order = listOf(2, 1, 3), buttons = listOf(1, 2, 3))
        val (s1, o1) = orderedTapStep(spec, 2)
        assertTrue(o1 is TaskOutcome.Incomplete)
        val (s2, o2) = orderedTapStep(s1, 1)
        assertTrue(o2 is TaskOutcome.Incomplete)
        val (_, o3) = orderedTapStep(s2, 3)
        assertTrue(o3 is TaskOutcome.Complete)

        val (_, bad) = orderedTapStep(spec, 1)
        assertTrue(bad is TaskOutcome.Fail)
    }

    @Test
    fun reactionDelayScaling() {
        val fast = generateReaction(1.0f, Random(1))
        assertTrue(fast.goDelayMs in 200..400)
        val mid = generateReaction(1.5f, Random(1))
        assertTrue(mid.goDelayMs in 250..450)
        val slow = generateReaction(5.0f, Random(1))
        assertTrue(slow.goDelayMs in 700..1300)
    }

    @Test
    fun arrowValidation() {
        val same = ArrowSpec(sameMode = true, left = true)
        assertTrue(validateArrow(same, choseLeft = true) is TaskOutcome.Complete)
        assertTrue(validateArrow(same, choseLeft = false) is TaskOutcome.Fail)
        val opp = ArrowSpec(sameMode = false, left = true)
        assertTrue(validateArrow(opp, choseLeft = false) is TaskOutcome.Complete)
    }

    @Test
    fun dragThresholdScalesWithDensity() {
        val spec = DragSpec(DragCorner.TOP_RIGHT)
        // 35dp at 3x density = 105px
        assertTrue(validateDrag(spec, 1000f, 100f, 1080f, 480f, threshold = 105f))
        assertFalse(validateDrag(spec, 1000f, 100f, 1080f, 480f))
    }

    @Test
    fun impossibleOnlyAppliesToTaskRuns() {
        val time = ShortTermSettings(
            difficulty = Difficulty.IMPOSSIBLE,
            itemCount = 5,
            distractionMode = DistractionMode.TIME,
        )
        assertFalse(time.isImpossible)
        assertEquals(5, time.effectiveItemCount)
        val zen = ShortTermSettings(difficulty = Difficulty.IMPOSSIBLE, itemCount = 4, distractionsEnabled = false)
        assertFalse(zen.isImpossible)
        assertEquals(4, zen.effectiveItemCount)
        val state = ShortTermState(stage = ShortTermStage.Memorize, settings = zen)
        assertEquals(null, state.revealIndex)
    }

    @Test
    fun currentTaskKindWrapsPastQueueEnd() {
        val state = ShortTermState(taskQueue = listOf(3, 7), currentTaskIndex = 3)
        assertEquals(7, state.currentTaskKind)
        assertEquals(0, ShortTermState(taskQueue = emptyList(), currentTaskIndex = 5).currentTaskKind)
    }

    @Test
    fun reactionEarlyTapIsNotTiedToGreen() {
        val outcome = validateReaction(go = false, clicked = true)
        assertEquals(R.string.stm_fail_too_soon, (outcome as TaskOutcome.Fail).reasonRes)
    }

    @Test
    fun lettersListHasNoEmptyEntries() {
        assertEquals(26, LETTERS_LIST.size)
        assertTrue(LETTERS_LIST.none { it.isEmpty() })
    }
}
