package com.policymesh.compiler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Raw, loosely-typed representation of a parsed policy YAML document,
 * before validation/compilation. Never exposed outside the compiler
 * package.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParsedPolicyDocument {
    private String id;
    private String name;
    private String jurisdiction;
    private String dataClass;
    private List<String> allowedRegions;
    private List<String> deniedRegions;
}
