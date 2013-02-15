package org.jboss.maven.plugins.qschecker;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = QSChecker.class)
public class AChecker implements QSChecker {

    public void check() {
        System.out.println("Funcionou");

    }

}
