package capstone_project.common.enums;

import java.math.BigDecimal;

public enum UnitEnum {
    Kí,
    Yến, Tạ, Tấn;

    public BigDecimal toTon() {
        return switch (this) {
            case Kí -> new BigDecimal("0.001");
            case Yến -> new BigDecimal("0.01");
            case Tạ -> new BigDecimal("0.1");
            case Tấn -> BigDecimal.ONE;
        };
    }
}
