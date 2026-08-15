package nl.alexeyu.structmatcher.e2e;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * The consumer model behind the JSON and XML fixtures. It looks overbuilt on purpose: between
 * them these types cover each property shape core knows how to match.
 */
public final class DeploymentSnapshot {

    private Metadata metadata;

    @JacksonXmlElementWrapper(localName = "services")
    @JacksonXmlProperty(localName = "service")
    private List<Service> services;

    @JacksonXmlElementWrapper(localName = "capabilities")
    @JacksonXmlProperty(localName = "capability")
    private Set<String> capabilities;

    @JacksonXmlElementWrapper(localName = "shardPlan")
    @JacksonXmlProperty(localName = "shard")
    private int[] shardPlan;

    private Optional<Approval> approval = Optional.empty();

    private Map<String, String> labels;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<String> capabilities) {
        this.capabilities = capabilities;
    }

    public int[] getShardPlan() {
        return shardPlan;
    }

    public void setShardPlan(int[] shardPlan) {
        this.shardPlan = shardPlan;
    }

    public Optional<Approval> getApproval() {
        return approval;
    }

    public void setApproval(Optional<Approval> approval) {
        this.approval = approval;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    /**
     * Keep this component named {@code url}, matching the URLs on {@code Endpoint}, so the
     * {@code *.Url} wildcard reaches it and the spec's exact registration has to override it.
     * Rename it and the precedence test stops testing precedence.
     */
    public record Metadata(String requestId, String generatedAt, String url,
            int reportedInstances) {
    }

    public static final class Service {

        private String name;

        private Endpoint endpoint;

        @JacksonXmlElementWrapper(localName = "instances")
        @JacksonXmlProperty(localName = "instance")
        private List<Instance> instances;

        private Map<String, Metric> metrics;

        @JacksonXmlElementWrapper(localName = "regions")
        @JacksonXmlProperty(localName = "region")
        private Set<String> regions;

        @JacksonXmlElementWrapper(localName = "tags")
        @JacksonXmlProperty(localName = "tag")
        private List<String> tags;

        @JacksonXmlElementWrapper(localName = "aliases")
        @JacksonXmlProperty(localName = "alias")
        private String[] aliases;

        private String notes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Endpoint getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(Endpoint endpoint) {
            this.endpoint = endpoint;
        }

        public List<Instance> getInstances() {
            return instances;
        }

        public void setInstances(List<Instance> instances) {
            this.instances = instances;
        }

        public Map<String, Metric> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, Metric> metrics) {
            this.metrics = metrics;
        }

        public Set<String> getRegions() {
            return regions;
        }

        public void setRegions(Set<String> regions) {
            this.regions = regions;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public String[] getAliases() {
            return aliases;
        }

        public void setAliases(String[] aliases) {
            this.aliases = aliases;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    public record Endpoint(String url, int port) {
    }

    public record Instance(String id, String version, int latencyMs, boolean healthy, Mode mode) {
    }

    public record Metric(int value, Unit unit) {
    }

    public record Approval(String reviewer, String ticket) {
    }

    public enum Mode {
        ACTIVE,
        STANDBY
    }

    public enum Unit {
        MILLISECONDS,
        PERCENT
    }

}
