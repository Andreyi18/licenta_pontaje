package ro.upt.pontaje.dto.secretariat;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * DTO pentru trimiterea raportului centralizat pe email.
 */
public class SendReportRequest {

    @NotEmpty(message = "Trebuie specificat cel puțin un destinatar")
    private List<@jakarta.validation.constraints.Email(message = "Adresă de email invalidă") String> to;

    private List<@jakarta.validation.constraints.Email(message = "Adresă de email invalidă") String> cc;

    private String subject;

    private String body;

    private UUID departmentId;

    public SendReportRequest() {}

    public List<String> getTo() { return to; }
    public void setTo(List<String> to) { this.to = to; }

    public List<String> getCc() { return cc; }
    public void setCc(List<String> cc) { this.cc = cc; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }
}
