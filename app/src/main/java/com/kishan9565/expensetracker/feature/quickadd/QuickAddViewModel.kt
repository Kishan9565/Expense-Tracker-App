package com.kishan9565.expensetracker.feature.quickadd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.R
import com.kishan9565.expensetracker.core.money.AmountError
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.money.Outcome
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.time.pickerMillisToLocalDate
import com.kishan9565.expensetracker.core.ui.UiText
import com.kishan9565.expensetracker.data.db.CategoryEntity
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TxSource
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.CategoryKeywords
import com.kishan9565.expensetracker.domain.model.NewTransaction
import com.kishan9565.expensetracker.domain.model.VoiceEntryParser
import com.kishan9565.expensetracker.domain.repository.CategoryRepository
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import com.kishan9565.expensetracker.domain.repository.UserPrefsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class QuickAddState(
    val existingId: Long? = null,
    val amountText: String = "",
    val type: TxType = TxType.EXPENSE,
    val categoryId: Long? = null,
    val payment: PaymentMethod = PaymentMethod.UPI,
    val note: String = "",
    val date: LocalDate,
    val source: TxSource = TxSource.MANUAL,
    val payeeId: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val amountError: AmountError? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface QuickAddAction {
    data class OnKey(val key: Char) : QuickAddAction
    data class OnCategory(val id: Long) : QuickAddAction
    data class OnPayment(val method: PaymentMethod) : QuickAddAction
    data class OnNote(val text: String) : QuickAddAction
    data class OnDate(val date: LocalDate) : QuickAddAction
    data class OnType(val type: TxType) : QuickAddAction
    data class OnVoiceText(val text: String) : QuickAddAction
    data object OnSave : QuickAddAction
}

sealed interface QuickAddEvent {
    data object Saved : QuickAddEvent
    data class Snack(val message: UiText) : QuickAddEvent
}

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
            dot >= 0 && current.length - dot > 2 -> current
            dot < 0 && current.length >= 9 -> current
            current == "0" -> key.toString()
            else -> current + key
        }
    }
    else -> current
}

@HiltViewModel
class QuickAddViewModel @Inject constructor(
    private val repo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val prefs: UserPrefsRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val _state = MutableStateFlow(QuickAddState(date = clock.today()))
    val state = _state.asStateFlow()
    
    private val _events = Channel<QuickAddEvent>()
    val events = _events.receiveAsFlow()
    
    private var initialLoadDone = false

    fun loadTransaction(id: Long?) {
        if (id == null) {
            if (!initialLoadDone) {
                // Initialize default state for a new transaction
                viewModelScope.launch {
                    val last = prefs.lastUsed.first()
                    _state.update { it.copy(categoryId = last.categoryId, payment = last.payment, existingId = null, amountText = "", note = "", date = clock.today(), amountError = null, isSaving = false) }
                    val cats = categoryRepo.observeAll().first()
                    _state.update { s -> s.copy(categories = cats.filter { it.type == s.type }) }
                }
                initialLoadDone = true
            } else {
                _state.update { it.copy(existingId = null, amountText = "", note = "", date = clock.today(), amountError = null, isSaving = false) }
            }
            return
        }
        
        // Editing existing transaction
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val existing = repo.getById(id)
            if (existing != null) {
                val cats = categoryRepo.observeAll().first()
                _state.update { s ->
                    s.copy(
                        existingId = existing.id,
                        amountText = BigDecimal(existing.amountPaise).movePointLeft(2).toPlainString(),
                        type = existing.type,
                        categoryId = existing.categoryId,
                        payment = existing.payment,
                        note = existing.note,
                        date = LocalDate.ofEpochDay(existing.localDate),
                        source = existing.source,
                        payeeId = existing.payeeId,
                        categories = cats.filter { it.type == existing.type },
                        isLoading = false,
                        isSaving = false,
                        amountError = null
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
                _events.send(QuickAddEvent.Snack(UiText.Dynamic("Transaction not found")))
            }
        }
    }

    fun onAction(a: QuickAddAction) = when (a) {
        is QuickAddAction.OnKey -> _state.update { it.copy(amountText = reduceAmount(it.amountText, a.key), amountError = null) }
        is QuickAddAction.OnCategory -> _state.update { it.copy(categoryId = a.id) }
        is QuickAddAction.OnPayment -> _state.update { it.copy(payment = a.method) }
        is QuickAddAction.OnNote -> _state.update { it.copy(note = a.text.take(120)) }
        is QuickAddAction.OnDate -> _state.update { it.copy(date = a.date) }
        is QuickAddAction.OnType -> {
            _state.update { it.copy(type = a.type, categoryId = null) }
            viewModelScope.launch {
                val cats = categoryRepo.observeAll().first()
                _state.update { s -> s.copy(categories = cats.filter { it.type == s.type }) }
            }
            Unit
        }
        is QuickAddAction.OnVoiceText -> handleVoiceText(a.text)
        QuickAddAction.OnSave -> save()
    }

    private fun handleVoiceText(text: String) {
        viewModelScope.launch {
            val cats = categoryRepo.observeAll().first()
            val keywords = cats.map { CategoryKeywords(it.id, it.type, it.name.lowercase().split(" ") + it.keywords.split(",").map { k -> k.trim() }.filter { k -> k.isNotEmpty() }) }
            val parser = VoiceEntryParser(keywords)
            val draft = parser.parse(text)
            
            _state.update { s -> 
                s.copy(
                    amountText = draft.amountPaise?.let { BigDecimal(it).movePointLeft(2).toPlainString() } ?: s.amountText,
                    categoryId = draft.categoryId ?: s.categoryId,
                    type = draft.type,
                    note = draft.note,
                    source = TxSource.VOICE,
                    categories = cats.filter { it.type == draft.type }
                )
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (s.isSaving || s.isLoading) return
        viewModelScope.launch {
            when (val amount = Money.parse(s.amountText)) {
                is Outcome.Err -> _state.update { it.copy(amountError = amount.error) }
                is Outcome.Ok -> {
                    val categoryId = s.categoryId
                        ?: return@launch _events.send(QuickAddEvent.Snack(UiText.Res(R.string.pick_category)))
                    
                    _state.update { it.copy(isSaving = true) }
                    
                    if (s.existingId != null) {
                        val existing = repo.getById(s.existingId)
                        if (existing != null) {
                            repo.update(existing.copy(
                                type = s.type,
                                amountPaise = amount.value,
                                categoryId = categoryId,
                                payment = s.payment,
                                note = s.note.trim(),
                                localDate = s.date.toEpochDay()
                            ))
                        }
                    } else {
                        repo.add(NewTransaction(s.type, amount.value, categoryId, s.payment, s.note.trim(), s.date, s.source, s.payeeId))
                    }
                    
                    prefs.saveLastUsed(categoryId, s.payment)
                    
                    // Reset on success so next open is a clean form
                    _state.update { 
                        it.copy(
                            isSaving = false,
                            existingId = null,
                            amountText = "",
                            note = "",
                            amountError = null,
                            date = clock.today()
                        )
                    }
                    _events.send(QuickAddEvent.Saved)
                }
            }
        }
    }
}
