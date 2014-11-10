package ru.concerteza.util.db.partition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Collection;

import static com.alexkasko.springjdbc.typedqueries.common.TypedQueriesUtils.STRING_ROW_MAPPER;
import static junit.framework.Assert.assertEquals;

/**
 * User: alexkasko
 * Date: 11/10/14
 */
public class PartitionManagerTest {

    @Test
    public void test() {
        DataSource ds = new DriverManagerDataSource("jdbc:h2:mem:" + getClass().getSimpleName() + ";DB_CLOSE_DELAY=-1");
        NamedParameterJdbcTemplate jt = new NamedParameterJdbcTemplate(ds);
        PartitionProvider pp = new H2PartitionProvider(jt);
        PartitionManager pm = PartitionManager.builder(pp).withTable("test_table", 2).build();
        ImmutableList<Partition> initted = pm.init();
        assertEquals(0, initted.size());
        pm.ensurePartition("test_table", new LocalDateTime(2012, 1, 1, 12, 30), "foo");
        pm.ensurePartition("test_table", new LocalDateTime(2012, 1, 1, 17, 30), "foo");
        pm.ensurePartition("test_table", new LocalDateTime(2012, 1, 1, 12, 35), "foo");
        pm.ensurePartition("test_table", new LocalDateTime(2012, 1, 1, 12, 35), "bar");
        ImmutableList<Partition> li1 = pm.finder("test_table").find();
        assertEquals(3, li1.size());
        ImmutableList<Partition> li2 = pm.finder("test_table").withUid("foo").find();
        assertEquals(2, li2.size());
        ImmutableList<Partition> li3 = pm.finder("test_table").withToDate(new LocalDateTime(2012, 1, 1, 13, 0)).find();
        assertEquals(2, li3.size());
        ImmutableList<Partition> li4 = pm.finder("test_table").withFromDate(new LocalDateTime(2012, 1, 1, 17, 0)).find();
        assertEquals(1, li4.size());
    }

    private static class H2PartitionProvider implements PartitionProvider {

        private final NamedParameterJdbcTemplate jt;

        private H2PartitionProvider(NamedParameterJdbcTemplate jt) {
            this.jt = jt;
        }

        @Override
        public Collection<String> loadPartitions(String prefix) {
            String sql = "select table_name from information_schema.tables\n" +
                    "    where table_name like :table";
            return jt.query(sql, ImmutableMap.of("table", prefix + "%"), STRING_ROW_MAPPER);
        }

        @Override
        public void createPartition(String prefix, String postfix) {
            if (!"test_table".equals(prefix)) throw new IllegalArgumentException(prefix);
            String sql = "create table test_table_" + postfix + " (foo int)";
            jt.getJdbcOperations().update(sql);
        }
    }
}
