package dev.krydo.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.krydo.mobile.data.AppContainer
import dev.krydo.mobile.data.AppSettings
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.domain.QrRequestParser
import dev.krydo.mobile.network.PresentationRequestDto
import dev.krydo.mobile.network.PresentationVerifyResultDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class ProveUiState(
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val request: PresentationRequestDto? = null,
    val selectedCredentialId: String? = null,
    val presentation: JsonElement? = null,
    val verifyResult: PresentationVerifyResultDto? = null,
)

data class SettingsUiState(
    val urlDraft: String = "",
    val holderDraft: String = "",
    val tokenDraft: String = "",
    val testing: Boolean = false,
    val testMessage: String? = null,
    val testOk: Boolean? = null,
)

data class CredentialsUiState(
    val loading: Boolean = false,
    val error: String? = null,
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
                holderAddress = "",
                authToken = "",
                onboardingDone = false,
            ),
        )

    val credentials: StateFlow<List<StoredCredential>> =
        container.credentialRepository.credentials

    private val _prove = MutableStateFlow(ProveUiState())
    val prove: StateFlow<ProveUiState> = _prove.asStateFlow()

    private val _settingsUi = MutableStateFlow(SettingsUiState())
    val settingsUi: StateFlow<SettingsUiState> = _settingsUi.asStateFlow()

    private val _credentialsUi = MutableStateFlow(CredentialsUiState())
    val credentialsUi: StateFlow<CredentialsUiState> = _credentialsUi.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { s ->
                _settingsUi.update { ui ->
                    ui.copy(
                        urlDraft = ui.urlDraft.ifBlank { s.apiBaseUrl },
                        holderDraft = ui.holderDraft.ifBlank { s.holderAddress },
                        tokenDraft = ui.tokenDraft.ifBlank { s.authToken },
                    )
                }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            container.settingsRepository.setOnboardingDone(true)
        }
    }

    /** Login gate: persist Stellar SIWS session + wallet account. */
    fun saveStellarLoginSession() {
        viewModelScope.launch {
            val ui = _settingsUi.value
            if (ui.holderDraft.isBlank() || ui.tokenDraft.isBlank()) {
                _settingsUi.update {
                    it.copy(testMessage = "Holder address and JWT are required", testOk = false)
                }
                return@launch
            }
            if (!ui.holderDraft.trim().startsWith("G")) {
                _settingsUi.update {
                    it.copy(testMessage = "Stellar address must start with G", testOk = false)
                }
                return@launch
            }
            container.settingsRepository.setApiBaseUrl(ui.urlDraft.ifBlank { "https://krydo.onrender.com" })
            container.settingsRepository.setHolderAddress(ui.holderDraft)
            container.settingsRepository.setAuthToken(ui.tokenDraft)
            container.settingsRepository.setOnboardingDone(true)
            container.walletSessionStore.upsert(
                dev.krydo.mobile.wallet.WalletAccount(
                    chainType = "STELLAR",
                    chainId = dev.krydo.mobile.wallet.WalletChains.STELLAR_TESTNET,
                    address = ui.holderDraft.trim(),
                    walletProvider = "siws-web",
                    label = "Stellar",
                ),
            )
            _settingsUi.update { it.copy(testMessage = "Stellar session saved", testOk = true) }
            refreshCredentials()
        }
    }

    fun disconnectWallet(chainId: String, address: String) {
        viewModelScope.launch {
            container.walletSessionStore.remove(chainId, address)
            val s = container.settingsRepository.settings.first()
            if (s.holderAddress.equals(address, ignoreCase = true)) {
                container.settingsRepository.clearSession()
                container.walletSessionStore.clear()
            }
        }
    }

    fun clearLoginSession() {
        viewModelScope.launch {
            container.settingsRepository.clearSession()
            container.walletSessionStore.clear()
            _settingsUi.update {
                it.copy(
                    holderDraft = "",
                    tokenDraft = "",
                    testMessage = "Signed out",
                    testOk = true,
                )
            }
        }
    }

    fun setProveInput(value: String) {
        _prove.update { it.copy(input = value, error = null) }
    }

    fun refreshCredentials() {
        viewModelScope.launch {
            _credentialsUi.update { it.copy(loading = true, error = null) }
            val result = container.credentialRepository.refresh()
            _credentialsUi.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
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
            // Ensure credentials are available for matching.
            container.credentialRepository.refresh()
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
            _prove.update { it.copy(loading = true, error = null) }
            runCatching {
                container.presentationRepository.createPresentation(request.id, credId)
            }.onSuccess { vp ->
                _prove.update {
                    it.copy(loading = false, presentation = vp)
                }
            }.onFailure { err ->
                _prove.update {
                    it.copy(loading = false, error = err.message ?: "Create failed")
                }
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
        _prove.update { ProveUiState(input = it.input) }
    }

    fun setUrlDraft(url: String) {
        _settingsUi.update { it.copy(urlDraft = url, testMessage = null, testOk = null) }
    }

    fun setHolderDraft(value: String) {
        _settingsUi.update { it.copy(holderDraft = value) }
    }

    fun setTokenDraft(value: String) {
        _settingsUi.update { it.copy(tokenDraft = value) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val ui = _settingsUi.value
            container.settingsRepository.setApiBaseUrl(ui.urlDraft)
            container.settingsRepository.setHolderAddress(ui.holderDraft)
            container.settingsRepository.setAuthToken(ui.tokenDraft)
            _settingsUi.update { it.copy(testMessage = "Saved", testOk = true) }
            refreshCredentials()
        }
    }

    fun testConnection() {
        viewModelScope.launch {
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
        _prove.value.presentation?.let {
            Json { prettyPrint = true }.encodeToString(JsonElement.serializer(), it)
        }

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
