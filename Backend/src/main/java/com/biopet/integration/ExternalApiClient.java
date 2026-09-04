package com.biopet.integration;

import com.biopet.exception.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Low-level HTTP client for the api-ninjas animals API. Translates
 * transport-level failures (timeouts, 4xx/5xx responses) into
 * {@link ExternalApiException}; caching and BIOPET-specific response
 * shaping live in {@link ExternalApiService}.
 */
@Component
public class ExternalApiClient {

    private final RestTemplate restTemplate;

    @Value("${app.external-api.base-url:https://api.api-ninjas.com/v1/animals}")
    private String baseUrl;

    @Value("${app.external-api.key:}")
    private String apiKey;

    public ExternalApiClient(RestTemplate externalApiRestTemplate) {
        this.restTemplate = externalApiRestTemplate;
    }

    /**
     * Queries the external animals API by species name.
     *
     * @param especie species name to search for
     * @return matching animal records (possibly empty if none found)
     * @throws ExternalApiException if the external API times out, rate-limits, or returns a client/server error
     */
    public List<AnimalApiNinjasDto> buscarPorEspecie(String especie) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("name", especie)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<AnimalApiNinjasDto[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AnimalApiNinjasDto[].class);
            AnimalApiNinjasDto[] body = response.getBody();
            return body == null ? List.of() : List.of(body);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            throw new ExternalApiException("Límite de solicitudes a la API externa excedido (429).", ex);
        } catch (HttpClientErrorException ex) {
            throw new ExternalApiException("La API externa rechazó la solicitud (" + ex.getStatusCode() + ").", ex);
        } catch (HttpServerErrorException ex) {
            throw new ExternalApiException("La API externa presentó un error interno (" + ex.getStatusCode() + ").", ex);
        } catch (ResourceAccessException ex) {
            throw new ExternalApiException("Tiempo de espera agotado al consultar la API externa.", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AnimalApiNinjasDto(
            String name,
            Taxonomy taxonomy,
            Characteristics characteristics,
            List<String> locations
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Taxonomy(String kingdom, String phylum, String scientific_name, String order) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Characteristics(String diet, String habitat, String lifespan, String slogan) {}
    }
}