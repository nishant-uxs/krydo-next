package dev.krydo.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.krydo.mobile.data.AppContainer
import dev.krydo.mobile.data.AppSettings
import dev.krydo.mobile.data.StoredCredential
import dev.krydo.mobile.domain.QrRequestParser
import dev.krydo.mobile.network.CredentialRequestDto
import dev.krydo.mobile.network.IssuerDto
import dev.krydo.mobile.network.PresentationRequestDto
import dev.krydo.mobile.network.PresentationVerifyResultDto
import dev.krydo.mobile.network.ZkProofDto
import dev.krydo.mobile.network.ZkShareLinks
import dev.krydo.mobile.network.ZkVerifyResultDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import dev.krydo.mobile.data.ClaimCategories

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
    /** false = Active tab, true = Archived tab */
    val showArchived: Boolean = false,
    val searchQuery: String = "",
    /** null = all categories; otherwise claimType key */
    val categoryFilter: String? = null,
    val sortMode: CredentialSortMode = CredentialSortMode.Newest,
)

enum class CredentialSortMode(val label: String) {
    Newest("Newest first"),
    Oldest("Oldest first"),
    Title("Title A–Z"),
    Issuer("Issuer A–Z"),
}

data class RequestUiState(
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
)

data class ZkUiState(
    val loading: Boolean = false,
    val generating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val lastProof: ZkProofDto? = null,
    val shareProofId: String? = null,
    /** One-shot preselect when navigating from credential detail. */
    val preferredCredentialId: String? = null,
)

data class VerifierUiState(
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val result: ZkVerifyResultDto? = null,
)

