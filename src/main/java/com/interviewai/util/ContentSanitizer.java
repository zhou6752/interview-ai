package com.interviewai.util;

public class ContentSanitizer {

    private String state = "NORMAL";

    public String sanitize(String chunk) {
        if (chunk == null || chunk.isEmpty()) return "";

        StringBuilder output = new StringBuilder();
        for (int i = 0; i < chunk.length(); i++) {
            char c = chunk.charAt(i);
            String result = transition(c);
            if (result != null) {
                output.append(result);
            }
        }
        return output.toString();
    }

    private String transition(char c) {
        switch (state) {
            case "NORMAL":
                if (c == 'd') { state = "SAW_D"; return null; }
                if (c == '[') { state = "SAW_BRACKET"; return null; }
                if (c == 'e') { state = "SAW_E"; return null; }
                return String.valueOf(c);

            case "SAW_D":
                if (c == 'a') { state = "SAW_DA"; return null; }
                else { state = "NORMAL"; return "d" + c; }

            case "SAW_DA":
                if (c == 't') { state = "SAW_DAT"; return null; }
                else { state = "NORMAL"; return "da" + c; }

            case "SAW_DAT":
                if (c == 'a') { state = "SAW_DATA"; return null; }
                else { state = "NORMAL"; return "dat" + c; }

            case "SAW_DATA":
                if (c == ':') { state = "NORMAL"; return null; }
                else { state = "NORMAL"; return "data" + c; }

            case "SAW_BRACKET":
                if (c == 'D') { state = "SAW_DONE_D"; return null; }
                else { state = "NORMAL"; return "[" + c; }

            case "SAW_DONE_D":
                if (c == 'O') { state = "SAW_DONE_O"; return null; }
                else { state = "NORMAL"; return "[D" + c; }

            case "SAW_DONE_O":
                if (c == 'N') { state = "SAW_DONE_N"; return null; }
                else { state = "NORMAL"; return "[DO" + c; }

            case "SAW_DONE_N":
                if (c == 'E') { state = "SAW_DONE_E"; return null; }
                else { state = "NORMAL"; return "[DON" + c; }

            case "SAW_DONE_E":
                if (c == ']') { state = "NORMAL"; return null; }
                else { state = "NORMAL"; return "[DONE" + c; }

            case "SAW_E":
                if (c == 'v') { state = "SAW_EV"; return null; }
                else { state = "NORMAL"; return "e" + c; }

            case "SAW_EV":
                if (c == 'e') { state = "SAW_EVE"; return null; }
                else { state = "NORMAL"; return "ev" + c; }

            case "SAW_EVE":
                if (c == 'n') { state = "SAW_EVEN"; return null; }
                else { state = "NORMAL"; return "eve" + c; }

            case "SAW_EVEN":
                if (c == 't') { state = "SAW_EVENT"; return null; }
                else { state = "NORMAL"; return "even" + c; }

            case "SAW_EVENT":
                if (c == ':') { state = "NORMAL"; return null; }
                else { state = "NORMAL"; return "event" + c; }

            default:
                state = "NORMAL";
                return String.valueOf(c);
        }
    }

    public String flush() {
        switch (state) {
            case "SAW_D":        state = "NORMAL"; return "d";
            case "SAW_DA":       state = "NORMAL"; return "da";
            case "SAW_DAT":      state = "NORMAL"; return "dat";
            case "SAW_DATA":     state = "NORMAL"; return "data";
            case "SAW_BRACKET":  state = "NORMAL"; return "[";
            case "SAW_DONE_D":   state = "NORMAL"; return "[D";
            case "SAW_DONE_O":   state = "NORMAL"; return "[DO";
            case "SAW_DONE_N":   state = "NORMAL"; return "[DON";
            case "SAW_DONE_E":   state = "NORMAL"; return "[DONE";
            case "SAW_E":        state = "NORMAL"; return "e";
            case "SAW_EV":       state = "NORMAL"; return "ev";
            case "SAW_EVE":      state = "NORMAL"; return "eve";
            case "SAW_EVEN":     state = "NORMAL"; return "even";
            case "SAW_EVENT":    state = "NORMAL"; return "event";
            default:             state = "NORMAL"; return "";
        }
    }
}