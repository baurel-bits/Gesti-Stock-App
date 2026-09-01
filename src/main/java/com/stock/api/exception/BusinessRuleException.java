package com.stock.api.exception;

/**
 * Exception levée lorsqu'une règle de gestion est violée.
 * Utilisée pour les erreurs métier (RG-01, RG-02, RG-03, etc.).
 */
public class BusinessRuleException extends RuntimeException {

    private final String ruleCode;

    public BusinessRuleException(String message) {
        super(message);
        this.ruleCode = null;
    }

    public BusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
