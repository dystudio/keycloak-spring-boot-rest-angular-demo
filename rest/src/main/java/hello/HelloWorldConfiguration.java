package hello;

import java.io.InputStream;

import org.apache.catalina.Context;
import org.apache.tomcat.util.descriptor.web.LoginConfig;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.keycloak.adapters.HttpFacade;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.tomcat.KeycloakAuthenticatorValve;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HelloWorldConfiguration {

	public static void main(String[] args) {
		SpringApplication.run(HelloWorldConfiguration.class, args);
	}

	@Bean
    public EmbeddedServletContainerCustomizer getKeycloakContainerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer) {
                if (configurableEmbeddedServletContainer instanceof TomcatEmbeddedServletContainerFactory) {
                    TomcatEmbeddedServletContainerFactory container = (TomcatEmbeddedServletContainerFactory) configurableEmbeddedServletContainer;

                    container.addContextValves(new KeycloakAuthenticatorValve());

                    container.addContextCustomizers(getKeycloakContextCustomizer());
                }
            }
        };
    }

    @Bean
    public TomcatContextCustomizer getKeycloakContextCustomizer() {
        return new TomcatContextCustomizer() {
            @Override
            public void customize(Context context) {
                LoginConfig loginConfig = new LoginConfig();
                loginConfig.setAuthMethod("KEYCLOAK");
                context.setLoginConfig(loginConfig);

                context.addSecurityRole("user");

                SecurityConstraint constraint = new SecurityConstraint();
                constraint.addAuthRole("user");

                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/hello-world");
                constraint.addCollection(collection);

                context.addConstraint(constraint);

                context.addParameter("keycloak.config.resolver", SpringBootKeycloakConfigResolver.class.getName());
            }
        };
    }

    public static class SpringBootKeycloakConfigResolver implements KeycloakConfigResolver {

        private KeycloakDeployment keycloakDeployment;

        @Override
        public KeycloakDeployment resolve(HttpFacade.Request request) {
            if (keycloakDeployment != null) {
                return keycloakDeployment;
            }

            InputStream configInputStream = getClass().getResourceAsStream("/keycloak.json");
            if (configInputStream == null) {
                keycloakDeployment = new KeycloakDeployment();
            } else {
                keycloakDeployment = KeycloakDeploymentBuilder.build(configInputStream);
            }

            return keycloakDeployment;
        }
    }
}
