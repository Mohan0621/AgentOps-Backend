package com.agentops.DTO;

import java.time.LocalDateTime;

import com.agentops.Models.Comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorEmail;

    public static CommentResponse fromEntity(Comment comment) {
        if (comment == null) return null;
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .authorId(comment.getAuthor() != null ? comment.getAuthor().getId() : null)
                .authorEmail(comment.getAuthor() != null ? comment.getAuthor().getEmail() : null)
                .build();
    }
}

