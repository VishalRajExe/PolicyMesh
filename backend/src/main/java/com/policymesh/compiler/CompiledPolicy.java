package com.policymesh.compiler;

import lombok.*;

import java.util List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompiledPolicy {
    private String policyCode;
    private String name;
    private String jurisdiction;
    private String dataClass;
    private List<String> allowedRegions;
    private List<String> deniedRegions;
}