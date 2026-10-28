# Expense Tracker — Production Rebuild in Jetpack Compose

*Master prompt for Gemini · Owner: Kishan · Written 25 Oct 2026 · Paste the whole document into Gemini, then follow Section 0.*

## 0. How to use this document

**For the owner:** paste everything into Gemini (Android Studio Gemini or AI Studio). Then send exactly: *"Read the whole document. Do Phase 0 only. Stop and wait for my approval."* After each phase, test on a real phone, then send *"Approved. Do Phase N."* Never let Gemini do two phases in one reply. Also attach your reference UI screenshot (the dark navy typography and colour board) to the first message.

**For Gemini — output contract (applies to every reply):**

1. Work in the phases of Section 19. One phase per reply. Stop at the end of the phase and wait.
2. Before editing, list the files you will create or change and why. Then output **complete files**, each under a header with its full path. Never use `// ...rest unchanged`. Never output partial snippets for files that exist.
3. Do not invent APIs, artifact names, or version numbers. If you are not sure an API exists or what its exact signature is, write `UNSURE:` and say what to verify. Use the latest stable versions that Android Studio or Google Maven show; do not guess from memory.
4. Do not use deprecated stacks: no LiveData, no kapt (use KSP), no `startActivityForResult`, no `onBackPressed()`, no Accompanist, no XML layouts, no `fallbackToDestructiveMigration()`.
5. After each phase report: files changed, how to verify manually (numbered steps), what you could NOT test, and a one-line git commit message.
6. If a requirement is unclear, list your assumption in one line and continue. Do not stop to ask unless it blocks the phase.
7. Prefer deleting code over adding code. No speculative features. No placeholder buttons. No fake data in release builds (Section 8).
8. Think for yourself. If something in this document is wrong, risky, or outdated, say so before implementing it.

## 1. Role

You are a senior Android engineer and product designer who ships premium, minimalist finance apps. You know Kotlin, Compose, Room, Hilt, Coroutines/Flow, WorkManager, Android permissions, privacy, accessibility, and Google Play policy. You are honest about what Android can and cannot do.

## 2. Context and decisions already made

**Legacy app** (reference only, do not modify): `github.com/Kishan9565/Expense-Tracker-App`. Kotlin + XML Views, 3 Activities plus Splash, Room v3 (transactions + 4 group-split tables), LiveData/MediatorLiveData, williamchart, about 2,500 lines of Kotlin. Features: add/delete income and expense, categories, month view, day/week/month totals, line chart, date-range report, group splitter (events, members, expenses, settlement). It was only ever run on the developer's own phone; **there is no real user data, so no data migration is needed**.

**Decision: start a new Jetpack Compose project** and port only the ideas. Reasons: the app is small; the owner wants a premium minimalist UI; there are no users to migrate; and the legacy problems are structural (Double money, Activity-per-screen navigation, fake chart data inside the ViewModel), so patching costs more than rebuilding.

**Constraints:** free tools only; offline-first; no backend; no accounts; **no INTERNET permission** (verify in the merged manifest); India-first (INR, UPI, `en-IN`); app must be Play-Store-ready; single developer, so keep the architecture simple.

**New applicationId:** the legacy `com.example.expensetracker` cannot be published (Play rejects `com.example.*`). Use a namespace the owner controls, e.g. `com.kishan9565.expensetracker` (ask the owner to confirm in Phase 0). It is permanent after the first upload.

## 3. Defects in the legacy app — never port these

| # | Legacy defect (file / symbol) | Required behavior in the new app |
| --- | --- | --- |
| 1 | `AppDatabase`: `fallbackToDestructiveMigration()`, `exportSchema=false` | Never destructive. `exportSchema=true`, real `Migration`s, migration tests |
| 2 | `TransactionViewModel.mergeDummyWithReal` + `DummyDataProvider` show fake spending in the chart | No fake data in release, ever (Section 8) |
| 3 | `targetSdk = 34` | compileSdk/targetSdk **36** (Play requires it for new apps and updates since 31 Aug 2026) |
| 4 | Money stored as `Double`, printed raw (`₹1500.0`) | `Long` paise everywhere; one formatter |
| 5 | "Reset Data" deletes everything with one tap | Typed confirmation ("DELETE") + offer CSV export first |
| 6 | `observeData()` mutates one `Calendar`, so month start became 1st at 23:59:59 | Date ranges use `java.time.LocalDate` epoch days; no mutable Calendar |
| 7 | `getDailyTotalsForRange` uses `strftime(..., 'unixepoch')` (UTC) and `GROUP BY date` on raw millis | Store `localDate` (epoch day) at entry time and group by it |
| 8 | Date picker locked to today (`minDate = maxDate = now`) | Any past date; future dates only for recurring reminders |
| 9 | Split: expense insert and "who pays whom" race; delete deferred to Snackbar dismiss; event-delete Undo loses expenses (CASCADE); equal split only | Section 13 |
| 10 | Activity per screen, `startActivity` + `finish()`, new `MainActivity` per Home tap | Single Activity + Navigation Compose |
| 11 | `observeForever` in ViewModel `init`, never removed | StateFlow + `collectAsStateWithLifecycle` |
| 12 | `isMinifyEnabled=false`, 1.4 MB `logo.png` used twice in splash | R8 on; vector or WebP assets; SplashScreen API |
| 13 | Gson only for an unused TypeConverter; Kotlin 1.9.24 vs kapt plugin 2.0.21 | No Gson; KSP; one Kotlin version |
| 14 | 88 hardcoded strings, many hardcoded colors, 2 `contentDescription` for 17 images | `strings.xml` for all text; theme tokens only; full accessibility |
| 15 | Amount may be 0 or negative; duplicate member names; event without members; income note mandatory but expense note not | Validation rules in Sections 6, 7, 13 |
| 16 | Event `budget` stored but never used; README promised edit, search, PDF that do not exist | Only ship features that work (Section 4) |
| 17 | Only template tests | Real tests (Section 17) |

## 4. Product principles and scope

**Principle:** the goal is not to remove every manual step. It is to make recording an expense *fast, accurate, transparent and private*. Suggest or import, show what was understood, user confirms, save locally. **Never silently create or mark a transaction as paid from uncertain data.**

**Design principles:** follow the owner's reference board (Section 10): dark navy fintech, tonal layers instead of shadows, one indigo accent, mint and coral only for money direction. Premium = restraint: large numerals, generous whitespace, thin dividers, small uppercase micro-labels, no gradients or decorative cards, 150–250 ms motion only, everything scannable in two seconds.

