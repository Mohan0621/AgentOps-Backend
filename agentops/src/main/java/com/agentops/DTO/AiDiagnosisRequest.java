package com.agentops.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiDiagnosisRequest {
    private String title;
    private String description;
    private String errorLogs;
}

