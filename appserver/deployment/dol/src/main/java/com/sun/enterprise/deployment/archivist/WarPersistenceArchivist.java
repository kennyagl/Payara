/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.deployment.archivist;

import org.glassfish.deployment.common.RootDeploymentDescriptor;
import com.sun.enterprise.deployment.io.DescriptorConstants;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.Archive;
import org.xml.sax.SAXParseException;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.Map;
import java.util.HashMap;

@Service
@ExtensionsArchivistFor("war-jpa")
public class WarPersistenceArchivist extends PersistenceArchivist {

    @Override
    public boolean supportsModuleType(ArchiveType moduleType) {
        return moduleType != null && moduleType.equals(DOLUtils.warType());
    }

    @Override
    public Object open(Archivist main, ReadableArchive warArchive, RootDeploymentDescriptor descriptor) throws IOException, SAXParseException {
        final String CLASSES_DIR = "WEB-INF/classes/";

        if(logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, "WarPersistenceArchivist",
                    "readPersistenceDeploymentDescriptors", "archive = {0}",
                    warArchive.getURI());
        }
        Map<String, ReadableArchive> probablePersitenceArchives =  new HashMap<String, ReadableArchive>();
        try {
            SubArchivePURootScanner warLibScanner = new SubArchivePURootScanner() {
                String getPathOfSubArchiveToScan() {
                    return "WEB-INF/lib";
                }
            };
            probablePersitenceArchives = getProbablePersistenceRoots(warArchive, warLibScanner);

            final String pathOfPersistenceXMLInsideClassesDir = CLASSES_DIR+ DescriptorConstants.PERSISTENCE_DD_ENTRY;
            InputStream is = warArchive.getEntry(pathOfPersistenceXMLInsideClassesDir);
            if (is!=null) {
                is.close();
                probablePersitenceArchives.put(CLASSES_DIR, warArchive.getSubArchive(CLASSES_DIR));
            }

            for(Map.Entry<String, ReadableArchive> pathToArchiveEntry : probablePersitenceArchives.entrySet()) {
                readPersistenceDeploymentDescriptor(main, pathToArchiveEntry.getValue(), pathToArchiveEntry.getKey(), descriptor);
            }
        } finally {
            for(Archive probablePersitenceArchive : probablePersitenceArchives.values()) {
                probablePersitenceArchive.close();
            }
        }
        return null;
    }
}
