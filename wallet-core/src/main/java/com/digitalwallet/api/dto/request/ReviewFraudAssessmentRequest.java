package com.digitalwallet.api.dto.request;

import com.digitalwallet.domain.enums.FraudReviewAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewFraudAssessmentRequest {

    @NotNull(message = "Review action is required")
    private FraudReviewAction action;

    @Size(max = 1000, message = "Review note must not exceed 1000 characters")
    private String note;

    public FraudReviewAction getAction() {
        return action;
    }

    public void setAction(FraudReviewAction action) {
        this.action = action;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}