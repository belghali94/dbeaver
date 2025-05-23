/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.registry;

/**
 * Data source descriptors constants
 */
public class RegistryConstants {

    public static final String CONNECTION_TYPES_FILE_NAME = "connection-types.xml"; //$NON-NLS-1$

    public static final String TAG_DRIVERS = "drivers"; //$NON-NLS-1$
    public static final String TAG_DRIVER = "driver"; //$NON-NLS-1$
    public static final String TAG_PROVIDER = "provider"; //$NON-NLS-1$
    public static final String TAG_PARAMETER = "parameter"; //$NON-NLS-1$
    public static final String TAG_PROPERTY = "property"; //$NON-NLS-1$
    public static final String TAG_PROPERTIES = "properties"; //$NON-NLS-1$
    public static final String TAG_EXTENSIONS = "extensions"; //$NON-NLS-1$
    public static final String TAG_TAGS = "tags"; //$NON-NLS-1$
    public static final String TAG_FILE = "file"; //$NON-NLS-1$
    public static final String TAG_FILE_SOURCE = "fileSource"; //$NON-NLS-1$
    public static final String TAG_LIBRARY = "library"; // [LEGACY: from DBeaver 1.1.0]  //$NON-NLS-1$
    public static final String TAG_PATH = "path"; //$NON-NLS-1$
    public static final String TAG_REPLACE = "replace"; //$NON-NLS-1$
    public static final String TAG_CLIENT_HOME = "clientHome"; //$NON-NLS-1$

    public static final String TAG_OBJECT_TYPE = "objectType"; //$NON-NLS-1$
    public static final String TAG_PROFILES = "profiles"; //$NON-NLS-1$
    public static final String TAG_PROFILE = "profile"; //$NON-NLS-1$

    public static final String TAG_TYPES = "types"; //$NON-NLS-1$
    public static final String TAG_TYPE = "type"; //$NON-NLS-1$
    public static final String TAG_DATASOURCE = "datasource"; //$NON-NLS-1$
    public static final String TAG_DATASOURCE_PATCH = "datasourcePatch"; //$NON-NLS-1$
    public static final String TAG_DATASOURCE_ORIGIN = "datasourceOrigin"; //$NON-NLS-1$
    public static final String TAG_DRIVER_SUBSTITUTION = "driverSubstitution"; //$NON-NLS-1$
    public static final String TAG_OS = "os"; //NON-NLS-1

    public static final String ATTR_ID = "id"; //$NON-NLS-1$
    public static final String ATTR_VERSION = "version"; //$NON-NLS-1$
    public static final String ATTR_CATEGORY = "category"; //$NON-NLS-1$
    public static final String ATTR_CATEGORIES = "categories"; //$NON-NLS-1$
    public static final String ATTR_DISABLED = "disabled"; //$NON-NLS-1$
    public static final String ATTR_CUSTOM = "custom"; //$NON-NLS-1$
    public static final String ATTR_NAME = "name"; //$NON-NLS-1$
    public static final String ATTR_VALUE = "value"; //$NON-NLS-1$
    public static final String ATTR_ALIAS = "alias"; //$NON-NLS-1$
    public static final String ATTR_CLASS = "class"; //$NON-NLS-1$
    public static final String ATTR_SNAPSHOT = "snapshot"; //$NON-NLS-1$
    public static final String ATTR_URL = "url"; //$NON-NLS-1$
    public static final String ATTR_LINK = "link"; //$NON-NLS-1$
    public static final String ATTR_SCOPE = "scope"; //$NON-NLS-1$
    public static final String ATTR_PORT = "port"; //$NON-NLS-1$
    public static final String ATTR_DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String ATTR_NOTE = "note"; //$NON-NLS-1$
    public static final String ATTR_PATH = "path"; //$NON-NLS-1$
    public static final String ATTR_AFTER = "after"; //$NON-NLS-1$
    public static final String ATTR_PROVIDER = "provider"; //$NON-NLS-1$
    public static final String ATTR_COMMENT = "comment"; //$NON-NLS-1$
    public static final String ATTR_ORDER = "order"; //$NON-NLS-1$
    public static final String ATTR_ENABLED = "enabled"; //$NON-NLS-1$
    public static final String ATTR_DRIVER = "driver"; //$NON-NLS-1$
    public static final String ATTR_BUNDLE = "bundle"; //$NON-NLS-1$

