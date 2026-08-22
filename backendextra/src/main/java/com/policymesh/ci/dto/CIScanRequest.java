package com.policymesh.ci.dto;

public record CIScanRequest(String commitHash, String branch) {
}
