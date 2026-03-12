package com.liatrio.dora.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "dora.labels")
@Component
public class DoraLabelsProperties {

    private List<String> incident = List.of("incident", "outage");
    private List<String> bug = List.of("bug", "defect");
    private List<String> hotfix = List.of("hotfix", "hot-fix", "hotpatch");
    private List<String> revert = List.of("revert", "rollback");

    public List<String> getIncident() {
        return incident;
    }

    public void setIncident(List<String> incident) {
        this.incident = incident;
    }

    public List<String> getBug() {
        return bug;
    }

    public void setBug(List<String> bug) {
        this.bug = bug;
    }

    public List<String> getHotfix() {
        return hotfix;
    }

    public void setHotfix(List<String> hotfix) {
        this.hotfix = hotfix;
    }

    public List<String> getRevert() {
        return revert;
    }

    public void setRevert(List<String> revert) {
        this.revert = revert;
    }
}
