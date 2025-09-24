package capstone_project.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("Local Environment");

        return new OpenAPI()
                .info(new Info()
                        .title("Capstone Project API Documentation")
                        .version("v1.0.0")
                        .description("API documentation for Capstone Project")
                        .license(new License()
                                .name("API License")
                                .url("http://domain.vn/license")))
                .servers(List.of(localServer))
                .components(new Components()); // không cần securityScheme nữa vì dùng cookie
    }

    /**
     * Bật withCredentials để Swagger UI tự động gửi kèm cookie (bao gồm HttpOnly).
     */
    @Bean
    public SwaggerUiConfigParameters swaggerUiConfigParameters(SwaggerUiConfigParameters params) {
        params.setWithCredentials(true); // Cho phép swagger gửi cookie kèm request
        return params;
    }


    @Bean
    public CommandLineRunner printSwaggerUrl() {
        return args -> {
            String swaggerUrl = "http://localhost:8080/swagger-ui.html";
            System.out.println("\n----------------------------------------------------------");
            System.out.println("Swagger UI is available at: " + swaggerUrl);
            System.out.println("----------------------------------------------------------\n");
            System.out.println("Swagger UI on Render is available at: "
                    + "https://truckiesep2025.onrender.com/swagger-ui.html");
            System.out.println("----------------------------------------------------------\n");
        };
    }
}
