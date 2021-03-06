package cz.metacentrum.perun.core.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import cz.metacentrum.perun.core.api.NamespaceRules;
import cz.metacentrum.perun.core.api.exceptions.InternalErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SponsoredAccountsConfigLoader {

	private static final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
	private static final Logger log = LoggerFactory.getLogger(SponsoredAccountsConfigLoader.class);

	private Resource configurationPath;

	public void setConfigurationPath(Resource configurationPath) {
		this.configurationPath = configurationPath;
	}

	public Map<String, NamespaceRules> loadSponsoredAccountsConfig() {
		Map<String, NamespaceRules> namespacesRules = new HashMap<>();

		try {
			JsonNode rootNode = loadConfigurationFile(configurationPath);
			loadNamespacesRulesFromJsonNode(rootNode)
				.forEach(namespace -> namespacesRules.put(namespace.getNamespaceName(), namespace));

		} catch(RuntimeException e) {
			throw new InternalErrorException("Configuration file has invalid syntax. Configuration file: " +
				configurationPath.getFilename());
		}

		return namespacesRules;
	}

	private Set<NamespaceRules> loadNamespacesRulesFromJsonNode(JsonNode rootNode) {
		Set<NamespaceRules> rules = new HashSet<>();
		//Fetch all namespaces from the configuration file
		JsonNode namespacesNodes = rootNode.get("namespaces");

		// For each namespace node construct NamespaceRules and add it to the set
		Iterator<String> namespacesNames = namespacesNodes.fieldNames();
		while (namespacesNames.hasNext()) {
			String namespaceName = namespacesNames.next();
			JsonNode namespaceNode = namespacesNodes.get(namespaceName);
			JsonNode requiredAttributesNode = namespaceNode.get("required_attributes");
			JsonNode optionalAttributesNode = namespaceNode.get("optional_attributes");
			Set<String> requiredAttributes = objectMapper.convertValue(requiredAttributesNode, new TypeReference<>() {});
			Set<String> optionalAttributes = objectMapper.convertValue(optionalAttributesNode, new TypeReference<>() {});

			rules.add(new NamespaceRules(namespaceName,requiredAttributes, optionalAttributes));
		}

		return rules;
	}

	private JsonNode loadConfigurationFile(Resource resource) {

		JsonNode rootNode;
		try (InputStream is = resource.getInputStream()) {
			rootNode = objectMapper.readTree(is);
		} catch (FileNotFoundException e) {
			throw new InternalErrorException("Configuration file not found for namespaces rules. It should be in: " + resource, e);
		} catch (IOException e) {
			throw new InternalErrorException("IO exception was thrown during the processing of the file: " + resource, e);
		}

		return rootNode;
	}
}
