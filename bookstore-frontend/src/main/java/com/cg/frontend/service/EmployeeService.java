package com.cg.frontend.service;

import com.cg.frontend.dto.EmployeeDto;
import com.cg.frontend.dto.JobDto;
import com.cg.frontend.dto.PublisherDtoForEmployee;
import com.cg.frontend.dto.PageMetaDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${backend.base-url}")
    private String baseUrl;

    public EmployeeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<EmployeeDto> getAllEmployees(int page, int size) {
        try {
            String url = baseUrl + "/employees?page=" + page + "&size=" + size;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return parseEmployeeList(response.getBody().path("_embedded").path("employees"));
        } catch (Exception e) {
            System.err.println("[EmployeeService] getAllEmployees failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public PageMetaDto getPageMeta(int page, int size) {
        try {
            String url = baseUrl + "/employees?page=" + page + "&size=" + size;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode pageMeta = response.getBody().path("page");
            return objectMapper.treeToValue(pageMeta, PageMetaDto.class);
        } catch (Exception e) {
            System.err.println("[EmployeeService] getPageMeta failed: " + e.getMessage());
            return new PageMetaDto();
        }
    }

    public EmployeeDto getEmployeeById(String empId) {
        try {
            String url = baseUrl + "/employees/" + empId;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            EmployeeDto dto = parseEmployee(response.getBody());
            // If still null after parse (shouldn't happen on direct lookup), set it manually
            if (dto.getEmpId() == null) dto.setEmpId(empId);
            return dto;
        } catch (Exception e) {
            System.err.println("[EmployeeService] getEmployeeById failed for id=" + empId + ": " + e.getMessage());
            return null;
        }
    }

    public List<EmployeeDto> searchEmployees(String query) {
        try {
            String url = baseUrl + "/employees?page=0&size=200";
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            JsonNode embedded = response.getBody().path("_embedded").path("employees");
            List<EmployeeDto> all = parseEmployeeList(embedded);
            String q = query.toLowerCase();
            List<EmployeeDto> result = new ArrayList<>();
            for (EmployeeDto e : all) {
                if (matchesQuery(e, q)) result.add(e);
            }
            return result;
        } catch (Exception e) {
            System.err.println("[EmployeeService] searchEmployees failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<EmployeeDto> getEmployeesByPubId(String pubId) {
        try {
            String url = baseUrl + "/employees/search/findByPubId?pubId=" + pubId + "&page=0&size=200";
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return parseEmployeeList(response.getBody().path("_embedded").path("employees"));
        } catch (Exception e) {
            System.err.println("[EmployeeService] getEmployeesByPubId failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public JobDto getJobById(Short jobId) {
        try {
            String url = baseUrl + "/jobs/" + jobId;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return objectMapper.treeToValue(response.getBody(), JobDto.class);
        } catch (Exception e) {
            System.err.println("[EmployeeService] getJobById failed for id=" + jobId + ": " + e.getMessage());
            return null;
        }
    }

    public PublisherDtoForEmployee getPublisherById(String pubId) {
        try {
            String url = baseUrl + "/publishers/" + pubId;
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return objectMapper.treeToValue(response.getBody(), PublisherDtoForEmployee.class);
        } catch (Exception e) {
            System.err.println("[EmployeeService] getPublisherById failed for id=" + pubId + ": " + e.getMessage());
            return null;
        }
    }

    public String createEmployee(EmployeeDto employee) {
        try {
            String url = baseUrl + "/employees";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = buildEmployeeJson(employee);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            System.out.println("[EmployeeService] POST " + url + " -> " + body);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            System.out.println("[EmployeeService] createEmployee response status: " + response.getStatusCode());
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[EmployeeService] createEmployee HTTP error: " + e.getStatusCode() + " body: " + e.getResponseBodyAsString());
            return extractErrorMessage(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("[EmployeeService] createEmployee exception: " + e.getMessage());
            return "Could not connect to server. Please try again.";
        }
    }

    public String updateEmployee(String empId, EmployeeDto employee) {
        try {
            String url = baseUrl + "/employees/" + empId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = buildEmployeeJson(employee);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            System.out.println("[EmployeeService] PUT " + url + " -> " + body);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            System.out.println("[EmployeeService] updateEmployee response status: " + response.getStatusCode());
            return null;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("[EmployeeService] updateEmployee HTTP error: " + e.getStatusCode() + " body: " + e.getResponseBodyAsString());
            return extractErrorMessage(e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println("[EmployeeService] updateEmployee exception: " + e.getMessage());
            return "Could not connect to server. Please try again.";
        }
    }

    // ── Private helpers ──────────────────────────────────────────

    private List<EmployeeDto> parseEmployeeList(JsonNode embedded) throws Exception {
        List<EmployeeDto> list = new ArrayList<>();
        if (embedded.isArray()) {
            for (JsonNode node : embedded) {
                list.add(parseEmployee(node));
            }
        }
        return list;
    }

    /**
     * Parses a single employee JSON node.
     *
     * IMPORTANT: Spring Data REST does NOT include empId in the JSON by default
     * unless Employee.class is added to exposeIdsFor() in RepositoryConfig.
     * We extract the ID from _links.self.href as a reliable fallback.
     */
    private EmployeeDto parseEmployee(JsonNode node) throws Exception {
        EmployeeDto dto = new EmployeeDto();

        // Primary: read empId if exposed via exposeIdsFor(Employee.class)
        String empId = node.path("empId").asText(null);

        // Fallback: extract from _links.self.href e.g. "http://localhost:8085/api/employees/PTC11962M"
        if (empId == null || empId.isBlank()) {
            String selfHref = node.path("_links").path("self").path("href").asText(null);
            if (selfHref != null && selfHref.contains("/employees/")) {
                empId = selfHref.substring(selfHref.lastIndexOf("/employees/") + "/employees/".length());
                // Strip any trailing template params like "{?projection}"
                if (empId.contains("{")) {
                    empId = empId.substring(0, empId.indexOf("{"));
                }
                // Strip trailing slash
                if (empId.endsWith("/")) {
                    empId = empId.substring(0, empId.length() - 1);
                }
            }
        }

        dto.setEmpId(empId);
        dto.setFname(node.path("fname").asText(null));
        dto.setMinit(node.path("minit").asText(null));
        dto.setLname(node.path("lname").asText(null));

        if (!node.path("jobId").isMissingNode() && !node.path("jobId").isNull()) {
            dto.setJobId((short) node.path("jobId").asInt());
        }
        if (!node.path("jobLvl").isMissingNode() && !node.path("jobLvl").isNull()) {
            dto.setJobLvl(node.path("jobLvl").asInt());
        }
        dto.setPubId(node.path("pubId").asText(null));

        // hireDate: Spring Data REST serialises LocalDateTime as array [yyyy,M,d,H,m,s] or ISO string
        JsonNode hireDateNode = node.path("hireDate");
        if (!hireDateNode.isMissingNode() && !hireDateNode.isNull()) {
            if (hireDateNode.isArray() && hireDateNode.size() >= 3) {
                int year  = hireDateNode.get(0).asInt();
                int month = hireDateNode.get(1).asInt();
                int day   = hireDateNode.get(2).asInt();
                int hour  = hireDateNode.size() > 3 ? hireDateNode.get(3).asInt() : 0;
                int min   = hireDateNode.size() > 4 ? hireDateNode.get(4).asInt() : 0;
                int sec   = hireDateNode.size() > 5 ? hireDateNode.get(5).asInt() : 0;
                dto.setHireDate(LocalDateTime.of(year, month, day, hour, min, sec));
            } else if (hireDateNode.isTextual()) {
                String text = hireDateNode.asText();
                if (text.length() == 10) text += "T00:00:00";
                dto.setHireDate(LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        return dto;
    }

    /**
     * Builds JSON manually so we can send hireDate as ISO datetime string.
     * datetime-local input gives "yyyy-MM-ddTHH:mm", backend expects LocalDateTime.
     */
    private String buildEmployeeJson(EmployeeDto e) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"empId\":").append(jsonStr(e.getEmpId())).append(",");
        sb.append("\"fname\":").append(jsonStr(e.getFname())).append(",");
        sb.append("\"minit\":").append(jsonStr(e.getMinit())).append(",");
        sb.append("\"lname\":").append(jsonStr(e.getLname())).append(",");
        sb.append("\"jobId\":").append(e.getJobId() != null ? e.getJobId() : "null").append(",");
        sb.append("\"jobLvl\":").append(e.getJobLvl() != null ? e.getJobLvl() : "null").append(",");
        sb.append("\"pubId\":").append(jsonStr(e.getPubId())).append(",");
        String hd = e.getHireDate() != null ? e.getHireDate().toString() : null;
        if (hd != null && hd.length() == 10) hd += "T00:00:00";
        sb.append("\"hireDate\":").append(jsonStr(hd));
        sb.append("}");
        return sb.toString();
    }

    private String jsonStr(String val) {
        if (val == null || val.isBlank()) return "null";
        return "\"" + val.replace("\"", "\\\"") + "\"";
    }

    private boolean matchesQuery(EmployeeDto e, String q) {
        return (e.getEmpId() != null && e.getEmpId().toLowerCase().contains(q))
            || (e.getFname() != null && e.getFname().toLowerCase().contains(q))
            || (e.getLname() != null && e.getLname().toLowerCase().contains(q))
            || (e.getPubId() != null && e.getPubId().toLowerCase().contains(q));
    }
    /**
     * Parses the backend JSON error body and returns just the human-readable "message" field.
     * Falls back to a generic message if parsing fails.
     * Example body: {"error":"Bad Request","message":"Job level 100 is out of range...","status":400}
     */
    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("message") && !root.path("message").isNull()) {
                return root.path("message").asText();
            }
            if (root.has("error")) {
                return root.path("error").asText();
            }
        } catch (Exception ignored) {}
        return "An unexpected error occurred. Please check your inputs and try again.";
    }

}