data class IssuerUiState(
    val loading: Boolean = false,
    val acting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
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
                walletRole = "user",
                knownCredentialCount = 0,
            ),
        )

    val credentials: StateFlow<List<StoredCredential>> =
        container.credentialRepository.credentials

    val archivedCredentialIds: StateFlow<Set<String>> =
        container.settingsRepository.archivedCredentialIds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val pinnedCredentialIds: StateFlow<Set<String>> =
        container.settingsRepository.pinnedCredentialIds
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Active (non-archived) credentials for home counts / prove matching. */
    val activeCredentials: StateFlow<List<StoredCredential>> =
        combine(credentials, archivedCredentialIds) { list, archived ->
            list.filter { it.id !in archived }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val issuers: StateFlow<List<IssuerDto>> =
        container.issuerRequestRepository.issuers

    val credentialRequests: StateFlow<List<CredentialRequestDto>> =
        container.issuerRequestRepository.requests

    val issuerInbox: StateFlow<List<CredentialRequestDto>> =
        container.issuerRequestRepository.inbox

    val zkProofs: StateFlow<List<ZkProofDto>> =
        container.zkProofRepository.proofs

    private val _prove = MutableStateFlow(ProveUiState())
    val prove: StateFlow<ProveUiState> = _prove.asStateFlow()

    private val _settingsUi = MutableStateFlow(SettingsUiState())
    val settingsUi: StateFlow<SettingsUiState> = _settingsUi.asStateFlow()

    private val _credentialsUi = MutableStateFlow(CredentialsUiState())
    val credentialsUi: StateFlow<CredentialsUiState> = _credentialsUi.asStateFlow()

    private val _requestUi = MutableStateFlow(RequestUiState())
    val requestUi: StateFlow<RequestUiState> = _requestUi.asStateFlow()

    private val _zkUi = MutableStateFlow(ZkUiState())
    val zkUi: StateFlow<ZkUiState> = _zkUi.asStateFlow()

    private val _guestVerifier = MutableStateFlow(false)
    val guestVerifier: StateFlow<Boolean> = _guestVerifier.asStateFlow()

    private val _verifierUi = MutableStateFlow(VerifierUiState())
    val verifierUi: StateFlow<VerifierUiState> = _verifierUi.asStateFlow()

    private val _issuerUi = MutableStateFlow(IssuerUiState())
    val issuerUi: StateFlow<IssuerUiState> = _issuerUi.asStateFlow()

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
        viewModelScope.launch {
            container.credentialRepository.loadOfflineCache()
        }
        // Poll for newly issued credentials while session is active.
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(45_000)
                val s = container.settingsRepository.settings.first()
                if (!s.hasSession) continue
                val before = s.knownCredentialCount
                val result = container.credentialRepository.refresh()
                val count = result.getOrNull()?.size ?: continue
                if (count > before) {
                    container.issueNotifier.notifyIssued(
                        title = "Credential issued",
                        body = "You have $count credential(s). Open Krydo to review.",
                    )
                }
                container.settingsRepository.setKnownCredentialCount(count)
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
            applyStellarSession(
                address = ui.holderDraft.trim(),
                token = ui.tokenDraft.trim(),
                provider = "siws-web",
            )
        }
    }

    /**
     * Deep-link from Freighter / Custom Tab after web SIWS:
     * krydo://auth?address=G…&token=…
     */
    fun applyMobileAuthDeepLink(address: String, token: String) {
        viewModelScope.launch {
            if (!address.startsWith("G") || token.isBlank()) {
                _settingsUi.update {
                    it.copy(testMessage = "Invalid Freighter handoff", testOk = false)
                }
                return@launch
            }
            applyStellarSession(address = address.trim(), token = token.trim(), provider = "freighter-mobile")
        }
    }

    private suspend fun applyStellarSession(address: String, token: String, provider: String) {
        container.settingsRepository.setApiBaseUrl(
            _settingsUi.value.urlDraft.ifBlank { "https://krydo.onrender.com" },
        )
        container.settingsRepository.setHolderAddress(address)
        container.settingsRepository.setAuthToken(token)
        container.settingsRepository.setWalletRole(
            dev.krydo.mobile.util.JwtPeek.role(token) ?: "user",
        )
        container.settingsRepository.setOnboardingDone(true)
        container.issueNotifier.ensureChannel()
        container.walletSessionStore.upsert(
            dev.krydo.mobile.wallet.WalletAccount(
                chainType = "STELLAR",
                chainId = dev.krydo.mobile.wallet.WalletChains.STELLAR_TESTNET,
                address = address,
                walletProvider = provider,
                label = "Stellar",
            ),
        )
        _settingsUi.update {
            it.copy(
                holderDraft = address,
                tokenDraft = token,
                testMessage = "Freighter connected",
                testOk = true,
            )
        }
        refreshCredentials()
        refreshRequestFlow()
        refreshZkProofs()
        refreshIssuerInbox()
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

    fun refreshRequestFlow() {
        viewModelScope.launch {
            _requestUi.update { it.copy(loading = true, error = null) }
            val issuersResult = container.issuerRequestRepository.refreshIssuers()
            val requestsResult = container.issuerRequestRepository.refreshMyRequests()
            val err = issuersResult.exceptionOrNull()?.message
                ?: requestsResult.exceptionOrNull()?.message
            _requestUi.update {
                it.copy(loading = false, error = err)
            }
            // Issued credentials may have landed while we were waiting.
            container.credentialRepository.refresh()
        }
    }

    fun submitCredentialRequest(
        claimType: String,
        issuer: IssuerDto?,
        message: String?,
    ) {
        viewModelScope.launch {
            _requestUi.update {
                it.copy(submitting = true, error = null, successMessage = null)
            }
            val result = container.issuerRequestRepository.requestCredential(
                claimType = claimType,
                issuer = issuer,
                message = message,
            )
            _requestUi.update {
                if (result.isSuccess) {
                    it.copy(
                        submitting = false,
                        successMessage = "Request sent to ${issuer?.name ?: "issuer"}. They’ll issue on the web inbox.",
                    )
                } else {
                    it.copy(
                        submitting = false,
                        error = result.exceptionOrNull()?.message ?: "Request failed",
                    )
                }
            }
        }
    }

    fun cancelCredentialRequest(id: String) {
        viewModelScope.launch {
            _requestUi.update { it.copy(error = null, successMessage = null) }
            val result = container.issuerRequestRepository.cancelRequest(id)
            if (result.isFailure) {
                _requestUi.update {
                    it.copy(error = result.exceptionOrNull()?.message ?: "Cancel failed")
                }
            } else {
                _requestUi.update { it.copy(successMessage = "Request cancelled") }
            }
        }
    }

    fun refreshZkProofs() {
        viewModelScope.launch {
            _zkUi.update { it.copy(loading = true, error = null) }
            val result = container.zkProofRepository.refresh()
            _zkUi.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun generateZkProof(
        credentialId: String,
        proofType: String,
        threshold: Double?,
        targetValue: String?,
    ) {
        viewModelScope.launch {
            _zkUi.update {
                it.copy(generating = true, error = null, successMessage = null)
            }
            val result = container.zkProofRepository.generate(
                credentialId = credentialId,
                proofType = proofType,
                threshold = threshold,
                targetValue = targetValue,
            )
            _zkUi.update {
                if (result.isSuccess) {
                    val proof = result.getOrThrow()
                    it.copy(
                        generating = false,
                        lastProof = proof,
                        shareProofId = proof.id,
                        successMessage = if (proof.verified) {
                            "ZK proof ready — share the QR / link with a verifier."
                        } else {
                            "Proof generated but claim does not satisfy the condition."
                        },
                    )
                } else {
                    it.copy(
                        generating = false,
                        error = result.exceptionOrNull()?.message ?: "Generate failed",
                    )
                }
            }
        }
    }

    fun openShareProof(proofId: String) {
        _zkUi.update { it.copy(shareProofId = proofId) }
    }

    fun clearShareProof() {
        _zkUi.update { it.copy(shareProofId = null) }
    }

    fun enterGuestVerifier() {
        _guestVerifier.value = true
        _verifierUi.value = VerifierUiState()
    }

    fun exitGuestVerifier() {
        _guestVerifier.value = false
        _verifierUi.value = VerifierUiState()
    }

    fun setVerifierInput(value: String) {
        _verifierUi.update { it.copy(input = value, error = null, result = null) }
    }

    fun verifyZkFromInput() {
        viewModelScope.launch {
            val proofId = ZkShareLinks.parseProofId(_verifierUi.value.input)
            if (proofId == null) {
                _verifierUi.update {
                    it.copy(error = "Paste a Krydo verify link or proof ID")
                }
                return@launch
            }
            _verifierUi.update { it.copy(loading = true, error = null, result = null) }
            val result = container.zkProofRepository.verifyPublic(proofId)
            _verifierUi.update {
                if (result.isSuccess) {
                    it.copy(loading = false, result = result.getOrThrow())
                } else {
                    it.copy(
                        loading = false,
                        error = result.exceptionOrNull()?.message ?: "Verify failed",
                    )
                }
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
                    val archived = container.settingsRepository.archivedCredentialIds.first()
                    val matches = container.credentialRepository.matchForClaim(
                        req.requestedCredentials.firstOrNull()?.claimType
                            ?: req.policy.claimType,
                    ).filter { it.id !in archived }
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

    fun refreshCredentials() {
        viewModelScope.launch {
            _credentialsUi.update { it.copy(loading = true, error = null) }
            val result = container.credentialRepository.refresh()
            result.getOrNull()?.let { list ->
                container.settingsRepository.setKnownCredentialCount(list.size)
            }
            _credentialsUi.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun setCredentialsShowArchived(showArchived: Boolean) {
        _credentialsUi.update { it.copy(showArchived = showArchived) }
    }

    fun setCredentialsSearchQuery(query: String) {
        _credentialsUi.update { it.copy(searchQuery = query) }
    }

    fun setCredentialsCategoryFilter(claimType: String?) {
        _credentialsUi.update { it.copy(categoryFilter = claimType) }
    }

    fun setCredentialsSortMode(sortMode: CredentialSortMode) {
        _credentialsUi.update { it.copy(sortMode = sortMode) }
    }

    fun archiveCredential(id: String) {
        viewModelScope.launch {
            container.settingsRepository.setCredentialArchived(id, true)
            // Archived credentials shouldn't stay pinned.
            container.settingsRepository.setCredentialPinned(id, false)
        }
    }

    fun unarchiveCredential(id: String) {
        viewModelScope.launch {
            container.settingsRepository.setCredentialArchived(id, false)
        }
    }

    fun pinCredential(id: String) {
        viewModelScope.launch {
            container.settingsRepository.setCredentialPinned(id, true)
        }
    }

    fun unpinCredential(id: String) {
        viewModelScope.launch {
            container.settingsRepository.setCredentialPinned(id, false)
        }
    }

    fun preferZkCredential(id: String) {
        _zkUi.update { it.copy(preferredCredentialId = id) }
    }

    fun consumeZkPreferredCredential(): String? {
        val id = _zkUi.value.preferredCredentialId ?: return null
        _zkUi.update { it.copy(preferredCredentialId = null) }
        return id
    }

    fun isCredentialArchived(id: String): Boolean =
        archivedCredentialIds.value.contains(id)

    /** Grouped sections for the credentials list (Active or Archived tab). */
    fun credentialSections(
        all: List<StoredCredential>,
        archivedIds: Set<String>,
        showArchived: Boolean,
        searchQuery: String = "",
        categoryFilter: String? = null,
        pinnedIds: Set<String> = emptySet(),
        sortMode: CredentialSortMode = CredentialSortMode.Newest,
    ): List<Pair<String, List<StoredCredential>>> {
        var filtered = if (showArchived) {
            all.filter { it.id in archivedIds }
        } else {
            all.filter { it.id !in archivedIds }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            filtered = filtered.filter { cred ->
                listOf(
                    cred.title,
                    cred.claimType,
                    cred.issuerName,
                    cred.displaySummary,
                    cred.claimValue.orEmpty(),
                    ClaimCategories.labelFor(cred.claimType),
                ).any { it.lowercase().contains(q) }
            }
        }
        if (!categoryFilter.isNullOrBlank()) {
            filtered = filtered.filter { it.claimType == categoryFilter }
        }
        fun sortList(list: List<StoredCredential>): List<StoredCredential> =
            when (sortMode) {
                CredentialSortMode.Newest -> list.sortedByDescending { it.issuedAt }
                CredentialSortMode.Oldest -> list.sortedBy { it.issuedAt }
                CredentialSortMode.Title -> list.sortedBy { it.title.lowercase() }
                CredentialSortMode.Issuer -> list.sortedBy { it.issuerName.lowercase() }
            }

        if (!showArchived) {
            val pinned = sortList(filtered.filter { it.id in pinnedIds })
            val rest = filtered.filter { it.id !in pinnedIds }
            val sections = mutableListOf<Pair<String, List<StoredCredential>>>()
            if (pinned.isNotEmpty()) sections += "Pinned" to pinned
            sections += ClaimCategories.groupByCategory(rest).map { (label, list) ->
                label to sortList(list)
            }
            return sections
        }
        return ClaimCategories.groupByCategory(filtered).map { (label, list) ->
            label to sortList(list)
        }
    }

    fun refreshIssuerInbox() {
        viewModelScope.launch {
            val s = container.settingsRepository.settings.first()
            if (!s.isIssuerOrRoot) return@launch
            _issuerUi.update { it.copy(loading = true, error = null) }
            val result = container.issuerRequestRepository.refreshIssuerInbox()
            _issuerUi.update {
                it.copy(
                    loading = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun rejectIssuerRequest(id: String, message: String?) {
        viewModelScope.launch {
            _issuerUi.update { it.copy(acting = true, error = null, successMessage = null) }
            val result = container.issuerRequestRepository.rejectRequest(id, message)
            _issuerUi.update {
                if (result.isSuccess) {
                    it.copy(acting = false, successMessage = "Request rejected")
                } else {
                    it.copy(acting = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
    }

    fun approveIssuerRequest(
        id: String,
        claimSummary: String,
        claimValue: String,
        responseMessage: String?,
    ) {
        viewModelScope.launch {
            _issuerUi.update { it.copy(acting = true, error = null, successMessage = null) }
            val result = container.issuerRequestRepository.approveAndIssue(
                id = id,
                claimSummary = claimSummary,
                claimValue = claimValue,
                responseMessage = responseMessage,
            )
            _issuerUi.update {
                if (result.isSuccess) {
                    it.copy(acting = false, successMessage = "Credential issued to holder")
                } else {
                    it.copy(acting = false, error = result.exceptionOrNull()?.message)
                }
            }
        }
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