    public static final String ATTR_CODE = "code"; //$NON-NLS-1$
    public static final String ATTR_LABEL = "label"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_HOST = "defaultHost"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_PORT = "defaultPort"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_DATABASE = "defaultDatabase"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_SERVER = "defaultServer"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_USER = "defaultUser"; //$NON-NLS-1$
    public static final String ATTR_SAMPLE_URL = "sampleURL"; //$NON-NLS-1$
    public static final String ATTR_WEB_URL = "webURL"; //$NON-NLS-1$
    public static final String ATTR_PROPERTIES_WEB_URL = "propertiesURL"; //$NON-NLS-1$
    public static final String ATTR_DATABASE_DOCUMENTATION_SUFFIX_URL = "databaseDocumentationSuffixURL"; //$NON-NLS-1$
    public static final String ATTR_SUPPORTS_DRIVER_PROPERTIES = "supportsDriverProperties"; //$NON-NLS-1$
    public static final String ATTR_CLIENT_REQUIRED = "clientRequired"; //$NON-NLS-1$
    public static final String ATTR_ANONYMOUS = "anonymous"; //$NON-NLS-1$
    public static final String ATTR_LICENSE_REQUIRED = "licenseRequired"; //$NON-NLS-1$
    public static final String ATTR_EMBEDDED = "embedded"; //$NON-NLS-1$
    public static final String ATTR_PROPAGATE_DRIVER_PROPERTIES = "propagateDriverProperties"; //$NON-NLS-1$
    public static final String ATTR_SINGLE_CONNECTION = "singleConnection"; //$NON-NLS-1$
    public static final String ATTR_CUSTOM_DRIVER_LOADER = "customDriverLoader"; //$NON-NLS-1$
    public static final String ATTR_USE_URL_TEMPLATE = "useURL"; //$NON-NLS-1$
    public static final String ATTR_CUSTOM_ENDPOINT = "customEndpoint"; //$NON-NLS-1$
    public static final String ATTR_INSTANTIABLE = "instantiable"; //$NON-NLS-1$
    public static final String ATTR_PROMOTED = "promoted"; //$NON-NLS-1$

    public static final String ATTR_ICON = "icon"; //$NON-NLS-1$
    public static final String ATTR_ICON_BIG = "iconBig"; //$NON-NLS-1$
    public static final String ATTR_STANDARD = "standard"; //$NON-NLS-1$
    public static final String ATTR_COLOR = "color"; //$NON-NLS-1$
    public static final String ATTR_KEEP_ALIVE = "keepAlive"; //$NON-NLS-1$
    public static final String ATTR_CLOSE_IDLE_ENABLED = "closeIdleConnection";
    public static final String ATTR_CLOSE_IDLE = "closeIdle";
    public static final String ATTR_AUTOCOMMIT = "autocommit"; //$NON-NLS-1$
    public static final String ATTR_TXN_ISOLATION = "txnIsolation"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_OBJECT = "defaultObject"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_CATALOG = "defaultCatalog"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT_SCHEMA = "defaultSchema"; //$NON-NLS-1$
    public static final String ATTR_CONFIRM_EXECUTE = "confirmExecute"; //$NON-NLS-1$
    public static final String ATTR_CONFIRM_DATA_CHANGE = "confirmDataChange"; //$NON-NLS-1$
    public static final String ATTR_SMART_COMMIT = "smartCommit"; //$NON-NLS-1$
    public static final String ATTR_SMART_COMMIT_RECOVER = "smartCommitRecover"; //$NON-NLS-1$
    public static final String ATTR_AUTO_CLOSE_TRANSACTIONS = "autoCloseTransactions"; //$NON-NLS-1$
    public static final String ATTR_CLOSE_TRANSACTIONS_PERIOD = "closeTransactionsPeriod"; //$NON-NLS-1$
    public static final String ATTR_AUTO_CLOSE_CONNECTIONS = "autoCloseConnections"; //$NON-NLS-1$
    public static final String ATTR_CLOSE_CONNECTIONS_PERIOD = "closeConnectionsPeriod"; //$NON-NLS-1$
    public static final String ATTR_PARENT = "parent"; //$NON-NLS-1$
    public static final String ATTR_GROUP = "group"; //$NON-NLS-1$
    public static final String ATTR_SINGLETON = "singleton"; //$NON-NLS-1$
    public static final String ATTR_IGNORE_ERRORS = "ignoreErrors"; //$NON-NLS-1$
    public static final String ATTR_DIALECT = "dialect"; //$NON-NLS-1$
    public static final String ATTR_REQUIRES_MUTABILITY = "requiresMutability"; //$NON-NLS-1$
    public static final String ATTR_REQUIRES_EXPORT_PRIVILEGES = "requiresExportPrivileges"; //$NON-NLS-1$