| Feature | Pri | Acceptance criteria |
| --- | --- | --- |
| Quick Add | P0 | Amount-first custom keypad. Common expense in ≤5 taps (amount → category chip → Save). Remembers last category and payment method. Recent categories first. Date defaults to today, any past date selectable. Voice and UPI entry points exist (P1) |
| Home dashboard | P0 | Month switcher (any year). Spent / Income / Balance for the selected month. 7-day bar chart from real data only. Recent transactions grouped by day. Real empty state |
| Activity list | P0 | All transactions grouped by day; search by note/category; filters (type, category, payment, date range); tap to edit; swipe to delete with Undo |
| Categories | P0 | Seeded defaults; add, rename, archive custom categories; archived ones stay on old transactions |
| Settings | P0 | Theme (Dark by default; Light and System available), reset with typed confirmation, CSV export, about/privacy |
| CSV export | P0 | Share/save a CSV via the system file picker (no storage permission) |
| CSV import | P1 | Preview, duplicate detection, user confirms |
| Budgets | P1 | Monthly limit per category, progress bars, local alert at 80% and 100% |
| Recurring reminders | P1 | Rent, subscriptions, bills. A reminder is NOT a transaction; user taps to confirm |
| Voice entry | P1 | Section 11 |
| UPI pay + QR scan + payees | P1 | Section 12 |
| Split bills v2 | P1 | Section 13 |
| Insights | P1 | Category breakdown, month-over-month, top categories. Only numbers that are exact |
| Biometric app lock, widget / quick tile, share-sheet import | P2 | Optional, only after P0/P1 are solid |

**Explicitly rejected:** cloud sync, login, SMS reading, notification-listener parsing, scraping banking apps, ads, analytics SDKs, crypto/stocks, AI chat. If the owner later wants one, warn about privacy, Play policy and reliability first.

## 5. Tech stack and structure

**Stack:** Kotlin, Jetpack Compose (BOM) + Material 3, Navigation Compose with type-safe `@Serializable` routes, Hilt + KSP, Room (+ KSP), DataStore (Preferences), WorkManager, Coroutines/Flow, `java.time`, `core-splashscreen`. Tests: JUnit4, `kotlinx-coroutines-test`, Turbine, Compose UI test, Room `MigrationTestHelper`. **minSdk 26**, compile/target SDK 36, JDK 17, Gradle Kotlin DSL + version catalog. Charts: custom `Canvas` (no chart library). Add no other dependency without listing it first with its reason and maintenance cost.

The owner's skill set (Compose/MVI/Koin/multi-module) is adapted, not copied: **single module**, feature packages, Hilt instead of Koin, MVI only for screens with real logic, a typed `Outcome` instead of `kotlin.Result`, fakes instead of mocks.

```
app/src/main/java/com/kishan9565/expensetracker/
  core/        money/ time/ ui/ (UiText, ObserveAsEvents) designsystem/
  data/        db/ (AppDatabase, entities, daos) repo/ prefs/
  domain/      model/ repository interfaces, pure functions (no android.* imports)
  feature/     home/ activity/ quickadd/ split/ payees/ voice/ budgets/ recurring/ settings/
  navigation/  Routes.kt, AppNavHost.kt
  ExpenseApp.kt  MainActivity.kt
```

Dependency rule: `feature → domain ← data`. `domain` never imports `data`, Android, or Compose.

## 6. Data layer (this is where most legacy bugs lived — be strict)

### 6.1 Money = `Long` paise

```kotlin
interface AppError
enum class AmountError : AppError { EMPTY, INVALID, ZERO, TOO_LARGE }

sealed interface Outcome<out D, out E : AppError> {
    data class Ok<out D>(val value: D) : Outcome<D, Nothing>
    data class Err<out E : AppError>(val error: E) : Outcome<Nothing, E>
}

object Money {
    private const val MAX_PAISE = 10_000_000_000L          // ₹10 crore
    private val AMOUNT = Regex("""\d{1,10}(\.\d{1,2})?""")

    fun parse(input: String): Outcome<Long, AmountError> {
        val s = input.trim().replace(",", "").removeSuffix(".")
        if (s.isEmpty()) return Outcome.Err(AmountError.EMPTY)
        if (!AMOUNT.matches(s)) return Outcome.Err(AmountError.INVALID)
        val paise = BigDecimal(s).movePointRight(2).longValueExact()
        return when {
            paise <= 0 -> Outcome.Err(AmountError.ZERO)
            paise > MAX_PAISE -> Outcome.Err(AmountError.TOO_LARGE)
            else -> Outcome.Ok(paise)
        }
    }

    /** 150000 -> "₹1,500"; 150050 -> "₹1,500.50". Indian digit grouping via en-IN. */
    fun format(paise: Long): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            currency = Currency.getInstance("INR")
            minimumFractionDigits = if (paise % 100 == 0L) 0 else 2
            maximumFractionDigits = 2
        }
        return nf.format(BigDecimal.valueOf(paise, 2))
    }
}
```

Rules: no `Double`/`Float` anywhere near money. Percentages for budgets are computed with `Long` math (`spent * 100 / limit`). Amounts are always positive; direction comes from `TxType`.

### 6.2 Dates — fixes the UTC / Calendar bugs

```kotlin
/** The calendar day in the user's own zone, as epoch day. Group and filter by THIS, never by millis. */
fun LocalDate.epochDay(): Long = toEpochDay()

/** Exact instant for an entry made on [this] day, keeping the current wall-clock time. */
fun LocalDate.toEntryMillis(zone: ZoneId, now: Instant): Long =
    atTime(now.atZone(zone).toLocalTime()).atZone(zone).toInstant().toEpochMilli()

/** Material3 DatePicker returns UTC-midnight millis for the chosen day. Convert with UTC, not the device zone. */
fun Long.pickerMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

fun YearMonth.epochRange(): LongRange = atDay(1).toEpochDay()..atEndOfMonth().toEpochDay()
```

Inject a `Clock` (or a `TimeProvider`) so tests control "today". Never call `LocalDate.now()` inside a repository.

### 6.3 Entities, DAO, database

```kotlin
enum class TxType { EXPENSE, INCOME }
enum class PaymentMethod { CASH, UPI, CARD, WALLET, OTHER }
enum class TxSource { MANUAL, VOICE, UPI, CSV, RECURRING }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val iconKey: String,              // maps to a vector in the design system
    val keywords: String = "",         // comma separated, used by voice parsing / suggestions
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = CategoryEntity::class, parentColumns = ["id"],
        childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("localDate"), Index("categoryId"), Index(value = ["type", "localDate"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    val amountPaise: Long,            // always > 0
    val categoryId: Long,
    val payment: PaymentMethod,
    val note: String = "",
    val occurredAtMillis: Long,       // exact instant
    val localDate: Long,              // epoch day in the user's zone at entry time
    val source: TxSource = TxSource.MANUAL,
    val payeeId: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

data class DayTotal(val day: Long, val totalPaise: Long)
data class CategoryTotal(val categoryId: Long, val totalPaise: Long)

@Dao
interface TransactionDao {
    @Insert suspend fun insert(t: TransactionEntity): Long
    @Update suspend fun update(t: TransactionEntity)
    @Delete suspend fun delete(t: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE localDate BETWEEN :from AND :to ORDER BY occurredAtMillis DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amountPaise), 0) FROM transactions WHERE type = :type AND localDate BETWEEN :from AND :to")
    fun observeTotal(type: TxType, from: Long, to: Long): Flow<Long>

    @Query("""SELECT localDate AS day, SUM(amountPaise) AS totalPaise FROM transactions
              WHERE type = 'EXPENSE' AND localDate BETWEEN :from AND :to
              GROUP BY localDate ORDER BY localDate""")
    fun observeDailyExpense(from: Long, to: Long): Flow<List<DayTotal>>

    @Query("""SELECT categoryId, SUM(amountPaise) AS totalPaise FROM transactions
              WHERE type = 'EXPENSE' AND localDate BETWEEN :from AND :to
              GROUP BY categoryId ORDER BY totalPaise DESC""")
    fun observeCategoryTotals(from: Long, to: Long): Flow<List<CategoryTotal>>
}
```

