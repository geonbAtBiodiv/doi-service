package au.org.ala.doi

import org.apache.commons.lang.ObjectUtils
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType
import org.postgresql.util.PGobject

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException
import java.sql.Types;

class CitextType implements UserType {

    private final Type userType = String

    @Override
    int[] sqlTypes() {
        return Types.OTHER as int[]
    }

    @Override
    Class returnedClass() {
        return userType
    }

    @Override
    boolean equals(Object x, Object y) throws HibernateException {
        if (x instanceof String && y instanceof String) {
            return x.equalsIgnoreCase(y)
        } else {
            return ObjectUtils.equals(x,y)
        }
    }

    @Override
    int hashCode(Object x) throws HibernateException {
        x ? x.hashCode() : 0
    }

    @Override
    Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        PGobject o = rs.getObject(names[0]) as PGobject
        return o?.value
    }

    @Override
    void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER)
        } else {
            st.setString(index, (String)value)
        }
    }

    @Override
    Object deepCopy(Object value) throws HibernateException {
        (String) value
    }

    @Override
    boolean isMutable() {
        return false
    }

    @Override
    Serializable disassemble(Object value) throws HibernateException {
        (String) value
    }

    @Override
    Object assemble(Serializable cached, Object owner) throws HibernateException {
        (String) cached
    }

    @Override
    Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original
    }
}
