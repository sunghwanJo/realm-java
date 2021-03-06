/*
 * Copyright 2015 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm.dynamic;

import java.util.Date;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.internal.CheckedRow;
import io.realm.internal.ColumnType;
import io.realm.internal.ColumnInfo;
import io.realm.internal.InvalidRow;
import io.realm.internal.LinkView;
import io.realm.internal.Row;
import io.realm.internal.Table;
import io.realm.internal.UncheckedRow;

/**
 * Class that wraps a normal RealmObject in order to allow dynamic access instead of a typed interface.
 * Using a DynamicRealmObject is slower than using the regular model class.
 */
public class DynamicRealmObject extends RealmObject {

     Realm realm;
     Row row;

    /**
     * Creates a dynamic Realm object based on a existing object.
     *
     * @param obj the Realm object to convert to a dynamic object. Only objects managed by Realm can be used.
     * @throws IllegalArgumentException if object isn't managed by a Realm.
     */
    public DynamicRealmObject(RealmObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Non-null object must be provided.");
        }
        Row row = RealmObject.getRow(obj);
        if (row == null) {
            throw new IllegalArgumentException("A object managed by Realm must be provided. This is a standalone object.");
        }
        this.realm = RealmObject.getRealm(obj);
        this.row = (row instanceof CheckedRow) ? (CheckedRow) row : ((UncheckedRow) row).convertToChecked();
    }

    // Create a dynamic object. Only used internally
    DynamicRealmObject(Realm realm, CheckedRow row) {
        this.realm = realm;
        this.row = row;
    }

    /**
     * Returns the {@code boolean} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the boolean value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain booleans.
     */
    public boolean getBoolean(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getBoolean(columnIndex);
    }

    /**
     * Returns the {@code int} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the int value. Integer values exceeding {@code Integer.MAX_VALUE} will wrap.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain integers.
     */
    public int getInt(String fieldName) {
        return (int) getLong(fieldName);
    }

    /**
     * Returns the {@code short} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the short value. Integer values exceeding {@code Short.MAX_VALUE} will wrap.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain integers.
     */
    public short getShort(String fieldName) {
        return (short) getLong(fieldName);
    }

    /**
     * Returns the {@code long} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the long value. Integer values exceeding {@code Long.MAX_VALUE} will wrap.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain integers.
     */
    public long getLong(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getLong(columnIndex);
    }

    /**
     * Returns the {@code byte} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the byte value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain integers.
     */
    public byte getByte(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return (byte) row.getLong(columnIndex);
    }

    /**
     * Returns the {@code float} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the float value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain floats.
     */
    public float getFloat(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getFloat(columnIndex);
    }

    /**
     * Returns the {@code double} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the double value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain doubles.
     */
    public double getDouble(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getDouble(columnIndex);
    }

    /**
     * Returns the {@code byte[]} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the byte[] value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain binary data.
     */
    public byte[] getBlob(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getBinaryByteArray(columnIndex);
    }

    /**
     * Returns the {@code String} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the string value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain Strings.
     */
    public String getString(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getString(columnIndex);
    }

    /**
     * Returns the {@code Date} value for a given field.
     *
     * @param fieldName the name of field.
     * @return the Date value.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain Dates.
     */
    public Date getDate(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return row.getDate(columnIndex);
    }

    /**
     * Returns the object being linked to from this field.
     *
     * @param fieldName Name of field.
     * @return the {@link DynamicRealmObject} representation of the linked object or {@code null} if no object is
     * linked.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain links to other objects.
     */
    public DynamicRealmObject getObject(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        if (row.isNullLink(columnIndex)) {
            return null;
        } else {
            long linkRowIndex = row.getLink(columnIndex);
            CheckedRow linkRow = row.getTable().getCheckedRow(linkRowIndex);
            return new DynamicRealmObject(realm, linkRow);
        }
    }

    /**
     * Returns the {@link io.realm.RealmList} of objects being linked to from this field. This list is returned
     * as a {@link DynamicRealmList}.
     *
     * @param fieldName the name of field.
     * @return the {@link DynamicRealmList} representation of the RealmList.
     * @throws IllegalArgumentException if field name doesn't exists or it doesn't contain a list of links.
     */
    public DynamicRealmList getList(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        return new DynamicRealmList(row.getLinkList(columnIndex), realm);
    }

    /**
     * Checks if the value of a given field is {@code null}.
     *
     * @param fieldName the name of field.
     * @return {@code true} if field value is null, {@code false} otherwise.
     * @throws IllegalArgumentException if field name doesn't exists.
     */
    public boolean isNull(String fieldName) {
        long columnIndex = row.getColumnIndex(fieldName);
        ColumnType type = row.getColumnType(columnIndex);
        switch (type) {
            case LINK:
            case LINK_LIST:
                return row.isNullLink(columnIndex);
            case BOOLEAN:
            case INTEGER:
            case FLOAT:
            case DOUBLE:
            case STRING:
            case BINARY:
            case DATE:
            case TABLE:
            case MIXED:
            default:
                return false;
        }
    }

    /**
     * Checks whether an object has the given field or not.
     *
     * @param fieldName the field name to check.
     * @return {@code true} if the object has a field with the given name, {@code false} otherwise.
     */
    public boolean hasField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }
        return row.hasColumn(fieldName);
    }

    /**
     * Returns the list of field names on this object.
     *
     * @return list of field names on this objects or the empty list if the object doesn't have any fields.
     */
    public String[] getFieldNames() {
        String[] keys = new String[(int) row.getColumnCount()];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = row.getColumnName(i);
        }
        return keys;
    }

    /**
     * Sets the {@code boolean} value of the given field.
     *
     * @param fieldName the field name to update.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't a boolean field.
     */
    public void setBoolean(String fieldName, boolean value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setBoolean(columnIndex, value);
    }

    /**
     * Sets the {@code short} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't an integer field.
     */
    public void setShort(String fieldName, short value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setLong(columnIndex, value);
    }

    /**
     * Sets the {@code int} value of the given field.
     *
     * @param fieldName the field name to update.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't an integer field.
     */
    public void setInt(String fieldName, int value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setLong(columnIndex, value);
    }

    /**
     * Sets the {@code long} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't an integer field.
     */
    public void setLong(String fieldName, long value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setLong(columnIndex, value);
    }

    /**
     * Sets the {@code byte} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't an integer field.
     */
    public void setByte(String fieldName, byte value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setLong(columnIndex, value);
    }

    /**
     * Sets the {@code float} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't an integer field.
     */
    public void setFloat(String fieldName, float value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setFloat(columnIndex, value);
    }

    /**
     * Sets the {@code double} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't a double field.
     */
    public void setDouble(String fieldName, double value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setDouble(columnIndex, value);
    }

    /**
     * Sets the {@code String} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't a String field.
     */
    public void setString(String fieldName, String value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setString(columnIndex, value);
    }

    /**
     * Sets the binary value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't a binary field.
     */
    public void setBlob(String fieldName, byte[] value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setBinaryByteArray(columnIndex, value);
    }

    /**
     * Sets the {@code Date} value of the given field.
     *
     * @param fieldName the field name.
     * @param value the value to insert.
     * @throws IllegalArgumentException if field name doesn't exists or isn't a Date field.
     */
    public void setDate(String fieldName, Date value) {
        long columnIndex = row.getColumnIndex(fieldName);
        row.setDate(columnIndex, value);
    }

    /**
     * Sets a reference to another object on the given field.
     *
     * @param fieldName the field name.
     * @param value the object to link to.
     * @throws IllegalArgumentException if field name doesn't exists, it doesn't link to other Realm objects, or the
     * type of DynamicRealmObject doesn't match.
     */
    public void setObject(String fieldName, DynamicRealmObject value) {
        long columnIndex = row.getColumnIndex(fieldName);
        if (value == null) {
            row.nullifyLink(columnIndex);
        } else {
            if (value.realm == null || value.row == null) {
                throw new IllegalArgumentException("Cannot link to objects that are not part of the Realm.");
            }
            if (!realm.getConfiguration().equals(value.realm.getConfiguration())) {
                throw new IllegalArgumentException("Cannot add an object from another Realm");
            }
            Table table = row.getTable();
            Table inputTable = value.row.getTable();
            if (!table.hasSameSchema(inputTable)) {
                throw new IllegalArgumentException(String.format("Type of object is wrong. Was %s, expected %s",
                        inputTable.getName(), table.getName()));
            }
            row.setLink(columnIndex, value.row.getIndex());
        }
    }

    /**
     * Sets the reference to a {@link DynamicRealmList} on the given field.
     *
     * @param fieldName the field name.
     * @param list the list of references.
     * @throws IllegalArgumentException if field name doesn't exists, it doesn't contain a list of links or the type of
     * the object represented by the DynamicRealmObject doesn't match.
     */
    public void setList(String fieldName, DynamicRealmList list) {
        long columnIndex = row.getColumnIndex(fieldName);
        LinkView links = row.getLinkList(columnIndex);
        links.clear();
        for (DynamicRealmObject obj : list) {
            links.add(obj.row.getIndex());
        }
    }

    /**
     * Deletes this object from the Realm. Accessing any fields after removing the object will throw an
     * {@link IllegalStateException}.
     */
    public void removeFromRealm() {
        row.getTable().moveLastOver(row.getIndex());
        row = InvalidRow.INSTANCE;
    }

    /**
     * Return the type of object. This will normally correspond to the name of a model class that is extending
     * {@link RealmObject}.
     *
     * @return this objects type.
     */
    public String getType() {
        return row.getTable().getName().substring(Table.TABLE_PREFIX.length());
    }

    @Override
    public int hashCode() {
        String realmName = realm.getPath();
        String tableName = row.getTable().getName();
        long rowIndex = row.getIndex();

        int result = 17;
        result = 31 * result + ((realmName != null) ? realmName.hashCode() : 0);
        result = 31 * result + ((tableName != null) ? tableName.hashCode() : 0);
        result = 31 * result + (int) (rowIndex ^ (rowIndex >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DynamicRealmObject other = (DynamicRealmObject) o;

        String path = realm.getPath();
        String otherPath = other.realm.getPath();
        if (path != null ? !path.equals(otherPath) : otherPath != null) {
            return false;
        }

        String tableName = row.getTable().getName();
        String otherTableName = other.row.getTable().getName();
        if (tableName != null ? !tableName.equals(otherTableName) : otherTableName != null) {
            return false;
        }

        return row.getIndex() == other.row.getIndex();
    }

    @Override
    public String toString() {
        if (row == null || !row.isAttached()) {
            return "Invalid object";
        }
        StringBuilder sb = new StringBuilder(row.getTable().getName() + " = [");
        String[] fields = getFieldNames();
        for (String field : fields) {
            long columnIndex = row.getColumnIndex(field);
            ColumnType type = row.getColumnType(columnIndex);
            sb.append("{");
            switch (type) {
                case BOOLEAN: sb.append(field + ": " + row.getBoolean(columnIndex)); break;
                case INTEGER: sb.append(field + ": " + row.getLong(columnIndex)); break;
                case FLOAT: sb.append(field + ": " + row.getFloat(columnIndex)); break;
                case DOUBLE: sb.append(field + ": " + row.getDouble(columnIndex)); break;
                case STRING: sb.append(field + ": " + row.getString(columnIndex)); break;
                case BINARY: sb.append(field + ": " + row.getBinaryByteArray(columnIndex)); break;
                case DATE: sb.append(field + ": " + row.getDate(columnIndex)); break;
                case LINK:
                    if (row.isNullLink(columnIndex)) {
                        sb.append("null");
                    } else {
                        sb.append(field + ": " + row.getTable().getLinkTarget(columnIndex).getName());
                    }
                    break;
                case LINK_LIST:
                    String targetType = row.getTable().getLinkTarget(columnIndex).getName();
                    sb.append(String.format("%s: RealmList<%s>[%s]", field, targetType, row.getLinkList(columnIndex).size()));
                    break;
                case TABLE:
                case MIXED:
                default:
                    sb.append(field + ": ?");
            }
            sb.append("}, ");
        }
        sb.replace(sb.length() - 2, sb.length(), "");
        sb.append("]");
        return sb.toString();
    }
}
