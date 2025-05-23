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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBCUtils
 */
public class JDBCUtils {
    public static boolean LOG_JDBC_WARNINGS = CommonUtils.toBoolean(System.getProperty("dbeaver.jdbc.log.warnings"));

    private static final Log log = Log.getLog(JDBCUtils.class);

    private static final Map<String, Integer> badColumnNames = new HashMap<>();

    @Nullable
    public static String safeGetString(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getString(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static String safeGetStringTrimmed(ResultSet dbResult, String columnName) {
        try {
            final String value = dbResult.getString(columnName);
            if (value != null && !value.isEmpty()) {
                return value.trim();
            } else {
                return value;
            }
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static String safeGetString(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getString(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static String safeGetStringTrimmed(ResultSet dbResult, int columnIndex) {
        try {
            final String value = dbResult.getString(columnIndex);
            if (value != null && !value.isEmpty()) {
                return value.trim();
            } else {
                return value;
            }
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    public static void setStringOrNull(PreparedStatement dbStat, int columnIndex, String value) throws SQLException {
        if (value != null) {
            dbStat.setString(columnIndex, value);
        } else {
            dbStat.setNull(columnIndex, Types.VARCHAR);
        }
    }

    public static int safeGetInt(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getInt(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return 0;
        }
    }

    public static int safeGetInt(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getInt(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0;
        }
    }

    @Nullable
    public static Integer safeGetInteger(ResultSet dbResult, String columnName) {
        try {
            final int result = dbResult.getInt(columnName);
            if (dbResult.wasNull()) {
                return null;
            } else {
                return result;
            }
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Integer safeGetInteger(ResultSet dbResult, int columnIndex) {
        try {
            final int result = dbResult.getInt(columnIndex);
            if (dbResult.wasNull()) {
                return null;
            } else {
                return result;
            }
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    public static long safeGetLong(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getLong(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return 0;
        }
    }

    public static long safeGetLong(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getLong(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0;
        }
    }

    @Nullable
    public static Long safeGetLongNullable(ResultSet dbResult, String columnName) {
        try {
            final long result = dbResult.getLong(columnName);
            return dbResult.wasNull() ? null : result;
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    public static double safeGetDouble(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getDouble(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return 0.0;
        }
    }

    public static double safeGetDouble(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getDouble(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0.0;
        }
    }

    public static float safeGetFloat(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getFloat(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return 0;
        }
    }

    public static float safeGetFloat(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getFloat(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return 0;
        }
    }

    @Nullable
    public static BigDecimal safeGetBigDecimal(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getBigDecimal(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static BigDecimal safeGetBigDecimal(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getBigDecimal(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName) {
        return safeGetBoolean(dbResult, columnName, false);
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName, boolean defValue) {
        try {
            return dbResult.getBoolean(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return defValue;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getBoolean(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return false;
        }
    }

    public static boolean safeGetBoolean(@NotNull ResultSet dbResult, int columnIndex, @NotNull String trueValue) {
        try {
            final String strValue = dbResult.getString(columnIndex);
            return strValue != null && strValue.startsWith(trueValue);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return false;
        }
    }

    public static boolean safeGetBoolean(ResultSet dbResult, String columnName, String trueValue) {
        try {
            final String strValue = dbResult.getString(columnName);
            return strValue != null && strValue.startsWith(trueValue);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return false;
        }
    }

    @Nullable
    public static byte[] safeGetBytes(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getBytes(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Timestamp safeGetTimestamp(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getTimestamp(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Timestamp safeGetTimestamp(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getTimestamp(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static Date safeGetDate(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getDate(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Date safeGetDate(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getDate(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static Time safeGetTime(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getTime(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Time safeGetTime(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getTime(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static SQLXML safeGetXML(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getSQLXML(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static SQLXML safeGetXML(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getSQLXML(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static Object safeGetObject(ResultSet dbResult, String columnName) {
        try {
            return dbResult.getObject(columnName);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Object safeGetObject(ResultSet dbResult, int columnIndex) {
        try {
            return dbResult.getObject(columnIndex);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static <T> T safeGetArray(ResultSet dbResult, String columnName) {
        try {
            Array array = dbResult.getArray(columnName);
            return array == null ? null : (T) array.getArray();
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
            return null;
        }
    }

    @Nullable
    public static Object safeGetArray(ResultSet dbResult, int columnIndex) {
        try {
            Array array = dbResult.getArray(columnIndex);
            return array == null ? null : array.getArray();
        } catch (Exception e) {
            debugColumnRead(dbResult, columnIndex, e);
            return null;
        }
    }

    @Nullable
    public static <T extends Enum<T> & DBPEnumWithValue> T safeGetEnum(@NotNull ResultSet dbResult, @NotNull String columnName, @NotNull Class<T> type) {
        try {
            final int value = dbResult.getInt(columnName);
            for (T constant : type.getEnumConstants()) {
                if (constant.getValue() == value) {
                    return constant;
                }
            }
            log.debug("Can't convert value " + value + " to enum type " + type);
        } catch (Exception e) {
            debugColumnRead(dbResult, columnName, e);
        }
        return null;
    }

    @Nullable
    public static String normalizeIdentifier(@Nullable String value) {
        return value == null ? null : value.trim();
    }

    public static boolean isConnectionAlive(DBPDataSource dataSource, Connection connection) {
        try {
            if (connection == null || connection.isClosed()) {
                return false;
            }
        } catch (SQLException e) {
            log.debug(e);
            return false;
        }

        // Check for active tasks. Do not run ping if there is active task
        for (DBPDataSourceTask task : dataSource.getContainer().getTasks()) {
            if (task.isActiveTask()) {
                return true;
            }
        }

        // Run ping query
        final String testSQL = dataSource.getSQLDialect().getTestSQL();
        int invalidateTimeout = dataSource.getContainer().getPreferenceStore().getInt(ModelPreferences.CONNECTION_VALIDATION_TIMEOUT);

        // Invalidate in non-blocking task.
        // Timeout is CONNECTION_VALIDATION_TIMEOUT + 2 seconds
        final boolean[] isValid = new boolean[1];
        RuntimeUtils.runTask(monitor -> {
            try {
                if (!CommonUtils.isEmpty(testSQL)) {
                    // Execute test SQL
                    try (Statement dbStat = connection.createStatement()) {
                        dbStat.execute(testSQL);
                        isValid[0] = true;
                    }
                } else {
                    try {
                        isValid[0] = connection.isValid(invalidateTimeout);
                    } catch (Throwable e) {
                        // isValid may be unsupported by driver
                        // Let's try to read table list
                        connection.getMetaData().getTables(null, null, "DBEAVERFAKETABLENAMEFORPING", null);
                        isValid[0] = true;
                    }
                }
            } catch (SQLException e) {
                isValid[0] = false;
            }
        }, "Ping connection " + dataSource.getContainer().getName(), invalidateTimeout + 2000, true);
        return isValid[0];
    }

    public static void scrollResultSet(ResultSet dbResult, long offset, boolean forceFetch) throws SQLException {
        // Scroll to first row
        boolean scrolled = false;
        if (!forceFetch) {
            try {
                scrolled = dbResult.absolute((int) offset);
            } catch (SQLException | UnsupportedOperationException | IncompatibleClassChangeError e) {
                // Seems to be not supported
                log.debug(e.getMessage());
            }
        }
        if (!scrolled) {
            // Just fetch first 'firstRow' rows
            for (long i = 1; i <= offset; i++) {
                try {
                    dbResult.next();
                } catch (SQLException e) {
                    throw new SQLException("Can't scroll result set to row " + offset, e);
                }
            }
        }
    }

    public static void reportWarnings(JDBCSession session, SQLWarning rootWarning) {
        for (SQLWarning warning = rootWarning; warning != null; warning = warning.getNextWarning()) {
            if (warning.getMessage() == null && warning.getErrorCode() == 0) {
                // Skip trash [Excel driver]
                continue;
            }
            log.warn("SQL Warning (DataSource: " + session.getDataSource().getContainer().getName() + "; Code: "
                + warning.getErrorCode() + "; State: " + warning.getSQLState() + "): " + warning.getLocalizedMessage());
        }
    }

    @NotNull
    public static String limitQueryLength(@NotNull String query, int maxLength) {
        return query.length() <= maxLength ? query : query.substring(0, maxLength);
    }

    public static DBSForeignKeyModifyRule getCascadeFromNum(int num) {
        return switch (num) {
            case DatabaseMetaData.importedKeyNoAction -> DBSForeignKeyModifyRule.NO_ACTION;
            case DatabaseMetaData.importedKeyCascade -> DBSForeignKeyModifyRule.CASCADE;
            case DatabaseMetaData.importedKeySetNull -> DBSForeignKeyModifyRule.SET_NULL;
            case DatabaseMetaData.importedKeySetDefault -> DBSForeignKeyModifyRule.SET_DEFAULT;
            case DatabaseMetaData.importedKeyRestrict -> DBSForeignKeyModifyRule.RESTRICT;
            default -> DBSForeignKeyModifyRule.UNKNOWN;
        };
    }

    public static DBSForeignKeyModifyRule getCascadeFromName(String name) {
        return switch (name) {
            case "NO ACTION" -> DBSForeignKeyModifyRule.NO_ACTION;
            case "CASCADE" -> DBSForeignKeyModifyRule.CASCADE;
            case "SET NULL" -> DBSForeignKeyModifyRule.SET_NULL;
            case "SET DEFAULT" -> DBSForeignKeyModifyRule.SET_DEFAULT;
            case "RESTRICT" -> DBSForeignKeyModifyRule.RESTRICT;
            default -> DBSForeignKeyModifyRule.UNKNOWN;
        };
    }

    public static void executeSQL(Connection session, String sql, Object... params) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            dbStat.execute();
        }
    }

    public static int executeUpdate(Connection session, String sql, Object... params) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            return dbStat.executeUpdate();
        }
    }

    public static void executeProcedure(Connection session, String sql) throws SQLException {
        try (PreparedStatement dbStat = session.prepareCall(sql)) {
            dbStat.execute();
        }
    }

    public static <T> T executeQuery(Connection session, String sql, Object... params) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            try (ResultSet resultSet = dbStat.executeQuery()) {
                if (resultSet.next()) {
                    return (T) resultSet.getObject(1);
                } else {
                    return null;
                }
            }
        }
    }

    public static void executeStatement(Connection session, String sql, Object... params) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            dbStat.execute();
        }
    }

    public static void executeStatement(Connection session, String sql) throws SQLException {
        try (Statement dbStat = session.createStatement()) {
            dbStat.execute(sql);
        }
    }

    @Nullable
    public static String queryString(Connection session, String sql, Object... args) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    dbStat.setObject(i + 1, args[i]);
                }
            }
            try (ResultSet resultSet = dbStat.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    public static <T> T queryObject(Connection session, String sql, Object... args) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    dbStat.setObject(i + 1, args[i]);
                }
            }
            try (ResultSet resultSet = dbStat.executeQuery()) {
                if (resultSet.next()) {
                    return (T) resultSet.getObject(1);
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Executes query that returns multiple strings as a result
     *
     * @param session current connection session
     * @param sql     query text
     * @param args    optional parameters for the prepared statement
     * @return collection of strings
     */
    @NotNull
    public static List<String> queryStrings(Connection session, String sql, Object... args) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql)) {
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    dbStat.setObject(i + 1, args[i]);
                }
            }
            ArrayList<String> results = new ArrayList<>();
            try (ResultSet resultSet = dbStat.executeQuery()) {
                while (resultSet.next()) {
                    results.add(resultSet.getString(1));
                }
            }
            return results;
        }
    }

    private static void debugColumnRead(ResultSet dbResult, String columnName, Exception error) {
        String colFullId = columnName;
        if (dbResult instanceof JDBCResultSet) {
            colFullId += ":" + ((JDBCResultSet) dbResult).getSession().getDataSource().getContainer().getId();
        }
        synchronized (badColumnNames) {
            final Integer errorCount = badColumnNames.get(colFullId);
            if (errorCount == null) {
                log.debug("Can't get column '" + columnName + "': " + error.getMessage());
            }
            badColumnNames.put(colFullId, errorCount == null ? 0 : errorCount + 1);
        }
    }

    private static void debugColumnRead(ResultSet dbResult, int columnIndex, Exception error) {
        debugColumnRead(dbResult, "#" + columnIndex, error);
    }

    public static void appendFilterClause(@NotNull StringBuilder sql,
                                          @NotNull DBSObjectFilter filter,
                                          @NotNull String columnAlias,
                                          @NotNull boolean firstClause) {
        appendFilterClause(sql, filter, columnAlias, firstClause, null);
    }

    public static void appendFilterClause(@NotNull StringBuilder sql,
                                          @NotNull DBSObjectFilter filter,
                                          @NotNull String columnAlias,
                                          @NotNull boolean firstClause,
                                          DBPDataSource dataSource) {
        if (filter.isNotApplicable()) {
            return;
        }
        if (filter.hasSingleMask()) {
            if (columnAlias != null) {
                firstClause = SQLUtils.appendFirstClause(sql, firstClause);
                sql.append(columnAlias);
            }
            SQLUtils.appendLikeCondition(sql, filter.getSingleMask(), false, dataSource != null ? dataSource.getSQLDialect() : null);
            return;
        }
        List<String> include = filter.getInclude();
        if (!CommonUtils.isEmpty(include)) {
            if (columnAlias != null) {
                firstClause = SQLUtils.appendFirstClause(sql, firstClause);
            }
            sql.append("(");
            for (int i = 0, includeSize = include.size(); i < includeSize; i++) {
                if (i > 0)
                    sql.append(" OR ");
                if (columnAlias != null) {
                    sql.append(columnAlias);
                }
                SQLUtils.appendLikeCondition(sql, include.get(i), false, dataSource != null ? dataSource.getSQLDialect() : null);
            }
            sql.append(")");
        }
        List<String> exclude = filter.getExclude();
        if (!CommonUtils.isEmpty(exclude)) {
            if (columnAlias != null) {
                SQLUtils.appendFirstClause(sql, firstClause);
            }
            sql.append("NOT (");
            for (int i = 0, excludeSize = exclude.size(); i < excludeSize; i++) {
                if (i > 0)
                    sql.append(" OR ");
                if (columnAlias != null) {
                    sql.append(columnAlias);
                }

                SQLUtils.appendLikeCondition(sql, exclude.get(i), false, dataSource != null ? dataSource.getSQLDialect() : null);
            }
            sql.append(")");
        }
    }

    public static void setFilterParameters(PreparedStatement statement, int paramIndex, DBSObjectFilter filter)
        throws SQLException {
        if (filter.isNotApplicable()) {
            return;
        }
        for (String inc : CommonUtils.safeCollection(filter.getInclude())) {
            statement.setString(paramIndex++, SQLUtils.makeSQLLike(inc));
        }
        for (String exc : CommonUtils.safeCollection(filter.getExclude())) {
            statement.setString(paramIndex++, SQLUtils.makeSQLLike(exc));
        }
    }

    public static void rethrowSQLException(Throwable e) throws SQLException {
        if (e instanceof InvocationTargetException) {
            Throwable targetException = ((InvocationTargetException) e).getTargetException();
            if (targetException instanceof SQLException) {
                throw (SQLException) targetException;
            } else {
                throw new SQLException(targetException);
            }
        }
    }

    @NotNull
    public static DBPDataKind resolveDataKind(@Nullable DBPDataSource dataSource, String typeName, int typeID) {
        if (dataSource == null) {
            return JDBCDataSource.getDataKind(typeName, typeID);
        } else if (dataSource instanceof DBPDataTypeProvider) {
            return ((DBPDataTypeProvider) dataSource).resolveDataKind(typeName, typeID);
        } else {
            return DBPDataKind.UNKNOWN;
        }
    }

    public static String escapeWildCards(JDBCSession session, String string) {
        if (string == null || string.isEmpty() || (string.indexOf('%') == -1 && string.indexOf('_') == -1)) {
            return string;
        }
        try {
            SQLDialect dialect = SQLUtils.getDialectFromDataSource(session.getDataSource());
            String escapeStr = dialect.getSearchStringEscape();
            if (CommonUtils.isEmpty(escapeStr) || escapeStr.equals(" ")) {
                return string;
            }
            return string.replace("%", escapeStr + "%").replace("_", escapeStr + "_");
        } catch (Throwable e) {
            log.debug("Error escaping wildcard string", e);
            return string;
        }
    }

    public static boolean queryHasOutputParameters(SQLDialect sqlDialect, String sqlQuery) {
        return sqlQuery.contains("?");
    }


    public static Long queryLong(Connection session, String sql, Object... params) throws SQLException {
        final Number result = executeQuery(session, sql, params);
        if (result != null) {
            return result.longValue();
        }
        return null;
    }

    public static long executeInsertAutoIncrement(Connection session, String sql, String columnName, Object... params) throws SQLException {
        try (PreparedStatement dbStat = session.prepareStatement(sql, getColumnList(columnName))) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    dbStat.setObject(i + 1, params[i]);
                }
            }
            dbStat.execute();
            return getGeneratedKey(dbStat);
        }
    }

    public static long getGeneratedKey(PreparedStatement dbStat) throws SQLException {
        try (final ResultSet keysRS = dbStat.getGeneratedKeys()) {
            keysRS.next();
            return keysRS.getLong(1);
        }
    }

    public static void executeInMetaSession(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object, @NotNull String task,
                                            @NotNull String sql) throws DBCException, SQLException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, task)) {
            try (JDBCStatement statement = session.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    /**
     * Needed for {@link Connection#prepareStatement(String, String[])}
     * Postgres can't find column if column id is in upper case.
     * Oracle doesn't return id of inserted row for {@link Connection#prepareStatement(String, int)}.
     * @param columnName name of column.
     * @return array of column name.
     */
    public static String[] getColumnList(@NotNull String columnName) {
        return new String[] {columnName.toLowerCase()};
    }

    public static boolean isRollbackWarning(SQLException sqlError) {
        return
            SQLState.SQL_25P01.getCode().equals(sqlError.getSQLState());
    }

    /**
     * Checks whether the given exception indicates an unsupported feature error.
     *
     * @param dataSource the data source involved in the operation.
     * @param ex the exception to analyze.
     * @return {@code true} if the exception represents an unsupported feature error;
     *         {@code false} otherwise.
     */
    public static boolean isFeatureNotSupportedError(@Nullable DBPDataSource dataSource, @NotNull Throwable ex) {
        return ex instanceof SQLFeatureNotSupportedException || (dataSource != null && DBExecUtils.discoverErrorType(dataSource, ex)
            == DBPErrorAssistant.ErrorType.FEATURE_UNSUPPORTED);
    }

}