For the 7-day chart, the repository fills missing days with `0` (so a week with 3 spending days shows 3 real bars and 4 empty ones).

```kotlin
@Database(entities = [CategoryEntity::class, TransactionEntity::class /* + later phases */],
          version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() { abstract fun transactionDao(): TransactionDao /* ... */ }

// Module: ONE instance, provided by Hilt as @Singleton
Room.databaseBuilder(ctx, AppDatabase::class.java, "expenses.db")
    .addCallback(SeedDefaultCategories)      // default CATEGORIES only. Never fake transactions.
    .addMigrations(*ALL_MIGRATIONS)
    .build()                                  // NEVER fallbackToDestructiveMigration()
```

`build.gradle.kts`: use the Room Gradle plugin (`room { schemaDirectory("$projectDir/schemas") }`) and commit the `schemas/` folder. Every schema change needs a `Migration` and a migration test. Default categories — Expense: Food, Transport, Shopping, Bills, Entertainment, Health, Education, Other. Income: Salary, Business, Gift, Other.

Domain models are separate from entities. Mappers are small extension functions in `data/`. Repositories are interfaces in `domain/`; implementations are named for what is special about them (`RoomTransactionRepository`), never `Impl`.

## 7. Presentation conventions (MVI, adapted)

Each screen with logic has: `State` (one data class), `Action` (sealed interface of user intents), `Event` (one-shot: navigation, snackbar), a `@HiltViewModel` with `StateFlow<State>` and a `Channel` for events, a stateless `Screen(state, onAction)` composable, and a `Root` composable that wires the ViewModel. State is updated only with `_state.update { it.copy(...) }`. Composables contain no business logic. Collect with `collectAsStateWithLifecycle()`. All user-visible text goes through `UiText` (string resource or dynamic string).

```kotlin
@Composable
fun <T> ObserveAsEvents(flow: Flow<T>, onEvent: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(flow, lifecycleOwner.lifecycle) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) { flow.collect(onEvent) }
        }
    }
}
```

### Quick Add (reference implementation of the pattern)

```kotlin
data class QuickAddState(
    val amountText: String = "",
    val type: TxType = TxType.EXPENSE,
    val categoryId: Long? = null,
    val payment: PaymentMethod = PaymentMethod.UPI,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val categories: List<CategoryUi> = emptyList(),   // recent first
    val amountError: AmountError? = null,
    val isSaving: Boolean = false,
)

sealed interface QuickAddAction {
    data class OnKey(val key: Char) : QuickAddAction          // digits, '.', '⌫'
    data class OnCategory(val id: Long) : QuickAddAction
    data class OnPayment(val method: PaymentMethod) : QuickAddAction
    data class OnNote(val text: String) : QuickAddAction
    data class OnDate(val date: LocalDate) : QuickAddAction
    data class OnType(val type: TxType) : QuickAddAction
    data object OnSave : QuickAddAction
}

sealed interface QuickAddEvent {
    data object Saved : QuickAddEvent
    data class Snack(val message: UiText) : QuickAddEvent
}

/** Pure function — unit test it. */
fun reduceAmount(current: String, key: Char): String = when {
    key == '⌫' -> current.dropLast(1)
    key == '.' -> when {
        '.' in current -> current
        current.isEmpty() -> "0."
        else -> "$current."
    }
    key.isDigit() -> {
        val dot = current.indexOf('.')
        when {
            dot >= 0 && current.length - dot > 2 -> current   // max 2 decimals
            dot < 0 && current.length >= 9 -> current          // max 9 integer digits
            current == "0" -> key.toString()
            else -> current + key
        }
    }
    else -> current
}

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val prefs: UserPrefsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val _state = MutableStateFlow(QuickAddState(date = clock.today()))
    val state = _state.asStateFlow()
    private val _events = Channel<QuickAddEvent>()
    val events = _events.receiveAsFlow()

    init {   // remembered defaults: fewer taps next time
        viewModelScope.launch {
            val last = prefs.lastUsed.first()
            _state.update { it.copy(categoryId = last.categoryId, payment = last.payment) }
        }
    }

    fun onAction(a: QuickAddAction) = when (a) {
        is QuickAddAction.OnKey -> _state.update { it.copy(amountText = reduceAmount(it.amountText, a.key), amountError = null) }
        is QuickAddAction.OnCategory -> _state.update { it.copy(categoryId = a.id) }
        is QuickAddAction.OnPayment -> _state.update { it.copy(payment = a.method) }
        is QuickAddAction.OnNote -> _state.update { it.copy(note = a.text.take(120)) }
        is QuickAddAction.OnDate -> _state.update { it.copy(date = a.date) }
        is QuickAddAction.OnType -> _state.update { it.copy(type = a.type, categoryId = null) }
        QuickAddAction.OnSave -> save()
    }

    private fun save() {
        val s = _state.value
        if (s.isSaving) return                       // double-tap guard: prevents duplicate rows
        viewModelScope.launch {
            when (val amount = Money.parse(s.amountText)) {
                is Outcome.Err -> _state.update { it.copy(amountError = amount.error) }
                is Outcome.Ok -> {
                    val categoryId = s.categoryId
                        ?: return@launch _events.send(QuickAddEvent.Snack(UiText.Res(R.string.pick_category)))
                    _state.update { it.copy(isSaving = true) }
                    repo.add(NewTransaction(s.type, amount.value, categoryId, s.payment, s.note.trim(), s.date))
                    prefs.saveLastUsed(categoryId, s.payment)
                    _events.send(QuickAddEvent.Saved)
                }
            }
        }
    }
}

@Composable
fun QuickAddRoot(onDone: () -> Unit, vm: QuickAddViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    ObserveAsEvents(vm.events) { e ->
        when (e) {
            QuickAddEvent.Saved -> onDone()
            is QuickAddEvent.Snack -> snackbar.showSnackbar(e.message.asString(context))
        }
    }
    QuickAddScreen(state, vm::onAction, snackbar)
}
```

Validation in the UI: amount error text under the amount (not a Toast); Save button disabled while `isSaving`; the keypad cannot produce an invalid amount.

## 8. Demo / sample data policy (replaces the legacy dummy chart data)

The legacy app showed fake spending so the chart "looked good". That is a trust problem in a finance app. New rules:

