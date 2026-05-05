package com.flowboard.workspace.dto.request;
import lombok.Data;
@Data
public class AddMemberRequest {
    private Long userId;
    private String role = "MEMBER";
}