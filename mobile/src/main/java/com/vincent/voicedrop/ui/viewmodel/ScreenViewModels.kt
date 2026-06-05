package com.vincent.voicedrop.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vincent.voicedrop.data.Category
import com.vincent.voicedrop.data.Memo
import com.vincent.voicedrop.data.MemoDatabase
import com.vincent.voicedrop.data.Place
import com.vincent.voicedrop.data.PlaceType
import com.vincent.voicedrop.data.ShoppingGroupOrder
import com.vincent.voicedrop.data.ShoppingSection
import com.vincent.voicedrop.data.buildShoppingSections
import com.vincent.voicedrop.reminder.ReminderNotifications
import com.vincent.voicedrop.reminder.ReminderScheduler
import com.vincent.voicedrop.widget.ShoppingWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MemoListViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MemoDatabase.get(app).memoDao()
    private val placeDao = MemoDatabase.get(app).placeDao()
    val query = MutableStateFlow("")

    // Notities-tab: Ideeën + Overig (doorzoekbaar archief).
    private val noteCategories = listOf(Category.IDEEEN.name, Category.OVERIG.name)
    val notes: StateFlow<List<Memo>> = query
        .flatMapLatest { q ->
            if (q.isBlank()) dao.byCategories(noteCategories)
            else dao.searchInCategories(q, noteCategories)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Taken-tab: alle TAKEN; opsplitsing in Gepland/Open gebeurt in de UI.
    val tasks: StateFlow<List<Memo>> = dao.byCategory(Category.TAKEN.name)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val placesById: StateFlow<Map<Long, Place>> = placeDao.getAll()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setQuery(value: String) { query.value = value }

    fun delete(memo: Memo) = viewModelScope.launch {
        // Geplande herinnering-alarm + eventuele "voeg tijd toe"-notificatie opruimen,
        // anders vuurt de melding later alsnog terwijl de memo al weg is.
        val app = getApplication<Application>()
        ReminderScheduler.cancel(app, memo.id)
        ReminderNotifications.cancelAddTime(app, memo.id)
        dao.delete(memo)
    }

    fun togglePin(memo: Memo) = viewModelScope.launch {
        dao.setPinned(memo.id, if (memo.pinnedAt == null) System.currentTimeMillis() else null)
    }
}

private const val CHECKED_TTL_MS = 15 * 60 * 1000L

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MemoDatabase.get(app).memoDao()
    private val prefs = app.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    val sections: StateFlow<List<ShoppingSection>> =
        combine(
            dao.byCategory(Category.BOODSCHAPPEN.name),
            ShoppingGroupOrder.observe(prefs)
        ) { memos, order -> buildShoppingSections(memos, order) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setChecked(memo: Memo, checked: Boolean) = viewModelScope.launch {
        dao.setChecked(memo.id, if (checked) System.currentTimeMillis() else null)
        ShoppingWidget.refresh(getApplication())
    }

    fun cleanupAndRefresh() = viewModelScope.launch {
        dao.deleteCheckedBefore(System.currentTimeMillis() - CHECKED_TTL_MS)
        ShoppingWidget.refresh(getApplication())
    }
}

class ReminderTimeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MemoDatabase.get(app).memoDao()
    private val _memo = MutableStateFlow<Memo?>(null)
    val memo: StateFlow<Memo?> = _memo

    fun load(memoId: String) = viewModelScope.launch {
        _memo.value = dao.getById(memoId)
    }

    fun setRemindAt(memoId: String, millis: Long, onDone: () -> Unit) = viewModelScope.launch {
        dao.setRemindAt(memoId, millis)
        val app = getApplication<Application>()
        dao.getById(memoId)?.let { ReminderScheduler.schedule(app, it) }
        ReminderNotifications.cancelAddTime(app, memoId)
        onDone()
    }
}

class PlacesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MemoDatabase.get(app).placeDao()
    val places: StateFlow<List<Place>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(place: Place) = viewModelScope.launch {
        dao.delete(place)
        withContext(Dispatchers.IO) {
            com.vincent.voicedrop.location.GeofenceManager.registerAll(getApplication())
        }
    }
}

class MapPickerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = MemoDatabase.get(app).placeDao()

    private val _place = MutableStateFlow<Place?>(null)
    val place: StateFlow<Place?> = _place

    fun load(placeId: Long) = viewModelScope.launch {
        _place.value = dao.getById(placeId)
    }

    fun clear() { _place.value = null }

    fun save(
        existingId: Long?,
        name: String,
        lat: Double,
        lng: Double,
        address: String?,
        type: PlaceType,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        if (existingId != null) {
            dao.update(
                Place(id = existingId, name = name, lat = lat, lng = lng, address = address, type = type)
            )
        } else {
            dao.insert(Place(name = name, lat = lat, lng = lng, address = address, type = type))
        }
        withContext(Dispatchers.IO) {
            com.vincent.voicedrop.location.GeofenceManager.registerAll(getApplication())
        }
        onDone()
    }
}
