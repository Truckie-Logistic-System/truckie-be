package capstone_project.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server()
                .url("https://truckie-be-e3bre6hmfqhcabba.southeastasia-01.azurewebsites.net")
                .description("Azure Production Environment");

        final String securitySchemeName = "bearerAuth";
        SecurityScheme securityScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("Capstone Project API Documentation")
                        .version("v1.0.0")
                        .description("API documentation for Capstone Project")
                        .license(new License()
                                .name("API License")
                                .url("http://domain.vn/license")))
                .servers(List.of(localServer))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, securityScheme));
    }

    @Bean
    public CommandLineRunner printSwaggerUrl() {
        return args -> {
            String swaggerUrl = "https://truckie-be-e3bre6hmfqhcabba.southeastasia-01.azurewebsites.net/swagger-ui.html";
            System.out.println("\n----------------------------------------------------------");
            System.out.println("Swagger UI is available at: " + swaggerUrl);
            System.out.println("----------------------------------------------------------\n");
            System.out.println("Swagger UI on Render is available at: " +  "https://truckiesep2025.onrender.com/swagger-ui.html");
            System.out.println("----------------------------------------------------------\n");
        };
    }
}