1. **Release builds never display, generate, or bundle fake transactions.** Seeded *categories* are fine; seeded *amounts* are not.
2. **Empty state:** with no data, the chart area shows a calm placeholder: seven short neutral bars, the text "Your week will appear here" and a button "Add expense". No numbers.
3. **Partial data:** only real days have bars; other days are empty (zero), never filled with invented values.
4. **For screenshots, demos and manual testing**, use a debug-only seeder in the `debug` source set, with a no-op twin in `release` so release code cannot call real seeding:

```kotlin
// app/src/debug/java/.../debug/DemoData.kt
object DemoData {
    suspend fun seed(repo: TransactionRepository, categories: List<CategoryEntity>, today: LocalDate) {
        val rnd = Random(42)   // deterministic
        repeat(60) { i ->
            val day = today.minusDays((i / 2).toLong())
            repo.add(NewTransaction(TxType.EXPENSE, (rnd.nextInt(20, 900) * 100).toLong(),
                categories.random(rnd).id, PaymentMethod.values().random(rnd), "", day))
        }
    }
}
// app/src/release/java/.../debug/DemoData.kt
object DemoData { suspend fun seed(repo: TransactionRepository, categories: List<CategoryEntity>, today: LocalDate) = Unit }
```

The Settings screen shows "Load sample data" only when `BuildConfig.DEBUG` is true (enable `buildFeatures { buildConfig = true }`). Take Play Store screenshots on a debug build after seeding, then use "Reset all data". 5. `@Preview` sample data may live next to previews; R8 removes unused code. A unit test must assert that the release `DemoData.seed` does nothing.

## 9. Screens and navigation

Single `MainActivity`, `NavHost` with type-safe routes. Bottom bar with four items: **Home, Activity, Split, More**. A single "+" button on Home and Activity opens Quick Add as a modal bottom sheet. Back always goes to the previous screen (predictive back enabled). Tab state survives rotation and process death.

```
HOME                                   QUICK ADD (bottom sheet)
+------------------------------+       +------------------------------+
|  <  October 2026  >      (o) |       |  [Expense] [Income]          |
|                              |       |                              |
|  Spent this month            |       |          ₹ 1,250.5           |  <- 40sp, tabular
|  ₹12,450                     |       |   (error text appears here)  |
|  up 8% vs September          |       |  Food  Transport  Bills  ... |  <- chips, recent first
|                              |       |  Cash [UPI] Card Wallet      |
|  [ 7-day bars, tap = value ] |       |  Today v   Note (optional)   |
|   M  T  W  T  F  S  S       |       |  1 2 3 | 4 5 6 | 7 8 9       |
|                              |       |  .  0  <x                    |
|  Food   ####--  3.2k / 5k    |       |  (mic)  [ Save ]  [Pay UPI]  |
|                              |       +------------------------------+
|  Today                       |
|  Chai   UPI            -₹20  |
|  Salary Bank         +₹25,000|
+--------------[ + ]-----------+
  Home   Activity   Split   More
```

**Activity:** search field on top; filter chips (Type, Category, Payment, Dates); list grouped by day with sticky day headers and a daily total; tap opens the same Quick Add sheet in edit mode; swipe-to-delete shows an Undo snackbar (re-insert the same entity). **More:** Categories, Budgets, Recurring, Payees, Export/Import CSV, Theme, Privacy, About. In debug builds only: Developer (sample data). **States every screen must have:** loading (skeleton, no spinner flash under 300 ms), empty (one sentence + one button), error (what happened + Retry), and content.

## 10. Design system (premium + minimal)

**Visual direction — taken from the owner's reference screenshot** (a dark navy crypto-platform style board; the owner attaches it to Gemini too). Dark-first: near-black navy canvas, three stacked surface tones, one indigo-blue accent, mint green for income, coral red for expense, cool grey secondary text. **Dark is the default and is designed first.** The light theme is derived from the same roles (Settings: Dark / Light / System, default Dark).

**Tokens.** Values marked *reference* come straight from the screenshot; *derived* ones were added for light mode, dividers and contrast. Verify contrast ≥ 4.5:1 for body text and ≥ 3:1 for large text and icons. White text on the accent `#556EE6` is only about 4.4:1, so button labels must be SemiBold 16 sp or larger; use `accentText` for small accent-coloured text on dark.

