/*
 * Copyright 2011-15 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.webui.channelconfigurator.conf;

import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.webui.channelconfigurator.*;
import org.openmuc.framework.webui.spi.ResourceLoader;

import javax.servlet.http.HttpServletRequest;
import java.text.Collator;
import java.util.Collections;
import java.util.List;

/**
 * @author Frederic Robra
 */
public class OptionsConfigurator extends Configurator {

    /**
     * @param config
     */
    public OptionsConfigurator(Config config) {
        super(config);
    }

    @Override
    public Content getContent(String localPath, HttpServletRequest request) throws ProcessRequestException, IdCollisionException {

        Content content = null;
        ResourceLoader loader = config.getLoader();

        if (localPath.endsWith("/restore")) {
            String backup = Util.parseString(request, "backup");
            config.restoreBackup(backup);
            content = Content.createMessage("Message", "Backup " + backup + " restored");
        } else if (localPath.endsWith("/open")) {
            String backup = Util.parseString(request, "backup");
            content = Content.createAjax(config.openBackupFile(backup));
        } else if (localPath.endsWith("/delete")) {
            String backup = Util.parseString(request, "backup");
            config.deleteBackup(backup);
            content = Content.createMessage("Message", "Backup " + backup + " deleted");
        } else {
            content = new Content();
            content.setTitle("Options");
            content.setHtml(loader.getResourceAsString("options/options.html"));
            content.setMenuItem(MenuItem.OPTIONS);
            List<String> backups = config.getBackups();
            Collections.sort(backups, Collator.getInstance());
            content.addToContext("backups", backups);
        }

        return content;
    }

}
