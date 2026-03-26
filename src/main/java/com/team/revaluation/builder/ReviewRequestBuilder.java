package com.team.revaluation.builder;

import com.team.revaluation.model.AnswerScript;
import com.team.revaluation.model.ReviewRequest;
import com.team.revaluation.model.Student;

public class ReviewRequestBuilder {
    private ReviewRequest request;

    public ReviewRequestBuilder() {
        this.request = new ReviewRequest();
    }

    public ReviewRequestBuilder withStudent(Student student) {
        request.setStudent(student);
        return this;
    }

    public ReviewRequestBuilder withAnswerScript(AnswerScript script) {
        request.setAnswerScript(script);
        return this;
    }

    public ReviewRequestBuilder withReviewFee(Float fee) {
        request.setReviewFee(fee);
        return this;
    }

    public ReviewRequestBuilder withReviewStatus(String status) {
        request.setReviewStatus(status);
        return this;
    }

    public ReviewRequest build() {
        return request;
    }
}