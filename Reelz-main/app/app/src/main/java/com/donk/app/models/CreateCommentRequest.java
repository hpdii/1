package com.donk.app.models;

public class CreateCommentRequest {
    private String content;
    private Long replyToId;

    public CreateCommentRequest(String content) {
        this.content = content;
        this.replyToId = null;
    }

    public CreateCommentRequest(String content, Long replyToId) {
        this.content = content;
        this.replyToId = replyToId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Long replyToId) {
        this.replyToId = replyToId;
    }
}
