package rim.util;

import java.util.Date;

/**
 * Map要素型変換対応.
 */
public interface TypesKeyValue<K, V> {

    /**
     * 取得処理.
     *
     * @parma n 対象の条件を設定します.
     * @return Object 対象情報が返却されます.
     */
    public V get(Object n);

    /**
     * boolean情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Boolean 情報が返却されます.
     */
    default Boolean getBoolean(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getBoolean(ret);
    }

    /**
     * Byte情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Byte 情報が返却されます.
     */
    default Byte getByte(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getByte(ret);
    }

    /**
     * short情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Short 情報が返却されます.
     */
    default Short getShort(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getShort(ret);
    }

    /**
     * int情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Integer 情報が返却されます.
     */
    default Integer getInteger(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getInteger(ret);
    }

    /**
     * long情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Long 情報が返却されます.
     */
    default Long getLong(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getLong(ret);
    }

    /**
     * float情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Float 情報が返却されます.
     */
    default Float getFloat(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getFloat(ret);
    }

    /**
     * double情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Double 情報が返却されます.
     */
    default Double getDouble(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return TypesUtil.getDouble(ret);
    }

    /**
     * String情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return String 情報が返却されます.
     */
    default String getString(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return String.valueOf(ret);
    }

    /**
     * Date情報を取得.
     *
     * @parma n 対象の条件を設定します.
     * @return Date 情報が返却されます.
     */
    default Date getDate(K n) {
        Object ret = get(n);
        if(ret == null) {
            return null;
        }
        return DateUtil.parseDate(ret);
    }

}
