package ro.ecoregistru.service.export;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Versiunea programului, pentru documentele tipărite (OMFP 2634/2015 anexa 1 pct. 58 lit. k)): data build-ului, din
 * {@code build-info.properties}. Fără el (rulare din IDE, unele teste) — „dev”.
 */
@Component
public class AppVersion {

    private final String value;

    public AppVersion(ObjectProvider<BuildProperties> build) {
        BuildProperties props = build.getIfAvailable();
        this.value = props == null || props.getTime() == null ? "dev"
                : DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneId.of("Europe/Bucharest")).format(props.getTime());
    }

    public String value() {
        return value;
    }
}
