package com.sip.wbx;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sert l'interface Angular (dossier static/) et renvoie index.html pour les routes
 * du front (ex. /login, /produits) afin que l'actualisation de la page fonctionne.
 * Les URL /api/** et les fichiers manquants (.js, .css...) gardent leur 404.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        if (path.startsWith("api/")) {
                            return null;
                        }
                        Resource file = location.createRelative(path);
                        if (!path.isEmpty() && file.exists() && file.isReadable()) {
                            return file;
                        }
                        if (path.contains(".")) {
                            return null;
                        }
                        Resource index = location.createRelative("index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
