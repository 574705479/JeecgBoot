package com.license.server.service;

import com.license.server.entity.DockerServiceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DockerComposeService {

    @SuppressWarnings("unchecked")
    public List<DockerServiceConfig> parseToServices(Long serverId, String composeContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(composeContent);
        if (root == null || !(root.get("services") instanceof Map<?, ?> services)) {
            return Collections.emptyList();
        }
        Map<String, Map<String, Object>> topLevelNetworks = parseTopLevelNetworks(root.get("networks"));
        List<DockerServiceConfig> result = new ArrayList<>();
        for (Map.Entry<?, ?> entry : services.entrySet()) {
            String serviceName = String.valueOf(entry.getKey());
            if (!(entry.getValue() instanceof Map<?, ?> serviceMapAny)) {
                continue;
            }
            Map<String, Object> serviceMap = (Map<String, Object>) serviceMapAny;
            DockerServiceConfig config = new DockerServiceConfig();
            config.setServerId(serverId);
            config.setServiceName(serviceName);
            config.setContainerName(defaultIfBlank(asString(serviceMap.get("container_name")), serviceName));
            config.setHostname(asString(serviceMap.get("hostname")));
            config.setRestartPolicy(defaultIfBlank(asString(serviceMap.get("restart")), "no"));
            config.setCommand(convertCommand(serviceMap.get("command")));
            config.setDependsOn(asStringList(serviceMap.get("depends_on")));
            config.setVolumes(asStringList(serviceMap.get("volumes")));
            config.setPorts(asStringList(serviceMap.get("ports")));
            config.setEnvironment(parseEnvironment(serviceMap.get("environment"), config));
            config.setNetworks(parseNetworks(serviceMap.get("networks"), topLevelNetworks));
            config.setExtraConfig(parseExtraConfig(serviceMap));

            String image = asString(serviceMap.get("image"));
            if (image == null || image.isBlank()) {
                config.setImageName("unknown");
                config.setCurrentVersion("latest");
                config.setTargetVersion("latest");
                result.add(config);
                continue;
            }
            if (image != null && image.contains(":")) {
                int i = image.lastIndexOf(":");
                config.setImageName(image.substring(0, i));
                config.setCurrentVersion(image.substring(i + 1));
            } else {
                config.setImageName(image);
                config.setCurrentVersion("latest");
            }
            config.setTargetVersion(config.getCurrentVersion());
            result.add(config);
        }
        return result;
    }

    public String extractVersion(String composeContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> root = yaml.load(composeContent);
        Object version = root == null ? null : root.get("version");
        return version == null ? "3" : String.valueOf(version);
    }

    public String exportComposeContent(List<DockerServiceConfig> services, String version) {
        StringBuilder sb = new StringBuilder();
        if (version != null && !version.isBlank()) {
            sb.append("version: '").append(version).append("'\n");
        }
        sb.append("services:\n");
        for (DockerServiceConfig service : services) {
            sb.append("  ").append(service.getServiceName()).append(":\n");
            sb.append("    image: ").append(service.getImageName());
            if (service.getTargetVersion() != null && !service.getTargetVersion().isBlank()) {
                sb.append(":").append(service.getTargetVersion());
            } else if (service.getCurrentVersion() != null && !service.getCurrentVersion().isBlank()) {
                sb.append(":").append(service.getCurrentVersion());
            }
            sb.append("\n");

            if (service.getContainerName() != null && !service.getContainerName().isBlank()) {
                sb.append("    container_name: ").append(service.getContainerName()).append("\n");
            }
            if (service.getHostname() != null && !service.getHostname().isBlank()) {
                sb.append("    hostname: ").append(service.getHostname()).append("\n");
            }

            appendList(sb, "ports", service.getPorts(), 4);
            appendEnvironment(sb, service);
            appendList(sb, "volumes", service.getVolumes(), 4);
            appendList(sb, "depends_on", service.getDependsOn(), 4);
            if (service.getRestartPolicy() != null && !service.getRestartPolicy().isBlank()) {
                sb.append("    restart: ").append(service.getRestartPolicy()).append("\n");
            }
            if (service.getCommand() != null && !service.getCommand().isBlank()) {
                sb.append("    command: ").append(service.getCommand()).append("\n");
            }
            appendServiceNetworks(sb, service.getNetworks());
            appendExtraConfig(sb, service.getExtraConfig());
        }

        Map<String, Map<String, Object>> topNetworks = buildTopLevelNetworks(services);
        if (!topNetworks.isEmpty()) {
            sb.append("networks:\n");
            for (Map.Entry<String, Map<String, Object>> entry : topNetworks.entrySet()) {
                String networkName = entry.getKey();
                Map<String, Object> config = entry.getValue();
                sb.append("  ").append(networkName).append(":\n");
                if (config.get("driver") != null) {
                    sb.append("    driver: ").append(config.get("driver")).append("\n");
                }
                if (config.get("name") != null) {
                    sb.append("    name: ").append(config.get("name")).append("\n");
                }
                if (Boolean.TRUE.equals(config.get("external"))) {
                    sb.append("    external: true\n");
                }
                Object subnet = config.get("subnet");
                Object gateway = config.get("gateway");
                if (subnet != null || gateway != null) {
                    sb.append("    ipam:\n");
                    sb.append("      config:\n");
                    sb.append("        -");
                    if (subnet != null) {
                        sb.append(" subnet: ").append(subnet);
                    }
                    if (gateway != null) {
                        sb.append(subnet != null ? "\n          gateway: " : " gateway: ").append(gateway);
                    }
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    public String exportComposeContent(List<DockerServiceConfig> services) {
        return exportComposeContent(services, "3");
    }

    private Map<String, Object> parseExtraConfig(Map<String, Object> serviceMap) {
        Map<String, Object> extra = new HashMap<>();
        if (serviceMap.containsKey("logging")) {
            extra.put("logging", serviceMap.get("logging"));
        }
        if (serviceMap.containsKey("labels")) {
            extra.put("labels", serviceMap.get("labels"));
        }
        if (serviceMap.containsKey("healthcheck")) {
            extra.put("healthcheck", serviceMap.get("healthcheck"));
        }
        return extra.isEmpty() ? null : extra;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> parseTopLevelNetworks(Object networks) {
        if (!(networks instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String networkName = String.valueOf(entry.getKey());
            if (networkName.isBlank()) {
                continue;
            }
            Map<String, Object> cfg = new LinkedHashMap<>();
            if (entry.getValue() instanceof Map<?, ?> valueMap) {
                Object driver = valueMap.get("driver");
                Object name = valueMap.get("name");
                Object external = valueMap.get("external");
                if (driver != null) {
                    cfg.put("driver", String.valueOf(driver));
                }
                if (name != null) {
                    cfg.put("name", String.valueOf(name));
                }
                if (external != null) {
                    cfg.put("external", Boolean.parseBoolean(String.valueOf(external)));
                }
                Object ipam = valueMap.get("ipam");
                if (ipam instanceof Map<?, ?> ipamMap) {
                    Object config = ipamMap.get("config");
                    if (config instanceof List<?> configList && !configList.isEmpty() && configList.get(0) instanceof Map<?, ?> firstCfg) {
                        if (firstCfg.get("subnet") != null) {
                            cfg.put("subnet", String.valueOf(firstCfg.get("subnet")));
                        }
                        if (firstCfg.get("gateway") != null) {
                            cfg.put("gateway", String.valueOf(firstCfg.get("gateway")));
                        }
                    }
                }
            }
            result.put(networkName, cfg);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseNetworks(Object networks, Map<String, Map<String, Object>> topLevelNetworks) {
        if (networks == null) {
            return null;
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        if (networks instanceof List<?> list) {
            for (Object item : list) {
                if (item == null) {
                    continue;
                }
                String networkName = String.valueOf(item).trim();
                if (networkName.isBlank()) {
                    continue;
                }
                Map<String, Object> cfg = new LinkedHashMap<>();
                Map<String, Object> global = topLevelNetworks.get(networkName);
                if (global != null) {
                    cfg.putAll(global);
                }
                merged.put(networkName, cfg);
            }
            return merged.isEmpty() ? null : merged;
        }
        if (networks instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String networkName = String.valueOf(entry.getKey());
                if (networkName.isBlank()) {
                    continue;
                }
                Map<String, Object> cfg = new LinkedHashMap<>();
                if (entry.getValue() instanceof Map<?, ?> valMap) {
                    for (Map.Entry<?, ?> valEntry : valMap.entrySet()) {
                        if (valEntry.getKey() != null) {
                            cfg.put(String.valueOf(valEntry.getKey()), valEntry.getValue());
                        }
                    }
                }
                Map<String, Object> global = topLevelNetworks.get(networkName);
                if (global != null) {
                    for (Map.Entry<String, Object> globalEntry : global.entrySet()) {
                        cfg.putIfAbsent(globalEntry.getKey(), globalEntry.getValue());
                    }
                }
                merged.put(networkName, cfg);
            }
            return merged.isEmpty() ? null : merged;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseEnvironment(Object env, DockerServiceConfig config) {
        if (env == null) {
            return null;
        }
        if (env instanceof List<?> list) {
            config.setUseParamsMode(0);
            return list.stream().map(String::valueOf).toList();
        }
        if (env instanceof Map<?, ?> map) {
            // PARAMS 模式特判
            if (map.containsKey("PARAMS")) {
                config.setUseParamsMode(1);
                String params = String.valueOf(map.get("PARAMS"));
                List<String> envList = new ArrayList<>();
            String normalized = params.replaceAll("\\s*\\n\\s*", " ").trim();
            for (String line : normalized.split("\\s+")) {
                    String item = line.trim();
                    if (item.startsWith("--") && item.contains("=")) {
                        String noPrefix = item.substring(2);
                        envList.add(noPrefix);
                    }
                }
                return envList;
            }
            config.setUseParamsMode(0);
            List<String> envList = new ArrayList<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                envList.add(String.valueOf(e.getKey()) + "=" + String.valueOf(e.getValue()));
            }
            return envList;
        }
        return null;
    }

    private List<String> asStringList(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String convertCommand(Object command) {
        if (command == null) {
            return null;
        }
        if (command instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(" "));
        }
        return String.valueOf(command);
    }

    private void appendEnvironment(StringBuilder sb, DockerServiceConfig service) {
        List<String> env = service.getEnvironment();
        if (env == null || env.isEmpty()) {
            return;
        }
        sb.append("    environment:\n");
        if (Objects.equals(service.getUseParamsMode(), 1)) {
            sb.append("      PARAMS: >\n");
            for (String item : env) {
                if (item == null || item.isBlank()) {
                    continue;
                }
                sb.append("        --").append(item.trim()).append("\n");
            }
            return;
        }
        for (String item : env) {
            sb.append("      - ").append(item).append("\n");
        }
    }

    @SuppressWarnings("unchecked")
    private void appendServiceNetworks(StringBuilder sb, Map<String, Object> networks) {
        if (networks == null || networks.isEmpty()) {
            return;
        }
        boolean hasServiceSpecific = false;
        for (Object value : networks.values()) {
            if (!(value instanceof Map<?, ?> cfgMap)) {
                continue;
            }
            if (cfgMap.get("ipv4_address") != null || cfgMap.get("ipv6_address") != null || cfgMap.get("aliases") != null) {
                hasServiceSpecific = true;
                break;
            }
        }

        sb.append("    networks:\n");
        if (!hasServiceSpecific) {
            for (String key : networks.keySet()) {
                sb.append("      - ").append(key).append("\n");
            }
            return;
        }
        for (Map.Entry<String, Object> entry : networks.entrySet()) {
            sb.append("      ").append(entry.getKey()).append(":\n");
            if (entry.getValue() instanceof Map<?, ?> cfgMap) {
                Object ipv4 = cfgMap.get("ipv4_address");
                Object ipv6 = cfgMap.get("ipv6_address");
                Object aliases = cfgMap.get("aliases");
                if (ipv4 != null) {
                    sb.append("        ipv4_address: ").append(ipv4).append("\n");
                }
                if (ipv6 != null) {
                    sb.append("        ipv6_address: ").append(ipv6).append("\n");
                }
                if (aliases instanceof List<?> aliasList && !aliasList.isEmpty()) {
                    sb.append("        aliases:\n");
                    for (Object alias : aliasList) {
                        sb.append("          - ").append(alias).append("\n");
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void appendExtraConfig(StringBuilder sb, Map<String, Object> extraConfig) {
        if (extraConfig == null || extraConfig.isEmpty()) {
            return;
        }
        Object logging = extraConfig.get("logging");
        if (logging instanceof Map<?, ?> loggingMap) {
            sb.append("    logging:\n");
            if (loggingMap.get("driver") != null) {
                sb.append("      driver: \"").append(loggingMap.get("driver")).append("\"\n");
            }
            Object options = loggingMap.get("options");
            if (options instanceof Map<?, ?> optionMap && !optionMap.isEmpty()) {
                sb.append("      options:\n");
                for (Map.Entry<?, ?> option : optionMap.entrySet()) {
                    sb.append("        ").append(option.getKey()).append(": \"").append(option.getValue()).append("\"\n");
                }
            }
        }

        Object labels = extraConfig.get("labels");
        if (labels instanceof Map<?, ?> labelsMap && !labelsMap.isEmpty()) {
            sb.append("    labels:\n");
            for (Map.Entry<?, ?> label : labelsMap.entrySet()) {
                sb.append("      ").append(label.getKey()).append(": \"").append(label.getValue()).append("\"\n");
            }
        }

        Object healthcheck = extraConfig.get("healthcheck");
        if (healthcheck instanceof Map<?, ?> healthMap && !healthMap.isEmpty()) {
            sb.append("    healthcheck:\n");
            if (healthMap.get("test") instanceof List<?> testList && !testList.isEmpty()) {
                sb.append("      test: [");
                for (int i = 0; i < testList.size(); i++) {
                    Object item = testList.get(i);
                    sb.append("\"").append(item).append("\"");
                    if (i < testList.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]\n");
            } else if (healthMap.get("test") != null) {
                sb.append("      test: ").append(healthMap.get("test")).append("\n");
            }
            if (healthMap.get("interval") != null) {
                sb.append("      interval: ").append(healthMap.get("interval")).append("\n");
            }
            if (healthMap.get("timeout") != null) {
                sb.append("      timeout: ").append(healthMap.get("timeout")).append("\n");
            }
            if (healthMap.get("retries") != null) {
                sb.append("      retries: ").append(healthMap.get("retries")).append("\n");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> buildTopLevelNetworks(List<DockerServiceConfig> services) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (DockerServiceConfig service : services) {
            Map<String, Object> networks = service.getNetworks();
            if (networks == null || networks.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Object> networkEntry : networks.entrySet()) {
                String networkName = networkEntry.getKey();
                if (networkName == null || networkName.isBlank()) {
                    continue;
                }
                Map<String, Object> top = result.computeIfAbsent(networkName, k -> new LinkedHashMap<>());
                if (!(networkEntry.getValue() instanceof Map<?, ?> cfgMap)) {
                    continue;
                }
                if (cfgMap.get("driver") != null) {
                    top.putIfAbsent("driver", cfgMap.get("driver"));
                }
                if (cfgMap.get("name") != null) {
                    top.putIfAbsent("name", cfgMap.get("name"));
                }
                if (cfgMap.get("external") != null) {
                    top.putIfAbsent("external", cfgMap.get("external"));
                }
                if (cfgMap.get("subnet") != null) {
                    top.putIfAbsent("subnet", cfgMap.get("subnet"));
                }
                if (cfgMap.get("gateway") != null) {
                    top.putIfAbsent("gateway", cfgMap.get("gateway"));
                }
            }
        }
        for (Map<String, Object> cfg : result.values()) {
            cfg.putIfAbsent("driver", "bridge");
        }
        return result;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void appendList(StringBuilder sb, String key, List<String> values, int indent) {
        if (values == null || values.isEmpty()) {
            return;
        }
        String spaces = " ".repeat(Math.max(0, indent));
        sb.append(spaces).append(key).append(":\n");
        for (String value : values) {
            sb.append(spaces).append("- ").append(value).append("\n");
        }
    }
}
