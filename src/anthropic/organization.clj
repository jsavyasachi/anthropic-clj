(ns anthropic.organization
  "Clojure wrappers over the beta Organization administration APIs of the
  official Anthropic Java SDK: the organization itself plus users, API keys,
  external keys, invites, rate limits, service accounts, workspaces, workspace
  members/rate-limits/service-accounts, and identity federation (issuers, rules,
  and rule workspaces).

  These wrap beta endpoints that Anthropic may change. Build a request as a
  Clojure map, get a Clojure map back. Errors follow `anthropic.core`'s
  contract: API/IO failures are ex-info keyed `:anthropic/error` with the SDK
  exception as cause.

  Deeply nested response sub-objects (an API key's principal/scope, an external
  key's provider config/attachment, a federation issuer's JWKS, a rule's
  match/target/attributes) are returned as full-fidelity Clojure data. For the
  matching request inputs, scalar fields are accepted as idiomatic maps; the
  richest union inputs (inline JWKS keys, Azure external-key configs, match
  claims) also accept a ready SDK object as an escape hatch."
  (:require [anthropic.core]
            [clojure.string :as str])
  (:import (com.anthropic.client AnthropicClient)
           (com.anthropic.core JsonValue)
           (com.anthropic.errors AnthropicException)
           (com.anthropic.models.beta.organization.compliancesettings
                                             BetaComplianceSettings
                                             ComplianceSettingUpdateParams)
           (java.util Optional)))

(set! *warn-on-reflection* true)

;; ---- Shared helpers -------------------------------------------------------

