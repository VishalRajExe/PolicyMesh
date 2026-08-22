package com.policymesh;

import com.policymesh.ci.ComplianceCli;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** The standalone gate used by GitHub Actions must exit 0/1/2 with the documented meaning. */
class ComplianceCliTest {
  @TempDir
  Path temp;

  @Test
  void compliantFlowExitsZero() throws IOException {
    writePolicy();
    writeServices();
    writeFlows("valid", """
        {"name":"valid","dataFlows":[{"source":"orders-api","destination":"payments-api","dataClasses":["PII"]}]}
        """);
    assertThat(ComplianceCli.runFiles(args())).isEqualTo(0);
    assertThat(Files.exists(temp.resolve("report.json"))).isTrue();
    assertThat(Files.readString(temp.resolve("report.json"))).contains("\"result\" : \"PASS\"");
  }

  @Test
  void violatingFlowExitsOne() throws IOException {
    writePolicy();
    writeServices();
    writeFlows("blocked", """
        {"name":"blocked","dataFlows":[{"source":"orders-api","destination":"analytics-api","dataClasses":["PII"]}]}
        """);
    assertThat(ComplianceCli.runFiles(args())).isEqualTo(1);
    assertThat(Files.readString(temp.resolve("report.json"))).contains("EU-PII-001");
  }

  @Test
  void missingInputsExitTwo() throws IOException {
    assertThat(ComplianceCli.runFiles(new String[]{})).isEqualTo(2);
    assertThat(ComplianceCli.runFiles(new String[]{
        "--policies", temp.toString(), "--services", temp.resolve("missing.json").toString(), "--dataflows", temp.resolve("missing.json").toString()
    })).isEqualTo(2);
  }

  @Test
  void invalidPolicyFileExitsTwo() throws IOException {
    Files.createDirectories(temp.resolve("policies"));
    Files.writeString(temp.resolve("policies").resolve("broken.yaml"), "policy: [not a policy");
    writeServices();
    writeFlows("any", """
        {"name":"any","dataFlows":[{"source":"orders-api","destination":"payments-api","dataClasses":["PII"]}]}
        """);
    assertThat(ComplianceCli.runFiles(args())).isEqualTo(2);
  }

  private String[] args() {
    return new String[]{
        "--policies", temp.resolve("policies").toString(),
        "--services", temp.resolve("services.json").toString(),
        "--dataflows", temp.resolve("flows.json").toString(),
        "--report", temp.resolve("report.json").toString()};
  }

  private void writePolicy() throws IOException {
    Files.createDirectories(temp.resolve("policies"));
    Files.writeString(temp.resolve("policies").resolve("eu-pii.yaml"), """
        policy:
          id: EU-PII-001
          name: EU PII Protection
          jurisdiction: EU
          dataClass: PII
          allowedRegions: [EU]
          deniedRegions: [US, CN]
        """);
  }

  private void writeServices() throws IOException {
    Files.writeString(temp.resolve("services.json"), """
        {"services":[
          {"id":"orders-api","name":"Orders API","region":"EU","environment":"production"},
          {"id":"payments-api","name":"Payments API","region":"EU","environment":"production"},
          {"id":"analytics-api","name":"Analytics API","region":"US","environment":"production"}
        ]}
        """);
  }

  private void writeFlows(String name, String content) throws IOException {
    Files.writeString(temp.resolve("flows.json"), content);
  }
}
