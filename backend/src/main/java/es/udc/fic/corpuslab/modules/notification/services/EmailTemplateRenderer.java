package es.udc.fic.corpuslab.modules.notification.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailTemplateRenderer {

    private static final String TEMPLATE_ROOT = "email/";
    private static final String LAYOUT_TEMPLATE = "layout.html";

    private final ConcurrentHashMap<String, String> templateCache = new ConcurrentHashMap<>();

    public String render(String templateName, Map<String, ?> variables) {
        String content = replaceVariables(loadTemplate(templateName), variables);
        return loadTemplate(LAYOUT_TEMPLATE)
                .replace("{{content}}", content)
                .replace("{{year}}", value(Year.now()));
    }

    private String replaceVariables(String template, Map<String, ?> variables) {
        String result = template;
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", value(entry.getValue()));
        }
        return result;
    }

    private String value(Object value) {
        return HtmlUtils.htmlEscape(value == null ? "" : String.valueOf(value));
    }

    private String loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, this::readTemplate);
    }

    private String readTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_ROOT + templateName);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read email template " + templateName, ex);
        }
    }
}
