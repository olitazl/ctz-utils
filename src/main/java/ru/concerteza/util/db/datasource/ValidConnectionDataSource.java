package ru.concerteza.util.db.datasource;

import org.apache.commons.lang.UnhandledException;
import org.apache.tomcat.jdbc.pool.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: alexey
 * Date: 6/13/12
 */
public class ValidConnectionDataSource extends DataSource {
    private int checkValidTimeoutSeconds = 0;
    private int checkValidCyclesCount = -1;
    private boolean checkValidEnable = true;

    public void setCheckValidTimeoutSeconds(int checkValidTimeoutSeconds) {
        this.checkValidTimeoutSeconds = checkValidTimeoutSeconds;
    }

    public void setCheckValidCyclesCount(int checkValidCyclesCount) {
        this.checkValidCyclesCount = checkValidCyclesCount;
    }

    public void setCheckValidEnable(boolean checkValidEnable) {
        this.checkValidEnable = checkValidEnable;
    }

    @Override
    public ConnectionPool createPool() throws SQLException {
        if(pool != null) {
            return pool;
        } else {
            synchronized(this) {
                if(pool != null) {
                    return pool;
                } else {
                    pool = checkValidEnable ? new ValidConnectionPool(poolProperties) : new ConnectionPool(poolProperties);
                    return pool;
                }
            }
        }
    }


    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return poolProperties.getUrl();
    }

    private class ValidConnectionPool extends ConnectionPool {

        /**
         * Instantiate a connection pool. This will create connections if initialSize is larger than 0.
         * The {@link org.apache.tomcat.jdbc.pool.PoolProperties} should not be reused for another connection pool.
         *
         * @param prop PoolProperties - all the properties for this connection pool
         * @throws java.sql.SQLException
         */
        public ValidConnectionPool(PoolConfiguration prop) throws SQLException {
            super(prop);
        }

        @Override
        public Connection getConnection() throws SQLException {
            PooledConnection con;
            Connection res;
            int count = 1;
            for(;;) {
                con = borrow();
                res = setupConnection(con);
                if(res.isValid(checkValidTimeoutSeconds)) break;
                if(checkValidCyclesCount > 0 && count == checkValidCyclesCount) break;
                super.abandon(con);
                count += 1;
            }
            return res;
        }

        //            PooledConnection con = borrowConnection(-1,null,null);
        private PooledConnection borrow() {
            try {
                Method borrow = ConnectionPool.class.getDeclaredMethod("borrowConnection", Integer.TYPE, String.class, String.class);
                if(!borrow.isAccessible()) borrow.setAccessible(true);
                return (PooledConnection) borrow.invoke(this, -1, null, null);
            } catch(NoSuchMethodException e) {
                throw new UnhandledException(e);
            } catch(InvocationTargetException e) {
                throw new UnhandledException(e);
            } catch(IllegalAccessException e) {
                throw new UnhandledException(e);
            }
        }
    }
}
