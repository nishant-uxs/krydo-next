package dev.krydo.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.krydo.mobile.data.AppContainer
import dev.krydo.mobile.data.AppSettings
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.domain.DemoPresentationBuilder
import dev.krydo.mobile.domain.QrRequestParser
import dev.krydo.mobile.network.PresentationRequestDto
import dev.krydo.mobile.network.PresentationVerifyResultDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

data class ProveUiState(
    val input: String = "krydo://present?request=req_demo001",
    val loading: Boolean = false,
    val error: String? = null,
    val request: PresentationRequestDto? = null,
    val selectedCredentialId: String? = null,
    val presentation: JsonElement? = null,
    val presentationLabel: String? = null,
    val verifyResult: PresentationVerifyResultDto? = null,
)

data class SettingsUiState(
    val urlDraft: String = "",
    val testing: Boolean = false,
    val testMessage: String? = null,
    val testOk: Boolean? = null,
)

class AppViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings(
                apiBaseUrl = "https://krydo.onrender.com",
                useMockData = true,
                onboardingDone = false,
            ),
        )

    val credentials: List<StoredCredential> = container.credentialRepository.list()

    private val _prove = MutableStateFlow(ProveUiState())
    val prove: StateFlow<ProveUiState> = _prove.asStateFlow()

    private val _settingsUi = MutableStateFlow(SettingsUiState())
    val settingsUi: StateFlow<SettingsUiState> = _settingsUi.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                _settingsUi.update { ui ->
                    if (ui.urlDraft.isBlank()) ui.copy(urlDraft = s.apiBaseUrl) else ui
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingDone(true)
        }
    }

    fun setProveInput(value: String) {
        _prove.update { it.copy(input = value, error = null) }
    }

    fun useDemoRequest() {
        _prove.update {
            it.copy(
                input = "krydo://present?request=req_demo001",
                error = null,
                request = null,
                presentation = null,
                verifyResult = null,
            )
        }
    }

    fun loadRequestFromInput(onLoaded: (String) -> Unit = {}) {
        viewModelScope.launch {
            val parsed = QrRequestParser.parse(_prove.value.input)
            parsed.onFailure { err ->
                _prove.update { it.copy(error = err.message, loading = false) }
                return@launch
            }
            val id = parsed.getOrThrow().requestId
            loadRequest(id, onLoaded)
        }
    }

    fun loadRequest(requestId: String, onLoaded: (String) -> Unit = {}) {
        viewModelScope.launch {
            _prove.update {
                it.copy(
                    loading = true,
                    error = null,
                    request = null,
                    presentation = null,
                    verifyResult = null,
                    selectedCredentialId = null,
                    input = if (it.input.contains(requestId)) it.input else "krydo://present?request=$requestId",
                )
            }
            runCatching { container.presentationRepository.getRequest(requestId) }
                .onSuccess { req ->
                    val matches = container.credentialRepository.matchForClaim(
                        req.requestedCredentials.firstOrNull()?.claimType
                            ?: req.policy.claimType,
                    )
                    _prove.update {
                        it.copy(
                            loading = false,
                            request = req,
                            selectedCredentialId = matches.firstOrNull()?.id,
                        )
                    }
                    onLoaded(requestId)
                }
                .onFailure { err ->
                    _prove.update {
                        it.copy(loading = false, error = err.message ?: "Failed to load request")
                    }
                }
        }
    }

    fun selectCredential(id: String) {
        _prove.update { it.copy(selectedCredentialId = id) }
    }

    fun createPresentation() {
        viewModelScope.launch {
            val state = _prove.value
            val request = state.request ?: return@launch
            val credId = state.selectedCredentialId ?: run {
                _prove.update { it.copy(error = "Select a credential") }
                return@launch
            }
            val cred = container.credentialRepository.get(credId) ?: run {
                _prove.update { it.copy(error = "Credential not found") }
                return@launch
            }
            _prove.update { it.copy(loading = true, error = null) }
            val (vp, label) = container.presentationRepository.buildDemoPresentation(request, cred)
            _prove.update {
                it.copy(
                    loading = false,
                    presentation = vp,
                    presentationLabel = label,
                )
            }
        }
    }

    fun verifyPresentation() {
        viewModelScope.launch {
            val vp = _prove.value.presentation ?: return@launch
            _prove.update { it.copy(loading = true, error = null) }
            runCatching { container.presentationRepository.verifyPresentation(vp) }
                .onSuccess { result ->
                    _prove.update { it.copy(loading = false, verifyResult = result) }
                }
                .onFailure { err ->
                    _prove.update {
                        it.copy(loading = false, error = err.message ?: "Verify failed")
                    }
                }
        }
    }

    fun clearProveFlow() {
        _prove.update {
            ProveUiState(input = it.input)
        }
    }

    fun setUrlDraft(url: String) {
        _settingsUi.update { it.copy(urlDraft = url, testMessage = null, testOk = null) }
    }

    fun saveApiUrl() {
        viewModelScope.launch {
            container.settingsRepository.setApiBaseUrl(_settingsUi.value.urlDraft)
            _settingsUi.update { it.copy(testMessage = "Saved API base URL", testOk = true) }
        }
    }

    fun setUseMock(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setUseMockData(enabled)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            // Persist draft first so test uses the URL the user typed.
            container.settingsRepository.setApiBaseUrl(_settingsUi.value.urlDraft)
            _settingsUi.update { it.copy(testing = true, testMessage = null, testOk = null) }
            val result = container.presentationRepository.testConnection()
            _settingsUi.update {
                it.copy(
                    testing = false,
                    testOk = result.isSuccess,
                    testMessage = result.getOrElse { e -> e.message ?: "Failed" }.toString(),
                )
            }
        }
    }

    fun presentationPretty(): String? =
        _prove.value.presentation?.let { DemoPresentationBuilder.toPrettyJson(it) }

    fun credential(id: String): StoredCredential? = container.credentialRepository.get(id)
}

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppViewModel::class.java)) {
            return AppViewModel(container) as T
        }
        throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    }
}