(def ^:private throw-normalized! @#'anthropic.core/throw-normalized!)
(def ^:private json->clj @#'anthropic.core/json->clj)

(defmacro ^:private with-api-errors [& body]
  `(try ~@body
        (catch AnthropicException e# (throw-normalized! e#))))

(defn- missing-key! [k]
  (throw (ex-info (str "Missing required key " k)
                  {:anthropic/error :missing-key :key k})))

(defn- ->keyword [x]
  (-> x str str/lower-case (str/replace "_" "-") keyword))

(defn- unopt [^Optional o]
  (when (and o (.isPresent o)) (.get o)))

(defn- obj->clj
  "Full-fidelity Clojure data for any SDK model object, via its JSON form."
  [^Object o]
  (when (some? o) (json->clj (JsonValue/from o))))

(defn- kw<-
  "Wire enum string -> kebab keyword. Call sites type-hint the concrete enum
  before `.asString`, since the string accessor lives on each SDK enum type."
  [s]
  (when (some? s) (->keyword s)))

(defn- ->wire
  "Kebab keyword/string -> the SDK's lower_snake wire string."
  [value]
  (-> value name str/lower-case (str/replace "-" "_")))

(defn- check-enum! [value allowed k]
  (let [kw (if (keyword? value) value (->keyword value))]
    (when-not (contains? allowed kw)
      (throw (ex-info (str "Unknown " (name k) " " value)
                      {:anthropic/error :invalid-enum-value :key k :value value})))
    kw))

(def ^:private invite-roles #{:billing :claude-code-user :developer :managed :user})
(def ^:private user-roles #{:billing :claude-code-user :developer :managed :user})
(def ^:private sa-org-roles #{:admin :developer})
(def ^:private ws-roles
  #{:workspace-admin :workspace-billing :workspace-developer
    :workspace-restricted-developer :workspace-user})
(def ^:private no-billing-ws-roles
  #{:workspace-admin :workspace-developer :workspace-restricted-developer :workspace-user})
(def ^:private api-key-statuses #{:active :archived :expired :inactive})
(def ^:private api-key-update-statuses #{:active :archived :inactive})
(def ^:private invite-list-statuses #{:accepted :expired :pending})
(def ^:private group-types #{:batch :files :model-group :skills :token-count :web-search})
(def ^:private geos #{:us})

;; ---- Organization ---------------------------------------------------------

(defn- organization->map [^com.anthropic.models.beta.organization.BetaOrganization r]
  {:id (.id r) :name (.name r)})

(defn get-organization
  "Retrieve the caller's organization, as `{:id ... :name ...}`."
  [^AnthropicClient client]
  (with-api-errors
    (organization->map (-> (.beta client) (.organization) (.retrieve)))))

;; ---- Compliance settings --------------------------------------------------

(defn- ->compliance-state [state]
  (case (check-enum! state #{:enabled :disabled} :state)
    :enabled (com.anthropic.models.beta.organization.compliancesettings.BetaComplianceSettingsStateParam/ofEnabled
              (.build (com.anthropic.models.beta.organization.compliancesettings.BetaComplianceSettingsStateEnabledParam/builder)))
    :disabled (com.anthropic.models.beta.organization.compliancesettings.BetaComplianceSettingsStateParam/ofDisabled
               (.build (com.anthropic.models.beta.organization.compliancesettings.BetaComplianceSettingsStateDisabledParam/builder)))))

(defn- ->compliance-update-params ^ComplianceSettingUpdateParams [{:keys [state]}]
  (when-not state (missing-key! :state))
  (let [b (ComplianceSettingUpdateParams/builder)
        ^com.anthropic.models.beta.organization.compliancesettings.BetaComplianceSettingsStateParam state-value
        (->compliance-state state)]
    (.state b state-value)
    (.build b)))

(defn- compliance-settings->map [^BetaComplianceSettings settings]
  {:state (kw<- (.asString (.type (.state settings))))})

(defn get-compliance-settings
  "Retrieve organization compliance settings as `{:state :enabled|:disabled}`."
  [^AnthropicClient client]
  (with-api-errors
    (compliance-settings->map (-> (.beta client) (.organization)
                                  (.complianceSettings) (.retrieve)))))

(defn update-compliance-settings
  "Update organization compliance settings with `{:state :enabled|:disabled}`."
  [^AnthropicClient client changes]
  (with-api-errors
    (compliance-settings->map (-> (.beta client) (.organization)
                                  (.complianceSettings)
                                  (.update (->compliance-update-params changes))))))

;; ---- Users ----------------------------------------------------------------

(defn- org-user->map [^com.anthropic.models.beta.organization.users.BetaOrganizationUser r]
  {:id (.id r)
   :added-at (str (.addedAt r))
   :email (.email r)
   :name (.name r)
   :role (kw<- (.asString ^com.anthropic.models.beta.organization.BetaOrganizationRole (.role r)))})

(defn get-org-user
  "Retrieve one organization user by id."
  [^AnthropicClient client ^String user-id]
  (with-api-errors
    (org-user->map (-> (.beta client) (.organization) (.users) (.retrieve user-id)))))

(defn update-org-user
  "Update an organization user's `:role` (one of the organization roles)."
  [^AnthropicClient client ^String user-id {:keys [role]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.users.UserUpdateParams/builder)]
      (.userId b ^String user-id)
      (when role
        (.role b (com.anthropic.models.beta.organization.users.UserUpdateParams$Role/of
                  (->wire (check-enum! role user-roles :role)))))
      (org-user->map (-> (.beta client) (.organization) (.users) (.update (.build b)))))))

(defn list-org-users
  "List organization users. Options: `:limit`, `:after-id`, `:before-id`,
  `:email`, `:roles` (a seq of role strings)."
  ([^AnthropicClient client] (list-org-users client {}))
  ([^AnthropicClient client {:keys [limit after-id before-id email roles]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.users.UserListParams/builder)]
       (when limit (.limit b (long limit)))
       (when after-id (.afterId b ^String after-id))
       (when before-id (.beforeId b ^String before-id))
       (when email (.email b ^String email))
       (when (seq roles) (.roles b ^java.util.List (mapv str roles)))
       (mapv org-user->map (.autoPager (-> (.beta client) (.organization) (.users)
                                           (.list (.build b)))))))))

(defn remove-org-user
  "Remove a user from the organization. Returns the removal response as a map."
  [^AnthropicClient client ^String user-id]
  (with-api-errors
    (obj->clj (-> (.beta client) (.organization) (.users) (.remove user-id)))))

;; ---- API keys -------------------------------------------------------------

(defn- api-key->map [^com.anthropic.models.beta.organization.apikeys.BetaApiKey r]
  {:id (.id r)
   :created-at (str (.createdAt r))
   :created-by (obj->clj (unopt (.createdBy r)))
   :expires-at (some-> (.expiresAt r) unopt str)
   :name (.name r)
   :partial-key-hint (unopt (.partialKeyHint r))
   :principal (obj->clj (unopt (.principal r)))
   :scope (obj->clj (.scope r))
   :status (kw<- (.asString ^com.anthropic.models.beta.organization.apikeys.BetaApiKey$Status (.status r)))
   :workspace-id (unopt (.workspaceId r))})

(defn get-api-key
  "Retrieve one organization API key by id."
  [^AnthropicClient client ^String api-key-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.apikeys.ApiKeyRetrieveParams/builder)]
      (.apiKeyId b ^String api-key-id)
      (api-key->map (-> (.beta client) (.organization) (.apiKeys) (.retrieve (.build b)))))))

(defn update-api-key
  "Update an API key's `:name` and/or `:status` (`:active`, `:archived`,
  `:inactive`)."
  [^AnthropicClient client ^String api-key-id {:keys [name status]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.apikeys.ApiKeyUpdateParams/builder)]
      (.apiKeyId b ^String api-key-id)
      (when name (.name b ^String name))
      (when status
        (.status b (com.anthropic.models.beta.organization.apikeys.ApiKeyUpdateParams$Status/of
                    (->wire (check-enum! status api-key-update-statuses :status)))))
      (api-key->map (-> (.beta client) (.organization) (.apiKeys) (.update (.build b)))))))

(defn list-api-keys
  "List organization API keys. Options: `:limit`, `:after-id`, `:before-id`,
  `:status` (`:active`/`:archived`/`:expired`/`:inactive`), `:workspace-id`,
  `:created-by-user-id`."
  ([^AnthropicClient client] (list-api-keys client {}))
  ([^AnthropicClient client {:keys [limit after-id before-id status workspace-id created-by-user-id]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.apikeys.ApiKeyListParams/builder)]
       (when limit (.limit b (long limit)))
       (when after-id (.afterId b ^String after-id))
       (when before-id (.beforeId b ^String before-id))
       (when workspace-id (.workspaceId b ^String workspace-id))
       (when created-by-user-id (.createdByUserId b ^String created-by-user-id))
       (when status
         (.status b (com.anthropic.models.beta.organization.apikeys.ApiKeyListParams$Status/of
                     (->wire (check-enum! status api-key-statuses :status)))))
       (mapv api-key->map (.autoPager (-> (.beta client) (.organization) (.apiKeys)
                                          (.list (.build b)))))))))

;; ---- External keys --------------------------------------------------------

(defn- external-key->map [^com.anthropic.models.beta.organization.externalkeys.BetaExternalKey r]
  {:id (.id r)
   :attachment (obj->clj (.attachment r))
   :created-at (str (.createdAt r))
   :display-name (unopt (.displayName r))
   :geo (.geo r)
   :provider-config (obj->clj (.providerConfig r))
   :updated-at (str (.updatedAt r))})

(defn create-external-key
  "Create an external key. Provide a provider config as one of
  `:aws-provider-config`/`:gcp-provider-config` (a key name string) or
  `:provider-config` (a ready SDK config object). Options: `:display-name`,
  `:geo` (`:us`)."
  [^AnthropicClient client {:keys [display-name geo aws-provider-config gcp-provider-config provider-config]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyCreateParams/builder)]
      (cond
        aws-provider-config (.awsProviderConfig b ^String aws-provider-config)
        gcp-provider-config (.gcpProviderConfig b ^String gcp-provider-config)
        provider-config (.providerConfig b ^com.anthropic.models.beta.organization.externalkeys.ExternalKeyCreateParams$ProviderConfig provider-config))
      (when display-name (.displayName b ^String display-name))
      (when geo
        (.geo b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyCreateParams$Geo/of
                 (->wire (check-enum! geo geos :geo)))))
      (external-key->map (-> (.beta client) (.organization) (.externalKeys)
                             (.create (.build b)))))))

(defn get-external-key
  "Retrieve one external key by id."
  [^AnthropicClient client ^String external-key-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyRetrieveParams/builder)]
      (.externalKeyId b ^String external-key-id)
      (external-key->map (-> (.beta client) (.organization) (.externalKeys)
                             (.retrieve (.build b)))))))

(defn update-external-key
  "Update an external key's `:display-name`, provider config
  (`:aws-provider-config`/`:gcp-provider-config`/`:provider-config`), or `:geo`."
  [^AnthropicClient client ^String external-key-id
   {:keys [display-name geo aws-provider-config gcp-provider-config provider-config]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyUpdateParams/builder)]
      (.externalKeyId b ^String external-key-id)
      (cond
        aws-provider-config (.awsProviderConfig b ^String aws-provider-config)
        gcp-provider-config (.gcpProviderConfig b ^String gcp-provider-config)
        provider-config (.providerConfig b ^com.anthropic.models.beta.organization.externalkeys.ExternalKeyUpdateParams$ProviderConfig provider-config))
      (when display-name (.displayName b ^String display-name))
      (when geo
        (.geo b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyUpdateParams$Geo/of
                 (->wire (check-enum! geo geos :geo)))))
      (external-key->map (-> (.beta client) (.organization) (.externalKeys)
                             (.update (.build b)))))))

(defn list-external-keys
  "List external keys. Options: `:limit`, `:page`."
  ([^AnthropicClient client] (list-external-keys client {}))
  ([^AnthropicClient client {:keys [limit page]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyListParams/builder)]
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (mapv external-key->map (.autoPager (-> (.beta client) (.organization) (.externalKeys)
                                               (.list (.build b)))))))))

(defn delete-external-key
  "Delete an external key by id. Returns the delete response as a map."
  [^AnthropicClient client ^String external-key-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyDeleteParams/builder)]
      (.externalKeyId b ^String external-key-id)
      (obj->clj (-> (.beta client) (.organization) (.externalKeys) (.delete (.build b)))))))

(defn validate-external-key
  "Validate an external key by id. Returns the validation response as a map."
  [^AnthropicClient client ^String external-key-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.externalkeys.ExternalKeyValidateParams/builder)]
      (.externalKeyId b ^String external-key-id)
      (obj->clj (-> (.beta client) (.organization) (.externalKeys) (.validate (.build b)))))))

;; ---- Invites --------------------------------------------------------------

(defn- invite->map [^com.anthropic.models.beta.organization.invites.BetaOrganizationInvite r]
  {:id (.id r)
   :accepted-at (some-> (.acceptedAt r) unopt str)
   :email (.email r)
   :expires-at (str (.expiresAt r))
   :invited-at (str (.invitedAt r))
   :rbac-group-ids (vec (.rbacGroupIds r))
   :role (kw<- (.asString ^com.anthropic.models.beta.organization.BetaOrganizationRole (.role r)))
   :status (kw<- (.asString ^com.anthropic.models.beta.organization.invites.BetaOrganizationInvite$Status (.status r)))})

(defn create-invite
  "Create an organization invite. Requires `:email` and `:role` (one of the
  invite roles). Optional `:rbac-group-ids` (a seq of strings)."
  [^AnthropicClient client {:keys [email role rbac-group-ids]}]
  (with-api-errors
    (when-not email (missing-key! :email))
    (when-not role (missing-key! :role))
    (let [b (com.anthropic.models.beta.organization.invites.InviteCreateParams/builder)]
      (.email b ^String email)
      (.role b (com.anthropic.models.beta.organization.invites.InviteCreateParams$Role/of
                (->wire (check-enum! role invite-roles :role))))
      (when (seq rbac-group-ids) (.rbacGroupIds b ^java.util.List (mapv str rbac-group-ids)))
      (invite->map (-> (.beta client) (.organization) (.invites) (.create (.build b)))))))

(defn get-invite
  "Retrieve one invite by id."
  [^AnthropicClient client ^String invite-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.invites.InviteRetrieveParams/builder)]
      (.inviteId b ^String invite-id)
      (invite->map (-> (.beta client) (.organization) (.invites) (.retrieve (.build b)))))))

(defn list-invites
  "List invites. Options: `:limit`, `:after-id`, `:before-id`, `:email`,
  `:roles` (seq of role strings), `:statuses` (seq of `:accepted`/`:expired`/
  `:pending`)."
  ([^AnthropicClient client] (list-invites client {}))
  ([^AnthropicClient client {:keys [limit after-id before-id email roles statuses]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.invites.InviteListParams/builder)]
       (when limit (.limit b (long limit)))
       (when after-id (.afterId b ^String after-id))
       (when before-id (.beforeId b ^String before-id))
       (when email (.email b ^String email))
       (when (seq roles) (.roles b ^java.util.List (mapv str roles)))
       (when (seq statuses)
         (.statuses b ^java.util.List
                    (mapv #(com.anthropic.models.beta.organization.invites.InviteListParams$Status/of
                            (->wire (check-enum! % invite-list-statuses :status)))
                          statuses)))
       (mapv invite->map (.autoPager (-> (.beta client) (.organization) (.invites)
                                         (.list (.build b)))))))))

(defn delete-invite
  "Delete an invite by id. Returns the delete response as a map."
  [^AnthropicClient client ^String invite-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.invites.InviteDeleteParams/builder)]
      (.inviteId b ^String invite-id)
      (obj->clj (-> (.beta client) (.organization) (.invites) (.delete (.build b)))))))

;; ---- Rate limits (organization) -------------------------------------------

(defn list-rate-limits
  "List organization rate limits. Options: `:limit`, `:page`, `:model`,
  `:group-type` (`:batch`/`:files`/`:model-group`/`:skills`/`:token-count`/
  `:web-search`)."
  ([^AnthropicClient client] (list-rate-limits client {}))
  ([^AnthropicClient client {:keys [limit page model group-type]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.ratelimits.RateLimitListParams/builder)]
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (when model (.model b ^String model))
       (when group-type
         (.groupType b (com.anthropic.models.beta.organization.ratelimits.RateLimitListParams$GroupType/of
                        (->wire (check-enum! group-type group-types :group-type)))))
       (mapv obj->clj (.autoPager (-> (.beta client) (.organization) (.rateLimits)
                                      (.list (.build b)))))))))

;; ---- Service accounts -----------------------------------------------------

(defn- service-account->map
  [^com.anthropic.models.beta.organization.serviceaccounts.BetaServiceAccount r]
  {:id (.id r)
   :archived-at (some-> (.archivedAt r) unopt str)
   :archived-by-actor-id (unopt (.archivedByActorId r))
   :created-at (str (.createdAt r))
   :created-by-actor-id (unopt (.createdByActorId r))
   :description (unopt (.description r))
   :name (.name r)
   :organization-role (kw<- (.asString ^com.anthropic.models.beta.organization.serviceaccounts.BetaServiceAccount$OrganizationRole (.organizationRole r)))
   :updated-at (str (.updatedAt r))
   :updated-by-actor-id (unopt (.updatedByActorId r))})

(defn create-service-account
  "Create a service account. Requires `:name`. Optional `:description`,
  `:organization-role` (`:admin`/`:developer`)."
  [^AnthropicClient client {:keys [name description organization-role]}]
  (with-api-errors
    (when-not name (missing-key! :name))
    (let [b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountCreateParams/builder)]
      (.name b ^String name)
      (when description (.description b ^String description))
      (when organization-role
        (.organizationRole b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountCreateParams$OrganizationRole/of
                              (->wire (check-enum! organization-role sa-org-roles :organization-role)))))
      (service-account->map (-> (.beta client) (.organization) (.serviceAccounts)
                                (.create (.build b)))))))

(defn get-service-account
  "Retrieve one service account by id."
  [^AnthropicClient client ^String service-account-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountRetrieveParams/builder)]
      (.serviceAccountId b ^String service-account-id)
      (service-account->map (-> (.beta client) (.organization) (.serviceAccounts)
                                (.retrieve (.build b)))))))

(defn update-service-account
  "Update a service account's `:description` and/or `:organization-role`."
  [^AnthropicClient client ^String service-account-id {:keys [description organization-role]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountUpdateParams/builder)]
      (.serviceAccountId b ^String service-account-id)
      (when description (.description b ^String description))
      (when organization-role
        (.organizationRole b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountUpdateParams$OrganizationRole/of
                              (->wire (check-enum! organization-role sa-org-roles :organization-role)))))
      (service-account->map (-> (.beta client) (.organization) (.serviceAccounts)
                                (.update (.build b)))))))

(defn list-service-accounts
  "List service accounts. Options: `:limit`, `:page`, `:include-archived`."
  ([^AnthropicClient client] (list-service-accounts client {}))
  ([^AnthropicClient client {:keys [limit page include-archived]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountListParams/builder)]
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (when (some? include-archived) (.includeArchived b (boolean include-archived)))
       (mapv service-account->map (.autoPager (-> (.beta client) (.organization) (.serviceAccounts)
                                                  (.list (.build b)))))))))

(defn archive-service-account
  "Archive a service account by id. Returns the archived service account."
  [^AnthropicClient client ^String service-account-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.serviceaccounts.ServiceAccountArchiveParams/builder)]
      (.serviceAccountId b ^String service-account-id)
      (service-account->map (-> (.beta client) (.organization) (.serviceAccounts)
                                (.archive (.build b)))))))

;; ---- Service-account workspaces -------------------------------------------

(defn- sa-workspace-member->map
  [^com.anthropic.models.beta.organization.serviceaccounts.BetaServiceAccountWorkspaceMember r]
  {:service-account-id (.serviceAccountId r)
   :workspace-id (.workspaceId r)
   :workspace-role (kw<- (.asString ^com.anthropic.models.beta.organization.workspaces.BetaWorkspaceRole (.workspaceRole r)))
   :created-by-actor-id (unopt (.createdByActorId r))
   :implicit (unopt (.implicit r))})

(defn add-service-account-workspace
  "Add a service account to a workspace with `:workspace-role` (a non-billing
  workspace role)."
  [^AnthropicClient client ^String service-account-id {:keys [workspace-id workspace-role]}]
  (with-api-errors
    (when-not workspace-id (missing-key! :workspace-id))
    (let [b (com.anthropic.models.beta.organization.serviceaccounts.workspaces.WorkspaceAddParams/builder)]
      (.serviceAccountId b ^String service-account-id)
      (.workspaceId b ^String workspace-id)
      (when workspace-role
        (.workspaceRole b (com.anthropic.models.beta.organization.workspaces.BetaNoBillingWorkspaceRole/of
                           (->wire (check-enum! workspace-role no-billing-ws-roles :workspace-role)))))
      (sa-workspace-member->map (-> (.beta client) (.organization) (.serviceAccounts)
                                    (.workspaces) (.add (.build b)))))))

(defn list-service-account-workspaces
  "List the workspaces a service account belongs to. Options: `:limit`, `:page`."
  ([^AnthropicClient client ^String service-account-id]
   (list-service-account-workspaces client service-account-id {}))
  ([^AnthropicClient client ^String service-account-id {:keys [limit page]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.serviceaccounts.workspaces.WorkspaceListParams/builder)]
       (.serviceAccountId b ^String service-account-id)
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (mapv sa-workspace-member->map
             (.autoPager (-> (.beta client) (.organization) (.serviceAccounts) (.workspaces)
                             (.list (.build b)))))))))

(defn remove-service-account-workspace
  "Remove a service account from a workspace. Returns the removal response map."
  [^AnthropicClient client ^String service-account-id ^String workspace-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.serviceaccounts.workspaces.WorkspaceRemoveParams/builder)]
      (.serviceAccountId b ^String service-account-id)
      (.workspaceId b ^String workspace-id)
      (obj->clj (-> (.beta client) (.organization) (.serviceAccounts) (.workspaces)
                    (.remove (.build b)))))))

;; ---- Workspaces -----------------------------------------------------------

(defn- workspace->map [^com.anthropic.models.beta.organization.workspaces.BetaWorkspace r]
  {:id (.id r)
   :archived-at (some-> (.archivedAt r) unopt str)
   :compartment-id (.compartmentId r)
   :created-at (str (.createdAt r))
   :data-residency (obj->clj (.dataResidency r))
   :display-color (.displayColor r)
   :external-key-id (unopt (.externalKeyId r))
   :name (.name r)
   :tags (obj->clj (.tags r))})

(defn create-workspace
  "Create a workspace. Requires `:name`. Optional `:display-color`,
  `:external-key-id`, and `:data-residency`/`:tags` as ready SDK config objects."
  [^AnthropicClient client {:keys [name display-color external-key-id data-residency tags]}]
  (with-api-errors
    (when-not name (missing-key! :name))
    (let [b (com.anthropic.models.beta.organization.workspaces.WorkspaceCreateParams/builder)]
      (.name b ^String name)
      (when display-color (.displayColor b ^String display-color))
      (when external-key-id (.externalKeyId b ^String external-key-id))
      (when data-residency (.dataResidency b ^com.anthropic.models.beta.organization.workspaces.BetaDataResidencyCreateConfig data-residency))
      (when tags (.tags b ^com.anthropic.models.beta.organization.workspaces.WorkspaceCreateParams$Tags tags))
      (workspace->map (-> (.beta client) (.organization) (.workspaces) (.create (.build b)))))))

(defn get-workspace
  "Retrieve one workspace by id."
  [^AnthropicClient client ^String workspace-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.WorkspaceRetrieveParams/builder)]
      (.workspaceId b ^String workspace-id)
      (workspace->map (-> (.beta client) (.organization) (.workspaces) (.retrieve (.build b)))))))

(defn update-workspace
  "Update a workspace's `:name`, `:display-color`, `:external-key-id`, or
  `:data-residency`/`:tags` (ready SDK config objects)."
  [^AnthropicClient client ^String workspace-id {:keys [name display-color external-key-id data-residency tags]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.WorkspaceUpdateParams/builder)]
      (.workspaceId b ^String workspace-id)
      (when name (.name b ^String name))
      (when display-color (.displayColor b ^String display-color))
      (when external-key-id (.externalKeyId b ^String external-key-id))
      (when data-residency (.dataResidency b ^com.anthropic.models.beta.organization.workspaces.BetaDataResidencyUpdateConfig data-residency))
      (when tags (.tags b ^com.anthropic.models.beta.organization.workspaces.WorkspaceUpdateParams$Tags tags))
      (workspace->map (-> (.beta client) (.organization) (.workspaces) (.update (.build b)))))))

(defn list-workspaces
  "List workspaces. Options: `:limit`, `:after-id`, `:before-id`,
  `:include-archived`."
  ([^AnthropicClient client] (list-workspaces client {}))
  ([^AnthropicClient client {:keys [limit after-id before-id include-archived]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.workspaces.WorkspaceListParams/builder)]
       (when limit (.limit b (long limit)))
       (when after-id (.afterId b ^String after-id))
       (when before-id (.beforeId b ^String before-id))
       (when (some? include-archived) (.includeArchived b (boolean include-archived)))
       (mapv workspace->map (.autoPager (-> (.beta client) (.organization) (.workspaces)
                                            (.list (.build b)))))))))

(defn archive-workspace
  "Archive a workspace by id. Returns the archived workspace."
  [^AnthropicClient client ^String workspace-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.WorkspaceArchiveParams/builder)]
      (.workspaceId b ^String workspace-id)
      (workspace->map (-> (.beta client) (.organization) (.workspaces) (.archive (.build b)))))))

;; ---- Workspace members ----------------------------------------------------

(defn- workspace-member->map
  [^com.anthropic.models.beta.organization.workspaces.BetaWorkspaceMember r]
  {:user-id (.userId r)
   :workspace-id (.workspaceId r)
   :workspace-role (kw<- (.asString ^com.anthropic.models.beta.organization.workspaces.BetaWorkspaceRole (.workspaceRole r)))})

(defn get-workspace-member
  "Retrieve one workspace member by workspace id and user id."
  [^AnthropicClient client ^String workspace-id ^String user-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.members.MemberRetrieveParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.userId b ^String user-id)
      (workspace-member->map (-> (.beta client) (.organization) (.workspaces) (.members)
                                 (.retrieve (.build b)))))))

(defn update-workspace-member
  "Update a workspace member's `:workspace-role` (any workspace role)."
  [^AnthropicClient client ^String workspace-id ^String user-id {:keys [workspace-role]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.members.MemberUpdateParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.userId b ^String user-id)
      (when workspace-role
        (.workspaceRole b (com.anthropic.models.beta.organization.workspaces.BetaWorkspaceRole/of
                           (->wire (check-enum! workspace-role ws-roles :workspace-role)))))
      (workspace-member->map (-> (.beta client) (.organization) (.workspaces) (.members)
                                 (.update (.build b)))))))

(defn list-workspace-members
  "List a workspace's members. Options: `:limit`, `:after-id`, `:before-id`."
  ([^AnthropicClient client ^String workspace-id]
   (list-workspace-members client workspace-id {}))
  ([^AnthropicClient client ^String workspace-id {:keys [limit after-id before-id]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.workspaces.members.MemberListParams/builder)]
       (.workspaceId b ^String workspace-id)
       (when limit (.limit b (long limit)))
       (when after-id (.afterId b ^String after-id))
       (when before-id (.beforeId b ^String before-id))
       (mapv workspace-member->map
             (.autoPager (-> (.beta client) (.organization) (.workspaces) (.members)
                             (.list (.build b)))))))))

(defn add-workspace-member
  "Add a user to a workspace with `:workspace-role` (a non-billing workspace
  role)."
  [^AnthropicClient client ^String workspace-id {:keys [user-id workspace-role]}]
  (with-api-errors
    (when-not user-id (missing-key! :user-id))
    (let [b (com.anthropic.models.beta.organization.workspaces.members.MemberAddParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.userId b ^String user-id)
      (when workspace-role
        (.workspaceRole b (com.anthropic.models.beta.organization.workspaces.BetaNoBillingWorkspaceRole/of
                           (->wire (check-enum! workspace-role no-billing-ws-roles :workspace-role)))))
      (workspace-member->map (-> (.beta client) (.organization) (.workspaces) (.members)
                                 (.add (.build b)))))))

(defn remove-workspace-member
  "Remove a user from a workspace. Returns the removal response as a map."
  [^AnthropicClient client ^String workspace-id ^String user-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.members.MemberRemoveParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.userId b ^String user-id)
      (obj->clj (-> (.beta client) (.organization) (.workspaces) (.members)
                    (.remove (.build b)))))))

;; ---- Workspace rate limits ------------------------------------------------

(defn list-workspace-rate-limits
  "List a workspace's rate limits. Options: `:limit`, `:page`, `:group-type`."
  ([^AnthropicClient client ^String workspace-id]
   (list-workspace-rate-limits client workspace-id {}))
  ([^AnthropicClient client ^String workspace-id {:keys [limit page group-type]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.workspaces.ratelimits.RateLimitListParams/builder)]
       (.workspaceId b ^String workspace-id)
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (when group-type
         (.groupType b (com.anthropic.models.beta.organization.workspaces.ratelimits.RateLimitListParams$GroupType/of
                        (->wire (check-enum! group-type group-types :group-type)))))
       (mapv obj->clj (.autoPager (-> (.beta client) (.organization) (.workspaces) (.rateLimits)
                                      (.list (.build b)))))))))

;; ---- Workspace service accounts -------------------------------------------

(defn get-workspace-service-account
  "Retrieve a service account's membership in a workspace."
  [^AnthropicClient client ^String workspace-id ^String service-account-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.serviceaccounts.ServiceAccountRetrieveParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.serviceAccountId b ^String service-account-id)
      (sa-workspace-member->map (-> (.beta client) (.organization) (.workspaces) (.serviceAccounts)
                                    (.retrieve (.build b)))))))

(defn update-workspace-service-account
  "Update a workspace service account's `:workspace-role` (a non-billing role)."
  [^AnthropicClient client ^String workspace-id ^String service-account-id {:keys [workspace-role]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.serviceaccounts.ServiceAccountUpdateParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.serviceAccountId b ^String service-account-id)
      (when workspace-role
        (.workspaceRole b (com.anthropic.models.beta.organization.workspaces.BetaNoBillingWorkspaceRole/of
                           (->wire (check-enum! workspace-role no-billing-ws-roles :workspace-role)))))
      (sa-workspace-member->map (-> (.beta client) (.organization) (.workspaces) (.serviceAccounts)
                                    (.update (.build b)))))))

(defn list-workspace-service-accounts
  "List a workspace's service accounts. Options: `:limit`, `:page`."
  ([^AnthropicClient client ^String workspace-id]
   (list-workspace-service-accounts client workspace-id {}))
  ([^AnthropicClient client ^String workspace-id {:keys [limit page]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.workspaces.serviceaccounts.ServiceAccountListParams/builder)]
       (.workspaceId b ^String workspace-id)
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (mapv sa-workspace-member->map
             (.autoPager (-> (.beta client) (.organization) (.workspaces) (.serviceAccounts)
                             (.list (.build b)))))))))

(defn add-workspace-service-account
  "Add a service account to a workspace with `:workspace-role` (non-billing)."
  [^AnthropicClient client ^String workspace-id {:keys [service-account-id workspace-role]}]
  (with-api-errors
    (when-not service-account-id (missing-key! :service-account-id))
    (let [b (com.anthropic.models.beta.organization.workspaces.serviceaccounts.ServiceAccountAddParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.serviceAccountId b ^String service-account-id)
      (when workspace-role
        (.workspaceRole b (com.anthropic.models.beta.organization.workspaces.BetaNoBillingWorkspaceRole/of
                           (->wire (check-enum! workspace-role no-billing-ws-roles :workspace-role)))))
      (sa-workspace-member->map (-> (.beta client) (.organization) (.workspaces) (.serviceAccounts)
                                    (.add (.build b)))))))

(defn remove-workspace-service-account
  "Remove a service account from a workspace. Returns the removal response map."
  [^AnthropicClient client ^String workspace-id ^String service-account-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.workspaces.serviceaccounts.ServiceAccountRemoveParams/builder)]
      (.workspaceId b ^String workspace-id)
      (.serviceAccountId b ^String service-account-id)
      (obj->clj (-> (.beta client) (.organization) (.workspaces) (.serviceAccounts)
                    (.remove (.build b)))))))

;; ---- Federation issuers ---------------------------------------------------

(defn- federation-issuer->map
  [^com.anthropic.models.beta.organization.federation.issuers.BetaFederationIssuer r]
  {:id (.id r)
   :archived-at (some-> (.archivedAt r) unopt str)
   :archived-by-actor-id (unopt (.archivedByActorId r))
   :check-jti (.checkJti r)
   :created-at (str (.createdAt r))
   :created-by-actor-id (unopt (.createdByActorId r))
   :issuer-url (.issuerUrl r)
   :jwks (obj->clj (.jwks r))
   :jwks-polling-disabled-at (some-> (.jwksPollingDisabledAt r) unopt str)
   :max-jwt-lifetime-seconds (.maxJwtLifetimeSeconds r)
   :name (.name r)
   :poll-status (obj->clj (unopt (.pollStatus r)))
   :updated-at (str (.updatedAt r))
   :updated-by-actor-id (unopt (.updatedByActorId r))})

(defn create-federation-issuer
  "Create a federation issuer. Requires `:name` and `:issuer-url`. JWKS via
  `:explicit-url-jwks` (a URL string) or `:jwks` (a ready SDK JWKS object).
  Optional `:check-jti`, `:max-jwt-lifetime-seconds`."
  [^AnthropicClient client {:keys [name issuer-url explicit-url-jwks jwks check-jti max-jwt-lifetime-seconds]}]
  (with-api-errors
    (when-not name (missing-key! :name))
    (when-not issuer-url (missing-key! :issuer-url))
    (let [b (com.anthropic.models.beta.organization.federation.issuers.IssuerCreateParams/builder)]
      (.name b ^String name)
      (.issuerUrl b ^String issuer-url)
      (when explicit-url-jwks (.explicitUrlJwks b ^String explicit-url-jwks))
      (when jwks (.jwks b ^com.anthropic.models.beta.organization.federation.issuers.IssuerCreateParams$Jwks jwks))
      (when (some? check-jti) (.checkJti b (boolean check-jti)))
      (when max-jwt-lifetime-seconds (.maxJwtLifetimeSeconds b (long max-jwt-lifetime-seconds)))
      (federation-issuer->map (-> (.beta client) (.organization) (.federation) (.issuers)
                                  (.create (.build b)))))))

(defn get-federation-issuer
  "Retrieve one federation issuer by id."
  [^AnthropicClient client ^String issuer-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.issuers.IssuerRetrieveParams/builder)]
      (.federationIssuerId b ^String issuer-id)
      (federation-issuer->map (-> (.beta client) (.organization) (.federation) (.issuers)
                                  (.retrieve (.build b)))))))

(defn update-federation-issuer
  "Update a federation issuer's `:name`, `:issuer-url`, JWKS
  (`:explicit-url-jwks`/`:jwks`), `:check-jti`, `:max-jwt-lifetime-seconds`, or
  `:jwks-polling-disabled`."
  [^AnthropicClient client ^String issuer-id
   {:keys [name issuer-url explicit-url-jwks jwks check-jti max-jwt-lifetime-seconds jwks-polling-disabled]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.issuers.IssuerUpdateParams/builder)]
      (.federationIssuerId b ^String issuer-id)
      (when name (.name b ^String name))
      (when issuer-url (.issuerUrl b ^String issuer-url))
      (when explicit-url-jwks (.explicitUrlJwks b ^String explicit-url-jwks))
      (when jwks (.jwks b ^com.anthropic.models.beta.organization.federation.issuers.IssuerUpdateParams$Jwks jwks))
      (when (some? check-jti) (.checkJti b (boolean check-jti)))
      (when max-jwt-lifetime-seconds (.maxJwtLifetimeSeconds b (long max-jwt-lifetime-seconds)))
      (when (some? jwks-polling-disabled) (.jwksPollingDisabled b (boolean jwks-polling-disabled)))
      (federation-issuer->map (-> (.beta client) (.organization) (.federation) (.issuers)
                                  (.update (.build b)))))))

(defn list-federation-issuers
  "List federation issuers. Options: `:limit`, `:page`, `:include-archived`."
  ([^AnthropicClient client] (list-federation-issuers client {}))
  ([^AnthropicClient client {:keys [limit page include-archived]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.federation.issuers.IssuerListParams/builder)]
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (when (some? include-archived) (.includeArchived b (boolean include-archived)))
       (mapv federation-issuer->map
             (.autoPager (-> (.beta client) (.organization) (.federation) (.issuers)
                             (.list (.build b)))))))))

(defn archive-federation-issuer
  "Archive a federation issuer by id. Returns the archived issuer."
  [^AnthropicClient client ^String issuer-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.issuers.IssuerArchiveParams/builder)]
      (.federationIssuerId b ^String issuer-id)
      (federation-issuer->map (-> (.beta client) (.organization) (.federation) (.issuers)
                                  (.archive (.build b)))))))

;; ---- Federation rules -----------------------------------------------------

(defn- federation-rule->map
  [^com.anthropic.models.beta.organization.federation.rules.BetaFederationRule r]
  {:id (.id r)
   :applies-to-all-workspaces (.appliesToAllWorkspaces r)
   :archived-at (some-> (.archivedAt r) unopt str)
   :archived-by-actor-id (unopt (.archivedByActorId r))
   :attributes (obj->clj (unopt (.attributes r)))
   :created-at (str (.createdAt r))
   :created-by-actor-id (unopt (.createdByActorId r))
   :description (unopt (.description r))
   :issuer-id (.issuerId r)
   :issuer-name (unopt (.issuerName r))
   :match (obj->clj (.match r))
   :name (.name r)
   :oauth-scope (.oauthScope r)
   :target (obj->clj (.target r))
   :token-lifetime-seconds (.tokenLifetimeSeconds r)
   :updated-at (str (.updatedAt r))
   :updated-by-actor-id (unopt (.updatedByActorId r))
   :workspace-id (unopt (.workspaceId r))
   :workspace-ids (vec (.workspaceIds r))})

(defn- ->target
  ^com.anthropic.models.beta.organization.federation.rules.BetaServiceAccountTarget
  [target]
  (if (instance? com.anthropic.models.beta.organization.federation.rules.BetaServiceAccountTarget target)
    target
    (let [{:keys [service-account-id service-account-name]} target
          b (com.anthropic.models.beta.organization.federation.rules.BetaServiceAccountTarget/builder)]
      (.serviceAccountId b ^String service-account-id)
      (when service-account-name (.serviceAccountName b ^String service-account-name))
      (.build b))))

(defn- ->match
  ^com.anthropic.models.beta.organization.federation.rules.BetaFederationRuleMatch
  [match]
  (if (instance? com.anthropic.models.beta.organization.federation.rules.BetaFederationRuleMatch match)
    match
    (let [{:keys [audience condition subject-prefix claims]} match
          b (com.anthropic.models.beta.organization.federation.rules.BetaFederationRuleMatch/builder)]
      (when audience (.audience b ^String audience))
      (when condition (.condition b ^String condition))
      (when subject-prefix (.subjectPrefix b ^String subject-prefix))
      (when claims (.claims b ^com.anthropic.models.beta.organization.federation.rules.BetaFederationRuleMatch$Claims claims))
      (.build b))))

(defn create-federation-rule
  "Create a federation rule. Requires `:name`, `:issuer-id`, `:oauth-scope`,
  `:token-lifetime-seconds`, and `:target` (a map `{:service-account-id ...}`
  or a ready SDK target). Optional `:description`, `:applies-to-all-workspaces`,
  `:workspace-id`, and `:match` (a map of `:audience`/`:condition`/
  `:subject-prefix`/`:claims`, or a ready SDK match)."
  [^AnthropicClient client
   {:keys [name issuer-id oauth-scope token-lifetime-seconds target description
           applies-to-all-workspaces workspace-id match]}]
  (with-api-errors
    (when-not name (missing-key! :name))
    (when-not issuer-id (missing-key! :issuer-id))
    (when-not oauth-scope (missing-key! :oauth-scope))
    (when-not token-lifetime-seconds (missing-key! :token-lifetime-seconds))
    (when-not target (missing-key! :target))
    (let [b (com.anthropic.models.beta.organization.federation.rules.RuleCreateParams/builder)]
      (.name b ^String name)
      (.issuerId b ^String issuer-id)
      (.oauthScope b ^String oauth-scope)
      (.tokenLifetimeSeconds b (long token-lifetime-seconds))
      (.target b (->target target))
      (when description (.description b ^String description))
      (when (some? applies-to-all-workspaces) (.appliesToAllWorkspaces b (boolean applies-to-all-workspaces)))
      (when workspace-id (.workspaceId b ^String workspace-id))
      (when match (.match b (->match match)))
      (federation-rule->map (-> (.beta client) (.organization) (.federation) (.rules)
                                (.create (.build b)))))))

(defn get-federation-rule
  "Retrieve one federation rule by id."
  [^AnthropicClient client ^String rule-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.rules.RuleRetrieveParams/builder)]
      (.federationRuleId b ^String rule-id)
      (federation-rule->map (-> (.beta client) (.organization) (.federation) (.rules)
                                (.retrieve (.build b)))))))

(defn update-federation-rule
  "Update a federation rule's `:name`, `:description`, `:oauth-scope`,
  `:token-lifetime-seconds`, `:applies-to-all-workspaces`, `:workspace-id`,
  `:target`, or `:match`."
  [^AnthropicClient client ^String rule-id
   {:keys [name description oauth-scope token-lifetime-seconds applies-to-all-workspaces
           workspace-id target match]}]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.rules.RuleUpdateParams/builder)]
      (.federationRuleId b ^String rule-id)
      (when name (.name b ^String name))
      (when description (.description b ^String description))
      (when oauth-scope (.oauthScope b ^String oauth-scope))
      (when token-lifetime-seconds (.tokenLifetimeSeconds b (long token-lifetime-seconds)))
      (when (some? applies-to-all-workspaces) (.appliesToAllWorkspaces b (boolean applies-to-all-workspaces)))
      (when workspace-id (.workspaceId b ^String workspace-id))
      (when target (.target b (->target target)))
      (when match (.match b (->match match)))
      (federation-rule->map (-> (.beta client) (.organization) (.federation) (.rules)
                                (.update (.build b)))))))

(defn list-federation-rules
  "List federation rules. Options: `:limit`, `:page`, `:include-archived`,
  `:issuer-id`."
  ([^AnthropicClient client] (list-federation-rules client {}))
  ([^AnthropicClient client {:keys [limit page include-archived issuer-id]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.federation.rules.RuleListParams/builder)]
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (when (some? include-archived) (.includeArchived b (boolean include-archived)))
       (when issuer-id (.issuerId b ^String issuer-id))
       (mapv federation-rule->map
             (.autoPager (-> (.beta client) (.organization) (.federation) (.rules)
                             (.list (.build b)))))))))

(defn archive-federation-rule
  "Archive a federation rule by id. Returns the archived rule."
  [^AnthropicClient client ^String rule-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.rules.RuleArchiveParams/builder)]
      (.federationRuleId b ^String rule-id)
      (federation-rule->map (-> (.beta client) (.organization) (.federation) (.rules)
                                (.archive (.build b)))))))

;; ---- Federation rule workspaces -------------------------------------------

(defn- rule-workspace->map
  [^com.anthropic.models.beta.organization.federation.rules.BetaFederationRuleWorkspace r]
  {:federation-rule-id (.federationRuleId r)
   :workspace-id (.workspaceId r)
   :created-at (str (.createdAt r))
   :created-by-actor-id (unopt (.createdByActorId r))
   :workspace-name (unopt (.workspaceName r))})

(defn add-federation-rule-workspace
  "Attach a workspace to a federation rule."
  [^AnthropicClient client ^String rule-id ^String workspace-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.rules.workspaces.WorkspaceAddParams/builder)]
      (.federationRuleId b ^String rule-id)
      (.workspaceId b ^String workspace-id)
      (rule-workspace->map (-> (.beta client) (.organization) (.federation) (.rules) (.workspaces)
                               (.add (.build b)))))))

(defn list-federation-rule-workspaces
  "List the workspaces attached to a federation rule. Options: `:limit`, `:page`."
  ([^AnthropicClient client ^String rule-id]
   (list-federation-rule-workspaces client rule-id {}))
  ([^AnthropicClient client ^String rule-id {:keys [limit page]}]
   (with-api-errors
     (let [b (com.anthropic.models.beta.organization.federation.rules.workspaces.WorkspaceListParams/builder)]
       (.federationRuleId b ^String rule-id)
       (when limit (.limit b (long limit)))
       (when page (.page b ^String page))
       (mapv rule-workspace->map
             (.autoPager (-> (.beta client) (.organization) (.federation) (.rules) (.workspaces)
                             (.list (.build b)))))))))

(defn remove-federation-rule-workspace
  "Detach a workspace from a federation rule. Returns the removal response map."
  [^AnthropicClient client ^String rule-id ^String workspace-id]
  (with-api-errors
    (let [b (com.anthropic.models.beta.organization.federation.rules.workspaces.WorkspaceRemoveParams/builder)]
      (.federationRuleId b ^String rule-id)
      (.workspaceId b ^String workspace-id)
      (obj->clj (-> (.beta client) (.organization) (.federation) (.rules) (.workspaces)
                    (.remove (.build b)))))))
