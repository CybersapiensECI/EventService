package eci.dosw.alpha.EventService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EventService API")
                        .description("Gestión de eventos universitarios: listado, filtros, creación y reservas (RSVP).")
                        .version("1.0.0"));
    }
}
