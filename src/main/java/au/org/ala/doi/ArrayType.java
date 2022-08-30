package au.org.ala.doi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArrayType extends net.kaleidos.hibernate.usertype.ArrayType {

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        final Object values = super.nullSafeGet(rs, names, session, owner);

        String[] toConvert = values != null ? (String[])values : new String[1];

        List<String> result = Arrays.stream(toConvert).collect(Collectors.toList());
        return result;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        List<String> list = value != null ? (List) value : new ArrayList<>();
        String[] array = list.toArray(new String[list.size()]);

        super.nullSafeSet(st, array, index, session);
    }
}
