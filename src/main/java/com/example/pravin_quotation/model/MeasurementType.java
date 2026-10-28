package com.example.pravin_quotation.model;

public enum MeasurementType {
    SQFT("Sq.ft"),
    QUANTITY("Quantity"),
    FILES("Files"),
    FORMULA("Sq.ft"),
    IMAGE("Files");

    private final String displayName;

    MeasurementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSqft() {
        return this == SQFT || this == FORMULA;
    }

    public boolean isQuantity() {
        return this == QUANTITY;
    }

    public boolean isFiles() {
        return this == FILES || this == IMAGE;
    }

    public static MeasurementType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SQFT;
        }
        String clean = value.trim().toUpperCase();
        if (clean.equals("SQFT") || clean.equals("SQ.FT") || clean.equals("FORMULA")) {
            return SQFT;
        }
        if (clean.equals("QUANTITY") || clean.equals("QTY")) {
            return QUANTITY;
        }
        if (clean.equals("FILES") || clean.equals("FILE") || clean.equals("IMAGE")) {
            return FILES;
        }
        return SQFT;
    }
}
