package ro.ecoregistru.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ro.ecoregistru.audit.AuditInterceptor;

/**
 * Leagă interceptorul de audit de fabrica de sesiuni.
 *
 * <p>Un singur exemplar, pentru toate sesiunile — de aceea {@link AuditInterceptor} nu ţine nicio
 * stare proprie şi pune tot ce prinde într-un {@code ThreadLocal}.
 */
@Configuration
public class AuditConfig {

    @Bean
    public HibernatePropertiesCustomizer auditInterceptorCustomizer(AuditInterceptor interceptor) {
        return properties -> properties.put(AvailableSettings.INTERCEPTOR, interceptor);
    }
}
