package com.carenotificationservice.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class InetAddressConverter implements AttributeConverter<String, Object> {

    @Override
    public Object convertToDatabaseColumn(String attribute) {
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("inet");
            pgObject.setValue(attribute); // null is valid — PGobject preserves the type for setNull
            return pgObject;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to convert String to inet: " + attribute, e);
        }
    }

    @Override
    public String convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData.toString();
    }
}
