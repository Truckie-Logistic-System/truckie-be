package capstone_project.controller.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class LoginWithoutEmailRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 3339888256948628183L;
    private String username;
    private String password;
}