| Role | Dark (default) | Light | Source |
| --- | --- | --- | --- |
| background | `#161A27` | `#F4F6FB` | dark: reference |
| surface (cards) | `#222736` | `#FFFFFF` | dark: reference |
| panel (inputs, chips, sheets, nested cards) | `#2A3042` | `#EBEEF6` | dark: reference |
| deepest (splash, scrim, area behind sheets) | `#090D29` | `#161A27` | dark: reference |
| accent / primary | `#556EE6` | `#4A5FD0` | dark: reference |
| accentText (accent used as text on dark) | `#8396F0` | `#4A5FD0` | derived |
| onSurface (primary text) | `#E5E5E7` | `#161A27` | dark: reference |
| onSurfaceVariant (secondary text) | `#96A0BE` | `#5B6580` | dark: reference |
| income (the reference's buy green) | `#28BE87` | `#17805B` | dark: reference |
| expense (the reference's sell red) | `#F46A6A` | `#C73E3E` | dark: reference |
| warning (budget at 80%) | `#E3A94B` | `#8F5B00` | derived |
| outline (1 dp dividers) | `#343B52` | `#DCE1EE` | derived |

**Typography (from the reference):** two families. *Display* — headings and the big amount — a wide, techy light face (the reference uses Conthrax Light). *Interface* — labels, lists, buttons, numbers — **Poppins** Regular / Medium / SemiBold (OFL licence, free for commercial use). **Licence warning:** Conthrax is a commercial typeface that, as far as the foundry states, is free for personal use only (verify). Do not ship it in a Play release without a licence. Until the owner decides, set `Display = Poppins Light` (a one-line swap later), or pick a free OFL display face with a similar feel after checking that it has the ₹ glyph (U+20B9) and clean digits. Bundle fonts in `res/font`.

**Scale:** amount-display 40/48 Light (display face), headline 22/28 Light (display face), title 16/24 Medium, body 14/20 Regular, label 12 Medium with +0.5 sp tracking, often uppercase, in the secondary text colour (like the tiny section labels in the reference). Money uses tabular numerals (`fontFeatureSettings = "tnum"`) **only if the font supports them — check that digits line up**. **Spacing:** 4-dp grid (4, 8, 12, 16, 24, 32). **Shapes:** small 8 dp (chips), medium 12 dp (cards, inputs, buttons), extra-large 24 dp (top corners of bottom sheets).

**Layers, not shadows:** background → surface → panel; no elevation, no gradients, thin dividers. **Accent** is used for primary buttons, selected chips, the active tab and chart bars. **Green/red** only on amounts and small deltas, always with a `+`/`−` sign. **Motion:** 150–250 ms fade/slide; respect the system "remove animations" setting. Dynamic color is off. Map the Material 3 `surfaceContainer*` roles to the same tones (low and normal → surface, high and highest → panel) so Material defaults do not add a different tint; `UNSURE:` check the parameter names in the Material 3 version in use.

**Accessibility rules:** touch targets ≥ 48 dp; every icon has a `contentDescription` or is marked decorative; income/expense are never shown by color alone (also a `+`/`−` sign and a label); the UI must work at 200% font scale and on a 360 dp-wide phone; the chart exposes a text summary via `semantics { contentDescription = ... }`; every screen is fully TalkBack-navigable.

```kotlin
enum class ThemeMode { SYSTEM, DARK, LIGHT }

@Immutable
data class FinanceColors(
    val income: Color, val expense: Color, val warning: Color,
    val accentText: Color,   // accent as TEXT on dark (about 6:1); the button fill stays `primary`
    val deepest: Color,      // splash, scrims, area behind sheets
)
val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFF556EE6), onPrimary = Color.White,
    background = Color(0xFF161A27), onBackground = Color(0xFFE5E5E7),
    surface = Color(0xFF222736), onSurface = Color(0xFFE5E5E7),
    surfaceVariant = Color(0xFF2A3042), onSurfaceVariant = Color(0xFF96A0BE),
    outline = Color(0xFF343B52), error = Color(0xFFF46A6A), onError = Color(0xFF161A27),
    surfaceTint = Color.Transparent,
)
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A5FD0), onPrimary = Color.White,
    background = Color(0xFFF4F6FB), onBackground = Color(0xFF161A27),
    surface = Color.White, onSurface = Color(0xFF161A27),
    surfaceVariant = Color(0xFFEBEEF6), onSurfaceVariant = Color(0xFF5B6580),
    outline = Color(0xFFDCE1EE), error = Color(0xFFC73E3E),
    surfaceTint = Color.Transparent,
)
private val DarkFinance = FinanceColors(
    income = Color(0xFF28BE87), expense = Color(0xFFF46A6A), warning = Color(0xFFE3A94B),
    accentText = Color(0xFF8396F0), deepest = Color(0xFF090D29),
)
private val LightFinance = FinanceColors(
    income = Color(0xFF17805B), expense = Color(0xFFC73E3E), warning = Color(0xFF8F5B00),
    accentText = Color(0xFF4A5FD0), deepest = Color(0xFF161A27),
)

@Composable
fun ExpenseTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    CompositionLocalProvider(LocalFinanceColors provides if (dark) DarkFinance else LightFinance) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

val Poppins = FontFamily(
    Font(R.font.poppins_light, FontWeight.Light),
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
)
val DisplayFont = Poppins   // swap for the licensed or free display face once decided

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 40.sp, lineHeight = 48.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Light, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontFamily = Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
)
val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AmountText(paise: Long, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.displayLarge) =
    Text(Money.format(paise), modifier, style = style.copy(fontFeatureSettings = "tnum"))
```

**Chart (custom Canvas, no library):**

```kotlin
data class DayBar(val label: String, val totalPaise: Long)

@Composable
fun WeekBars(days: List<DayBar>, modifier: Modifier = Modifier) {
    val max = (days.maxOfOrNull { it.totalPaise } ?: 0L).coerceAtLeast(1L)
    val bar = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val summary = days.joinToString { "${it.label} ${Money.format(it.totalPaise)}" }
    Canvas(modifier.fillMaxWidth().height(96.dp).semantics { contentDescription = summary }) {
        val gap = 12.dp.toPx()
        val w = (size.width - gap * (days.size - 1)) / days.size
        val r = CornerRadius(8.dp.toPx())
        days.forEachIndexed { i, d ->
            val x = i * (w + gap)
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), r)
            val h = size.height * (d.totalPaise.toFloat() / max)   // Float is fine for DRAWING only
            if (h > 0f) drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), r)
        }
    }
}
```

Show day labels under the bars with `Text`. Add tap-to-reveal of the value as a small label (P1 polish).

**Edge-to-edge and system UI:** call `enableEdgeToEdge()` in `MainActivity`; use `Scaffold` insets; `imePadding()` on forms; no `FLAG_LAYOUT_NO_LIMITS`. `android:enableOnBackInvokedCallback="true"` with `BackHandler` where needed. Use `installSplashScreen()` (no fixed 3-second delay; the splash shows only while the app is actually starting).

## 11. Voice entry ("say it, confirm it")

**Honest scope:** this is a convenience, not Whisper-quality dictation. Android's speech recognition may send audio to Google's servers unless an offline language pack is installed; mention this in the privacy policy and in the UI hint. Hindi/Hinglish accuracy varies by device. **The app never saves a voice-parsed entry without showing it for confirmation.**

**MVP uses the system speech dialog** (`RecognizerIntent`): no `RECORD_AUDIO` permission, no custom audio code. A custom inline mic UI with `SpeechRecognizer` is a later polish step.

```kotlin
@Composable
fun rememberSpeechLauncher(onText: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) onText(text)
    }
    return {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")      // offer hi-IN in Settings
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say e.g. \"chai 20 rupees\"")
        try { launcher.launch(i) } catch (e: ActivityNotFoundException) { onUnavailable() }
    }
}
```

**Parser** (pure Kotlin, in `domain/`, fully unit-tested). Start small and honest: digits plus the multipliers *sau / hundred / hazaar / thousand / k*; number words like "do sau pachaas" are NOT supported in v1 — the amount stays empty and the user types it.

```kotlin
data class CategoryKeywords(val id: Long, val type: TxType, val keywords: List<String>)
data class VoiceDraft(val amountPaise: Long?, val categoryId: Long?, val type: TxType, val note: String)

class VoiceEntryParser(private val categories: List<CategoryKeywords>) {
    private val amountRegex = Regex("""(\d[\d,]*(?:\.\d{1,2})?)\s*(sau|hundred|hazaar|hajar|thousand|k)?\b""")
    private val incomeWords = listOf("salary", "received", "credited", "income", "aaya", "mila")
    private val filler = Regex("""\b(rupees|rupaye|rupaya|rs|inr|ka|ki|ke|for|on|paid|spent)\b|₹""")

    fun parse(raw: String): VoiceDraft {
        val text = raw.lowercase(Locale.ROOT).trim()
        val m = amountRegex.find(text)
        val paise = m?.let {
            val base = BigDecimal(it.groupValues[1].replace(",", ""))
            val mult = when (it.groupValues[2]) { "sau", "hundred" -> 100; "hazaar", "hajar", "thousand", "k" -> 1000; else -> 1 }
            (Money.parse((base * BigDecimal(mult)).toPlainString()) as? Outcome.Ok)?.value
        }
        val type = if (incomeWords.any { it in text }) TxType.INCOME else TxType.EXPENSE
        val cat = categories.firstOrNull { c -> c.type == type && c.keywords.any { it in text } }
        val note = text.replace(amountRegex, " ").replace(filler, " ").replace(Regex("\\s+"), " ").trim()
        return VoiceDraft(paise, cat?.id, type, note)
    }
}
```

| Input | Expected draft |
| --- | --- |
| "chai 20 rupees" | 2000 paise, Food (keyword *chai*), note "chai" |
| "auto 150" | 15000 paise, Transport |
| "5 hazaar rent" | 500000 paise, Bills |
| "salary 25000 aaya" | 2500000 paise, INCOME, Salary |
| "do sau ka khana" | amount **null** (v1 limit), Food; user types the amount |
| Flow: mic button → system dialog → `OnVoiceText` action → fields in Quick Add are **pre-filled and highlighted** → user edits → Save. `source = TxSource.VOICE`. |  |

## 12. UPI pay ("like Navi", but honest)

**Reality check — Gemini must respect this:**

- The app **cannot be a UPI app** and cannot move money itself. That needs NPCI/bank/TPAP partnership and regulatory compliance. Out of scope.
- What *is* possible: launch the user's **installed UPI app** with a pre-filled payee and amount using the standard `upi://pay` deep link. The user enters their PIN **inside that UPI app**.
- Android gives **no reliable payment result**. Some UPI apps return a status extra, some return nothing; treat any returned status as a *hint only*. The app **asks the user** "Did the payment go through?" and records the expense only after they say yes.
- Some UPI apps may restrict or reject intent-initiated payments to personal VPAs. Test with at least two apps (e.g. PhonePe, Google Pay) and show a friendly fallback ("Copy UPI ID") if launching fails.
- **Never guess a UPI ID.** A phone number is not enough: `9876543210@ybl` and `9876543210@okaxis` can be different people. VPAs are saved only when the user types/pastes them or scans a QR, and are shown back for confirmation. Do not read the contacts list. (Optional: the system contact *picker* can prefill the name only.)
- Play Console: this is a personal-finance app that launches other apps; do not describe it as a payment service. Re-check the current "Financial features" declaration requirements before release.

```kotlin
@Entity(tableName = "payees", indices = [Index(value = ["vpa"], unique = true)])
data class PayeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val vpa: String,
    val lastUsedMillis: Long = 0,
)

@Entity(tableName = "pending_payments")      // survives process death; shown again on next app open
data class PendingPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payeeId: Long?, val vpa: String, val amountPaise: Long,
    val categoryId: Long?, val note: String, val startedAtMillis: Long,
)

data class UpiPayRequest(val vpa: String, val name: String, val amountPaise: Long?, val note: String?)

object UpiLink {
    private val VPA = Regex("""^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z][a-zA-Z0-9]{1,63}$""")
    fun isValidVpa(v: String) = VPA.matches(v.trim())

    fun build(r: UpiPayRequest): Uri = Uri.Builder().scheme("upi").authority("pay")
        .appendQueryParameter("pa", r.vpa.trim())
        .appendQueryParameter("pn", r.name.trim())
        .apply {
            r.amountPaise?.let { appendQueryParameter("am", BigDecimal.valueOf(it, 2).toPlainString()) }
            r.note?.takeIf { it.isNotBlank() }?.let { appendQueryParameter("tn", it.take(50)) }
        }
        .appendQueryParameter("cu", "INR")
        .build()

    /** Parses a scanned UPI QR ("upi://pay?pa=...&pn=...&am=..."). Returns null if invalid. */
    fun parse(raw: String): UpiPayRequest? {
        val uri = Uri.parse(raw.trim())
        if (!uri.scheme.equals("upi", true) || !uri.host.equals("pay", true)) return null
        val vpa = uri.getQueryParameter("pa")?.takeIf(::isValidVpa) ?: return null
        val amount = uri.getQueryParameter("am")?.let { (Money.parse(it) as? Outcome.Ok)?.value }
        return UpiPayRequest(vpa, uri.getQueryParameter("pn").orEmpty(), amount, uri.getQueryParameter("tn"))
    }
}

fun launchUpi(launcher: ActivityResultLauncher<Intent>, r: UpiPayRequest): Boolean {
    val view = Intent(Intent.ACTION_VIEW, UpiLink.build(r))
    return try { launcher.launch(Intent.createChooser(view, "Pay with")); true }
           catch (e: ActivityNotFoundException) { false }
}
```

Manifest (package visibility for resolving UPI apps):

```xml
<queries>
  <intent>
    <action android:name="android.intent.action.VIEW" />
    <data android:scheme="upi" android:host="pay" />
  </intent>
</queries>
```

**QR scan:** use Google's code scanner (`play-services-code-scanner`, `GmsBarcodeScanning.getClient(...)`, QR format only) — it needs **no CAMERA permission**. `UNSURE:` confirm the exact artifact name and options in the official docs before using it; if Play services are missing, fall back to manual VPA entry.

**Flow:** pick/scan payee → enter amount, category, note → *review card* ("You are about to open your UPI app to pay **Rahul** `rahul@okaxis` ₹250. Check the name in your UPI app before entering your PIN.") → save a `pending_payments` row → launch → on return show **"Did the payment go through?"** with *Yes, record it* / *No* / *I'll check later*. *Yes* creates an expense with `source = UPI`, `payment = UPI`, and deletes the pending row. *Later* keeps the row and re-prompts on the next app open. Do not log VPAs or amounts in release builds.

## 13. Split bills v2

**Verdict on the legacy feature:** the idea is good and differentiating, but the implementation is not production-grade (Double money, equal split only, a race, lossy Undo, deferred delete). Keep it as the **Split** tab and rebuild it on a sound model. Keep it separate from personal spending by default (no double counting); an optional P2 toggle "Include my share in my spending" may be added later.

**Model** (all money in paise; invariant: for every expense `sum(shares) == amountPaise`):

```kotlin
@Entity(tableName = "split_groups")
data class SplitGroupEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    val createdAtMillis: Long, val archivedAtMillis: Long? = null)

@Entity(tableName = "split_members",
    foreignKeys = [ForeignKey(entity = SplitGroupEntity::class, parentColumns = ["id"], childColumns = ["groupId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("groupId"), Index(value = ["groupId", "nameKey"], unique = true)])
data class SplitMemberEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val groupId: Long,
    val name: String, val nameKey: String /* trimmed + lowercased */, val isSelf: Boolean = false, val upiVpa: String? = null)

enum class SplitType { EQUAL, EXACT, PERCENT, SHARES }

@Entity(tableName = "split_expenses", indices = [Index("groupId")])
data class SplitExpenseEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val groupId: Long,
    val paidByMemberId: Long, val amountPaise: Long, val note: String, val splitType: SplitType,
    val localDate: Long, val occurredAtMillis: Long, val deletedAtMillis: Long? = null)   // soft delete => safe Undo

@Entity(tableName = "split_shares", primaryKeys = ["expenseId", "memberId"])
data class SplitShareEntity(val expenseId: Long, val memberId: Long, val sharePaise: Long)

@Entity(tableName = "split_settlements", indices = [Index("groupId")])
data class SettlementEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val groupId: Long,
    val fromMemberId: Long, val toMemberId: Long, val amountPaise: Long, val occurredAtMillis: Long, val viaUpi: Boolean)
```

**Allocation without losing a paisa** (largest remainder):

```kotlin
fun allocate(totalPaise: Long, weights: List<Long>): List<Long> {
    require(weights.isNotEmpty() && weights.all { it >= 0 } && weights.sum() > 0)
    val sum = weights.sum()
    val result = weights.map { totalPaise * it / sum }.toMutableList()
    var remainder = totalPaise - result.sum()
    val order = weights.indices.sortedByDescending { (totalPaise * weights[it]) % sum }
    var i = 0
    while (remainder > 0) { result[order[i++ % order.size]] += 1; remainder-- }
    return result
}   // allocate(10000, listOf(1,1,1)) == [3334, 3333, 3333]
```

EQUAL = weights of 1; PERCENT = weights are basis points; SHARES = integer weights; EXACT = user enters amounts and the UI blocks Save until they add up.

**Balances and "who pays whom"** (pure functions, no I/O, no Double):

```kotlin
data class Transfer(val from: Long, val to: Long, val amountPaise: Long)

/** net > 0 = is owed money, net < 0 = owes money. Sum of all nets must be 0. */
fun computeNet(memberIds: List<Long>, expenses: List<ExpenseWithShares>, settlements: List<SettlementEntity>): Map<Long, Long> {
    val net = memberIds.associateWith { 0L }.toMutableMap()
    for (e in expenses) {
        net.merge(e.paidByMemberId, e.amountPaise, Long::plus)
        for (s in e.shares) net.merge(s.memberId, -s.sharePaise, Long::plus)
    }
    for (s in settlements) { net.merge(s.fromMemberId, s.amountPaise, Long::plus); net.merge(s.toMemberId, -s.amountPaise, Long::plus) }
    return net
}

fun minimizeTransfers(net: Map<Long, Long>): List<Transfer> {   // greedy; good, not provably minimal
    require(net.values.sum() == 0L)
    val debtors = net.filterValues { it < 0 }.map { it.key to -it.value }.sortedByDescending { it.second }.toMutableList()
    val creditors = net.filterValues { it > 0 }.map { it.key to it.value }.sortedByDescending { it.second }.toMutableList()
    val out = mutableListOf<Transfer>()
    var i = 0; var j = 0
    while (i < debtors.size && j < creditors.size) {
        val (d, dAmt) = debtors[i]; val (c, cAmt) = creditors[j]
        val pay = minOf(dAmt, cAmt)
        out += Transfer(d, c, pay)
        debtors[i] = d to dAmt - pay; creditors[j] = c to cAmt - pay
        if (debtors[i].second == 0L) i++
        if (creditors[j].second == 0L) j++
    }
    return out
}
```

**Rules and UX:** at least 2 members; member names unique per group (case-insensitive); amount > 0; the payer must be a member; add-expense is **one Room `@Transaction`** (expense + shares); balances are derived from a single Flow, so there is no race. Deleting an expense sets `deletedAtMillis` and shows Undo; hard-delete happens later in cleanup. Deleting a group needs confirmation and offers a CSV export first. **Settle up:** each suggested transfer has a "Pay via UPI" button (Section 12; only if the receiver has a saved VPA) and a "Mark as paid (cash)" button; both create a `SettlementEntity` only after the user confirms. Show a plain-language summary ("Aman owes Riya ₹340") and a shareable text of the summary. The group `budget` field is optional; if kept, show progress against it, otherwise drop it.

## 14. Budgets and recurring reminders

**Budgets:** `BudgetEntity(categoryId, monthlyLimitPaise)`. Progress = `spent * 100 / limit` in `Long` math. Show a thin progress bar (primary under 80%, warning from 80%, expense color from 100%) and the text "₹1,800 left". Local notification once per month per threshold (store a `lastNotifiedMonth/threshold` flag so it never repeats). No budget set = no bar, no nag.

**Recurring:** rent, subscriptions, bills. **A reminder is not a transaction.** Nothing is inserted until the user taps *Add* (opens Quick Add pre-filled) or *Skip*.

```kotlin
enum class Frequency { WEEKLY, MONTHLY, YEARLY }

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, val amountPaise: Long?, val categoryId: Long, val payment: PaymentMethod,
    val frequency: Frequency, val dayOfMonth: Int?, val nextDueEpochDay: Long, val active: Boolean = true,
)

/** Month-end safe: day 31 in a 30-day month becomes the 30th. */
fun nextMonthly(from: LocalDate, dayOfMonth: Int): LocalDate {
    val ym = YearMonth.from(from).plusMonths(1)
    return ym.atDay(minOf(dayOfMonth, ym.lengthOfMonth()))
}

@HiltWorker
class DueRemindersWorker @AssistedInject constructor(
    @Assisted ctx: Context, @Assisted params: WorkerParameters,
    private val repo: RecurringRepository, private val notifier: ReminderNotifier, private val clock: AppClock,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        repo.dueOn(clock.today().toEpochDay()).forEach { notifier.showDue(it) }
        return Result.success()
    }
}
// Schedule once at app start: PeriodicWorkRequestBuilder<DueRemindersWorker>(1, TimeUnit.DAYS),
// enqueueUniquePeriodicWork("due-reminders", ExistingPeriodicWorkPolicy.KEEP, request)
```

WorkManager timing is approximate; never promise an exact time. On Android 13+ request `POST_NOTIFICATIONS` only when the user creates their first reminder, with a one-line reason; if denied, the app still works and the Recurring screen shows what is due.

## 15. CSV export/import and backup

**CSV format (UTF-8, header row):** `date,type,amount,category,payment,note` — `date` = `yyyy-MM-dd`, `type` = `EXPENSE|INCOME`, `amount` = decimal rupees (`1500.50`). Use the system file picker (`CreateDocument("text/csv")` / `OpenDocument`) — no storage permission.

```kotlin
object Csv {
    fun escape(field: String): String {
        // CSV-injection guard: spreadsheets treat a leading = + - @ as a formula
        val safe = if (field.firstOrNull() in listOf('=', '+', '-', '@')) "'$field" else field
        return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + safe.replace("\"", "\"\"") + "\"" else safe
    }
    fun row(vararg f: String) = f.joinToString(",") { escape(it) }
}
// Do not run `escape` on the amount column (a plain number, never starts with an operator).
```

**Import:** parse with a real CSV parser that handles quotes and embedded newlines; show a **preview** (count, date range, rows with problems and why); skip invalid rows with a report; detect duplicates with a hash of `(localDate, amountPaise, normalizedNote, category)`; nothing is written until the user taps *Import N rows*; the whole import is one DB transaction.

**Backup decision:** finance data should survive a phone change. Decide `allowBackup` explicitly: recommended = enabled, with `data_extraction_rules.xml` including the database domain; state it in the privacy policy; test restore with `adb shell bmgr`. CSV export is the user's portable backup.

## 16. Quality bars

- **Strings:** every user-visible string in `strings.xml`. Prepare for a Hindi `values-hi` later; no concatenated sentences.
- **Errors:** never show raw exception text. Use typed errors + `UiText`. Database writes that fail show a retryable message.
- **Duplicates:** Save buttons are guarded against double tap; imports are deduplicated.
- **Privacy:** no analytics, no crash SDK that uploads data (Play Console vitals is enough), no logging of amounts/notes/VPAs in release, `android:allowBackup` decided as above, optional `FLAG_SECURE` on payee/payment screens.
- **Performance:** lists use `LazyColumn` with stable `key = { it.id }`; no work in composition; indexes as in Section 6; cold start under 1 s on a mid-range phone.
- **Insets / small screens:** test 360 dp width, 200% font, landscape, and gesture + 3-button navigation.

## 17. Testing plan (with examples)

| Layer | Tests |
| --- | --- |
| Pure logic (JUnit4) | `Money.parse/format`, `reduceAmount`, date helpers (incl. IST midnight edge), `VoiceEntryParser`, `UpiLink`, `allocate`, `computeNet`, `minimizeTransfers`, `Csv.escape`, `nextMonthly` |
| ViewModel (fakes + Turbine) | Quick Add validation, double-save guard, remembered defaults, Home totals for a month, chart zero-fill |
| Room (instrumented) | DAO queries incl. `localDate` grouping; **every migration** with `MigrationTestHelper`; cascade rules; unique member names |
| Compose UI | Quick Add happy path; empty states; 200% font; TalkBack labels exist |
| Release safety | release `DemoData.seed` is a no-op; merged manifest has no INTERNET permission and no `FLAG_DEBUGGABLE`; R8 build launches and saves an expense |

```kotlin
class MoneyTest {
    @Test fun parse_ok() = assertEquals(Outcome.Ok(150050L), Money.parse("1,500.50"))
    @Test fun parse_zero() = assertEquals(Outcome.Err(AmountError.ZERO), Money.parse("0"))
    @Test fun parse_garbage() = assertEquals(Outcome.Err(AmountError.INVALID), Money.parse("12.345"))
    @Test fun format_whole() = assertEquals("₹1,50,000", Money.format(15_000_000L))   // Indian grouping
}

class SplitMathTest {
    @Test fun allocate_keeps_every_paisa() = assertEquals(listOf(3334L, 3333L, 3333L), allocate(10_000, listOf(1, 1, 1)))
    @Test fun transfers_settle_everyone() {
        val net = mapOf(1L to 30_000L, 2L to -10_000L, 3L to -20_000L)
        val t = minimizeTransfers(net)
        assertEquals(30_000L, t.sumOf { it.amountPaise })
        assertTrue(t.all { it.to == 1L })
    }
}

class QuickAddViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()          // sets Dispatchers.setMain(UnconfinedTestDispatcher())
    @Test fun zero_amount_shows_error_and_saves_nothing() = runTest {
        val repo = FakeTransactionRepository()
        val vm = QuickAddViewModel(repo, FakeUserPrefs(), FixedClock(LocalDate.of(2026, 10, 25)))
        vm.onAction(QuickAddAction.OnKey('0')); vm.onAction(QuickAddAction.OnSave)
        assertEquals(AmountError.ZERO, vm.state.value.amountError)
        assertTrue(repo.saved.isEmpty())
    }
}
```

## 18. Release hardening (Play-Store-ready)

1. `applicationId` final and not `com.example.*`; `versionCode` starts at 1 and always increases.
2. Release build: `isMinifyEnabled = true`, `isShrinkResources = true`; test the **release** APK/AAB on a real phone (R8 can break reflection; Room, Hilt and kotlinx.serialization ship their own rules).
3. Signing: use Play App Signing; keep the upload keystore and passwords backed up outside the repo; never commit them.
4. `targetSdk = 36`; verify edge-to-edge, predictive back and notification permission on an Android 16 device or emulator.
5. Lint with `abortOnError = true` for release; fix all accessibility and hardcoded-text warnings.
6. Merged manifest check: no INTERNET, no unused permissions, `exported` set correctly.
7. Privacy policy (a public page, e.g. GitHub Pages) stating: data stays on the device, no accounts, no tracking; mention speech recognition may use Google's service and that CSV files are the user's own.
8. Play Console: complete Data safety, the financial-features declaration if asked, content rating, and the target-audience questions. `UNSURE:` re-read the current Play requirements for new personal developer accounts (closed-testing tester count and duration) before planning the launch date.
9. Store assets: icon (adaptive + monochrome), 4–8 screenshots from a debug build seeded with Section 8's seeder, short description without payment-service claims; confirm every bundled font and icon allows commercial use (Poppins is OFL; Conthrax needs a paid licence).
10. Release checklist run on a clean phone: first launch, add/edit/delete, rotate, kill app, reboot, dark mode, 200% font, airplane mode, export CSV, reset data.

## 19. Phases (one phase per reply; stop after each)

| Phase | Build | Done when |
| --- | --- | --- |
| **0 Foundation** | New project, applicationId, version catalog, theme + tokens, `AppClock`, `Money`, date helpers, Room DB v1 with categories + transactions + schema export, Hilt, nav shell with 4 tabs, splash, **dependency list for approval** | Builds and runs; unit tests for `Money` and dates pass; app shows empty Home |
| **1 Core** | Quick Add (keypad, chips, remembered defaults, any past date), Home (month switcher, totals, 7-day chart, recent list), Activity (list, search, filters, edit, delete + Undo), categories management | A 5-tap expense works; chart is real or empty; rotation and process death are safe; ViewModel + DAO tests pass |
| **2 Safety and utility** | CSV export + import with preview, Settings (theme, typed-confirm reset), budgets, recurring reminders + notifications | Export → reset → import restores everything; reminders never create transactions by themselves |
| **3 Faster entry** | Voice entry (system dialog + parser), payees, QR scan, UPI launch + confirm + pending payments | Voice draft always needs confirmation; UPI flow records nothing until the user says yes; works with two UPI apps or fails gracefully |
| **4 Split v2** | Groups, members, expenses with all split types, balances, who-pays-whom, settlements (cash + UPI), soft delete + Undo | Property tests: shares always sum to the amount; nets always sum to 0; no data loss on delete/undo |
| **5 Polish and release** | Insights, animations, TalkBack pass, 200% font pass, empty/error states audit, R8, release checklist, store assets | Section 18 checklist fully green on a real device |

In Phase 0, before writing code, Gemini must output: (a) the confirmed applicationId, (b) the dependency list with one-line reasons, (c) any point in this document it disagrees with.

## 20. Do not build (unless the owner explicitly changes this)

Cloud sync or login; SMS or notification-listener transaction detection; scraping or automating banking/UPI apps; reading the contacts list; ads; analytics or tracking SDKs; an in-app AI chatbot; multi-currency; multiple bank accounts; investments; a custom chart library; a multi-module split of the project; Compose Multiplatform. Each of these adds privacy, policy, reliability or maintenance cost that this app does not need yet.

## 21. Reply template for every phase

```
PHASE N — <name>
1. Plan (files to create/change, one line each)
2. Disagreements / UNSURE items (or "none")
3. Full files (path header + complete code)
4. How to verify (numbered manual steps on a real phone)
5. Not tested / known gaps
6. Commit message
```

Stop here. Wait for "Approved. Do Phase N+1."
