package com.oracle.osd.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Action event DTO for ActionUpdatesTopic messages.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionEvent extends BaseEvent {

    @JsonProperty("ActionId")
    private String actionId;

    @JsonProperty("Notes_Old")
    private String notesOld;

    // Constructors
    public ActionEvent() {
    }

    public ActionEvent(String actionId, String action, String notes, String notesOld, String timestamp) {
        super(action, notes, timestamp);
        this.actionId = actionId;
        this.notesOld = notesOld;
    }

    // Getters and setters
    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getNotesOld() {
        return notesOld;
    }

    public void setNotesOld(String notesOld) {
        this.notesOld = notesOld;
    }

    @Override
    public String toString() {
        return String.format("ActionEvent{actionId='%s', %s, notesOld='%s'}",
                actionId, super.toString(), notesOld);
    }
}
