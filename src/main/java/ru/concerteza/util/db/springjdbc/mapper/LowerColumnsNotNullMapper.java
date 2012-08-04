package ru.concerteza.util.db.springjdbc.mapper;

import com.google.common.collect.ImmutableMap;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import ru.concerteza.util.collection.maps.LowerKeysImmutableMapBuilder;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * <a href="http://static.springsource.org/spring/docs/3.1.x/javadoc-api/org/springframework/jdbc/core/RowMapper.html">RowMapper</a> implementation,
 * converts row into lower case keyed map throwing error on null values
 *
 * User: alexey
 * Date: 7/6/12
 */
public class LowerColumnsNotNullMapper implements RowMapper<Map<String, ?>> {
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ?> mapRow(ResultSet rs, int rowNum) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        ImmutableMap.Builder<String, Object> builder = LowerKeysImmutableMapBuilder.builder();
        for(int i = 1; i <= columnCount; i++) {
            String key = JdbcUtils.lookupColumnName(rsmd, i);
            Object obj = JdbcUtils.getResultSetValue(rs, i);
            builder.put(key, obj);
        }
        return builder.build();
    }
}
