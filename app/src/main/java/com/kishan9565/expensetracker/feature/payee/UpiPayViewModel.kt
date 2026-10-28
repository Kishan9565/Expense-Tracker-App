package com.kishan9565.expensetracker.feature.payee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kishan9565.expensetracker.core.money.Money
import com.kishan9565.expensetracker.core.money.Outcome
import com.kishan9565.expensetracker.core.time.AppClock
import com.kishan9565.expensetracker.data.db.PayeeDao
import com.kishan9565.expensetracker.data.db.PayeeEntity
import com.kishan9565.expensetracker.data.db.PaymentMethod
import com.kishan9565.expensetracker.data.db.PendingPaymentDao
import com.kishan9565.expensetracker.data.db.PendingPaymentEntity
import com.kishan9565.expensetracker.data.db.TxSource
import com.kishan9565.expensetracker.data.db.TxType
import com.kishan9565.expensetracker.domain.model.NewTransaction
import com.kishan9565.expensetracker.domain.model.UpiPayRequest
import com.kishan9565.expensetracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpiPayState(
    val vpa: String = "",
    val name: String = "",
    val amountText: String = "",
    val note: String = "",
    val pendingPayment: PendingPaymentEntity? = null
)

sealed interface UpiPayAction {
    data class UpdateVpa(val v: String) : UpiPayAction
    data class UpdateName(val n: String) : UpiPayAction
    data class UpdateAmount(val a: String) : UpiPayAction
    data class UpdateNote(val n: String) : UpiPayAction
    data object Pay : UpiPayAction
    data object UpiAppReturned : UpiPayAction
    data class PaymentStatus(val successful: Boolean) : UpiPayAction
}

sealed interface UpiPayEvent {
    data class LaunchUpi(val request: UpiPayRequest) : UpiPayEvent
    data class Snack(val message: String) : UpiPayEvent
    data object PaymentRecorded : UpiPayEvent
}

@HiltViewModel
class UpiPayViewModel @Inject constructor(
    private val pendingDao: PendingPaymentDao,
    private val payeeDao: PayeeDao,
    private val txRepo: TransactionRepository,
    private val clock: AppClock
) : ViewModel() {

    private val _state = MutableStateFlow(UpiPayState())
    val state = _state.asStateFlow()
    
    private val _events = Channel<UpiPayEvent>()
    val events = _events.receiveAsFlow()

    private var currentPendingId: Long? = null

    init {
        // Load pending payment if app was killed while in UPI app
        viewModelScope.launch {
            val pending = pendingDao.observeAll().firstOrNull()?.firstOrNull()
            if (pending != null) {
                currentPendingId = pending.id
                _state.update { it.copy(pendingPayment = pending) }
            }
        }
    }

    fun onAction(action: UpiPayAction) {
        when (action) {
            is UpiPayAction.UpdateVpa -> _state.update { it.copy(vpa = action.v) }
            is UpiPayAction.UpdateName -> _state.update { it.copy(name = action.n) }
            is UpiPayAction.UpdateAmount -> _state.update { it.copy(amountText = action.a) }
            is UpiPayAction.UpdateNote -> _state.update { it.copy(note = action.n) }
            UpiPayAction.Pay -> {
                val s = _state.value
                val amt = Money.parse(s.amountText)
                if (amt is Outcome.Ok) {
                    viewModelScope.launch {
                        val payeeId = payeeDao.insert(PayeeEntity(displayName = s.name, vpa = s.vpa, lastUsedMillis = clock.now().toEpochMilli()))
                        val pending = PendingPaymentEntity(
                            payeeId = payeeId,
                            vpa = s.vpa,
                            amountPaise = amt.value,
                            categoryId = null, // simplified for MVP
                            note = s.note,
                            startedAtMillis = clock.now().toEpochMilli()
                        )
                        pendingDao.insert(pending)
                        
                        val req = UpiPayRequest(s.vpa, s.name, amt.value, s.note)
                        _events.send(UpiPayEvent.LaunchUpi(req))
                    }
                } else {
                    viewModelScope.launch { _events.send(UpiPayEvent.Snack("Invalid Amount")) }
                }
            }
            UpiPayAction.UpiAppReturned -> {
                viewModelScope.launch {
                    val pending = pendingDao.observeAll().firstOrNull()?.lastOrNull()
                    if (pending != null) {
                        currentPendingId = pending.id
                        _state.update { it.copy(pendingPayment = pending) }
                    }
                }
            }
            is UpiPayAction.PaymentStatus -> {
                val pending = _state.value.pendingPayment
                if (pending != null) {
                    viewModelScope.launch {
                        if (action.successful) {
                            txRepo.add(
                                NewTransaction(
                                    type = TxType.EXPENSE,
                                    amountPaise = pending.amountPaise,
                                    categoryId = 1, // simplified MVP default
                                    payment = PaymentMethod.UPI,
                                    note = pending.note,
                                    date = clock.today(),
                                    source = TxSource.UPI,
                                    payeeId = pending.payeeId
                                )
                            )
                            pendingDao.delete(pending)
                            // Reset state after successful save
                            _state.update { 
                                it.copy(
                                    pendingPayment = null,
                                    vpa = "",
                                    name = "",
                                    amountText = "",
                                    note = ""
                                ) 
                            }
                            _events.send(UpiPayEvent.PaymentRecorded)
                        } else {
                            // Hide the dialog, but keep the input state in case they want to retry
                            _state.update { it.copy(pendingPayment = null) }
                        }
                    }
                }
            }
        }
    }
}
