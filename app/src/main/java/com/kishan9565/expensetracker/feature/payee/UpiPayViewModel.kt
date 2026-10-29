package com.kishan9565.expensetracker.feature.payee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.R
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.money.Outcome
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.core.ui.UiText
import com.kishan9565.expensetracker.data.db.PayeeDao
import com.kishan9565.expensetracker.data.db.PayeeEntity
import com.kishan9565.expensetracker.data.db.PendingDao
import com.kishan9565.expensetracker.data.db.PendingPaymentEntity
import com.kishan9565.expensetracker.data.db.TransactionEntity
import com.kishan9565.expensetracker.data.db.TransactionDao
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.TxSource
import com.kishan9565.expensetracker.data.db.TxType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kishan9565.expensetracker.domain.model.UpiPayRequest
import com.kishan9565.expensetracker.core.time.epochDay
import com.kishan9565.expensetracker.core.time.toEntryMillis

data class UpiPayState(
    val vpa: String = "",
    val name: String = "",
    val amountText: String = "",
    val note: String = "",
    val categoryId: Long? = null,
    val payees: List<PayeeEntity> = emptyList(),
    val isLoading: Boolean = false
)

sealed interface UpiPayAction {
    data class OnVpa(val vpa: String) : UpiPayAction
    data class OnName(val name: String) : UpiPayAction
    data class OnAmount(val text: String) : UpiPayAction
    data class OnNote(val note: String) : UpiPayAction
    data class OnCategory(val id: Long) : UpiPayAction
    data object Pay : UpiPayAction
    data class PaymentStatus(val successful: Boolean) : UpiPayAction
}

sealed interface UpiPayEvent {
    data class LaunchUpi(val request: UpiPayRequest) : UpiPayEvent
    data class Snack(val message: UiText) : UpiPayEvent
    data object Done : UpiPayEvent
}

object UpiLink {
    private val VPA = Regex("""^[a-zA-Z0-9.\-_]{2,256}@[a-zA-Z][a-zA-Z0-9]{1,63}$""")
    fun isValidVpa(v: String) = VPA.matches(v.trim())
}

@HiltViewModel
class UpiPayViewModel @Inject constructor(
    private val payeeDao: PayeeDao,
    private val pendingDao: PendingDao,
    private val txDao: TransactionDao,
    private val clock: AppClock
) : ViewModel() {

    private val _state = MutableStateFlow(UpiPayState())
    val state = _state.asStateFlow()
    private val _events = Channel<UpiPayEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            payeeDao.observeAll().collect { list ->
                _state.update { it.copy(payees = list) }
            }
        }
    }

    private suspend fun snack(res: Int) = _events.send(UpiPayEvent.Snack(UiText.Res(res)))

    fun onAction(a: UpiPayAction) {
        when (a) {
            is UpiPayAction.OnVpa -> _state.update { it.copy(vpa = a.vpa) }
            is UpiPayAction.OnName -> _state.update { it.copy(name = a.name) }
            is UpiPayAction.OnAmount -> _state.update { it.copy(amountText = a.text) }
            is UpiPayAction.OnNote -> _state.update { it.copy(note = a.note) }
            is UpiPayAction.OnCategory -> _state.update { it.copy(categoryId = a.id) }
            UpiPayAction.Pay -> {
                viewModelScope.launch {
                    val s = _state.value
                    if (!UpiLink.isValidVpa(s.vpa)) return@launch snack(R.string.upi_invalid_vpa)
                    if (s.name.isBlank()) return@launch snack(R.string.upi_name_required)
                    val amount = (Money.parse(s.amountText) as? Outcome.Ok)?.value ?: return@launch snack(R.string.invalid_amount)
                    val categoryId = s.categoryId ?: return@launch snack(R.string.pick_category)
                    
                    if (pendingDao.countOpen() > 0) return@launch snack(R.string.upi_pending_exists)
                    
                    val payeeId = payeeDao.upsert(s.name, s.vpa, clock.now().toEpochMilli())
                    
                    pendingDao.insert(
                        PendingPaymentEntity(
                            payeeId = payeeId,
                            vpa = s.vpa.trim().lowercase(),
                            amountPaise = amount,
                            categoryId = categoryId,
                            note = s.note.trim(),
                            startedAtMillis = clock.now().toEpochMilli()
                        )
                    )
                    
                    _events.send(UpiPayEvent.LaunchUpi(UpiPayRequest(s.vpa.trim(), s.name.trim(), amount, s.note)))
                }
            }
            is UpiPayAction.PaymentStatus -> {
                viewModelScope.launch {
                    val pending = pendingDao.observeAll().firstOrNull()?.firstOrNull()
                    if (pending != null) {
                        if (a.successful) {
                            val now = clock.now()
                            txDao.insert(
                                TransactionEntity(
                                    type = TxType.EXPENSE,
                                    amountPaise = pending.amountPaise,
                                    categoryId = pending.categoryId,
                                    payment = PaymentMethod.UPI,
                                    note = pending.note,
                                    localDate = clock.today().epochDay(),
                                    occurredAtMillis = clock.today().toEntryMillis(clock.zone(), now),
                                    source = TxSource.UPI,
                                    payeeId = pending.payeeId,
                                    createdAtMillis = now.toEpochMilli(),
                                    updatedAtMillis = now.toEpochMilli()
                                )
                            )
                            snack(R.string.saved)
                        }
                        pendingDao.delete(pending)
                    }
                    _events.send(UpiPayEvent.Done)
                }
            }
        }
    }
}
