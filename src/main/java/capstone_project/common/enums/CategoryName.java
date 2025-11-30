package capstone_project.common.enums;

public enum CategoryName {
    NORMAL("NORMAL"),
    FRAGILE("FRAGILE");

    private final String value;

    CategoryName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CategoryName fromString(String value) {
        if (value == null) {
            return NORMAL; // Default to NORMAL
        }
        
        for (CategoryName categoryName : CategoryName.values()) {
            if (categoryName.getValue().equalsIgnoreCase(value) || 
                categoryName.name().equalsIgnoreCase(value)) {
                return categoryName;
            }
        }
        
        return NORMAL; // Default for unknown values
    }
}