    public static final String ATTR_TARGET_ID = "targetID"; //$NON-NLS-1$
    public static final String ATTR_TYPE = "type"; //$NON-NLS-1$
    public static final String ATTR_OS = "os"; //$NON-NLS-1$
    public static final String ATTR_ARCH = "arch"; //$NON-NLS-1$
    public static final String ATTR_MAIN = "main"; //$NON-NLS-1$
    public static final String ATTR_POSITION = "position"; //$NON-NLS-1$
    public static final String ATTR_OBJECT_TYPE = "objectType"; //$NON-NLS-1$
    public static final String ATTR_SAMPLE_CLASS = "sampleClass"; //$NON-NLS-1$
    public static final String ATTR_SOURCE_TYPE = "sourceType"; //$NON-NLS-1$
    public static final String ATTR_EMBEDDABLE = "embeddable"; //$NON-NLS-1$
    public static final String ATTR_DATA_SOURCE = "dataSource"; //$NON-NLS-1$
    public static final String ATTR_AUTH_MODEL = "auth-model"; //$NON-NLS-1$

    public static final String TAG_FOLDER = "folder"; //$NON-NLS-1$
    public static final String TAG_ITEMS = "items"; //$NON-NLS-1$
    public static final String TAG_OBJECT = "object"; //$NON-NLS-1$

    static final String TAG_TREE = "tree"; //$NON-NLS-1$
    static final String TAG_TREE_CONTRIBUTION = "treeContribution"; //$NON-NLS-1$
    static final String TAG_TREE_INJECTION = "treeInjection"; //$NON-NLS-1$
    static final String TAG_EDITOR_CONTRIBUTION = "editorContribution"; //$NON-NLS-1$
    static final String TAG_DRIVER_PROPERTIES = "driver-properties"; //$NON-NLS-1$
    static final String TAG_VIEWS = "views"; //$NON-NLS-1$
    static final String TAG_VIEW = "view"; //$NON-NLS-1$

    public static final String ATTR_REF = "ref"; //$NON-NLS-1$
    public static final String ATTR_VISIBLE_IF = "visibleIf"; //$NON-NLS-1$
    public static final String ATTR_RECURSIVE = "recursive"; //$NON-NLS-1$
    public static final String ATTR_NAVIGABLE = "navigable"; //$NON-NLS-1$
    public static final String ATTR_ITEM_LABEL = "itemLabel"; //$NON-NLS-1$
    public static final String ATTR_PROPERTY = "property"; //$NON-NLS-1$
    public static final String ATTR_OPTIONAL = "optional"; //$NON-NLS-1$
    public static final String ATTR_VIRTUAL = "virtual"; //$NON-NLS-1$
    public static final String ATTR_STANDALONE = "standalone"; //$NON-NLS-1$
    public static final String ATTR_INLINE = "inline"; //$NON-NLS-1$
    public static final String ATTR_EDITOR = "editor"; //$NON-NLS-1$
    public static final String ATTR_IF = "if"; //$NON-NLS-1$
    public static final String ATTR_DEFAULT = "default"; //$NON-NLS-1$
    public static final String ATTR_MANAGABLE = "managable"; //$NON-NLS-1$
    public static final String ATTR_CONTRIBUTOR = "contributor"; //$NON-NLS-1$
    public static final String ATTR_INPUT_FACTORY = "inputFactory"; //$NON-NLS-1$

    public static final String ATTR_CONFIRMATION_MESSAGE = "confirmationMessage"; //$NON-NLS-1$
    public static final String ATTR_HANDLER_CLASS = "handlerClass"; //$NON-NLS-1$
    public static final String ATTR_SECURED = "secured"; //$NON-NLS-1$

