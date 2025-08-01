package ai4mbse.subsystems;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;

import ai4mbse.model.AllocationCandidate;

/**
 * AI Integration Subsystem.
 * Verwaltet die Kommunikation mit Google Gemini API.
 * 
 * KRITISCH: Dieses Service arbeitet ausschließlich mit Block-stereotyped Subsystemen!
 * Es empfängt nur Block-Elemente von Main.java für die KI-Analyse.
 */
public class AIIntegrationService {
    
    /** Standard-URL für die Gemini API */
    private static final String API_URL = System.getenv().getOrDefault("GEMINI_API_URL",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent");
    
    /** Timeout für HTTP-Verbindungen in Millisekunden */
    private static final int TIMEOUT = 60000;
    
    /** GSON-Instanz für JSON-Serialisierung */
    private static final Gson gson = new Gson();

    /** Speichert den letzten Request-Payload für Debugging-Zwecke */
    public static String lastRequestPayload;

    /**
     * Verarbeitet die Antwort der KI und wandelt sie in AllocationCandidate-Objekte um.
     * 
     * @param aiResponse Die rohe Antwort der Gemini API
     * @param requirementElement Das ausgewählte Requirement-Element
     * @param subsystemNameToIdMap Zuordnung von Subsystem-Namen zu IDs
     * @return Liste validierter Allokations-Kandidaten
     */
    public List<AllocationCandidate> processAIResponse(
            String aiResponse,
            Element requirementElement,
            java.util.Map<String, String> subsystemNameToIdMap
    ) {
        // AI-Antwort wird verarbeitet

        // Bereinigung der KI-Antwort: Entfernung von Markdown-Formatierung
        String cleanedAiResponse = aiResponse.trim();

        // Entfernung von führenden Markdown Code-Block-Markierungen
        if (cleanedAiResponse.startsWith("```json")) {
            cleanedAiResponse = cleanedAiResponse.substring("```json".length());
        } else if (cleanedAiResponse.startsWith("```")) {
            cleanedAiResponse = cleanedAiResponse.substring("```".length());
        }

        // Entfernung von abschließenden Markdown Code-Block-Markierungen
        if (cleanedAiResponse.endsWith("```")) {
            cleanedAiResponse = cleanedAiResponse.substring(0, cleanedAiResponse.length() - "```".length());
        }
        
        cleanedAiResponse = cleanedAiResponse.trim();

        try {
            // Deserialisierung der JSON-Antwort zu AllocationCandidate-Objekten
            Type listType = new TypeToken<List<AllocationCandidate>>() {}.getType();
            List<AllocationCandidate> candidates = gson.fromJson(cleanedAiResponse, listType);

            // Validierung der KI-Antwort
            if (candidates == null || candidates.isEmpty()) {
                log("Keine gültigen Kandidaten aus der KI-Antwort erhalten!");
                return new ArrayList<>();
            }

            // Filterung und Validierung der Kandidaten gegen das Modell
            List<AllocationCandidate> validCandidates = new ArrayList<>();
            for (AllocationCandidate candidate : candidates) {
                String subsystemId = subsystemNameToIdMap.get(candidate.getSubsystemName());
                if (subsystemId != null) {
                    candidate.setRequirementElement(requirementElement);
                    candidate.setSubsystemId(subsystemId);
                    validCandidates.add(candidate);
                } else {
                    log("WARNING: Subsystem '" + candidate.getSubsystemName() + "' from AI response not found in model's subsystem list. Skipping.");
                }
            }

            return validCandidates;

        } catch (JsonSyntaxException e) {
            log("Fehler beim Parsen der KI-Antwort: " + e.getMessage() + "\nAI Response was: " + aiResponse);
            return new ArrayList<>();
        } catch (Exception e) {
            log("Unerwarteter Fehler in processAIResponse: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Erstellt einen erweiterten Prompt für die KI-Analyse.
     * 
     * KRITISCH: Arbeitet nur mit Block-stereotyped Subsystem-Namen!
     * Die Liste subsystemNames enthält ausschließlich Block-Elemente.
     * 
     * @param reqText Der Text des Requirements
     * @param subsystemNames Liste der verfügbaren Block-Subsystem-Namen (nur Block-Stereotyp!)
     * @return Der optimierte Prompt-String
     */
    public String buildAdvancedPrompt(String reqText, List<String> subsystemNames) {
        StringBuilder subsystemListBuilder = new StringBuilder();       
        for (String n : subsystemNames) {
            subsystemListBuilder.append("- ").append(n).append("\n");
        }

        // Improved expert prompt using advanced prompting techniques for better accuracy
        String prompt = "Hello Gemini,\n\n" +
                        "Please act as an **expert Systems Engineer specializing in Model-Based Systems Engineering (MBSE)** with extensive experience in complex system architectures, particularly those modeled using tools like CATIA Magic / No Magic products (or similar SysML-compliant tools). Your analytical skills allow you to infer logical relationships even when they are not explicitly stated.\n\n" +
                        "I will provide you with a requirement from a systems engineering project. Your primary task is to meticulously analyze this requirement and provide well-founded, actionable suggestions for requirement allocation to appropriate subsystems.\n\n" +
                        "**CRITICAL INSTRUCTIONS – PLEASE ADHERE STRICTLY:**\n\n" +
                        "1. **Single-Pass Task:** This is a **single-pass analysis**. You will not receive follow-up clarifications or opportunities for correction. It is paramount that your first response is as accurate, complete, and helpful as possible.\n" +
                        "2. **Precision and Justification:** Every suggested allocation must be logically justified based *only* on the information present in the requirement description and subsystem names. Do not invent information or make assumptions beyond the data.\n" +
                        "3. **Semantic Fit:** Only suggest allocations where subsystems semantically fit the requirement's intent and technical domain.\n" +
                        "4. **Confidence Scoring:** Provide realistic confidence scores (0.0-1.0) based on how well each subsystem matches the requirement's purpose.\n\n" +
                        "**Your Detailed Task:**\n\n" +
                        "1. **Parse:** Thoroughly analyze the provided requirement description.\n" +
                        "2. **Evaluate:** For each available subsystem, assess its suitability for implementing or satisfying the requirement.\n" +
                        "3. **Score:** Assign realistic confidence scores where:\n" +
                        "   - 0.9-1.0: Perfect semantic match, primary responsibility\n" +
                        "   - 0.7-0.8: Strong match, likely implementation candidate\n" +
                        "   - 0.4-0.6: Moderate match, supporting role possible\n" +
                        "   - 0.1-0.3: Weak match, indirect contribution only\n" +
                        "   - 0.0: No logical connection\n" +
                        "4. **Justify:** Provide concise (1-2 sentences) justification explaining *why* each allocation is logical.\n\n" +
                        "**Requirement to Analyze:**\n" +
                        "\"" + reqText + "\"\n\n" +
                        "**Available Subsystems:**\n" +
                        subsystemListBuilder.toString() +
                        "\n" +
                        "**Output Format:** Provide your analysis as a JSON array of objects. Each object must have exactly three keys:\n" +
                        "- 'subsystemName' (string, exact name from Available Subsystems list)\n" +
                        "- 'score' (float, confidence value 0.0-1.0)\n" +
                        "- 'justification' (string, concise reasoning for the score)\n\n" +
                        "**Example JSON Structure:**\n" +
                        "[\n" +
                        "  { \"subsystemName\": \"Power Management System\", \"score\": 0.95, \"justification\": \"Direct responsibility for power-related requirements based on domain expertise.\" },\n" +
                        "  { \"subsystemName\": \"Control System\", \"score\": 0.65, \"justification\": \"May interface with power systems for monitoring and control functions.\" },\n" +
                        "  { \"subsystemName\": \"User Interface\", \"score\": 0.15, \"justification\": \"Minimal relevance, only potential status display capabilities.\" }\n" +
                        "]\n\n" +
                        "**IMPORTANT:** Return only valid JSON and nothing else. Ensure all subsystem names exactly match those provided in the Available Subsystems list.";

        return prompt;
    }

    /**
     * Extrahiert den Requirement-Text aus einem Element.
     * 
     * @param selectedRequirement Das Requirement-Element
     * @return Der extrahierte Text
     */
    public String extractRequirementText(Element selectedRequirement) {
        return selectedRequirement.getOwnedComment().stream()
            .map(Comment::getBody)
            .filter(b -> b != null && !b.trim().isEmpty())
            .findFirst().orElse(selectedRequirement.getHumanName());
    }

    /**
     * Ruft die Gemini API auf - delegiert an GeminiClient.
     * 
     * @param prompt Der Text-Prompt für die KI
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @return Die von der KI generierte Antwort als String
     */
    public String callGeminiAPI(String prompt, String apiKey) throws IOException, JsonSyntaxException {
        return GeminiClient.callGeminiAPI(prompt, apiKey);
    }

    // Temporäre Log-Methode - wird später durch LoggingService ersetzt
    private void log(String message) {
        try {
            if (Application.getInstance() != null && Application.getInstance().getGUILog() != null)
                Application.getInstance().getGUILog().log("[AI4MBSE] " + message);
            else System.out.println("[AI4MBSE] " + message);
        } catch (Exception e) {
            System.err.println("Logging failed: " + e.getMessage());
        }
    }
}

/**
 * Client für die Kommunikation mit der Google Gemini API.
 */
class GeminiClient {
    /** Standard-URL für die Gemini API */
    private static final String API_URL = System.getenv().getOrDefault("GEMINI_API_URL",
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent");
    
    /** Timeout für HTTP-Verbindungen in Millisekunden */
    private static final int TIMEOUT = 60000;
    
    /** GSON-Instanz für JSON-Serialisierung */
    private static final Gson gson = new Gson();

    /** Speichert den letzten Request-Payload für Debugging-Zwecke */
    public static String lastRequestPayload;

    /**
     * Führt einen API-Aufruf an Google Gemini durch.
     * 
     * @param prompt Der Text-Prompt für die KI
     * @param apiKey Der API-Schlüssel für die Authentifizierung
     * @return Die von der KI generierte Antwort als String
     * @throws IOException Bei Netzwerk- oder API-Fehlern
     * @throws JsonSyntaxException Bei fehlerhaften JSON-Antworten
     */
    public static String callGeminiAPI(String prompt, String apiKey) throws IOException, JsonSyntaxException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL + "?key=" + apiKey);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);

            // Aufbau des korrekten Gemini API JSON-Payloads
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", prompt);
            JsonArray partsArray = new JsonArray();
            partsArray.add(textPart);
            JsonObject content = new JsonObject();
            content.add("parts", partsArray);
            JsonArray contentsArray = new JsonArray();
            contentsArray.add(content);
            JsonObject req = new JsonObject();
            req.add("contents", contentsArray);

            // Serialisierung des Request-Payloads
            lastRequestPayload = gson.toJson(req);

            // HTTP-Request senden
            conn.setDoOutput(true);
            try (OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8)) {
                w.write(lastRequestPayload);
            }

            // Antwort lesen und verarbeiten
            int code = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                code < 400 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            if (code >= 400) throw new IOException("API Error: " + sb.toString());
            
            // JSON-Antwort parsen und Text extrahieren
            JsonObject resp = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray cands = resp.getAsJsonArray("candidates");
            return cands.get(0).getAsJsonObject()
                .getAsJsonObject("content").getAsJsonArray("parts")
                .get(0).getAsJsonObject().get("text").getAsString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }   
}