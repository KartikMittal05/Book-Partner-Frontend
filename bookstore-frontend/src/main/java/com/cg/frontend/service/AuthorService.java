package com.cg.frontend.service;

import com.cg.frontend.dto.AuthorDto;
import com.cg.frontend.dto.PageMetaDto;
import com.cg.frontend.dto.TitleAuthorDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${backend.base-url}")
    private String baseUrl;

    public AuthorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<AuthorDto> getAllAuthors(int page, int size) {
        try {
            String url = baseUrl + "/authors?page=" + page + "&size=" + size;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode embedded = response.getBody().path("_embedded").path("authors");
            List<AuthorDto> authors = new ArrayList<>();
            if (embedded.isArray()) {
                for (JsonNode node : embedded) {
                    authors.add(objectMapper.treeToValue(node, AuthorDto.class));
                }
            }
            return authors;
        } catch (Exception e) {
            System.err.println("[AuthorService] getAllAuthors failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public PageMetaDto getPageMeta(int page, int size) {
        try {
            String url = baseUrl + "/authors?page=" + page + "&size=" + size;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode pageMeta = response.getBody().path("page");
            return objectMapper.treeToValue(pageMeta, PageMetaDto.class);
        } catch (Exception e) {
            System.err.println("[AuthorService] getPageMeta failed: " + e.getMessage());
            return new PageMetaDto();
        }
    }

    public AuthorDto getAuthorById(String auId) {
        try {
            String url = baseUrl + "/authors/" + auId;
            ResponseEntity<AuthorDto> response = restTemplate.getForEntity(url, AuthorDto.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[AuthorService] getAuthorById failed for id=" + auId + ": " + e.getMessage());
            return null;
        }
    }

    public List<AuthorDto> searchAuthors(String query) {
        try {
            String url = baseUrl + "/authors?page=0&size=100";
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode embedded = response.getBody().path("_embedded").path("authors");
            List<AuthorDto> authors = new ArrayList<>();
            String q = query.toLowerCase();
            if (embedded.isArray()) {
                for (JsonNode node : embedded) {
                    AuthorDto a = objectMapper.treeToValue(node, AuthorDto.class);
                    if (matchesQuery(a, q)) authors.add(a);
                }
            }
            return authors;
        } catch (Exception e) {
            System.err.println("[AuthorService] searchAuthors failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean matchesQuery(AuthorDto a, String q) {
        return (a.getAuFname() != null && a.getAuFname().toLowerCase().contains(q))
            || (a.getAuLname() != null && a.getAuLname().toLowerCase().contains(q))
            || (a.getAuId() != null && a.getAuId().toLowerCase().contains(q))
            || (a.getCity() != null && a.getCity().toLowerCase().contains(q))
            || (a.getState() != null && a.getState().toLowerCase().contains(q));
    }

    /**
     * Returns null on success, error message string on failure.
     */
    public String createAuthor(AuthorDto author) {
        try {
            String url = baseUrl + "/authors";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuthorDto> request = new HttpEntity<>(author, headers);
            System.out.println("[AuthorService] POST " + url + " -> " + objectMapper.writeValueAsString(author));
            ResponseEntity<AuthorDto> response = restTemplate.postForEntity(url, request, AuthorDto.class);
            System.out.println("[AuthorService] createAuthor response status: " + response.getStatusCode());
            return null; // success
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[AuthorService] createAuthor HTTP error: " + e.getStatusCode() + " body: " + e.getResponseBodyAsString());
            return "Backend error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("[AuthorService] createAuthor exception: " + e.getMessage());
            return "Connection error: " + e.getMessage();
        }
    }

    /**
     * Returns null on success, error message string on failure.
     */
    public String updateAuthor(String auId, AuthorDto author) {
        try {
            String url = baseUrl + "/authors/" + auId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AuthorDto> request = new HttpEntity<>(author, headers);
            System.out.println("[AuthorService] PUT " + url + " -> " + objectMapper.writeValueAsString(author));
            ResponseEntity<AuthorDto> response = restTemplate.exchange(url, HttpMethod.PUT, request, AuthorDto.class);
            System.out.println("[AuthorService] updateAuthor response status: " + response.getStatusCode());
            return null; // success
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[AuthorService] updateAuthor HTTP error: " + e.getStatusCode() + " body: " + e.getResponseBodyAsString());
            return "Backend error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("[AuthorService] updateAuthor exception: " + e.getMessage());
            return "Connection error: " + e.getMessage();
        }
    }

    public List<TitleAuthorDto> getTitlesByAuthor(String auId) {
    try {
        String url = baseUrl + "/titleauthors/search/findByAuId?auId=" + auId;
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);

        JsonNode embedded = response.getBody().path("_embedded").path("titleauthors");
        List<TitleAuthorDto> list = new ArrayList<>();

        if (embedded.isArray()) {
            for (JsonNode node : embedded) {
                TitleAuthorDto ta = new TitleAuthorDto();

                ta.setAuId(node.path("auId").asText(null));
                ta.setTitleId(node.path("titleId").asText(null));
                ta.setAuOrd(node.path("auOrd").asInt(0));
                ta.setRoyaltyper(node.path("royaltyper").asInt(0));

                // 🔥 FIX START
                JsonNode titleNode = node.path("title");

                if (!titleNode.isMissingNode()) {
                    ta.setTitleName(titleNode.path("title").asText(null)); // column name
                    ta.setTitleType(titleNode.path("type").asText(null));
                }
                // 🔥 FIX END

                list.add(ta);
            }
        }
        return list;

    } catch (Exception e) {
        System.err.println("[AuthorService] getTitlesByAuthor failed: " + e.getMessage());
        return new ArrayList<>();
    }
}
}