    public static final String TAG_DATA_SOURCE = "data-source"; //$NON-NLS-1$
    public static final String TAG_EVENT = "event"; //$NON-NLS-1$
    public static final String TAG_EVENTS = "events"; //$NON-NLS-1$
    public static final String TAG_AUTH_PROPERTIES = "auth-properties"; //$NON-NLS-1$
    public static final String TAG_MAIN_PROPERTIES = "main-properties"; //$NON-NLS-1$
    public static final String TAG_PROVIDER_PROPERTIES = "provider-properties"; //$NON-NLS-1$
    public static final String TAG_PROVIDER_PROPERTY = "provider-property"; //$NON-NLS-1$
    public static final String TAG_CUSTOM_PROPERTY = "custom-property"; //$NON-NLS-1$
    public static final String TAG_CUSTOM_PROPERTIES = "custom-properties"; //$NON-NLS-1$
    public static final String TAG_NETWORK_HANDLER = "network-handler"; //$NON-NLS-1$
    public static final String TAG_HANDLERS = "handlers";
    public static final String TAG_DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String TAG_CONNECTION = "connection"; //$NON-NLS-1$
    public static final String TAG_BOOTSTRAP = "bootstrap"; //$NON-NLS-1$
    public static final String TAG_QUERY = "query"; //$NON-NLS-1$

    public static final String ATTR_CREATE_DATE = "create-date"; //$NON-NLS-1$
    public static final String ATTR_UPDATE_DATE = "update-date"; //$NON-NLS-1$
    public static final String ATTR_LOGIN_DATE = "login-date"; //$NON-NLS-1$
    public static final String ATTR_SAVE_PASSWORD = "save-password"; //$NON-NLS-1$
    public static final String ATTR_SHARED_CREDENTIALS = "shared-credentials"; //$NON-NLS-1$

    public static final String ATTR_READ_ONLY = "read-only"; //$NON-NLS-1$
    public static final String ATTR_FILTER_CATALOG = "filter-catalog"; //$NON-NLS-1$
    public static final String ATTR_FILTER_SCHEMA = "filter-schema"; //$NON-NLS-1$
    public static final String ATTR_HOST = "host"; //$NON-NLS-1$
    public static final String ATTR_SERVER = "server"; //$NON-NLS-1$
    public static final String ATTR_DATABASE = "database"; //$NON-NLS-1$
    public static final String ATTR_USER = "user"; //$NON-NLS-1$
    public static final String ATTR_PASSWORD = "password"; //$NON-NLS-1$
    public static final String ATTR_EMPTY_DATABASE_CREDENTIALS = "empty-credentials"; //$NON-NLS-1$
    public static final String ATTR_NATIVE_AUTH = "native-auth"; //$NON-NLS-1$
    public static final String ATTR_HOME = "home"; //$NON-NLS-1$
    public static final String ATTR_SHOW_PANEL = "show-panel"; //$NON-NLS-1$
    public static final String ATTR_WAIT_PROCESS = "wait-process"; //$NON-NLS-1$
    public static final String ATTR_WAIT_PROCESS_TIMEOUT = "wait-process-timeout"; //$NON-NLS-1$
    public static final String ATTR_TERMINATE_AT_DISCONNECT = "terminate-at-disconnect"; //$NON-NLS-1$
    public static final String ATTR_FOLDER = "folder"; //$NON-NLS-1$
    public static final String TAG_FILTERS = "filters"; //$NON-NLS-1$
    public static final String TAG_FILTER = "filter"; //$NON-NLS-1$
    public static final String TAG_INCLUDE = "include"; //$NON-NLS-1$
    public static final String TAG_EXCLUDE = "exclude"; //$NON-NLS-1$

    public static final String TAG_VIRTUAL_META_DATA = "virtual-meta-data"; //$NON-NLS-1$
    public static final String TAG_MODEL = "model"; //$NON-NLS-1$

    public static final String MAPPED_URL = "*";
    public static final String ATTR_FORMAT = "format";

    public static final String TAG_NODE = "node";
    public static final String TAG_PROCESSOR = "processor";
    public static final String TAG_PAGE = "page";
    public static final String ATTR_SETTINGS = "settings";
    public static final String ATTR_LOCK_PASSWORD = "lockPassword";
    public static final String ATTR_PAUSE_AFTER_EXECUTE = "pauseAfterExecute";
    public static final String ATTR_WORKING_DIRECTORY = "workingDirectory";
    public static final String ATTR_COMMAND = "command";
    public static final String ATTR_SUPPORTED_CONFIGURATION_TYPES = "supportedConfigurationTypes";
    public static final String ATTR_SUPPORTED_PAGE_FIELDS = "supportedPageFields";
    public static final String ATTR_CONFIGURATION_TYPE = "configurationType";
    public static final String ATTR_SUPPORTS_DISTRIBUTED_MODE = "supportsDistributedMode";
    public static final String ATTR_NOT_AVAILABLE_DRIVER = "notAvailableDriver";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_TITLE = "title";
}
