package de.mhus.kt2l.aaa;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class AaaUserRepositoryService {

    @Autowired
    private List<AaaUserRepository> userRepositories;
    @Autowired
    private LoginConfiguration loginConfig;
    @Getter
    private AaaUserRepository repository;

    @PostConstruct
    public void init() {
        var urc = loginConfig.getUserRepositoryClass();
        if (urc == null) {
            if (userRepositories.size() != 1)
                throw new IllegalArgumentException("User repository not configured properly. Options: " + userRepositories.stream().map(r -> r.getClass().getCanonicalName()).toList());
            repository = userRepositories.getFirst();
        } else {
            repository = userRepositories.stream()
                    .filter(r -> r.getClass().getCanonicalName().equals(urc))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("User repository not found: " + urc));
        }
    }

}
