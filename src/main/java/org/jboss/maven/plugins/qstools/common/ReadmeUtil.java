/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.maven.plugins.qstools.common;

import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jboss.maven.plugins.qstools.config.ConfigurationProvider;

/**
 * @author rafaelbenevides
 * 
 */
@Component(role = ReadmeUtil.class)
public class ReadmeUtil {

    @Requirement
    private ConfigurationProvider configurationProvider;

    /**
     * Create a regex pattern using all metadata clauses
     * 
     * Format: metadata1:|metadata2:|metadata3:
     * 
     * @param string
     * 
     * @param string
     */
    public String setupRegexPattern(String groupid, String folderName) {
        Map<String, String> metadatas = configurationProvider.getQuickstartsRules(groupid).getReadmeMetadatas();
        StringBuilder sb = new StringBuilder();
        for (String metadata : metadatas.keySet()) {
            sb.append(metadata + "|");
        }
        if (folderName == null) {
            sb.deleteCharAt(sb.lastIndexOf("|"));
        }
        else {
            sb.append(folderName);
        }
        return sb.toString();
    }
}
