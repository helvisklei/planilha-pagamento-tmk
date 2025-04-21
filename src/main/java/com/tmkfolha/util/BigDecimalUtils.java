package com.tmkfolha.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BigDecimalUtils {

    // Converte double para BigDecimal
    public static BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value);
    }

    // Converte String para BigDecimal (seguro)
    public static BigDecimal toBigDecimal(String value) {
        try {
            return new BigDecimal(value.replace(",", ".")); // Trata vírgula como decimal
        } catch (Exception e) {
            return BigDecimal.ZERO; // Valor padrão em caso de erro
        }
    }

    // Converte BigDecimal para double
    public static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    // Arredondamento com escala definida
    public static BigDecimal round(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    // Evita instanciação acidental
    private BigDecimalUtils() {}
}
