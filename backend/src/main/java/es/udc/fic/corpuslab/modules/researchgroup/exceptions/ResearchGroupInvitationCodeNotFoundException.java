package es.udc.fic.corpuslab.modules.researchgroup.exceptions;

public class ResearchGroupInvitationCodeNotFoundException extends RuntimeException {
    private final String code;

    public ResearchGroupInvitationCodeNotFoundException(String code) {
        super("Invitation code not found: " + code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
