package com.cg.frontend.service;

import com.cg.frontend.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TitleService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${backend.base-url}")
    private String baseUrl;

    public TitleService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // ── Titles ────────────────────────────────────────────────────────────────

    public List<TitleDto> getAllTitles(int page, int size) {
        try {
            String url = baseUrl + "/titles?page=" + page + "&size=" + size;
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            return parseTitles(resp.getBody().path("_embedded").path("titles"));
        } catch (Exception e) {
            System.err.println("[TitleService] getAllTitles failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public PageMetaDto getPageMeta(int page, int size) {
        try {
            String url = baseUrl + "/titles?page=" + page + "&size=" + size;
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            return objectMapper.treeToValue(resp.getBody().path("page"), PageMetaDto.class);
        } catch (Exception e) {
            System.err.println("[TitleService] getPageMeta failed: " + e.getMessage());
            return new PageMetaDto();
        }
    }

    public TitleDto getTitleById(String titleId) {
        try {
            String url = baseUrl + "/titles/" + titleId;
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            return parseTitle(resp.getBody());
        } catch (Exception e) {
            System.err.println("[TitleService] getTitleById failed: " + e.getMessage());
            return null;
        }
    }

    public List<TitleDto> searchTitles(String query) {
        try {
            String url = baseUrl + "/titles?page=0&size=200";
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            List<TitleDto> all = parseTitles(resp.getBody().path("_embedded").path("titles"));
            String q = query.toLowerCase();
            List<TitleDto> result = new ArrayList<>();
            for (TitleDto t : all) {
                if ((t.getTitleId() != null && t.getTitleId().toLowerCase().contains(q))
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(q))
                        || (t.getType() != null && t.getType().toLowerCase().contains(q))
                        || (t.getResolvedPubId() != null && t.getResolvedPubId().toLowerCase().contains(q))) {
                    result.add(t);
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("[TitleService] searchTitles failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String createTitle(TitleDto title) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("titleId", title.getTitleId());
            body.put("title", title.getTitle());
            body.put("type", title.getType());
            body.put("price", title.getPrice());
            body.put("advance", title.getAdvance());
            body.put("royalty", title.getRoyalty());
            body.put("ytdSales", title.getYtdSales());
            body.put("notes", title.getNotes());
            body.put("pubdate", title.getPubdate() != null ? title.getPubdate() + "T00:00:00" : null);
            if (title.getPubId() != null && !title.getPubId().isBlank()) {
                body.put("publisher", baseUrl + "/publishers/" + title.getPubId());
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            restTemplate.postForEntity(baseUrl + "/titles", req, JsonNode.class);
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[TitleService] createTitle HTTP error: " + e.getStatusCode());
            return "Backend error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("[TitleService] createTitle exception: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String updateTitle(String titleId, TitleDto title) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("titleId", titleId);
            body.put("title", title.getTitle());
            body.put("type", title.getType());
            body.put("price", title.getPrice());
            body.put("advance", title.getAdvance());
            body.put("royalty", title.getRoyalty());
            body.put("ytdSales", title.getYtdSales());
            body.put("notes", title.getNotes());
            body.put("pubdate", title.getPubdate() != null ? title.getPubdate() + "T00:00:00" : null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            restTemplate.exchange(baseUrl + "/titles/" + titleId, HttpMethod.PUT, req, JsonNode.class);

            // Update publisher association separately — Spring Data REST requires
            // this for LAZY @ManyToOne — the main PUT body ignores URI associations
            if (title.getPubId() != null && !title.getPubId().isBlank()) {
                HttpHeaders assocHeaders = new HttpHeaders();
                assocHeaders.setContentType(MediaType.valueOf("text/uri-list"));
                HttpEntity<String> assocReq = new HttpEntity<>(
                    baseUrl + "/publishers/" + title.getPubId(), assocHeaders);
                restTemplate.exchange(
                    baseUrl + "/titles/" + titleId + "/publisher",
                    HttpMethod.PUT, assocReq, JsonNode.class);
            }

            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[TitleService] updateTitle HTTP error: " + e.getStatusCode());
            return "Backend error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("[TitleService] updateTitle exception: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ── Publishers ────────────────────────────────────────────────────────────

    public List<PublisherDto> getAllPublishers() {
        try {
            String url = baseUrl + "/publishers?page=0&size=100";
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode arr = resp.getBody().path("_embedded").path("publishers");
            List<PublisherDto> list = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    list.add(objectMapper.treeToValue(n, PublisherDto.class));
                }
            }
            return list;
        } catch (Exception e) {
            System.err.println("[TitleService] getAllPublishers failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Authors ───────────────────────────────────────────────────────────────

    public List<Map<String, String>> getAllAuthors() {
        try {
            String url = baseUrl + "/authors?page=0&size=200";
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode arr = resp.getBody().path("_embedded").path("authors");
            List<Map<String, String>> list = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    Map<String, String> a = new LinkedHashMap<>();
                    a.put("auId",    n.path("auId").asText(""));
                    a.put("auFname", n.path("auFname").asText(""));
                    a.put("auLname", n.path("auLname").asText(""));
                    list.add(a);
                }
            }
            return list;
        } catch (Exception e) {
            System.err.println("[TitleService] getAllAuthors failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<TitleAuthorDto> getAuthorsByTitle(String titleId) {
        try {
            String url = baseUrl + "/titleauthors/search/findByTitleId?titleId=" + titleId;
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode arr = resp.getBody().path("_embedded").path("titleauthors");
            List<TitleAuthorDto> list = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    TitleAuthorDto ta = new TitleAuthorDto();
                    ta.setTitleId(n.path("titleId").asText(null));
                    ta.setAuId(n.path("auId").asText(null));
                    ta.setAuOrd(n.path("auOrd").asInt(0));
                    ta.setRoyaltyper(n.path("royaltyper").asInt(0));
                    list.add(ta);
                }
            }
            return list;
        } catch (Exception e) {
            System.err.println("[TitleService] getAuthorsByTitle failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, String> getTitleAuthorHrefs(String titleId) {
        Map<String, String> hrefs = new LinkedHashMap<>();
        try {
            String url = baseUrl + "/titleauthors/search/findByTitleId?titleId=" + titleId;
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode arr = resp.getBody().path("_embedded").path("titleauthors");
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    String auId     = n.path("auId").asText(null);
                    String selfHref = n.path("_links").path("self").path("href").asText(null);
                    if (auId != null && selfHref != null) {
                        hrefs.put(auId, selfHref);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[TitleService] getTitleAuthorHrefs failed: " + e.getMessage());
        }
        return hrefs;
    }

    public void linkAuthorsToTitle(String titleId, List<String> authorIds, List<Integer> royalties) {
        int ord = 1;
        for (int i = 0; i < authorIds.size(); i++) {
            String auId = authorIds.get(i);
            int royaltyper = (royalties != null && i < royalties.size() && royalties.get(i) != null)
                    ? royalties.get(i) : 0;
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("auId", auId);
                body.put("titleId", titleId);
                body.put("auOrd", ord++);
                body.put("royaltyper", royaltyper);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
                restTemplate.postForEntity(baseUrl + "/titleauthors", req, JsonNode.class);
            } catch (Exception e) {
                System.err.println("[TitleService] linkAuthorsToTitle failed for auId=" + auId + ": " + e.getMessage());
            }
        }
    }

    public void replaceAuthorsForTitle(String titleId, List<String> newAuthorIds) {
        Map<String, String> hrefs = getTitleAuthorHrefs(titleId);
        for (String href : hrefs.values()) {
            try {
                restTemplate.delete(href);
            } catch (Exception e) {
                System.err.println("[TitleService] replaceAuthors delete failed: " + e.getMessage());
            }
        }
        if (newAuthorIds != null && !newAuthorIds.isEmpty()) {
            linkAuthorsToTitle(titleId, newAuthorIds, null);
        }
    }

    // ── Royalty Schedule ──────────────────────────────────────────────────────

    public List<RoySchedDto> getRoySchedByTitle(String titleId) {
        try {
            String url = baseUrl + "/royscheds/search/findByTitleTitleId?titleId=" + titleId;
            ResponseEntity<JsonNode> resp = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode arr = resp.getBody().path("_embedded").path("royscheds");
            List<RoySchedDto> list = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode n : arr) {
                    RoySchedDto rs = objectMapper.treeToValue(n, RoySchedDto.class);
                    String selfHref = n.path("_links").path("self").path("href").asText(null);
                    if (selfHref != null && selfHref.contains("/royscheds/")) {
                        String id = selfHref.substring(selfHref.lastIndexOf("/royscheds/") + "/royscheds/".length());
                        try { rs.setRoySchedId(Integer.parseInt(id)); } catch (NumberFormatException ignored) {}
                    }
                    list.add(rs);
                }
            }
            return list;
        } catch (Exception e) {
            System.err.println("[TitleService] getRoySchedByTitle failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public String createRoySched(RoySchedDto rs) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("titleId", rs.getTitleId());
            body.put("lorange", rs.getLorange());
            body.put("hirange", rs.getHirange());
            body.put("royalty", rs.getRoyalty());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            restTemplate.postForEntity(baseUrl + "/royscheds", req, JsonNode.class);
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[TitleService] createRoySched HTTP error: " + e.getStatusCode());
            return "Backend error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("[TitleService] createRoySched exception: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    public String updateRoySched(Integer roySchedId, RoySchedDto rs) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("titleId", rs.getTitleId());
            body.put("lorange", rs.getLorange());
            body.put("hirange", rs.getHirange());
            body.put("royalty", rs.getRoyalty());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> req = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            restTemplate.exchange(baseUrl + "/royscheds/" + roySchedId, HttpMethod.PUT, req, JsonNode.class);
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[TitleService] updateRoySched HTTP error: " + e.getStatusCode());
            return "Backend error " + e.getStatusCode() + ": " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.err.println("[TitleService] updateRoySched exception: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    // ── Parsing Helpers ───────────────────────────────────────────────────────

    private List<TitleDto> parseTitles(JsonNode arr) throws Exception {
        List<TitleDto> list = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode n : arr) list.add(parseTitle(n));
        }
        return list;
    }

    private TitleDto parseTitle(JsonNode n) throws Exception {
        TitleDto t = objectMapper.treeToValue(n, TitleDto.class);
        String selfHref = n.path("_links").path("self").path("href").asText(null);
        if (selfHref != null && selfHref.contains("/titles/")) {
            String extractedId = selfHref.substring(
                selfHref.lastIndexOf("/titles/") + "/titles/".length());
            t.setTitleId(extractedId);
        }
        return t;
    }
}